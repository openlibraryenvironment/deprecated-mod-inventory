package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.resource.InventoryResource;

import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.Response;
import java.util.Map;

public class InventoryAPI implements InventoryResource {
  @Override
  public void deleteInventoryItems(
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
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

    asyncResultHandler.handle(Future.succeededFuture(Response.status(501).build()));
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
}
