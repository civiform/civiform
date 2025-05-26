package services.apibridge;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static play.test.Helpers.fakeApplication;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.ApiBridgeConfigurationModel;
import models.BridgeDefinition;
import models.BridgeDefinitionItem;
import org.junit.Test;
import play.Application;
import repository.ApiBridgeRepository;
import repository.ResetPostgres;
import services.Path;
import services.applicant.ApplicantData;

public class DispatcherTest extends ResetPostgres {
  private static final String BASE_URL = "http://mock-web-services:8000/api-bridge";

  Application app = fakeApplication();

  @Test
  public void run_dispatch() {
    var apiBridgeService = app.injector().instanceOf(ApiBridgeService.class);
    var apiBridgeRepository = app.injector().instanceOf(ApiBridgeRepository.class);
    var dispatcherConfigurationService =
        app.injector().instanceOf(DispatcherConfigurationService.class);

    var item =
        apiBridgeRepository
            .insert(
                new ApiBridgeConfigurationModel()
                    .setHostUri(BASE_URL)
                    .setUriPath("/bridge/success")
                    .setCompatibilityLevel("V1")
                    .setAdminName("admin-name-1")
                    .setDescription("description-1")
                    .setRequestSchema("{}")
                    .setRequestSchemaChecksum("requestSchemaChecksum")
                    .setResponseSchema("{}")
                    .setResponseSchemaChecksum("responseSchemaChecksum")
                    .setGlobalBridgeDefinition("{}")
                    .setEnabled(true))
            .toCompletableFuture()
            .join();

    var applicantData = new ApplicantData();
    applicantData.putString(Path.create("applicant.question_one.text"), "test");

    var dispatcher =
        new Dispatcher(apiBridgeService, apiBridgeRepository, dispatcherConfigurationService);

    var bridgeFields =
        BridgeDefinition.builder()
            .bridgeConfigurationId(item.id)
            .inputFields(
                ImmutableList.of(
                    BridgeDefinitionItem.builder()
                        .questionName("applicant.householdaddress.zip")
                        .externalName("zipCode")
                        .build(),
                    BridgeDefinitionItem.builder()
                        .questionName("applicant.sclaccount.number")
                        .externalName("accountNumber")
                        .build()))
            .outputFields(
                ImmutableList.of(
                    BridgeDefinitionItem.builder()
                        .questionName("applicant.valid_scl_account.selection")
                        .externalName("isValid")
                        .build()))
            .build();

    var result =
        dispatcher
            .dispatch(BASE_URL, "/bridge/success", "V1", applicantData, Optional.of(bridgeFields))
            .toCompletableFuture()
            .join();

    assertThat(result).isNotNull();
  }
}
