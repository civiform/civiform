package repository;

import static models.ApiBridgeConfigurationModel.ApiBridgeDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static services.apibridge.ApiBridgeServiceDto.CompatibilityLevel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import models.ApiBridgeConfigurationModel;
import org.junit.Before;
import org.junit.Test;
import services.applicant.question.Scalar;

public class ApiBridgeConfigurationRepositoryTest extends ResetPostgres {

  private ApiBridgeConfigurationRepository repo;

  @Before
  public void setUp() {
    repo = instanceOf(ApiBridgeConfigurationRepository.class);
  }

  @Test
  public void findAll_succeeds() {
    repo.insert(createBridgeConfigurationModel().setAdminName("admin-name-1"))
        .toCompletableFuture()
        .join();
    repo.insert(
            createBridgeConfigurationModel().setAdminName("admin-name-2").setUrlPath("urlPath2"))
        .toCompletableFuture()
        .join();
    repo.insert(
            createBridgeConfigurationModel().setAdminName("admin-name-3").setUrlPath("urlPath3"))
        .toCompletableFuture()
        .join();

    var model = repo.findAll().toCompletableFuture().join();
    assertThat(model).hasSize(3);
  }

  @Test
  public void findAllEnabledByAdminNames_succeeds() {
    repo.insert(createBridgeConfigurationModel().setAdminName("admin-name-1"))
        .toCompletableFuture()
        .join();
    repo.insert(
            createBridgeConfigurationModel().setAdminName("admin-name-2").setUrlPath("urlPath2"))
        .toCompletableFuture()
        .join();
    repo.insert(
            createBridgeConfigurationModel()
                .setAdminName("admin-name-3")
                .setUrlPath("urlPath3")
                .setEnabled(false))
        .toCompletableFuture()
        .join();

    var adminNames = ImmutableSet.of("admin-name-1", "admin-name-2", "admin-name-3");

    var model = repo.findAllEnabledByAdminNames(adminNames).toCompletableFuture().join();
    assertThat(model).hasSize(2);
  }

  @Test
  public void findByHostUrl_succeeds() {
    repo.insert(
            createBridgeConfigurationModel()
                .setAdminName("admin-name-1")
                .setHostUrl("dontDontThis"))
        .toCompletableFuture()
        .join();
    repo.insert(
            createBridgeConfigurationModel().setAdminName("admin-name-2").setUrlPath("urlPath2"))
        .toCompletableFuture()
        .join();
    repo.insert(
            createBridgeConfigurationModel().setAdminName("admin-name-3").setUrlPath("urlPath3"))
        .toCompletableFuture()
        .join();

    var model = repo.findByHostUrl("hostUrl").toCompletableFuture().join();
    assertThat(model).hasSize(2);
  }

  @Test
  public void insert_persists() {
    ApiBridgeConfigurationModel model =
        repo.insert(createBridgeConfigurationModel()).toCompletableFuture().join();
    ApiBridgeConfigurationModel persistedModel = refetchModelFromDb(model);
    assertThat(persistedModel).usingRecursiveComparison().isEqualTo(model);
  }

  @Test
  public void insert_fails_with_invalid_admin_name() {
    assertThatThrownBy(
            () -> {
              repo.insert(createBridgeConfigurationModel().setAdminName("1Hello"))
                  .toCompletableFuture()
                  .join();
            })
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void update_persists() {
    ApiBridgeConfigurationModel model =
        repo.insert(createBridgeConfigurationModel()).toCompletableFuture().join();

    model
        .setHostUrl("hostUrl2")
        .setUrlPath("urlPath2")
        .setCompatibilityLevel(CompatibilityLevel.V1)
        .setAdminName("admin-name-1")
        .setDescription("description-12")
        .setRequestSchema("{}")
        .setRequestSchemaChecksum("requestSchemaChecksum2")
        .setResponseSchema("{}")
        .setResponseSchemaChecksum("responseSchemaChecksum2")
        .setGlobalBridgeDefinition(
            new ApiBridgeDefinition(
                ImmutableList.of(
                    new ApiBridgeConfigurationModel.ApiBridgeDefinitionItem(
                        "questionName1", Scalar.TEXT, "externalName1")),
                ImmutableList.of()))
        .setEnabled(false);

    repo.update(model).toCompletableFuture().join();

    ApiBridgeConfigurationModel persistedModel = refetchModelFromDb(model);
    assertThat(persistedModel).usingRecursiveComparison().isEqualTo(model);
  }

  @Test
  public void delete_persists() {
    ApiBridgeConfigurationModel model =
        repo.insert(createBridgeConfigurationModel()).toCompletableFuture().join();

    boolean wasDeleted = repo.delete(model.id()).toCompletableFuture().join();
    assertThat(wasDeleted).isTrue();

    Optional<ApiBridgeConfigurationModel> optionalPersistedModel =
        repo.findByHostUrlAndUrlPathAndCompatibilityLevel(
                model.hostUrl(), model.urlPath(), model.compatibilityLevel())
            .toCompletableFuture()
            .join();
    assertThat(optionalPersistedModel.isPresent()).isFalse();
  }

  @Test
  public void delete_returns_false_when_id_to_delete_does_not_exist() {
    boolean wasDeleted = repo.delete(Long.MAX_VALUE).toCompletableFuture().join();
    assertThat(wasDeleted).isFalse();
  }

  private ApiBridgeDefinition createBridgeDefinitions() {
    return new ApiBridgeDefinition(
        ImmutableList.of(
            new ApiBridgeConfigurationModel.ApiBridgeDefinitionItem(
                "questionName1", Scalar.TEXT, "externalName1"),
            new ApiBridgeConfigurationModel.ApiBridgeDefinitionItem(
                "questionName2", Scalar.TEXT, "externalName2")),
        ImmutableList.of(
            new ApiBridgeConfigurationModel.ApiBridgeDefinitionItem(
                "questionName3", Scalar.TEXT, "externalName3")));
  }

  private ApiBridgeConfigurationModel createBridgeConfigurationModel() {
    return new ApiBridgeConfigurationModel()
        .setHostUrl("hostUrl")
        .setUrlPath("urlPath")
        .setCompatibilityLevel(CompatibilityLevel.V1)
        .setAdminName("admin-name-1")
        .setDescription("description-1")
        .setRequestSchema("{}")
        .setRequestSchemaChecksum("requestSchemaChecksum")
        .setResponseSchema("{}")
        .setResponseSchemaChecksum("responseSchemaChecksum")
        .setGlobalBridgeDefinition(createBridgeDefinitions())
        .setEnabled(true);
  }

  // Calling this exercises the findByHostUrlAndUrlPathAndCompatibilityLevel method
  // which is why it doesn't have a separate test
  private ApiBridgeConfigurationModel refetchModelFromDb(ApiBridgeConfigurationModel model) {
    Optional<ApiBridgeConfigurationModel> optionalPersistedModel =
        repo.findByHostUrlAndUrlPathAndCompatibilityLevel(
                model.hostUrl(), model.urlPath(), model.compatibilityLevel())
            .toCompletableFuture()
            .join();

    assertThat(optionalPersistedModel.isPresent()).isTrue();

    return optionalPersistedModel.get();
  }
}
