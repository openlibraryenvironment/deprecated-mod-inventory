package org.folio.inventory.common

import io.vertx.ext.web.RoutingContext

class RamlModuleBuilderContext implements Context {
  private final Map<String, String> okapiHeaders

  RamlModuleBuilderContext(Map<String, String> okapiHeaders) {
    this.okapiHeaders = okapiHeaders
  }

  @Override
  String getTenantId() {
    getHeader("X-Okapi-Tenant", "")
  }

  @Override
  String getToken() {
    getHeader("X-Okapi-Token", "")
  }

  @Override
  String getOkapiLocation() {
    getHeader("X-Okapi-Url", "")
  }

  @Override
  def getHeader(String header) {
    okapiHeaders.get(header)
  }

  @Override
  def getHeader(String header, String defaultValue) {
    okapiHeaders.get(header, defaultValue)
  }

  @Override
  boolean hasHeader(String header) {
    okapiHeaders.containsKey(header)
  }
}
