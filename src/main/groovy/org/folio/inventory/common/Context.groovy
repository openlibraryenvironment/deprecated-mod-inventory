package org.folio.inventory.common

interface Context {
  String getTenantId()

  String getToken()

  String getOkapiLocation()
  def getHeader(String header)
  def getHeader(String header, String defaultValue)
  boolean hasHeader(String header)
}
