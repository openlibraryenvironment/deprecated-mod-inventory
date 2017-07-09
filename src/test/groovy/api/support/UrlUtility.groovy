package api.support

class UrlUtility {
  static String toAbsolute(String location, URL base) {
    if(new URI(location).isAbsolute()) {
      location
    }
    else {
      base.toURI().resolve(location).toString()
    }
  }
}
