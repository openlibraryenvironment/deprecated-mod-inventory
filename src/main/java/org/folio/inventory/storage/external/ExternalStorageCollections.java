package org.folio.inventory.storage.external;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import org.folio.inventory.domain.CollectionProvider;
import org.folio.inventory.domain.HoldingCollection;
import org.folio.inventory.domain.InstanceCollection;
import org.folio.inventory.domain.ItemCollection;
import org.folio.inventory.domain.ingest.IngestJobCollection;
import org.folio.inventory.storage.memory.InMemoryIngestJobCollection;

public class ExternalStorageCollections implements CollectionProvider {
  private final Vertx vertx;
  private final HttpClient client;
  private final String baseAddress;
  private static final InMemoryIngestJobCollection ingestJobCollection = new InMemoryIngestJobCollection();

  public ExternalStorageCollections(Vertx vertx, HttpClient client, String baseAddress) {
    this.vertx = vertx;
    this.client = client;
    this.baseAddress = baseAddress;
  }

  @Override
  public ItemCollection getItemCollection(String tenantId, String token) {
    return new ExternalStorageModuleItemCollection(client, baseAddress,
      tenantId, token);
  }

  @Override
  public HoldingCollection getHoldingCollection(String tenantId, String token) {
    return new ExternalStorageModuleHoldingCollection(client, baseAddress,
      tenantId, token);
  }

  @Override
  public InstanceCollection getInstanceCollection(String tenantId, String token) {
    return new ExternalStorageModuleInstanceCollection(client, baseAddress,
      tenantId, token);
  }

  @Override
  public IngestJobCollection getIngestJobCollection(String tenantId, String token) {
    //There is no external storage implementation for Jobs yet
    return ingestJobCollection;
  }
}
