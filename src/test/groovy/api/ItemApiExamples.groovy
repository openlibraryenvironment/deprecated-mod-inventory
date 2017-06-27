package api

import api.support.ApiRoot
import api.support.InstanceApiClient
import api.support.ItemApiClient
import api.support.Preparation
import io.vertx.core.json.JsonObject
import org.folio.inventory.support.JsonArrayHelper
import org.folio.inventory.support.http.client.OkapiHttpClient
import org.folio.inventory.support.http.client.Response
import org.folio.inventory.support.http.client.ResponseHandler
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import static api.support.InstanceSamples.*

class ItemApiExamples extends Specification {
  private final OkapiHttpClient okapiClient = ApiTestSuite.createOkapiHttpClient()

  def setup() {
    def preparation = new Preparation(okapiClient)

    preparation.deleteItems()
    preparation.deleteInstances()
  }

  void "Can create an item"() {
    given:
      def createdInstance = createInstance(
        smallAngryPlanet(UUID.randomUUID()))

      def newItemRequest = new JsonObject()
        .put("title", createdInstance.title)
        .put("instanceId", createdInstance.id)
        .put("barcode", "645398607547")
        .put("status", new JsonObject().put("name", "Available"))
        .put("materialTypeId", ApiTestSuite.bookMaterialType)
        .put("permanentLoanTypeId", ApiTestSuite.canCirculateLoanType)
        .put("temporaryLoanTypeId", ApiTestSuite.courseReserveLoanType)
        .put("location", new JsonObject().put("name", "Annex Library"))

    when:
      Response postResponse = ItemApiClient.submitCreateItemRequest(okapiClient,
        newItemRequest)

    then:
      assert postResponse.statusCode == 201
      assert postResponse.location != null

      def getCompleted = new CompletableFuture<Response>()

      okapiClient.get(postResponse.location, ResponseHandler.json(getCompleted))

      Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

      assert getResponse.statusCode == 200

      def createdItem = getResponse.json.getJsonObject("item")

      assert createdItem.containsKey("id")
      assert createdItem.getString("title") == "Long Way to a Small Angry Planet"
      assert createdItem.getString("barcode") == "645398607547"
      assert createdItem.getJsonObject("status").getString("name") == "Available"
      assert createdItem.getJsonObject("location").getString("name") == "Annex Library"
      assert createdItem.getString("materialTypeId") == ApiTestSuite.bookMaterialType
      assert createdItem.getString("permanentLoanTypeId") == ApiTestSuite.canCirculateLoanType
      assert createdItem.getString("temporaryLoanTypeId") == ApiTestSuite.courseReserveLoanType

      def materialType = getResponse.json.getJsonObject("materialType")
      def permanentLoanType = getResponse.json.getJsonObject("permanentLoanType")
      def temporaryLoanType = getResponse.json.getJsonObject("temporaryLoanType")

      assert materialType.getString("id") == ApiTestSuite.bookMaterialType
      assert materialType.getString("name") == "Book"
      assert permanentLoanType.getString("id") == ApiTestSuite.canCirculateLoanType
      assert permanentLoanType.getString("name") == "Can Circulate"
      assert temporaryLoanType.getString("id") == ApiTestSuite.courseReserveLoanType
      assert temporaryLoanType.getString("name") == "Course Reserves"
  }

  void "Can create an item with an ID"() {
    given:
      def createdInstance = createInstance(
        smallAngryPlanet(UUID.randomUUID()))

      def itemId = UUID.randomUUID().toString()

      def newItemRequest = new JsonObject()
        .put("id", itemId)
        .put("title", createdInstance.title)
        .put("instanceId", createdInstance.id)
        .put("materialTypeId", ApiTestSuite.bookMaterialType)
        .put("permanentLoanTypeId", ApiTestSuite.canCirculateLoanType)
        .put("barcode", "645398607547")

    when:
      Response postResponse = ItemApiClient.submitCreateItemRequest(okapiClient,
        newItemRequest)

    then:
      assert postResponse.statusCode == 201
      assert postResponse.location != null

      def getCompleted = new CompletableFuture<Response>()

      okapiClient.get(postResponse.location, ResponseHandler.json(getCompleted))

      Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

      assert getResponse.statusCode == 200

      def createdItem = getResponse.json.getJsonObject("item")

      assert createdItem.getString("id") == itemId
  }

  void "Can create an item based upon an instance"() {
    given:
      def createdInstance = createInstance(
        smallAngryPlanet(UUID.randomUUID()))

      def newItemRequest = new JsonObject()
        .put("title", createdInstance.title)
        .put("instanceId", createdInstance.id)
        .put("barcode", "645398607547")
        .put("status", new JsonObject().put("name", "Available"))
        .put("materialTypeId", ApiTestSuite.bookMaterialType)
        .put("permanentLoanTypeId", ApiTestSuite.canCirculateLoanType)
        .put("location", new JsonObject().put("name", "Annex Library"))

    when:
      Response postResponse = ItemApiClient.submitCreateItemRequest(okapiClient,
        newItemRequest)

    then:
      assert postResponse.statusCode == 201
      assert postResponse.location != null

      def getCompleted = new CompletableFuture<Response>()

      okapiClient.get(postResponse.location, ResponseHandler.json(getCompleted))

      Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

      assert getResponse.statusCode == 200

      def createdItem = getResponse.json.getJsonObject("item")

      assert createdItem.containsKey("id")
      assert createdItem.getString("title") == "Long Way to a Small Angry Planet"
      assert createdItem.getString("instanceId") == createdInstance.id
      assert createdItem.getString("barcode") == "645398607547"
      assert createdItem.getJsonObject("status").getString("name") == "Available"
      assert createdItem.getJsonObject("location").getString("name") == "Annex Library"
      assert createdItem.getString("materialTypeId") == ApiTestSuite.bookMaterialType
      assert createdItem.getString("permanentLoanTypeId") == ApiTestSuite.canCirculateLoanType

      def materialType = getResponse.json.getJsonObject("materialType")
      def permanentLoanType = getResponse.json.getJsonObject("permanentLoanType")

      assert materialType.getString("id") == ApiTestSuite.bookMaterialType
      assert materialType.getString("name") == "Book"
      assert permanentLoanType.getString("id") == ApiTestSuite.canCirculateLoanType
      assert permanentLoanType.getString("name") == "Can Circulate"
  }

  void "Can create an item without a barcode"() {
    given:
      def newItemRequest = new JsonObject()
        .put("title", "Nod")
        .put("status", new JsonObject().put("name", "Available"))
        .put("materialTypeId", ApiTestSuite.bookMaterialType)
        .put("permanentLoanTypeId", ApiTestSuite.canCirculateLoanType)
        .put("location", new JsonObject().put("name", "Annex Library"))

    when:
      Response postResponse = ItemApiClient.submitCreateItemRequest(okapiClient,
        newItemRequest)

    then:
      assert postResponse.statusCode == 201
      assert postResponse.location != null

      def getCompleted = new CompletableFuture<Response>()

      okapiClient.get(postResponse.location, ResponseHandler.json(getCompleted))

      Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

      assert getResponse.statusCode == 200

      def createdItem = getResponse.json

      assert createdItem.containsKey("barcode") == false
  }

  void "Can create multiple items without a barcode"() {

    def firstItemRequest = new JsonObject()
      .put("title", "Temeraire")
      .put("status", new JsonObject().put("name", "Available"))
      .put("location", new JsonObject().put("name", "Main Library"))
      .put("materialTypeId", ApiTestSuite.bookMaterialType)
      .put("permanentLoanTypeId", ApiTestSuite.canCirculateLoanType)

    ItemApiClient.createItem(okapiClient, firstItemRequest)

    def newItemRequest = new JsonObject()
      .put("title", "Nod")
      .put("status", new JsonObject().put("name", "Available"))
      .put("materialTypeId", ApiTestSuite.bookMaterialType)
      .put("permanentLoanTypeId", ApiTestSuite.canCirculateLoanType)
      .put("location", new JsonObject().put("name", "Annex Library"))

    when:
      Response postResponse = ItemApiClient.submitCreateItemRequest(okapiClient,
        newItemRequest)

    then:
      assert postResponse.statusCode == 201
      assert postResponse.location != null

      def getCompleted = new CompletableFuture<Response>()

      okapiClient.get(postResponse.location, ResponseHandler.json(getCompleted))

      Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

      assert getResponse.statusCode == 200

      def createdItem = getResponse.json

      assert createdItem.containsKey("barcode") == false
  }

  void "Cannot create an item without a material type"() {
    given:
      def createdInstance = createInstance(
        smallAngryPlanet(UUID.randomUUID()))

      def newItemRequest = new JsonObject()
        .put("title", createdInstance.title)
        .put("instanceId", createdInstance.id)
        .put("barcode", "645398607547")
        .put("status", new JsonObject().put("name", "Available"))
        .put("permanentLoanTypeId", ApiTestSuite.canCirculateLoanType)
        .put("location", new JsonObject().put("name", "Annex Library"))

    when:
      Response postResponse = ItemApiClient.submitCreateItemRequest(okapiClient,
        newItemRequest)

    then:
      assert postResponse.statusCode == 422
  }

  void "Cannot create an item without a permanent loan type"() {
    given:
      def createdInstance = createInstance(
        smallAngryPlanet(UUID.randomUUID()))

      def newItemRequest = new JsonObject()
        .put("title", createdInstance.title)
        .put("instanceId", createdInstance.id)
        .put("barcode", "645398607547")
        .put("status", new JsonObject().put("name", "Available"))
        .put("materialTypeId", ApiTestSuite.bookMaterialType)
        .put("location", new JsonObject().put("name", "Annex Library"))

    when:
      Response postResponse = ItemApiClient.submitCreateItemRequest(okapiClient,
        newItemRequest)

    then:
      assert postResponse.statusCode == 422
  }

  void "Can create an item without a temporary loan type"() {
    given:
      def createdInstance = createInstance(
        smallAngryPlanet(UUID.randomUUID()))

      def newItemRequest = new JsonObject()
        .put("title", createdInstance.title)
        .put("instanceId", createdInstance.id)
        .put("barcode", "645398607547")
        .put("status", new JsonObject().put("name", "Available"))
        .put("materialTypeId", ApiTestSuite.bookMaterialType)
        .put("permanentLoanTypeId", ApiTestSuite.canCirculateLoanType)
        .put("location", new JsonObject().put("name", "Annex Library"))

    when:
      Response postResponse = ItemApiClient.submitCreateItemRequest(okapiClient,
        newItemRequest)

    then:
      assert postResponse.statusCode == 201
      assert postResponse.location != null

      def getCompleted = new CompletableFuture<Response>()

      okapiClient.get(postResponse.location, ResponseHandler.json(getCompleted))

      Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

      assert getResponse.statusCode == 200

      def createdItem = getResponse.json.getJsonObject("item")

      assert createdItem.containsKey("id")
      assert createdItem.getString("title") == "Long Way to a Small Angry Planet"
      assert createdItem.getString("instanceId") == createdInstance.id
      assert createdItem.getString("barcode") == "645398607547"
      assert createdItem.getJsonObject("status").getString("name") == "Available"
      assert createdItem.getJsonObject("location").getString("name") == "Annex Library"
      assert createdItem.getString("materialTypeId") == ApiTestSuite.bookMaterialType
      assert createdItem.getString("permanentLoanTypeId") == ApiTestSuite.canCirculateLoanType

      def materialType = getResponse.json.getJsonObject("materialType")
      def permanentLoanType = getResponse.json.getJsonObject("permanentLoanType")

      assert materialType.getString("id") == ApiTestSuite.bookMaterialType
      assert materialType.getString("name") == "Book"
      assert permanentLoanType.getString("id") == ApiTestSuite.canCirculateLoanType
      assert permanentLoanType.getString("name") == "Can Circulate"

      assert getResponse.json.containsKey("temporaryLoanType") == false
  }

  void "Cannot create a second item with the same barcode"() {
    given:
      def smallAngryInstance = createInstance(
        smallAngryPlanet(UUID.randomUUID()))

      createItem(smallAngryInstance.title, smallAngryInstance.id,
        "645398607547")

      def nodInstance = createInstance(nod(UUID.randomUUID()))

    when:
      def newItemRequest = new JsonObject()
        .put("title", nodInstance.title)
        .put("instanceId", nodInstance.id)
        .put("barcode", "645398607547")
        .put("status", new JsonObject().put("name", "Available"))
        .put("materialTypeId", ApiTestSuite.bookMaterialType)
        .put("location", new JsonObject().put("name", "Main Library"))

      Response postResponse = ItemApiClient.submitCreateItemRequest(okapiClient,
        newItemRequest)

    then:
      assert postResponse.statusCode == 400
      assert postResponse.body == "Barcodes must be unique, 645398607547 is already assigned to another item"
  }

  void "Can update an existing item"() {
    given:
      def createdInstance = createInstance(
        smallAngryPlanet(UUID.randomUUID()))

      def newItem = createItem(
        createdInstance.title, createdInstance.id, "645398607547")

      def updateItemRequest = newItem.copy()
        .put("status", new JsonObject().put("name", "Checked Out"))

      def itemLocation = new URL("${ApiRoot.items()}/${newItem.getString("id")}")

    when:
      def putCompleted = new CompletableFuture<Response>()

      okapiClient.put(itemLocation,
        new JsonObject().put("item", updateItemRequest), ResponseHandler.any(putCompleted))

      Response putResponse = putCompleted.get(5, TimeUnit.SECONDS)

    then:
      assert putResponse.statusCode == 204

      def getCompleted = new CompletableFuture<Response>()

      okapiClient.get(itemLocation, ResponseHandler.json(getCompleted))

      Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

      assert getResponse.statusCode == 200

      def updatedItem = getResponse.json.getJsonObject("item")

      assert updatedItem.containsKey("id")
      assert updatedItem.getString("title") == "Long Way to a Small Angry Planet"
      assert updatedItem.getString("instanceId") == createdInstance.id
      assert updatedItem.getString("barcode") == "645398607547"
      assert updatedItem.getJsonObject("status").getString("name") == "Checked Out"
      assert updatedItem.getJsonObject("location").getString("name") == "Main Library"
      assert updatedItem.getString("materialTypeId") == ApiTestSuite.bookMaterialType
      assert updatedItem.getString("permanentLoanTypeId") == ApiTestSuite.canCirculateLoanType

      def materialType = getResponse.json.getJsonObject("materialType")
      def permanentLoanType = getResponse.json.getJsonObject("permanentLoanType")

      assert materialType.getString("id") == ApiTestSuite.bookMaterialType
      assert materialType.getString("name") == "Book"
      assert permanentLoanType.getString("id") == ApiTestSuite.canCirculateLoanType
      assert permanentLoanType.getString("name") == "Can Circulate"
  }

  void "Cannot update an item that does not exist"() {
    given:
      def updateItemRequest = new JsonObject()
        .put("id", UUID.randomUUID().toString())
        .put("title", "Nod")
        .put("instanceId", UUID.randomUUID().toString())
        .put("barcode", "546747342365")
        .put("status", new JsonObject().put("name", "Available"))
        .put("materialTypeId", ApiTestSuite.bookMaterialType)
        .put("permanentLoanTypeId", ApiTestSuite.canCirculateLoanType)
        .put("location", new JsonObject().put("name", "Main Library"))

    when:
      def putCompleted = new CompletableFuture<Response>()

      okapiClient.put(new URL("${ApiRoot.items()}/${updateItemRequest.getString("id")}"),
        new JsonObject().put("item", updateItemRequest),
        ResponseHandler.any(putCompleted))

      Response putResponse = putCompleted.get(5, TimeUnit.SECONDS)

    then:
      assert putResponse.statusCode == 404
  }

  void "Cannot update an item to have the same barcode as an existing item"() {
    given:
    def smallAngryInstance = createInstance(
      smallAngryPlanet(UUID.randomUUID()))

    createItem(smallAngryInstance.title, smallAngryInstance.id,
      "645398607547")

    def nodInstance = createInstance(nod(UUID.randomUUID()))

    def nodItem = createItem(nodInstance.title, nodInstance.id,
      "654647774352")

    when:
      def changedNodItem = nodItem.copy()
        .put("barcode", "645398607547")

      def nodItemLocation = new URL(
        "${ApiRoot.items()}/${changedNodItem.getString("id")}")

      def putCompleted = new CompletableFuture<Response>()

      okapiClient.put(nodItemLocation,
        new JsonObject().put("item", changedNodItem),
        ResponseHandler.any(putCompleted))

      Response putResponse = putCompleted.get(5, TimeUnit.SECONDS)

    then:
      assert putResponse.statusCode == 400
      assert putResponse.body == "Barcodes must be unique, 645398607547 is already assigned to another item"
  }

  void "Can delete all items"() {
    given:
      def createdInstance = createInstance(smallAngryPlanet(UUID.randomUUID()))

      createItem(createdInstance.title, createdInstance.id, "645398607547")

      createItem(createdInstance.title, createdInstance.id, "175848607547")

      createItem(createdInstance.title, createdInstance.id, "645334645247")

    when:
      def deleteCompleted = new CompletableFuture<Response>()

      okapiClient.delete(ApiRoot.items(),
        ResponseHandler.any(deleteCompleted))

      Response deleteResponse = deleteCompleted.get(5, TimeUnit.SECONDS)

    then:
      assert deleteResponse.statusCode == 204
      assert deleteResponse.hasBody() == false

      def getAllCompleted = new CompletableFuture<Response>()

      okapiClient.get(ApiRoot.items(),
        ResponseHandler.json(getAllCompleted))

      Response getAllResponse = getAllCompleted.get(5, TimeUnit.SECONDS);

      assert getAllResponse.json.getJsonArray("items").size() == 0
      assert getAllResponse.json.getInteger("totalRecords") == 0
  }

  void "Can delete a single item"() {
    given:
      def createdInstance = createInstance(
        smallAngryPlanet(UUID.randomUUID()))

      createItem(createdInstance.title, createdInstance.id, "645398607547")

      createItem(createdInstance.title, createdInstance.id, "175848607547")

      def itemToDelete = createItem(createdInstance.title, createdInstance.id,
        "645334645247")

      def itemToDeleteLocation =
        new URL("${ApiRoot.items()}/${itemToDelete.getString("id")}")

    when:
      def deleteCompleted = new CompletableFuture<Response>()

      okapiClient.delete(itemToDeleteLocation,
        ResponseHandler.any(deleteCompleted))

      Response deleteResponse = deleteCompleted.get(5, TimeUnit.SECONDS);

    then:
      assert deleteResponse.statusCode == 204
      assert deleteResponse.hasBody() == false

      def getCompleted = new CompletableFuture<Response>()

      okapiClient.get(itemToDeleteLocation,
        ResponseHandler.any(getCompleted))

      Response getResponse = getCompleted.get(5, TimeUnit.SECONDS)

      assert getResponse.statusCode == 404

      def getAllCompleted = new CompletableFuture<Response>()

      okapiClient.get(ApiRoot.items(),
        ResponseHandler.json(getAllCompleted))

      Response getAllResponse = getAllCompleted.get(5, TimeUnit.SECONDS);

      assert getAllResponse.json.getJsonArray("items").size() == 2
      assert getAllResponse.json.getInteger("totalRecords") == 2
  }

  void "Can page all items"() {
    given:
      def smallAngryInstance = createInstance(smallAngryPlanet(UUID.randomUUID()))

      createItem(smallAngryInstance.title, smallAngryInstance.id,
        "645398607547", ApiTestSuite.bookMaterialType,
        ApiTestSuite.canCirculateLoanType, null)

      createItem(smallAngryInstance.title, smallAngryInstance.id,
        "175848607547", ApiTestSuite.bookMaterialType,
        ApiTestSuite.courseReserveLoanType, null)

      def girlOnTheTrainInstance = createInstance(girlOnTheTrain(UUID.randomUUID()))

      createItem(girlOnTheTrainInstance.title, girlOnTheTrainInstance.id,
        "645334645247", ApiTestSuite.dvdMaterialType,
        ApiTestSuite.canCirculateLoanType,
        ApiTestSuite.courseReserveLoanType)

      def nodInstance = createInstance(nod(UUID.randomUUID()))

      createItem(nodInstance.title, nodInstance.id, "564566456546")

      createItem(nodInstance.title, nodInstance.id, "943209584495")

    when:
      def firstPageGetCompleted = new CompletableFuture<Response>()
      def secondPageGetCompleted = new CompletableFuture<Response>()

      okapiClient.get(ApiRoot.items("limit=3"),
        ResponseHandler.json(firstPageGetCompleted))

      okapiClient.get(ApiRoot.items("limit=3&offset=3"),
        ResponseHandler.json(secondPageGetCompleted))

      Response firstPageResponse = firstPageGetCompleted.get(5, TimeUnit.SECONDS)
      Response secondPageResponse = secondPageGetCompleted.get(5, TimeUnit.SECONDS)

    then:
      assert firstPageResponse.statusCode == 200
      assert secondPageResponse.statusCode == 200

      def firstPageWrappedItems = JsonArrayHelper.toList(firstPageResponse.json.getJsonArray("items"))

      assert firstPageWrappedItems.size() == 3
      assert firstPageResponse.json.getInteger("totalRecords") == 5

      def secondPageWrappedItems = JsonArrayHelper.toList(secondPageResponse.json.getJsonArray("items"))

      assert secondPageWrappedItems.size() == 2
      assert secondPageResponse.json.getInteger("totalRecords") == 5

      firstPageWrappedItems.each {
        hasConsistentMaterialType(it)
      }

      firstPageWrappedItems.each {
        hasConsistentPermanentLoanType(it)
      }

      firstPageWrappedItems.each {
        hasConsistentTemporaryLoanType(it)
      }

      firstPageWrappedItems.each {
        hasStatus(it.getJsonObject("item"))
      }

      firstPageWrappedItems.each {
        hasLocation(it.getJsonObject("item"))
      }

      secondPageWrappedItems.each {
        hasConsistentMaterialType(it)
      }

      secondPageWrappedItems.each {
        hasConsistentPermanentLoanType(it)
      }

      secondPageWrappedItems.each {
        hasConsistentTemporaryLoanType(it)
      }

      secondPageWrappedItems.each {
        hasStatus(it.getJsonObject("item"))
      }

      secondPageWrappedItems.each {
        hasLocation(it.getJsonObject("item"))
      }
  }

  void "Can get all items with different permanent and temporary loan types"() {
    given:
      def smallAngryInstance = createInstance(smallAngryPlanet(UUID.randomUUID()))

      createItem(smallAngryInstance.title, smallAngryInstance.id,
        "645398607547", ApiTestSuite.bookMaterialType,
        ApiTestSuite.canCirculateLoanType, null)

      createItem(smallAngryInstance.title, smallAngryInstance.id,
        "175848607547", ApiTestSuite.bookMaterialType,
        ApiTestSuite.canCirculateLoanType, ApiTestSuite.courseReserveLoanType)

    when:
      def getAllCompleted = new CompletableFuture<Response>()

      okapiClient.get(ApiRoot.items(),
        ResponseHandler.json(getAllCompleted))

      Response getAllResponse = getAllCompleted.get(5, TimeUnit.SECONDS)

    then:
      assert getAllResponse.statusCode == 200

      def wrappedItems = JsonArrayHelper.toList(getAllResponse.json.getJsonArray("items"))

      assert wrappedItems.size() == 2
      assert getAllResponse.json.getInteger("totalRecords") == 2

      assert wrappedItems.stream()
        .filter({it.getJsonObject("item").getString("barcode") == "645398607547"})
        .findFirst().get().getJsonObject("permanentLoanType").getString("id") == ApiTestSuite.canCirculateLoanType

      assert wrappedItems.stream()
        .filter({it.getJsonObject("item").getString("barcode") == "645398607547"})
        .findFirst().get().containsKey("temporaryLoanType") == false

      assert wrappedItems.stream()
        .filter({it.getJsonObject("item").getString("barcode") == "175848607547"})
        .findFirst().get().getJsonObject("permanentLoanType").getString("id") == ApiTestSuite.canCirculateLoanType

      assert wrappedItems.stream()
        .filter({it.getJsonObject("item").getString("barcode") == "175848607547"})
        .findFirst().get().getJsonObject("temporaryLoanType").getString("id") == ApiTestSuite.courseReserveLoanType

      wrappedItems.each {
        hasConsistentPermanentLoanType(it)
      }

      wrappedItems.each {
        hasConsistentTemporaryLoanType(it)
      }
  }

  void "Page parameters must be numeric"() {
    when:
      def getPagedCompleted = new CompletableFuture<Response>()

      okapiClient.get(ApiRoot.items("limit=&offset="),
        ResponseHandler.text(getPagedCompleted))

      Response getPagedResponse = getPagedCompleted.get(5, TimeUnit.SECONDS)

    then:
      assert getPagedResponse.statusCode == 400
      assert getPagedResponse.body == "limit and offset must be numeric when supplied"
  }

  void "Can search for items by title"() {
    given:
      def smallAngryInstance = createInstance(smallAngryPlanet(UUID.randomUUID()))

      createItem(smallAngryInstance.title, smallAngryInstance.id, "645398607547")

      def nodInstance = createInstance(nod(UUID.randomUUID()))

      createItem(nodInstance.title, nodInstance.id, "564566456546")

    when:
      def searchGetCompleted = new CompletableFuture<Response>()

      okapiClient.get(ApiRoot.items("query=title=*Small%20Angry*"),
        ResponseHandler.json(searchGetCompleted))

      Response searchGetResponse = searchGetCompleted.get(5, TimeUnit.SECONDS)

    then:
      assert searchGetResponse.statusCode == 200

      def wrappedItems = JsonArrayHelper.toList(searchGetResponse.json.getJsonArray("items"))

      assert wrappedItems.size() == 1
      assert searchGetResponse.json.getInteger("totalRecords") == 1

      def firstItem = wrappedItems[0].getJsonObject("item")

      assert firstItem.getString("title") == "Long Way to a Small Angry Planet"
      assert firstItem.getJsonObject("status").getString("name") == "Available"

      wrappedItems.each {
        hasConsistentMaterialType(it)
      }

      wrappedItems.each {
        hasConsistentPermanentLoanType(it)
      }

      wrappedItems.each {
        hasConsistentTemporaryLoanType(it)
      }

      wrappedItems.each {
        hasStatus(it.getJsonObject("item"))
      }

      wrappedItems.each {
        hasLocation(it.getJsonObject("item"))
      }
  }

  private void hasStatus(JsonObject item) {
    assert item.containsKey("status")
    assert item.getJsonObject("status").containsKey("name")
  }

  private void hasConsistentMaterialType(JsonObject wrappedItem) {
    def materialType = wrappedItem.getJsonObject("materialType")

    assert wrappedItem.getJsonObject("item").getString("materialTypeId") ==
      materialType.getString("id")

    switch(materialType.getString("id")) {
      case ApiTestSuite.bookMaterialType:
        assert materialType.getString("id") == ApiTestSuite.bookMaterialType
        assert materialType.getString("name") == "Book"
        break

      case ApiTestSuite.dvdMaterialType:
        assert materialType.getString("id") == ApiTestSuite.dvdMaterialType
        assert materialType.getString("name") == "DVD"
        break

      default:
        assert materialType.getString("id") == null
        assert materialType.getString("name") == null
    }
  }

  private void hasConsistentPermanentLoanType(JsonObject wrappedItem) {
    def permanentLoanType = wrappedItem.getJsonObject("permanentLoanType")

    assert wrappedItem.getJsonObject("item").getString("permanentLoanTypeId") ==
      permanentLoanType.getString("id")

    hasConsistentLoanType(permanentLoanType)
  }

  private void hasConsistentTemporaryLoanType(JsonObject wrappedItem) {
    def temporaryLoanType = wrappedItem.getJsonObject("temporaryLoanType")

    if(temporaryLoanType == null) {
      return
    }

    assert wrappedItem.getJsonObject("item").getString("temporaryLoanTypeId") ==
      temporaryLoanType.getString("id")

    hasConsistentLoanType(temporaryLoanType)
  }

  private void hasConsistentLoanType(JsonObject loanType) {
    if(loanType == null) {
      return
    }

    switch (loanType.getString("id")) {
      case ApiTestSuite.canCirculateLoanType:
        assert loanType.getString("id") == ApiTestSuite.canCirculateLoanType
        assert loanType.getString("name") == "Can Circulate"
        break

      case ApiTestSuite.courseReserveLoanType:
        assert loanType.getString("id") == ApiTestSuite.courseReserveLoanType
        assert loanType.getString("name") == "Course Reserves"
        break

      default:
        assert loanType.getString("id") == null
        assert loanType.getString("name") == null
    }
  }

  private void hasLocation(JsonObject item) {
    assert item.getJsonObject("location").getString("name") != null
  }

  private def createInstance(JsonObject newInstanceRequest) {
    InstanceApiClient.createInstance(okapiClient, newInstanceRequest)
  }

  private JsonObject createItem(String title, String instanceId, String barcode) {
    createItem(title, instanceId, barcode, ApiTestSuite.bookMaterialType,
      ApiTestSuite.canCirculateLoanType, null)
  }

  private JsonObject createItem(
    String title,
    String instanceId,
    String barcode,
    String materialTypeId,
    String permanentLoanTypeId,
    String temporaryLoanTypeId) {

    def newItemRequest = new JsonObject()
      .put("title", title)
      .put("instanceId", instanceId)
      .put("barcode", barcode)
      .put("status", new JsonObject().put("name", "Available"))
      .put("location", new JsonObject().put("name", "Main Library"))

    if(materialTypeId != null) {
      newItemRequest.put("materialTypeId", materialTypeId)
    }

    if(permanentLoanTypeId != null) {
      newItemRequest.put("permanentLoanTypeId", permanentLoanTypeId)
    }

    if(temporaryLoanTypeId != null) {
      newItemRequest.put("temporaryLoanTypeId", temporaryLoanTypeId)
    }

    ItemApiClient.createItem(okapiClient, newItemRequest)
  }
}
