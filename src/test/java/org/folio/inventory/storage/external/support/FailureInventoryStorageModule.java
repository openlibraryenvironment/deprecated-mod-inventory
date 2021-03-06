package org.folio.inventory.storage.external.support;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.folio.inventory.common.WebRequestDiagnostics;
import org.folio.inventory.support.http.server.ClientErrorResponse;
import org.folio.inventory.support.http.server.ServerErrorResponse;

public class FailureInventoryStorageModule extends AbstractVerticle {
  private static final int PORT_TO_USE = 9493;
  private static final String address = String.format("http://localhost:%s", PORT_TO_USE);

  private HttpServer server;

  public static String getServerErrorAddress() {
    return address + "/server-error";
  }

  public static String getBadRequestAddress() {
    return address + "/bad-request";
  }

  @Override
  public void start(Future deployed) {
    server = vertx.createHttpServer();

    Router router = Router.router(vertx);

    server.requestHandler(router::accept)
      .listen(PORT_TO_USE, result -> {
        if (result.succeeded()) {
          System.out.println(
            String.format("Starting failing storage module listening on %s",
              server.actualPort()));
          deployed.complete();
        } else {
          deployed.fail(result.cause());
        }
      });

    router.route().handler(WebRequestDiagnostics::outputDiagnostics);

    router.route("/server-error/item-storage/items/*").handler(this::serverError);
    router.route("/server-error/instance-storage/instances/*").handler(this::serverError);
    router.route("/bad-request/item-storage/items/*").handler(this::badRequest);
    router.route("/bad-request/instance-storage/instances/*").handler(this::badRequest);
  }

  @Override
  public void stop(Future stopped) {
    System.out.println("Stopping failing storage module");
    server.close(result -> {
      if (result.succeeded()) {
        System.out.println(
          String.format("Stopped listening on %s", server.actualPort()));
        stopped.complete();
      } else {
        stopped.fail(result.cause());
      }
    });
  }

  private void serverError(RoutingContext routingContext) {
    ServerErrorResponse.internalError(routingContext.response(), "Server Error");
  }

  private void badRequest(RoutingContext routingContext) {
    ClientErrorResponse.badRequest(routingContext.response(), "Bad Request");
  }
}
