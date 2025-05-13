package repository;

import static org.assertj.core.api.Assertions.assertThat;

import models.ApiBridgeConfigurationModel;
import org.junit.Test;
import services.apibridge.FakeApiBridgeService;

public class ApiBridgeRepositoryTest extends ResetPostgres {
  @Test
  public void getBridgeConfigurationById_succeeds() {
    var repo = instanceOf(ApiBridgeRepository.class);

    var model =
        new ApiBridgeConfigurationModel()
            .setHostUri("hostUri")
            .setUriPath("uriPath")
            .setCompatibilityLevel("V1")
            .setAdminName("admin-name-1")
            .setDescription("description-1")
            .setRequestSchema(FakeApiBridgeService.SchemaDatabase.RequestJsonSchema)
            .setRequestSchemaChecksum(
                "dbc97a8ea3051db2b96991dde205cfabda5a29206e2e40394d23600280d1140a")
            .setResponseSchema(FakeApiBridgeService.SchemaDatabase.ResponseJsonSchema)
            .setResponseSchemaChecksum(
                "d2c31050ab0433ff3c74ac5a6ac9204b6e082bac5857dbfb4ffc6d6e3020f2ec")
            .setGlobalBridgeDefinition("{}")
            .setEnabled(true);

    repo.insert(model).toCompletableFuture().join();

    var optionalPersistedModel =
        repo.getBridgeConfigurationById("hostUri", "uriPath", "V1").toCompletableFuture().join();
    assertThat(optionalPersistedModel.isPresent()).isTrue();

    var persistedModel = optionalPersistedModel.get();

    assertThat(persistedModel.getId()).isGreaterThan(0);
    assertThat(persistedModel.getHostUri()).isEqualTo("hostUri");
    assertThat(persistedModel.getUriPath()).isEqualTo("uriPath");
    assertThat(persistedModel.getCompatibilityLevel()).isEqualTo("V1");
    assertThat(persistedModel.getAdminName()).isEqualTo("admin-name-1");
    assertThat(persistedModel.getDescription()).isEqualTo("description-1");
    assertThat(persistedModel.getRequestSchema()).isNotBlank();
    //
    // assertThat(persistedModel.getRequestSchema()).isEqualTo(FakeApiBridgeService.SchemaDatabase.RequestJsonSchema);
    assertThat(persistedModel.getRequestSchemaChecksum())
        .isEqualTo("dbc97a8ea3051db2b96991dde205cfabda5a29206e2e40394d23600280d1140a");
    assertThat(persistedModel.getResponseSchema()).isNotBlank();
    //
    // assertThat(persistedModel.getResponseSchema()).isEqualTo(FakeApiBridgeService.SchemaDatabase.ResponseJsonSchema);
    assertThat(persistedModel.getResponseSchemaChecksum())
        .isEqualTo("d2c31050ab0433ff3c74ac5a6ac9204b6e082bac5857dbfb4ffc6d6e3020f2ec");
    assertThat(persistedModel.getGlobalBridgeDefinition()).isEqualTo("{}");
    assertThat(persistedModel.isEnabled()).isEqualTo(true);
  }

  @Test
  public void insert_persists() {
    var repo = instanceOf(ApiBridgeRepository.class);

    var model =
        new ApiBridgeConfigurationModel()
            .setHostUri("hostUri")
            .setUriPath("uriPath")
            .setCompatibilityLevel("V1")
            .setAdminName("admin-name-1")
            .setDescription("description-1")
            .setRequestSchema(FakeApiBridgeService.SchemaDatabase.RequestJsonSchema)
            .setRequestSchemaChecksum("requestSchemaChecksum")
            .setResponseSchema(FakeApiBridgeService.SchemaDatabase.ResponseJsonSchema)
            .setResponseSchemaChecksum("responseSchemaChecksum")
            .setGlobalBridgeDefinition("{}")
            .setEnabled(true);

    var persistedModel = repo.insert(model).toCompletableFuture().join();

    assertThat(persistedModel.getId()).isGreaterThan(0);
    assertThat(persistedModel.getHostUri()).isEqualTo("hostUri");
    assertThat(persistedModel.getUriPath()).isEqualTo("uriPath");
    assertThat(persistedModel.getCompatibilityLevel()).isEqualTo("V1");
    assertThat(persistedModel.getAdminName()).isEqualTo("admin-name-1");
    assertThat(persistedModel.getDescription()).isEqualTo("description-1");
    assertThat(persistedModel.getRequestSchema())
        .isEqualTo(FakeApiBridgeService.SchemaDatabase.RequestJsonSchema);
    assertThat(persistedModel.getRequestSchemaChecksum()).isEqualTo("requestSchemaChecksum");
    assertThat(persistedModel.getResponseSchema())
        .isEqualTo(FakeApiBridgeService.SchemaDatabase.ResponseJsonSchema);
    assertThat(persistedModel.getResponseSchemaChecksum()).isEqualTo("responseSchemaChecksum");
    assertThat(persistedModel.getGlobalBridgeDefinition()).isEqualTo("{}");
    assertThat(persistedModel.isEnabled()).isEqualTo(true);
  }

  @Test
  public void update_persists() {
    var repo = instanceOf(ApiBridgeRepository.class);

    var model =
        new ApiBridgeConfigurationModel()
            .setHostUri("hostUri")
            .setUriPath("uriPath")
            .setCompatibilityLevel("V1")
            .setAdminName("admin-name-1")
            .setDescription("description-1")
            .setRequestSchema("{}")
            .setRequestSchemaChecksum("requestSchemaChecksum")
            .setResponseSchema("{}")
            .setResponseSchemaChecksum("responseSchemaChecksum")
            .setGlobalBridgeDefinition("{}")
            .setEnabled(true);

    model = repo.insert(model).toCompletableFuture().join();

    model
        .setHostUri("hostUri2")
        .setUriPath("uriPath2")
        .setCompatibilityLevel("V12")
        .setAdminName("admin-name-1")
        .setDescription("description-12")
        .setRequestSchema(FakeApiBridgeService.SchemaDatabase.RequestJsonSchema)
        .setRequestSchemaChecksum("requestSchemaChecksum2")
        .setResponseSchema(FakeApiBridgeService.SchemaDatabase.ResponseJsonSchema)
        .setResponseSchemaChecksum("responseSchemaChecksum2")
        .setGlobalBridgeDefinition("{}")
        .setEnabled(false);

    var persistedModel = repo.update(model).toCompletableFuture().join();

    assertThat(persistedModel.getId()).isEqualTo(model.getId());
    assertThat(persistedModel.getHostUri()).isEqualTo("hostUri2");
    assertThat(persistedModel.getUriPath()).isEqualTo("uriPath2");
    assertThat(persistedModel.getCompatibilityLevel()).isEqualTo("V12");
    assertThat(persistedModel.getAdminName()).isEqualTo("admin-name-1");
    assertThat(persistedModel.getDescription()).isEqualTo("description-12");
    assertThat(persistedModel.getRequestSchema())
        .isEqualTo(FakeApiBridgeService.SchemaDatabase.RequestJsonSchema);
    assertThat(persistedModel.getRequestSchemaChecksum()).isEqualTo("requestSchemaChecksum2");
    assertThat(persistedModel.getResponseSchema())
        .isEqualTo(FakeApiBridgeService.SchemaDatabase.ResponseJsonSchema);
    assertThat(persistedModel.getResponseSchemaChecksum()).isEqualTo("responseSchemaChecksum2");
    assertThat(persistedModel.getGlobalBridgeDefinition()).isEqualTo("{}");
    assertThat(persistedModel.isEnabled()).isEqualTo(false);
  }
}
