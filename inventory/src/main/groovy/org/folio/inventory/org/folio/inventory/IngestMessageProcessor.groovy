package org.folio.inventory.org.folio.inventory

import io.vertx.groovy.core.eventbus.EventBus
import io.vertx.groovy.core.eventbus.Message
import org.folio.inventory.domain.Instance
import org.folio.inventory.domain.InstanceCollection
import org.folio.inventory.domain.Item
import org.folio.inventory.domain.ItemCollection
import org.folio.metadata.common.CollectAll

class IngestMessageProcessor {
  private final ItemCollection itemCollection
  private final InstanceCollection instanceCollection

  IngestMessageProcessor(ItemCollection itemCollection,
                         InstanceCollection instanceCollection) {
    this.instanceCollection = instanceCollection
    this.itemCollection = itemCollection
  }

  void register(EventBus eventBus) {
    def consumer = eventBus.consumer("org.folio.inventory.ingest.records.process")

    consumer.handler(this.&processRecordsMessage)
  }

  private void processRecordsMessage(Message message) {
    def allItems = new CollectAll<Item>()
    def allInstances = new CollectAll<Instance>()

    def records = message.body().records

    records.stream()
      .map({
      new Item(UUID.randomUUID().toString(),
        it.title,
        it.barcode)
    })
      .forEach({ itemCollection.add(it, allItems.receive()) })

    records.stream()
      .map({
      new Instance(UUID.randomUUID().toString(), it.title)
    })
      .forEach({ instanceCollection.add(it, allInstances.receive()) })
  }
}
