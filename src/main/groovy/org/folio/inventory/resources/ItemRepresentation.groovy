package org.folio.inventory.resources

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.folio.inventory.common.WebRoutingContext
import org.folio.inventory.domain.Item

class ItemRepresentation {
  JsonObject toJson(
    Item item,
    JsonObject materialType,
    JsonObject permanentLoanType,
    JsonObject temporaryLoanType) {

    def representation = new JsonObject()

    def itemRepresentation = toJson(item)

    representation.put("item", itemRepresentation)

    if(materialType != null) {
      representation.put("materialType", materialType)
    }

    if(permanentLoanType != null) {
      representation.put("permanentLoanType", permanentLoanType)
    }

    if(temporaryLoanType != null) {
      representation.put("temporaryLoanType", temporaryLoanType)
    }

    representation
  }

  JsonObject toJson(Item item) {
    def representation = new JsonObject()

    representation.put("id", item.id)
    representation.put("title", item.title)

    if(item.status != null) {
      representation.put("status", new JsonObject().put("name", item.status))
    }

    includeIfPresent(representation, "instanceId", item.instanceId)
    includeIfPresent(representation, "barcode", item.barcode)

    includeIfPresent(representation, "materialTypeId",
      item.materialTypeId)

    includeIfPresent(representation, "permanentLoanTypeId",
      item.permanentLoanTypeId)

    includeIfPresent(representation, "temporaryLoanTypeId",
      item.temporaryLoanTypeId)

    if(item.location != null) {
      representation.put("location",
        new JsonObject().put("name", item.location))
    }

    representation
  }

  JsonObject toJson(
    Map wrappedItems,
    Map<String, JsonObject> materialTypes,
    Map<String, JsonObject> loanTypes) {

    def representation = new JsonObject()

    def results = new JsonArray()


    wrappedItems.items.each { item ->
      def materialType = materialTypes.get(item?.materialTypeId)
      def permanentLoanType = loanTypes.get(item?.permanentLoanTypeId)
      def temporaryLoanType = loanTypes.get(item?.temporaryLoanTypeId)

      results.add(toJson(item, materialType, permanentLoanType, temporaryLoanType))
    }

    representation
      .put("compositeItems", results)
      .put("totalRecords", wrappedItems.totalRecords)

    representation
  }

  JsonObject toJson(Map wrappedItems,
                    WebRoutingContext context) {

    def representation = new JsonObject()

    def results = new JsonArray()

    wrappedItems.items.each { item ->
      results.add(toJson(item, context))
    }

    representation
      .put("items", results)
      .put("totalRecords", wrappedItems.totalRecords)

    representation
  }

  private void includeIfPresent(
    JsonObject representation,
    String propertyName,
    String propertyValue) {

    if (propertyValue != null) {
      representation.put(propertyName, propertyValue)
    }
  }
}
