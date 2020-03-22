package api.events;

import api.support.ApiRoot;
import api.support.ApiTests;
import io.vertx.core.json.JsonObject;
import org.folio.DataImportEventPayload;
import org.folio.JobProfile;
import org.folio.MatchDetail;
import org.folio.MatchProfile;
import org.folio.UserInfo;
import org.folio.inventory.support.http.client.Response;
import org.folio.inventory.support.http.client.ResponseHandler;
import org.folio.rest.jaxrs.model.EntityType;
import org.folio.rest.jaxrs.model.Field;
import org.folio.rest.jaxrs.model.MatchExpression;
import org.folio.rest.jaxrs.model.ProfileSnapshotWrapper;
import org.junit.Test;
import support.fakes.FakeOkapi;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.folio.MatchDetail.MatchCriterion.EXACTLY_MATCHES;
import static org.folio.rest.jaxrs.model.MatchExpression.DataValueType.VALUE_FROM_RECORD;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EventHandlersApiTest extends ApiTests {

  @Test
  public void shouldReturnBadRequestOnEmptyBody() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {
    CompletableFuture<Response> conversionCompleted = new CompletableFuture<>();
    okapiClient.post(ApiRoot.dataImportEventHandler(), null, ResponseHandler.text(conversionCompleted));
    Response response = conversionCompleted.get(1, TimeUnit.SECONDS);
    assertThat(response.getStatusCode(), is(500));
  }

  @Test
  public void shouldReturnNoContentOnValidBody() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {
    CompletableFuture<Response> conversionCompleted = new CompletableFuture<>();
    okapiClient.post(ApiRoot.dataImportEventHandler(), new JsonObject(), ResponseHandler.any(conversionCompleted));
    Response response = conversionCompleted.get(5, TimeUnit.SECONDS);
    assertThat(response.getStatusCode(), is(204));
  }

  @Test
  public void shouldReturnNoContentOnValidData() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {
    CompletableFuture<Response> conversionCompleted = new CompletableFuture<>();
    okapiClient.post(ApiRoot.dataImportEventHandler(), JsonObject.mapFrom(prepareSnapshot()), ResponseHandler.any(conversionCompleted));
    Response response = conversionCompleted.get(5, TimeUnit.SECONDS);
    assertThat(response.getStatusCode(), is(204));
  }

  private DataImportEventPayload prepareSnapshot() {

    DataImportEventPayload payload = new DataImportEventPayload();
    payload.setJobExecutionId(UUID.randomUUID().toString());
    payload.setEventType("DI_SRS_MARC_BIB_RECORD_CREATED");
    payload.setOkapiUrl(FakeOkapi.getAddress());
    payload.setTenant("diku");
    payload.setToken("token");
    payload.setContext(new HashMap<>());
    payload.getContext().put(EntityType.MARC_BIBLIOGRAPHIC.value(), "{ \"leader\":\"01314nam  22003851a 4500\", \"fields\":[ { \"001\":\"ybp7406411\" } ] }");

    String jobProfileId = UUID.randomUUID().toString();
    String matchProfileId = UUID.randomUUID().toString();
    ProfileSnapshotWrapper root = new ProfileSnapshotWrapper();
    root.setContentType(ProfileSnapshotWrapper.ContentType.JOB_PROFILE);
    root.setId(UUID.randomUUID().toString());
    root.setProfileId(jobProfileId);
    root.setOrder(0);
    root.setContent(new JobProfile()
      .withId(jobProfileId)
      .withDataType(JobProfile.DataType.MARC)
      .withDescription("description")
      .withUserInfo(new UserInfo()
        .withUserName("diku")
        .withFirstName("diku")
        .withLastName("admin"))
      .withName("test"));
    root.setChildSnapshotWrappers(Collections.singletonList(new ProfileSnapshotWrapper()
      .withId(UUID.randomUUID().toString())
      .withContentType(ProfileSnapshotWrapper.ContentType.MATCH_PROFILE)
      .withOrder(0)
      .withProfileId(matchProfileId)
      .withContent(new MatchProfile()
        .withId(matchProfileId)
        .withDescription("description")
        .withExistingRecordType(EntityType.INSTANCE)
        .withIncomingRecordType(EntityType.MARC_BIBLIOGRAPHIC)
        .withName("name")
        .withMatchDetails(Collections.singletonList(new MatchDetail()
          .withIncomingMatchExpression(new MatchExpression()
            .withDataValueType(VALUE_FROM_RECORD)
            .withFields(Collections.singletonList(new Field().withLabel("id").withValue("instance.id"))))
          .withMatchCriterion(EXACTLY_MATCHES)
          .withExistingMatchExpression(new MatchExpression()
            .withDataValueType(VALUE_FROM_RECORD)
            .withFields(Collections.singletonList(new Field().withLabel("id").withValue("instance.id")))
          ))))));
    payload.setProfileSnapshot(root);
    return payload;
  }

}
