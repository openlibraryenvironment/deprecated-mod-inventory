package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.folio.inventory.support.CollectionResourceClient;
import org.folio.inventory.support.http.client.OkapiHttpClient;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.resource.InventoryResource;

import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class InventoryAPI implements InventoryResource {

  private final String OKAPI_URL = "X-Okapi-Url";

  @Override
  public void deleteInventoryItems(
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    CollectionResourceClient itemsClient;
    OkapiHttpClient client = createHttpClient(vertxContext, okapiHeaders);

    itemsClient = new CollectionResourceClient(client,
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), "/item-storage/items"));

    itemsClient.delete(response ->{
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

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  @Override
  public void postInventoryItems(
    String lang,
    Item entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  @Override
  public void getInventoryItemsByItemId(
    String itemId,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  @Override
  public void deleteInventoryItemsByItemId(
    String itemId,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  @Override
  public void putInventoryItemsByItemId(
    String itemId,
    String lang,
    Item entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
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
      okapiBasedUrl(okapiHeaders.get(OKAPI_URL), "/instance-storage/instances"));

    instancesStorageClient.delete(response ->{
      asyncResultHandler.handle(Future.succeededFuture(
        DeleteInventoryInstancesResponse.withNoContent()));
    });
  }

  @Override
  public void getInventoryInstances(
    int offset,
    int limit,
    String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  @Override
  public void postInventoryInstances(
    String lang,
    Instance entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  @Override
  public void getInventoryInstancesByInstanceId(
    String instanceId,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  @Override
  public void deleteInventoryInstancesByInstanceId(
    String instanceId,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  @Override
  public void putInventoryInstancesByInstanceId(
    String instanceId,
    String lang, Instance entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
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

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  @Override
  public void getInventoryIngestModsStatusById(
    String id, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
  }

  private OkapiHttpClient createHttpClient(Context context,
                                           Map<String, String> okapiHeaders)
    throws MalformedURLException {

    //TODO: Need to reinstate handling exceptions
    return new OkapiHttpClient(context.owner().createHttpClient(),
      new URL(okapiHeaders.get(OKAPI_URL)), okapiHeaders.get("X-Okapi-Tenant"),
      okapiHeaders.get("X-Okapi-Token"),
      exception -> {  });
  }

  private URL okapiBasedUrl(String okapiUrl, String path)
    throws MalformedURLException {

    URL currentRequestUrl = new URL(okapiUrl);

    return new URL(currentRequestUrl.getProtocol(), currentRequestUrl.getHost(),
      currentRequestUrl.getPort(), path);
  }
}
