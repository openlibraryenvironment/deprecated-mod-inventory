package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.folio.inventory.common.RamlModuleBuilderContext;
import org.folio.inventory.domain.ingest.IngestMessages;
import org.folio.inventory.parsing.ModsParser;
import org.folio.inventory.parsing.UTF8LiteralCharacterEncoding;
import org.folio.inventory.resources.ingest.IngestJob;
import org.folio.inventory.resources.ingest.IngestRecordConverter;
import org.folio.inventory.storage.memory.InMemoryIngestJobCollection;
import org.folio.inventory.support.CollectionResourceClient;
import org.folio.inventory.support.JsonArrayHelper;
import org.folio.inventory.support.http.client.OkapiHttpClient;
import org.folio.rest.jaxrs.model.*;
import org.folio.rest.jaxrs.resource.InventoryResource;
import org.folio.rest.tools.utils.OutStream;

import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InventoryAPI implements InventoryResource {

  private final String OKAPI_URL = "X-Okapi-Url";
  private final String INSTANCE_STORAGE_PATH = "/instance-storage/instances";
  private final String ITEM_STORAGE_PATH = "/item-storage/items";
  private final String MATERIAL_TYPE_PATH = "/material-types";
  private final String LOAN_TYPE_PATH = "/loan-types";

  @Override
  public void deleteInventoryItems(
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    CollectionResourceClient itemsClient = new CollectionResourceClient(client,
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), ITEM_STORAGE_PATH));

    itemsClient.delete(response -> {
      asyncResultHandler.handle(Future.succeededFuture(
        DeleteInventoryItemsResponse.withNoContent()));
    });
  }

  @Override
  public void getInventoryItems(
    int offset,
    int limit,
    String query,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    URL itemStorageUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), ITEM_STORAGE_PATH);
    URL materialTypesUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), MATERIAL_TYPE_PATH);
    URL loanTypesUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), LOAN_TYPE_PATH);

    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    CollectionResourceClient itemsClient = new CollectionResourceClient(client, itemStorageUrl);

    String fullQuery = String.format("limit=%s&offset=%s", limit, offset);

    if(StringUtils.isNotBlank(query)) {
      fullQuery = fullQuery + "&query=" + URLEncoder.encode(query, "UTF-8");
    }

    itemsClient.getMany(fullQuery, response -> {
      if (response.getStatusCode() == 200) {

        JsonObject body = response.getJson();

        List<CompositeItem> items = JsonArrayHelper.toList(body.getJsonArray("items"))
          .stream()
          .map(it -> new CompositeItem().withItem(mapToItem(it)))
          .collect(Collectors.toList());

        CollectionResourceClient materialTypesClient = new CollectionResourceClient(client, materialTypesUrl);
        CollectionResourceClient loanTypesClient = new CollectionResourceClient(client, loanTypesUrl);

        ArrayList<CompletableFuture<org.folio.inventory.support.http.client.Response>> allMaterialTypeFutures = new ArrayList<>();
        ArrayList<CompletableFuture<org.folio.inventory.support.http.client.Response>> allLoanTypeFutures = new ArrayList<>();
        ArrayList<CompletableFuture<org.folio.inventory.support.http.client.Response>> allFutures = new ArrayList<>();

        List<String> materialTypeIds = items.stream()
          .map(it -> it.getItem().getMaterialTypeId())
          .filter(it -> it != null)
          .distinct()
          .collect(Collectors.toList());

          materialTypeIds.stream().forEach(id -> {
            CompletableFuture<org.folio.inventory.support.http.client.Response> newFuture = new CompletableFuture<>();

            allFutures.add(newFuture);
            allMaterialTypeFutures.add(newFuture);

            materialTypesClient.get(id, newFuture::complete);
          });

          Stream.concat(
            items.stream().map(it -> it.getItem().getPermanentLoanTypeId()),
            items.stream().map(it -> it.getItem().getTemporaryLoanTypeId()))
          .filter(it -> it != null)
          .distinct()
          .forEach(id -> {
          CompletableFuture<org.folio.inventory.support.http.client.Response> newFuture = new CompletableFuture<>();

          allFutures.add(newFuture);
          allLoanTypeFutures.add(newFuture);

          loanTypesClient.get(id, newFuture::complete);
        });

        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(
          allFutures.toArray(new CompletableFuture[allFutures.size()]));

        allDoneFuture.thenAccept(v -> {
          Map<String, JsonObject> foundMaterialTypes = allMaterialTypeFutures.stream()
            .map(CompletableFuture::join)
            .filter(it -> it.getStatusCode() == 200)
            .map(it -> it.getJson())
            .collect(Collectors.toMap(it -> it.getString("id"), it -> it));

          Map<String, JsonObject> foundLoanTypes = allLoanTypeFutures.stream()
            .map(CompletableFuture::join)
            .filter(it -> it.getStatusCode() == 200)
            .map(it -> it.getJson())
            .collect(Collectors.toMap(it -> it.getString("id"), it -> it));

          items.stream().forEach(item -> {
            JsonObject foundMaterialType = foundMaterialTypes.getOrDefault(
              item.getItem().getMaterialTypeId(), null);

            if(foundMaterialType != null) {
              item.setMaterialType(new MaterialType()
                .withId(foundMaterialType.getString("id"))
                .withName(foundMaterialType.getString("name")));
            }

            JsonObject foundPermanentLoanType = foundLoanTypes.getOrDefault(
              item.getItem().getPermanentLoanTypeId(), null);

            if(foundPermanentLoanType != null) {
              item.setPermanentLoanType(new PermanentLoanType()
                .withId(foundPermanentLoanType.getString("id"))
                .withName(foundPermanentLoanType.getString("name")));
            }

            JsonObject foundTemporaryLoanType = foundLoanTypes.getOrDefault(
              item.getItem().getTemporaryLoanTypeId(), null);

            if(foundTemporaryLoanType != null) {
              item.setTemporaryLoanType(new PermanentLoanType()
                .withId(foundTemporaryLoanType.getString("id"))
                .withName(foundTemporaryLoanType.getString("name")));
            }
          });

          CompositeItems wrappedItems = new CompositeItems()
            .withCompositeItems(items)
            .withTotalRecords(body.getInteger("totalRecords"));

          asyncResultHandler.handle(Future.succeededFuture(
            GetInventoryItemsResponse.withJsonOK(wrappedItems)));
        });
      } else {
        asyncResultHandler.handle(Future.succeededFuture(
          convertResponseToJax(response)));
      }
    });
  }

  @Override
  public void postInventoryItems(
    String lang,
    CompositeItem entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    URL itemStorageUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), ITEM_STORAGE_PATH);
    URL materialTypesUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), MATERIAL_TYPE_PATH);
    URL loanTypesUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), LOAN_TYPE_PATH);

    CollectionResourceClient itemsClient = new CollectionResourceClient(client,
      itemStorageUrl);

    Item item = entity.getItem();

    if(StringUtils.isEmpty(item.getId())) {
      item.setId(UUID.randomUUID().toString());
    }

    if(StringUtils.isNotEmpty(item.getBarcode())) {
      String encodedBarcodeCql = "query=" + URLEncoder.encode(
        String.format("barcode=%s", item.getBarcode()), "UTF-8");

      itemsClient.getMany(encodedBarcodeCql, barcodeResponse -> {
        if(barcodeResponse.getStatusCode() == 200) {
          if(barcodeResponse.getJson().getInteger("totalRecords") > 0) {
            asyncResultHandler.handle(Future.succeededFuture(
              PostInventoryItemsResponse.withPlainBadRequest(
                String.format("Barcode must be unique, %s is already assigned to another item", item.getBarcode()))));
          }
          else {
            JsonObject storageItemRequest = mapToStorageRequest(entity);

            createItem(asyncResultHandler, client, materialTypesUrl, loanTypesUrl, itemsClient, storageItemRequest);
          }
        }
        else {
          asyncResultHandler.handle(Future.succeededFuture(
            PostInventoryItemsResponse.withPlainInternalServerError(
              "Error checking duplicate barcode " + barcodeResponse.getBody())));
        }
      });
    } else {
      JsonObject storageItemRequest = mapToStorageRequest(entity);

      createItem(asyncResultHandler, client, materialTypesUrl, loanTypesUrl, itemsClient, storageItemRequest);
    }
  }
  @Override
  public void getInventoryItemsByItemId(
    String itemId,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    URL itemStorageUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), ITEM_STORAGE_PATH);
    URL materialTypesUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), MATERIAL_TYPE_PATH);
    URL loanTypesUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), LOAN_TYPE_PATH);

    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    CollectionResourceClient itemsClient = new CollectionResourceClient(client,
      itemStorageUrl);

    itemsClient.get(itemId, response -> {
      if(response.getStatusCode() == 200) {
        Item mappedItem = mapToItem(response.getJson());

        CollectionResourceClient materialTypesClient = new CollectionResourceClient(client, materialTypesUrl);
        CollectionResourceClient loanTypesClient = new CollectionResourceClient(client, loanTypesUrl);

        CompletableFuture<org.folio.inventory.support.http.client.Response> materialTypeFuture = new CompletableFuture<>();
        CompletableFuture<org.folio.inventory.support.http.client.Response> permanentLoanTypeFuture = new CompletableFuture<>();
        CompletableFuture<org.folio.inventory.support.http.client.Response> temporaryLoanTypeFuture = new CompletableFuture<>();
        ArrayList<CompletableFuture<org.folio.inventory.support.http.client.Response>> allFutures = new ArrayList<>();

        if (mappedItem.getMaterialTypeId() != null) {
          allFutures.add(materialTypeFuture);

          materialTypesClient.get(mappedItem.getMaterialTypeId(),
            response1 -> materialTypeFuture.complete(response1));
        }

        if (mappedItem.getPermanentLoanTypeId() != null) {
          allFutures.add(permanentLoanTypeFuture);

          loanTypesClient.get(mappedItem.getPermanentLoanTypeId(),
            response2 -> permanentLoanTypeFuture.complete(response2));
        }

        if (mappedItem.getTemporaryLoanTypeId() != null) {
          allFutures.add(temporaryLoanTypeFuture);

          loanTypesClient.get(mappedItem.getTemporaryLoanTypeId(),
            response3 -> temporaryLoanTypeFuture.complete(response3));
        }

        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(
          allFutures.toArray(new CompletableFuture[allFutures.size()]));

        allDoneFuture.thenAccept(v -> {
          JsonObject foundMaterialType = mappedItem.getMaterialTypeId() != null && materialTypeFuture.join().getStatusCode() == 200
            ? materialTypeFuture.join().getJson()
            : null;

          JsonObject foundPermanentLoanType = mappedItem.getPermanentLoanTypeId() != null &&
            permanentLoanTypeFuture.join().getStatusCode() == 200 ?
            permanentLoanTypeFuture.join().getJson() : null;

          JsonObject foundTemporaryLoanType = mappedItem.getTemporaryLoanTypeId() != null &&
            temporaryLoanTypeFuture.join().getStatusCode() == 200 ?
            temporaryLoanTypeFuture.join().getJson() : null;

          CompositeItem compositeItem = new CompositeItem().withItem(mappedItem);

          compositeItem.setItem(mappedItem);

          if (foundMaterialType != null) {
            compositeItem.setMaterialType(
              new MaterialType().withId(foundMaterialType.getString("id"))
                .withName(foundMaterialType.getString("name")));
          }

          if (foundPermanentLoanType != null) {
            compositeItem.setPermanentLoanType(
              new PermanentLoanType().withId(foundPermanentLoanType.getString("id"))
                .withName(foundPermanentLoanType.getString("name")));
          }

          if (foundTemporaryLoanType != null) {
            compositeItem.setTemporaryLoanType(
              new PermanentLoanType().withId(foundTemporaryLoanType.getString("id"))
                .withName(foundTemporaryLoanType.getString("name")));
          }

          asyncResultHandler.handle(Future.succeededFuture(
            GetInventoryItemsByItemIdResponse.withJsonOK(compositeItem)));
        });
      }
      else {
        asyncResultHandler.handle(Future.succeededFuture(
          convertResponseToJax(response)));
      }
    });
  }

  @Override
  public void deleteInventoryItemsByItemId(
    String itemId,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    CollectionResourceClient itemStorageClient;
    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    itemStorageClient = new CollectionResourceClient(client,
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), ITEM_STORAGE_PATH));

    itemStorageClient.delete(itemId, response ->{
      asyncResultHandler.handle(Future.succeededFuture(
        convertResponseToJax(response)));
    });
  }

  @Override
  public void putInventoryItemsByItemId(
    String itemId,
    String lang,
    CompositeItem entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    CollectionResourceClient itemStorageClient = new CollectionResourceClient(client,
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), ITEM_STORAGE_PATH));

    Item item = entity.getItem();

    if(StringUtils.isNotEmpty(item.getBarcode())) {
      String encodedBarcodeCql = "query=" + URLEncoder.encode(
        String.format("barcode=%s and id<>%s", item.getBarcode(), item.getId()), "UTF-8");

      itemStorageClient.getMany(encodedBarcodeCql, barcodeResponse -> {
        if(barcodeResponse.getStatusCode() == 200) {
          if(barcodeResponse.getJson().getInteger("totalRecords") > 0) {
            asyncResultHandler.handle(Future.succeededFuture(
              PostInventoryItemsResponse.withPlainBadRequest(
                String.format("Barcode must be unique, %s is already assigned to another item", item.getBarcode()))));
          }
          else {
            JsonObject storageInstanceRequest = mapToStorageRequest(entity);

            updateItem(itemId, asyncResultHandler, itemStorageClient, storageInstanceRequest);
          }
        }
        else {
          asyncResultHandler.handle(Future.succeededFuture(
            PostInventoryItemsResponse.withPlainInternalServerError(
              "Error checking duplicate barcode " + barcodeResponse.getBody())));
        }
      });
    } else {
      JsonObject storageInstanceRequest = mapToStorageRequest(entity);

      updateItem(itemId, asyncResultHandler, itemStorageClient, storageInstanceRequest);
    }
  }

  private void updateItem(String itemId, Handler<AsyncResult<Response>> asyncResultHandler, CollectionResourceClient itemStorageClient, JsonObject storageInstanceRequest) {
    itemStorageClient.put(itemId, storageInstanceRequest, response -> {
      if(response.getStatusCode() == 204) {
        Instance mappedInstance = mapToInstance(response.getJson());

        OutStream stream = new OutStream();
        stream.setData(mappedInstance);

        asyncResultHandler.handle(Future.succeededFuture(
          PutInventoryInstancesByInstanceIdResponse.withNoContent()));
      }
      else {
        asyncResultHandler.handle(Future.succeededFuture(
          convertResponseToJax(response)));

      }
    });
  }

  @Override
  public void deleteInventoryInstances(
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    CollectionResourceClient instancesStorageClient;
    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    instancesStorageClient = new CollectionResourceClient(client,
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), INSTANCE_STORAGE_PATH));

    instancesStorageClient.delete(response ->{
      asyncResultHandler.handle(Future.succeededFuture(
        DeleteInventoryInstancesResponse.withNoContent()));
    });
  }

  @Override
  public void getInventoryInstances(
    int offset,
    int limit,
    String query,
    String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    CollectionResourceClient instancesClient = new CollectionResourceClient(client,
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), INSTANCE_STORAGE_PATH));

    String fullQuery = String.format("limit=%s&offset=%s", limit, offset);

    if(StringUtils.isNotBlank(query)) {
      fullQuery = fullQuery + "&query=" + URLEncoder.encode(query, "UTF-8");
    }

    instancesClient.getMany(fullQuery, response -> {
      JsonObject body = response.getJson();

      List<Instance> instances = JsonArrayHelper.toList(body.getJsonArray("instances"))
        .stream()
        .map(it -> mapToInstance(it))
        .collect(Collectors.toList());

      Instances wrappedInstances = new Instances()
        .withInstances(instances)
        .withTotalRecords(body.getInteger("totalRecords"));

      asyncResultHandler.handle(Future.succeededFuture(
        GetInventoryInstancesResponse.withJsonOK(wrappedInstances)));
    });
  }

  @Override
  public void postInventoryInstances(
    String lang,
    Instance entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    CollectionResourceClient instancesClient;
    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    instancesClient = new CollectionResourceClient(client,
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), INSTANCE_STORAGE_PATH));

    if(StringUtils.isEmpty(entity.getId())) {
      entity.setId(UUID.randomUUID().toString());
    }

    JsonObject storageInstanceRequest = mapToStorageRequest(entity);

    instancesClient.post(storageInstanceRequest, response -> {
      Instance mappedInstance = mapToInstance(response.getJson());

      OutStream stream = new OutStream();
      stream.setData(mappedInstance);

      asyncResultHandler.handle(Future.succeededFuture(
        PostInventoryInstancesResponse.withJsonCreated(
          "/inventory/instances/" + mappedInstance.getId().toString(),
          stream )));
    });
  }

  @Override
  public void getInventoryInstancesByInstanceId(
    String instanceId,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    CollectionResourceClient instancesClient;
    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    instancesClient = new CollectionResourceClient(client,
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), INSTANCE_STORAGE_PATH));

    instancesClient.get(instanceId, response -> {
      if(response.getStatusCode() == 200) {
        Instance mappedInstance = mapToInstance(response.getJson());

        asyncResultHandler.handle(Future.succeededFuture(
          GetInventoryInstancesByInstanceIdResponse.withJsonOK(mappedInstance)));
      }
      else {
        asyncResultHandler.handle(Future.succeededFuture(
          convertResponseToJax(response)));
      }
    });
  }

  @Override
  public void deleteInventoryInstancesByInstanceId(
    String instanceId,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    CollectionResourceClient instancesStorageClient;
    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    instancesStorageClient = new CollectionResourceClient(client,
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), INSTANCE_STORAGE_PATH));

    instancesStorageClient.delete(instanceId, response ->{
      asyncResultHandler.handle(Future.succeededFuture(
        convertResponseToJax(response)));
    });
  }

  @Override
  public void putInventoryInstancesByInstanceId(
    String instanceId,
    String lang, Instance entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    CollectionResourceClient instancesClient = new CollectionResourceClient(client,
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), INSTANCE_STORAGE_PATH));

    if(StringUtils.isEmpty(entity.getId())) {
      entity.setId(UUID.randomUUID().toString());
    }

    JsonObject storageInstanceRequest = mapToStorageRequest(entity);

    instancesClient.put(instanceId, storageInstanceRequest, response -> {
      if(response.getStatusCode() == 204) {
        asyncResultHandler.handle(Future.succeededFuture(
          PutInventoryInstancesByInstanceIdResponse.withNoContent()));
      }
      else {
        asyncResultHandler.handle(Future.succeededFuture(
          convertResponseToJax(response)));

      }
    });
  }

  @Override
  public void getInventoryInstancesContext(
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  @Override
  public void postInventoryIngestMods(
    MimeMultipart entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    if(entity.getCount() > 1) {
      asyncResultHandler.handle(Future.succeededFuture(
        PostInventoryIngestModsResponse.withPlainBadRequest("Cannot parse multiple files in a single request")));
      return;
    }

    String uploadedFileContents = IOUtils.toString(entity.getBodyPart(0).getInputStream());

    ModsParser parser = new ModsParser(new UTF8LiteralCharacterEncoding());

    Collection<JsonObject> convertedRecords = new IngestRecordConverter().toJson(
      parser.parseRecords(uploadedFileContents));

    URL materialTypesUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), MATERIAL_TYPE_PATH);
    URL loanTypesUrl = okapiBasedUrl(okapiHeaders.get(OKAPI_URL), LOAN_TYPE_PATH);

    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    CollectionResourceClient materialTypesClient = new CollectionResourceClient(client, materialTypesUrl);
    CollectionResourceClient loanTypesClient = new CollectionResourceClient(client, loanTypesUrl);

    CompletableFuture<org.folio.inventory.support.http.client.Response> materialTypesRequestCompleted = new CompletableFuture<>();
    CompletableFuture<org.folio.inventory.support.http.client.Response> loanTypesRequestCompleted = new CompletableFuture<>();

    materialTypesClient.getMany(
      "query=" + URLEncoder.encode("name=\"Book\"", "UTF-8"),
      materialTypesRequestCompleted::complete);

    loanTypesClient.getMany(
      "query=" + URLEncoder.encode("name=\"Can Circulate\"", "UTF-8"),
      loanTypesRequestCompleted::complete);

    CompletableFuture.allOf(materialTypesRequestCompleted, loanTypesRequestCompleted)
      .thenAccept(v -> {
        org.folio.inventory.support.http.client.Response materialTypeResponse = materialTypesRequestCompleted.getNow(null);
        org.folio.inventory.support.http.client.Response loanTypeResponse = loanTypesRequestCompleted.getNow(null);

        if (materialTypeResponse.getStatusCode() != 200) {
          asyncResultHandler.handle(Future.succeededFuture(
            PostInventoryIngestModsResponse.withPlainInternalServerError("Unable to retrieve material types")));
        }

        if (loanTypeResponse.getStatusCode() != 200) {
          asyncResultHandler.handle(Future.succeededFuture(
            PostInventoryIngestModsResponse.withPlainInternalServerError("Unable to retrieve loan types")));
        }

        Map<String, String> materialTypes =
          JsonArrayHelper.toList(materialTypeResponse.getJson().getJsonArray("mtypes"))
            .stream()
            .collect(Collectors.toMap(it -> it.getString("name"), it -> it.getString("id")));

        Map<String, String> loanTypes =
          JsonArrayHelper.toList(loanTypeResponse.getJson().getJsonArray("loantypes"))
            .stream()
            .collect(Collectors.toMap(it -> it.getString("name"), it -> it.getString("id")));

        String jobId = UUID.randomUUID().toString();

        IngestMessages.start(convertedRecords, materialTypes, loanTypes, jobId,
          new RamlModuleBuilderContext(okapiHeaders)).send(vertxContext.owner());

        asyncResultHandler.handle(Future.succeededFuture(
          PostInventoryIngestModsResponse.withAccepted(
            "/inventory/ingest/mods/status/" + jobId)));
      });
  }

  @Override
  public void getInventoryIngestModsStatusById(
    String id, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    InMemoryIngestJobCollection.getInstance().findById(id,
      success -> {
        IngestJob job = success.getResult();

        IngestStatus.Status status = null;

        switch(job.getState()) {

          case COMPLETED:
            status = IngestStatus.Status.COMPLETED;
            break;
          case IN_PROGRESS:
            status = IngestStatus.Status.IN_PROGRESS;
            break;
          case REQUESTED:
            status = IngestStatus.Status.REQUESTED;
            break;
          default:
            status = IngestStatus.Status.REQUESTED;
            break;
        }

        asyncResultHandler.handle(Future.succeededFuture(
          GetInventoryIngestModsStatusByIdResponse.withJsonOK(
            new IngestStatus().withStatus(status))));
      },
      failure -> {
        asyncResultHandler.handle(Future.succeededFuture(
          GetInventoryIngestModsStatusByIdResponse.withPlainInternalServerError(
            failure.getReason())));
      });
  }

  private OkapiHttpClient createHttpClient(Context context,
                                           Map<String, String> okapiHeaders)
    throws MalformedURLException {

    //TODO: Need to reinstate handling exceptions
    return new OkapiHttpClient(context.owner().createHttpClient(),
      new URL(okapiHeaders.get(OKAPI_URL)), okapiHeaders.get("X-Okapi-Tenant"),
      okapiHeaders.get("X-Okapi-Token"),
      exception -> {
        System.out.println(exception);
    });
  }

  private URL okapiBasedUrl(String okapiUrl, String path)
    throws MalformedURLException {

    URL currentRequestUrl = new URL(okapiUrl);

    return new URL(currentRequestUrl.getProtocol(), currentRequestUrl.getHost(),
      currentRequestUrl.getPort(), path);
  }

  private Item mapToItem(JsonObject storageItem) {
    Item item = new Item();

    item.setId(storageItem.getString("id"));
    item.setTitle(storageItem.getString("title"));
    item.setBarcode(storageItem.getString("barcode"));
    item.setMaterialTypeId(storageItem.getString("materialTypeId"));
    item.setPermanentLoanTypeId(storageItem.getString("permanentLoanTypeId"));

    if(storageItem.containsKey("temporaryLoanTypeId")) {
      item.setTemporaryLoanTypeId(storageItem.getString("temporaryLoanTypeId"));
    }

    if(storageItem.containsKey("instanceId")) {
      item.setInstanceId(storageItem.getString("instanceId"));
    }

    if(storageItem.containsKey("status")) {
      item.setStatus(new Status().withName(storageItem.getJsonObject("status").getString("name")));
    }

    if(storageItem.containsKey("location")) {
      item.setLocation(new Location().withName(storageItem.getJsonObject("location").getString("name")));
    }

    return item;
  }

  private Instance mapToInstance(JsonObject storageInstance) {
    Instance mappedInstance = new Instance();

    mappedInstance.setId(storageInstance.getString("id"));
    mappedInstance.setTitle(storageInstance.getString("title"));

    if(storageInstance.containsKey("identifiers")) {
      mappedInstance.setIdentifiers(
        JsonArrayHelper.toList(storageInstance.getJsonArray("identifiers"))
          .stream()
          .map(it -> {
            Identifier identifier = new Identifier();
            identifier.setNamespace(it.getString("namespace"));
            identifier.setValue(it.getString("value"));
            return identifier;
          })
          .collect(Collectors.toList()));
    }

    return mappedInstance;
  }

  private JsonObject mapToStorageRequest(Instance entity) {
    JsonObject storageInstanceRequest = new JsonObject()
      .put("id", entity.getId())
      .put("title", entity.getTitle());

    if(entity.getIdentifiers() != null && entity.getIdentifiers().size() != 0) {
      JsonArray identifiers = new JsonArray(
        entity.getIdentifiers().stream()
          .map(it -> new JsonObject()
            .put("namespace", it.getNamespace())
            .put("value", it.getValue()))
          .collect(Collectors.toList()));

      storageInstanceRequest.put("identifiers", identifiers);
    }
    return storageInstanceRequest;
  }

  private JsonObject mapToStorageRequest(CompositeItem entity) {
    Item item = entity.getItem();

    if(StringUtils.isEmpty(item.getId())) {
      item.setId(UUID.randomUUID().toString());
    }

    JsonObject storageItemRequest = new JsonObject();

    storageItemRequest.put("id", item.getId());
    storageItemRequest.put("title", item.getTitle());
    storageItemRequest.put("materialTypeId", item.getMaterialTypeId());
    storageItemRequest.put("permanentLoanTypeId", item.getPermanentLoanTypeId());

    if(item.getInstanceId() != null) {
      storageItemRequest.put("instanceId", item.getInstanceId());
    }

    if(item.getBarcode() != null) {
      storageItemRequest.put("barcode", item.getBarcode());
    }

    if(item.getBarcode() != null) {
      storageItemRequest.put("temporaryLoanTypeId", item.getTemporaryLoanTypeId());
    }

    if(item.getStatus() != null) {
      storageItemRequest.put("status", new JsonObject().put("name", item.getStatus().getName()));
    }

    if(item.getLocation() != null) {
      storageItemRequest.put("location", new JsonObject().put("name", item.getLocation().getName()));
    }

    return storageItemRequest;
  }

  private Response convertResponseToJax(org.folio.inventory.support.http.client.Response storageResponse) {
    return Response
      .status(storageResponse.getStatusCode())
      .header("Content-Type", storageResponse.getContentType())
      .entity(storageResponse.getBody())
      .build();
  }

  private void createItem(Handler<AsyncResult<Response>> asyncResultHandler, OkapiHttpClient client, URL materialTypesUrl, URL loanTypesUrl, CollectionResourceClient itemsClient, JsonObject storageItemRequest) {
    itemsClient.post(storageItemRequest, response -> {
      if (response.getStatusCode() == 201) {
        Item mappedItem = mapToItem(response.getJson());

        CollectionResourceClient materialTypesClient = new CollectionResourceClient(client, materialTypesUrl);
        CollectionResourceClient loanTypesClient = new CollectionResourceClient(client, loanTypesUrl);

        CompletableFuture<org.folio.inventory.support.http.client.Response> materialTypeFuture = new CompletableFuture<>();
        CompletableFuture<org.folio.inventory.support.http.client.Response> permanentLoanTypeFuture = new CompletableFuture<>();
        CompletableFuture<org.folio.inventory.support.http.client.Response> temporaryLoanTypeFuture = new CompletableFuture<>();
        ArrayList<CompletableFuture<org.folio.inventory.support.http.client.Response>> allFutures = new ArrayList<>();

        if (mappedItem.getMaterialTypeId() != null) {
          allFutures.add(materialTypeFuture);

          materialTypesClient.get(mappedItem.getMaterialTypeId(),
            response1 -> materialTypeFuture.complete(response1));
        }

        if (mappedItem.getPermanentLoanTypeId() != null) {
          allFutures.add(permanentLoanTypeFuture);

          loanTypesClient.get(mappedItem.getPermanentLoanTypeId(),
            response2 -> permanentLoanTypeFuture.complete(response2));
        }

        if (mappedItem.getTemporaryLoanTypeId() != null) {
          allFutures.add(temporaryLoanTypeFuture);

          loanTypesClient.get(mappedItem.getTemporaryLoanTypeId(),
            response3 -> temporaryLoanTypeFuture.complete(response3));
        }

        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[allFutures.size()]));

        allDoneFuture.thenAccept(v -> {
          JsonObject foundMaterialType = mappedItem.getMaterialTypeId() != null && materialTypeFuture.join().getStatusCode() == 200
            ? materialTypeFuture.join().getJson()
            : null;

          JsonObject foundPermanentLoanType = mappedItem.getPermanentLoanTypeId() != null &&
            permanentLoanTypeFuture.join().getStatusCode() == 200 ?
            permanentLoanTypeFuture.join().getJson() : null;

          JsonObject foundTemporaryLoanType = mappedItem.getTemporaryLoanTypeId() != null &&
            temporaryLoanTypeFuture.join().getStatusCode() == 200 ?
            temporaryLoanTypeFuture.join().getJson() : null;

          CompositeItem compositeItem = new CompositeItem().withItem(mappedItem);

          if(foundMaterialType != null) {
            compositeItem.setMaterialType(
              new MaterialType().withId(foundMaterialType.getString("id"))
                .withName(foundMaterialType.getString("name")) );
          }

          if(foundPermanentLoanType != null) {
            compositeItem.setPermanentLoanType(
              new PermanentLoanType().withId(foundPermanentLoanType.getString("id"))
                .withName(foundPermanentLoanType.getString("name")) );
          }

          if(foundTemporaryLoanType != null) {
            compositeItem.setTemporaryLoanType(
              new PermanentLoanType().withId(foundTemporaryLoanType.getString("id"))
                .withName(foundTemporaryLoanType.getString("name")) );
          }

          OutStream stream = new OutStream();
          stream.setData(compositeItem);

          asyncResultHandler.handle(Future.succeededFuture(
            PostInventoryInstancesResponse.withJsonCreated(
              "/inventory/items/" + mappedItem.getId().toString(),
              stream)));

        });
      } else {
        asyncResultHandler.handle(Future.succeededFuture(
          convertResponseToJax(response)));
      }
    });
  }
}
