package controllers.admin.apibridge;

import static org.assertj.core.api.Assertions.assertThat;
import static play.inject.Bindings.bind;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import controllers.WithMockedProfiles;
import models.ApiBridgeConfigurationModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import repository.ApiBridgeConfigurationRepository;
import services.apibridge.ApiBridgeServiceDto;
import services.applicant.question.Scalar;
import support.ProgramBuilder;

public class ProgramBridgeControllerTest extends WithMockedProfiles {
  private ProgramBridgeController controller;
  private ApiBridgeConfigurationRepository bridgeConfigurationRepository;
  private Config config =
      ConfigFactory.parseMap(
              ImmutableMap.<String, String>builder()
                  .put("api_bridge_enabled", "true")
                  .buildKeepingLast())
          .withFallback(ConfigFactory.load());

  @Before
  public void setup() {
    resetDatabase();
    createGlobalAdminWithMockedProfile();
    setupInjectorWithExtraBinding(bind(Config.class).toInstance(config));
    controller = instanceOf(ProgramBridgeController.class);
    bridgeConfigurationRepository = instanceOf(ApiBridgeConfigurationRepository.class);
  }

  @Test
  public void edit_programNotFound() {
    var request = fakeRequestBuilder().build();
    var response = controller.edit(request, -1L).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(404);
  }

  @Test
  public void edit_programNotDraft() {
    ProgramModel program = ProgramBuilder.newActiveProgram("program-name-1").build();

    var request = fakeRequestBuilder().build();
    var response = controller.edit(request, program.id).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(400);
    assertThat(contentAsString(response)).contains("Editing must be done with a draft program");
  }

  @Test
  public void edit_returnSuccess() {
    ProgramModel program = ProgramBuilder.newDraftProgram("program-name-1").build();
    bridgeConfigurationRepository.insert(createApiBridgeConfigurationModel());

    var request = fakeRequestBuilder().build();
    var response = controller.edit(request, program.id).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
  }

  @Test
  public void hxBridgeConfiguration_programNotFound() {
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("bridgeAdminName", "bridge-success")).build();
    var response = controller.hxBridgeConfiguration(request, -1L).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(404);
  }

  @Test
  public void hxBridgeConfiguration_programNotDraft() {
    ProgramModel program = ProgramBuilder.newActiveProgram("program-name-1").build();
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("bridgeAdminName", "bridge-success")).build();

    var response =
        controller.hxBridgeConfiguration(request, program.id).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(400);
    assertThat(contentAsString(response)).contains("Editing must be done with a draft program");
  }

  @Test
  public void hxBridgeConfiguration_selectEmptyBridgeOptionClearsForm() {
    ProgramModel program = ProgramBuilder.newDraftProgram("program-name-1").build();

    var request = fakeRequestBuilder().bodyForm(ImmutableMap.of()).build();
    var response =
        controller.hxBridgeConfiguration(request, program.id).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).isEqualTo("");
  }

  @Test
  public void hxBridgeConfiguration_bridgeConfigurationNotFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram("program-name-1").build();

    var request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of("bridgeAdminName", "non-existent-bridge"))
            .build();
    var response =
        controller.hxBridgeConfiguration(request, program.id).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(404);
  }

  @Test
  public void hxBridgeConfiguration_successWithExistingBridgeDefinition() {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("program-name-1")
            .withBridgeDefinitions(
                ImmutableMap.of(
                    "bridgeAdminName",
                    new ApiBridgeConfigurationModel.ApiBridgeDefinition(
                        ImmutableList.of(), ImmutableList.of())))
            .build();

    ApiBridgeConfigurationModel bridgeConfig = createApiBridgeConfigurationModel();
    bridgeConfigurationRepository.insert(bridgeConfig);

    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("bridgeAdminName", "bridge-success")).build();
    var response =
        controller.hxBridgeConfiguration(request, program.id).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("Input Fields");
  }

  @Test
  public void hxBridgeConfiguration_successWithoutExistingBridgeDefinition() {
    ProgramModel program = ProgramBuilder.newDraftProgram("program-name-1").build();
    ApiBridgeConfigurationModel bridgeConfig = createApiBridgeConfigurationModel();
    bridgeConfigurationRepository.insert(bridgeConfig);

    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("bridgeAdminName", "bridge-success")).build();
    var response =
        controller.hxBridgeConfiguration(request, program.id).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("Input Fields");
  }

  @Test
  public void save_programNotFound() {
    var request =
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(
                ImmutableMap.of(
                    "bridgeAdminName", "bridge-success",
                    "inputFields[0].questionName", "question1",
                    "inputFields[0].questionScalar", "TEXT",
                    "inputFields[0].externalName", "external1"))
            .build();

    var response = controller.save(request, -1L);
    assertThat(response.status()).isEqualTo(404);
  }

  @Test
  public void save_programNotDraft() {
    ProgramModel program = ProgramBuilder.newActiveProgram("program-name-1").build();

    var request =
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(
                ImmutableMap.of(
                    "bridgeAdminName", "bridge-success",
                    "inputFields[0].questionName", "question1",
                    "inputFields[0].questionScalar", "TEXT",
                    "inputFields[0].externalName", "external1"))
            .build();

    var response = controller.save(request, program.id);
    assertThat(response.status()).isEqualTo(400);
    assertThat(contentAsString(response)).contains("Editing must be done with a draft program");
  }

  @Test
  public void save_successfulSave() {
    ProgramModel program = ProgramBuilder.newDraftProgram("program-name-1").build();
    ApiBridgeConfigurationModel bridgeConfig = createApiBridgeConfigurationModel();
    bridgeConfigurationRepository.insert(bridgeConfig);

    var request =
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put("bridgeAdminName", "bridge-success")
                    .put("inputFields[0].questionName", "question1")
                    .put("inputFields[0].questionScalar", "TEXT")
                    .put("inputFields[0].externalName", "accountNumber")
                    .put("outputFields[0].questionName", "question2")
                    .put("outputFields[0].questionScalar", "SELECTION")
                    .put("outputFields[0].externalName", "isValid")
                    .buildKeepingLast())
            .build();
    var response = controller.save(request, program.id);

    assertThat(response.status()).isEqualTo(303);
    assertThat(response.redirectLocation()).isPresent();
  }

  @Test
  public void save_emptyForm() {
    ProgramModel program = ProgramBuilder.newDraftProgram("program-name-1").build();

    var request = fakeRequestBuilder().build();
    var response = controller.save(request, program.id);

    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("required");
  }

  private ApiBridgeConfigurationModel createApiBridgeConfigurationModel() {
    String requestSchema =
        """
        {
          "$id": "https://civiform.us/schemas/request.json",
          "type": "object",
          "title": "Response title",
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "required": [
            "accountNumber",
            "zipCode"
          ],
          "properties": {
            "zipCode": {
              "type": "string",
              "title": "ZIP Code",
              "description": "ZIP Code description"
            },
            "accountNumber": {
              "type": "number",
              "title": "Account Number",
              "description": "Account Number"
            }
          },
          "description": "Request schema description",
          "additionalProperties": false
        }
        """;

    String reponseSchema =
        """
        {
          "$id": "https://civiform.us/schemas/response-schema.json",
          "type": "object",
          "title": "Response title",
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "required": [
            "accountNumber, isValid"
          ],
          "properties": {
            "isValid": {
              "type": "boolean",
              "title": "Is Valid",
              "description": "Has valid account"
            },
            "accountNumber": {
              "type": "number",
              "title": "Account Number",
              "description": "Account Number"
            }
          },
          "description": "Response schema description",
          "additionalProperties": false
        }
        """;

    return new ApiBridgeConfigurationModel()
        .setHostUrl("http://mock-web-services:8000/api-bridge")
        .setUrlPath("/bridge/success")
        .setCompatibilityLevel(ApiBridgeServiceDto.CompatibilityLevel.V1)
        .setAdminName("bridge-success")
        .setDescription("description-1")
        .setRequestSchema(requestSchema)
        .setRequestSchemaChecksum("requestSchemaChecksum")
        .setResponseSchema(reponseSchema)
        .setResponseSchemaChecksum("responseSchemaChecksum")
        .setEnabled(true)
        .setGlobalBridgeDefinition(
            new ApiBridgeConfigurationModel.ApiBridgeDefinition(
                ImmutableList.of(
                    new ApiBridgeConfigurationModel.ApiBridgeDefinitionItem(
                        "questionName1", Scalar.TEXT, "accountNumber"),
                    new ApiBridgeConfigurationModel.ApiBridgeDefinitionItem(
                        "questionName2", Scalar.TEXT, "zipCode")),
                ImmutableList.of(
                    new ApiBridgeConfigurationModel.ApiBridgeDefinitionItem(
                        "questionName3", Scalar.TEXT, "accountNumber"),
                    new ApiBridgeConfigurationModel.ApiBridgeDefinitionItem(
                        "questionName4", Scalar.SELECTION, "isValid"))));
  }
}
