package api.support

import api.ApiTestSuite

class ApiRoot {
  static URL inventory() {
    new URL("${ApiTestSuite.apiRoot()}/inventory")
  }

  static URL instances() {
    new URL("${ApiTestSuite.apiRoot()}/inventory/instances")
  }

  static URL instances(String query) {
    new URL("${ApiTestSuite.apiRoot()}/inventory/instances?${query}")
  }

  static URL items() {
    new URL("${ApiTestSuite.apiRoot()}/inventory/items")
  }

  static URL items(String query) {
    new URL("${ApiTestSuite.apiRoot()}/inventory/items?${query}")
  }
}
