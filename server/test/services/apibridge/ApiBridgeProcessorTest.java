package services.apibridge;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static services.apibridge.ApiBridgeServiceDto.CompatibilityLevel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CompletableFuture;
import junitparams.JUnitParamsRunner;
import models.ApiBridgeConfigurationModel;
import models.ApiBridgeConfigurationModel.ApiBridgeDefinition;
import models.ApiBridgeConfigurationModel.ApiBridgeDefinitionItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import repository.ApiBridgeConfigurationRepository;
import repository.ResetPostgres;
import services.ErrorAnd;
import services.Path;
import services.apibridge.ApiBridgeServiceDto.BridgeResponse;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;

@RunWith(JUnitParamsRunner.class)
public class ApiBridgeProcessorTest extends ResetPostgres {
  private static final String BASE_URL = "http://mock-web-services:8000/api-bridge";

  @Mock private ApiBridgeService apiBridgeService;
  private ApiBridgeConfigurationRepository apiBridgeConfigurationRepository;
  private ApiBridgeProcessor apiBridgeProcessor;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    apiBridgeConfigurationRepository = instanceOf(ApiBridgeConfigurationRepository.class);
    addBridgeConfiguration1();
    addBridgeConfiguration2();
    addBridgeConfiguration3();

    apiBridgeProcessor =
        new ApiBridgeProcessor(
            apiBridgeService,
            apiBridgeConfigurationRepository,
            instanceOf(ApiBridgeExecutionContext.class),
            instanceOf(ObjectMapper.class),
            instanceOf(RequestPayloadMapper.class),
            instanceOf(ResponsePayloadMapper.class));

    when(apiBridgeService.bridge(eq("%s%s".formatted(BASE_URL, SampleBridgeUrl.Success)), any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                ErrorAnd.of(
                    new BridgeResponse(
                        CompatibilityLevel.V1,
                        ImmutableMap.of(
                            SampleExternalName.IsValidName,
                            true,
                            SampleExternalName.AccountNumberName,
                            1234)))));

    when(apiBridgeService.bridge(
            eq("%s%s".formatted(BASE_URL, SampleBridgeUrl.CreditCheck)), any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                ErrorAnd.of(
                    new BridgeResponse(
                        CompatibilityLevel.V1,
                        ImmutableMap.of(
                            SampleExternalName.CreditScoreName,
                            750,
                            SampleExternalName.RiskLevelName,
                            "LOW")))));

    when(apiBridgeService.bridge(
            eq("%s%s".formatted(BASE_URL, SampleBridgeUrl.IncomeVerification)), any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                ErrorAnd.of(
                    new BridgeResponse(
                        CompatibilityLevel.V1,
                        ImmutableMap.of(
                            SampleExternalName.IsVerifiedName,
                            true,
                            SampleExternalName.VerifiedIncomeName,
                            75000)))));
  }

  private void addBridgeConfiguration1() {
    final String requestSchema =
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

    final String responseSchema =
        """
        {
          "$id": "https://civiform.us/schemas/response-schema.json",
          "type": "object",
          "title": "Response title",
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "required": [
            "accountNumber",
            "isValid"
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

    apiBridgeConfigurationRepository
        .insert(
            new ApiBridgeConfigurationModel()
                .setHostUrl(BASE_URL)
                .setUrlPath(SampleBridgeUrl.Success)
                .setCompatibilityLevel(CompatibilityLevel.V1)
                .setAdminName(SampleBridgeAdminName.AccountValidation)
                .setDescription("description-1")
                .setRequestSchema(requestSchema)
                .setRequestSchemaChecksum("requestSchemaChecksum")
                .setResponseSchema(responseSchema)
                .setResponseSchemaChecksum("responseSchemaChecksum")
                .setGlobalBridgeDefinition(
                    new ApiBridgeConfigurationModel.ApiBridgeDefinition(
                        ImmutableList.of(), ImmutableList.of()))
                .setEnabled(true))
        .toCompletableFuture()
        .join();
  }

  private void addBridgeConfiguration2() {
    final String requestSchema =
        """
        {
          "$id": "https://civiform.us/schemas/credit-request.json",
          "type": "object",
          "title": "Credit Check Request",
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "required": [
            "socialSecurityNumber",
            "dateOfBirth"
          ],
          "properties": {
            "socialSecurityNumber": {
              "type": "string",
              "title": "Social Security Number",
              "description": "SSN for credit check"
            },
            "dateOfBirth": {
              "type": "string",
              "title": "Date of Birth",
              "description": "Date of birth in YYYY-MM-DD format"
            }
          },
          "description": "Credit check request schema",
          "additionalProperties": false
        }
        """;

    final String responseSchema =
        """
        {
          "$id": "https://civiform.us/schemas/credit-response.json",
          "type": "object",
          "title": "Credit Check Response",
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "required": [
            "creditScore",
            "riskLevel"
          ],
          "properties": {
            "creditScore": {
              "type": "number",
              "title": "Credit Score",
              "description": "Credit score value"
            },
            "riskLevel": {
              "type": "string",
              "title": "Risk Level",
              "description": "Risk assessment level"
            }
          },
          "description": "Credit check response schema",
          "additionalProperties": false
        }
        """;

    apiBridgeConfigurationRepository
        .insert(
            new ApiBridgeConfigurationModel()
                .setHostUrl(BASE_URL)
                .setUrlPath(SampleBridgeUrl.CreditCheck)
                .setCompatibilityLevel(CompatibilityLevel.V1)
                .setAdminName(SampleBridgeAdminName.CreditCheck)
                .setDescription("Credit check service")
                .setRequestSchema(requestSchema)
                .setRequestSchemaChecksum("creditRequestSchemaChecksum")
                .setResponseSchema(responseSchema)
                .setResponseSchemaChecksum("creditResponseSchemaChecksum")
                .setGlobalBridgeDefinition(
                    new ApiBridgeConfigurationModel.ApiBridgeDefinition(
                        ImmutableList.of(), ImmutableList.of()))
                .setEnabled(true))
        .toCompletableFuture()
        .join();
  }

  private void addBridgeConfiguration3() {
    final String requestSchema =
        """
        {
          "$id": "https://civiform.us/schemas/income-request.json",
          "type": "object",
          "title": "Income Verification Request",
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "required": [
            "employerName",
            "annualSalary"
          ],
          "properties": {
            "employerName": {
              "type": "string",
              "title": "Employer Name",
              "description": "Name of current employer"
            },
            "annualSalary": {
              "type": "number",
              "title": "Annual Salary",
              "description": "Reported annual salary"
            }
          },
          "description": "Income verification request schema",
          "additionalProperties": false
        }
        """;

    final String responseSchema =
        """
        {
          "$id": "https://civiform.us/schemas/income-response.json",
          "type": "object",
          "title": "Income Verification Response",
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "required": [
            "isVerified",
            "verifiedIncome"
          ],
          "properties": {
            "isVerified": {
              "type": "boolean",
              "title": "Is Verified",
              "description": "Whether income was verified"
            },
            "verifiedIncome": {
              "type": "number",
              "title": "Verified Income",
              "description": "Actual verified income amount"
            }
          },
          "description": "Income verification response schema",
          "additionalProperties": false
        }
        """;

    apiBridgeConfigurationRepository
        .insert(
            new ApiBridgeConfigurationModel()
                .setHostUrl(BASE_URL)
                .setUrlPath(SampleBridgeUrl.IncomeVerification)
                .setCompatibilityLevel(CompatibilityLevel.V1)
                .setAdminName(SampleBridgeAdminName.IncomeVerification)
                .setDescription("Income verification service")
                .setRequestSchema(requestSchema)
                .setRequestSchemaChecksum("incomeRequestSchemaChecksum")
                .setResponseSchema(responseSchema)
                .setResponseSchemaChecksum("incomeResponseSchemaChecksum")
                .setGlobalBridgeDefinition(
                    new ApiBridgeConfigurationModel.ApiBridgeDefinition(
                        ImmutableList.of(), ImmutableList.of()))
                .setEnabled(true))
        .toCompletableFuture()
        .join();
  }

  @Test
  public void run_callApiBridgeEndpoints_noBridge() {
    var bridgeFields = new ApiBridgeDefinition(ImmutableList.of(), ImmutableList.of());

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putLong(SamplePath.AccountNumberPath, 1234);
    originalApplicantData.putString(SamplePath.TextPath, "test value");

    // Run
    var resultApplicantData =
        apiBridgeProcessor
            .callApiBridgeEndpoints(
                originalApplicantData,
                ImmutableMap.of(SampleBridgeAdminName.AccountValidation, bridgeFields))
            .toCompletableFuture()
            .join();

    // Verify updated applicant data
    assertThat(originalApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    assertThat(originalApplicantData.readLong(SamplePath.AccountNumberPath)).hasValue(1234L);
    assertThat(originalApplicantData.readString(SamplePath.TextPath)).hasValue("test value");

    // Verify all bridge response is present in result
    assertThat(resultApplicantData).isNotNull();
    // Original applicant data is brought into the new object
    assertThat(resultApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    assertThat(resultApplicantData.readLong(SamplePath.AccountNumberPath)).hasValue(1234L);
    assertThat(resultApplicantData.readString(SamplePath.TextPath)).hasValue("test value");
  }

  @Test
  public void runs_successfully_with_single_bridge() {
    var bridgeFields =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AccountNumberName,
                    Scalar.NUMBER,
                    SampleExternalName.AccountNumberName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putLong(SamplePath.AccountNumberPath, 1234);
    originalApplicantData.putString(SamplePath.TextPath, "test value");

    // Run
    var resultApplicantData =
        apiBridgeProcessor
            .callApiBridgeEndpoints(
                originalApplicantData,
                ImmutableMap.of(SampleBridgeAdminName.AccountValidation, bridgeFields))
            .toCompletableFuture()
            .join();

    // Verify updated applicant data
    assertThat(originalApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    assertThat(originalApplicantData.readLong(SamplePath.AccountNumberPath)).hasValue(1234L);
    assertThat(originalApplicantData.readString(SamplePath.TextPath)).hasValue("test value");
    // New values not added to original applicant data
    assertThat(originalApplicantData.readString(SamplePath.IsValidPath)).isEmpty();

    // Verify all bridge response is present in result
    assertThat(resultApplicantData).isNotNull();
    // Original applicant data is brought into the new object
    assertThat(resultApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    assertThat(resultApplicantData.readLong(SamplePath.AccountNumberPath)).hasValue(1234L);
    assertThat(resultApplicantData.readString(SamplePath.TextPath)).hasValue("test value");
    assertThat(resultApplicantData.readLong(SamplePath.IsValidPath)).hasValue(1L);
  }

  @Test
  public void runs_successfully_with_multiple_bridges() {
    // Define bridge configurations for each bridge
    var accountValidationBridge =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AccountNumberName,
                    Scalar.NUMBER,
                    SampleExternalName.AccountNumberName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var creditCheckBridge =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.SsnName, Scalar.TEXT, SampleExternalName.SsnName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.DateOfBirthName,
                    Scalar.TEXT,
                    SampleExternalName.DateOfBirthName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.CreditScoreName,
                    Scalar.NUMBER,
                    SampleExternalName.CreditScoreName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.RiskLevelName,
                    Scalar.TEXT,
                    SampleExternalName.RiskLevelName)));

    var incomeVerificationBridge =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.EmployerName,
                    Scalar.TEXT,
                    SampleExternalName.EmployerNameName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AnnualSalaryName,
                    Scalar.NUMBER,
                    SampleExternalName.AnnualSalaryName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsVerifiedName,
                    Scalar.SELECTION,
                    SampleExternalName.IsVerifiedName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.VerifiedIncomeName,
                    Scalar.NUMBER,
                    SampleExternalName.VerifiedIncomeName)));

    // Set up applicant data with fields for all bridges
    var originalApplicantData = new ApplicantData();
    // Bridge 1 fields
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putLong(SamplePath.AccountNumberPath, 1234);
    // Bridge 2 fields
    originalApplicantData.putString(SamplePath.SsnPath, "123-45-6789");
    originalApplicantData.putString(SamplePath.DateOfBirthPath, "1990-01-01");
    // Bridge 3 fields
    originalApplicantData.putString(SamplePath.EmployerNamePath, "Tech Corp");
    originalApplicantData.putLong(SamplePath.AnnualSalaryPath, 80000);
    // Additional field
    originalApplicantData.putString(SamplePath.TextPath, "test value");

    // Run
    ApplicantData resultApplicantData =
        apiBridgeProcessor
            .callApiBridgeEndpoints(
                originalApplicantData,
                ImmutableMap.of(
                    SampleBridgeAdminName.AccountValidation, accountValidationBridge,
                    SampleBridgeAdminName.CreditCheck, creditCheckBridge,
                    SampleBridgeAdminName.IncomeVerification, incomeVerificationBridge))
            .toCompletableFuture()
            .join();

    // Verify original applicant data is preserved
    assertThat(originalApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    assertThat(originalApplicantData.readLong(SamplePath.AccountNumberPath)).hasValue(1234L);
    assertThat(originalApplicantData.readString(SamplePath.TextPath)).hasValue("test value");
    assertThat(originalApplicantData.readString(SamplePath.SsnPath)).hasValue("123-45-6789");
    assertThat(originalApplicantData.readString(SamplePath.DateOfBirthPath)).hasValue("1990-01-01");
    assertThat(originalApplicantData.readString(SamplePath.EmployerNamePath)).hasValue("Tech Corp");
    assertThat(originalApplicantData.readLong(SamplePath.AnnualSalaryPath)).hasValue(80000L);

    // Verify updated applicant data
    assertThat(resultApplicantData).isNotNull();
    // Original applicant data is brought into the new object
    assertThat(resultApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    assertThat(resultApplicantData.readLong(SamplePath.AccountNumberPath)).hasValue(1234L);
    assertThat(resultApplicantData.readString(SamplePath.TextPath)).hasValue("test value");
    assertThat(resultApplicantData.readString(SamplePath.SsnPath)).hasValue("123-45-6789");
    assertThat(resultApplicantData.readString(SamplePath.DateOfBirthPath)).hasValue("1990-01-01");
    assertThat(resultApplicantData.readString(SamplePath.EmployerNamePath)).hasValue("Tech Corp");
    assertThat(resultApplicantData.readLong(SamplePath.AnnualSalaryPath)).hasValue(80000L);
    // Account validation results
    assertThat(resultApplicantData.readLong(SamplePath.IsValidPath)).hasValue(1L);
    // Credit check results
    assertThat(resultApplicantData.readLong(SamplePath.CreditScorePath)).hasValue(750L);
    assertThat(resultApplicantData.readString(SamplePath.RiskLevelPath)).hasValue("LOW");
    // Income verification results
    assertThat(resultApplicantData.readLong(SamplePath.IsVerifiedPath)).hasValue(1L);
    assertThat(resultApplicantData.readLong(SamplePath.VerifiedIncomePath)).hasValue(75000L);
  }

  @Test
  public void run_callApiBridgeEndpoints_emptyBridgeDefinitionMap() {
    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putLong(SamplePath.AccountNumberPath, 1234);
    originalApplicantData.putString(SamplePath.TextPath, "test value");

    // Run with completely empty bridge definition map
    var resultApplicantData =
        apiBridgeProcessor
            .callApiBridgeEndpoints(originalApplicantData, ImmutableMap.of())
            .toCompletableFuture()
            .join();

    // Verify original data is returned unchanged
    assertThat(resultApplicantData).isNotNull();
    assertThat(resultApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    assertThat(resultApplicantData.readLong(SamplePath.AccountNumberPath)).hasValue(1234L);
    assertThat(resultApplicantData.readString(SamplePath.TextPath)).hasValue("test value");
  }

  @Test
  public void run_callApiBridgeEndpoints_disabledBridgeConfiguration() {
    // Create a disabled bridge configuration
    final String requestSchema =
        """
        {
          "$id": "https://civiform.us/schemas/disabled-request.json",
          "type": "object",
          "title": "Disabled Request",
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "required": ["testField"],
          "properties": {
            "testField": {
              "type": "string",
              "title": "Test Field"
            }
          }
        }
        """;

    final String responseSchema =
        """
        {
          "$id": "https://civiform.us/schemas/disabled-response.json",
          "type": "object",
          "title": "Disabled Response",
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "required": ["resultField"],
          "properties": {
            "resultField": {
              "type": "string",
              "title": "Result Field"
            }
          }
        }
        """;

    apiBridgeConfigurationRepository
        .insert(
            new ApiBridgeConfigurationModel()
                .setHostUrl(BASE_URL)
                .setUrlPath("/disabled-bridge")
                .setCompatibilityLevel(CompatibilityLevel.V1)
                .setAdminName("disabled-bridge-service")
                .setDescription("Disabled bridge service")
                .setRequestSchema(requestSchema)
                .setRequestSchemaChecksum("disabledRequestChecksum")
                .setResponseSchema(responseSchema)
                .setResponseSchemaChecksum("disabledResponseChecksum")
                .setGlobalBridgeDefinition(
                    new ApiBridgeConfigurationModel.ApiBridgeDefinition(
                        ImmutableList.of(), ImmutableList.of()))
                .setEnabled(false)) // This bridge is disabled
        .toCompletableFuture()
        .join();

    var bridgeFields =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem("test-question", Scalar.TEXT, "testField")),
            ImmutableList.of(
                new ApiBridgeDefinitionItem("result-question", Scalar.TEXT, "resultField")));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(Path.create("applicant.test-question.text"), "test value");

    // Run
    var resultApplicantData =
        apiBridgeProcessor
            .callApiBridgeEndpoints(
                originalApplicantData, ImmutableMap.of("disabled-bridge-service", bridgeFields))
            .toCompletableFuture()
            .join();

    // Verify that disabled bridge was not called and original data is returned
    assertThat(resultApplicantData).isNotNull();
    assertThat(resultApplicantData.readString(Path.create("applicant.test-question.text")))
        .hasValue("test value");
    // Result field should not be present since bridge was disabled
    assertThat(resultApplicantData.readString(Path.create("applicant.result-question.text")))
        .isEmpty();
  }

  @Test
  public void run_callApiBridgeEndpoints_bridgeConfigurationNotFound() {
    var bridgeFields =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");

    // Run with a bridge admin name that doesn't exist in the repository
    var resultApplicantData =
        apiBridgeProcessor
            .callApiBridgeEndpoints(
                originalApplicantData, ImmutableMap.of("non-existent-bridge-service", bridgeFields))
            .toCompletableFuture()
            .join();

    // Verify original data is returned unchanged since no matching config was found
    assertThat(resultApplicantData).isNotNull();
    assertThat(resultApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    // No bridge response should be present
    assertThat(resultApplicantData.readString(SamplePath.IsValidPath)).isEmpty();
  }

  @Test
  public void run_callApiBridgeEndpoints_mixedEnabledAndDisabledBridges() {
    // Disable one of the existing bridge configurations
    apiBridgeConfigurationRepository
        .findAll()
        .thenApply(
            configs -> {
              var creditCheckConfig =
                  configs.stream()
                      .filter(
                          config -> config.adminName().equals(SampleBridgeAdminName.CreditCheck))
                      .findFirst()
                      .orElseThrow();

              return apiBridgeConfigurationRepository
                  .update(creditCheckConfig.setEnabled(false))
                  .toCompletableFuture()
                  .join();
            })
        .toCompletableFuture()
        .join();

    // Define bridge configurations for multiple bridges
    var accountValidationBridge =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AccountNumberName,
                    Scalar.NUMBER,
                    SampleExternalName.AccountNumberName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var creditCheckBridge =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.SsnName, Scalar.TEXT, SampleExternalName.SsnName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.CreditScoreName,
                    Scalar.NUMBER,
                    SampleExternalName.CreditScoreName)));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putLong(SamplePath.AccountNumberPath, 1234);
    originalApplicantData.putString(SamplePath.SsnPath, "123-45-6789");

    // Run
    var resultApplicantData =
        apiBridgeProcessor
            .callApiBridgeEndpoints(
                originalApplicantData,
                ImmutableMap.of(
                    SampleBridgeAdminName.AccountValidation, accountValidationBridge,
                    SampleBridgeAdminName.CreditCheck, creditCheckBridge))
            .toCompletableFuture()
            .join();

    // Verify original data is preserved
    assertThat(resultApplicantData).isNotNull();
    assertThat(resultApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    assertThat(resultApplicantData.readLong(SamplePath.AccountNumberPath)).hasValue(1234L);
    assertThat(resultApplicantData.readString(SamplePath.SsnPath)).hasValue("123-45-6789");

    // Only account validation results should be present (enabled bridge)
    assertThat(resultApplicantData.readLong(SamplePath.IsValidPath)).hasValue(1L);

    // Credit check results should NOT be present (disabled bridge)
    assertThat(resultApplicantData.readLong(SamplePath.CreditScorePath)).isEmpty();
  }

  @Test
  public void run_callApiBridgeEndpoints_bridgeServiceError() {
    // Mock the bridge service to return an error
    when(apiBridgeService.bridge(eq("%s%s".formatted(BASE_URL, SampleBridgeUrl.Success)), any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                ErrorAnd.error(
                    ImmutableSet.of(
                        new ApiBridgeServiceDto.ProblemDetail(
                            "Test error message", "", 1, "", "")))));

    var bridgeFields =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AccountNumberName,
                    Scalar.NUMBER,
                    SampleExternalName.AccountNumberName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putLong(SamplePath.AccountNumberPath, 1234);

    // Run
    assertThatThrownBy(
            () ->
                apiBridgeProcessor
                    .callApiBridgeEndpoints(
                        originalApplicantData,
                        ImmutableMap.of(SampleBridgeAdminName.AccountValidation, bridgeFields))
                    .toCompletableFuture()
                    .join())
        .hasCauseInstanceOf(ApiBridgeProcessingException.class);
  }

  @Test
  public void run_callApiBridgeEndpoints_allBridgesDisabled() {
    // Disable all existing bridge configurations
    apiBridgeConfigurationRepository
        .findAll()
        .thenApply(
            configs -> {
              configs.forEach(
                  config -> {
                    apiBridgeConfigurationRepository
                        .update(config.setEnabled(false))
                        .toCompletableFuture()
                        .join();
                  });
              return null;
            })
        .toCompletableFuture()
        .join();

    var bridgeFields =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putString(SamplePath.TextPath, "test value");

    // Run
    var resultApplicantData =
        apiBridgeProcessor
            .callApiBridgeEndpoints(
                originalApplicantData,
                ImmutableMap.of(SampleBridgeAdminName.AccountValidation, bridgeFields))
            .toCompletableFuture()
            .join();

    // Verify original data is returned unchanged since all bridges are disabled
    assertThat(resultApplicantData).isNotNull();
    assertThat(resultApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    assertThat(resultApplicantData.readString(SamplePath.TextPath)).hasValue("test value");
    // No bridge response should be present
    assertThat(resultApplicantData.readString(SamplePath.IsValidPath)).isEmpty();
  }

  @Test
  public void run_callApiBridgeEndpoints_invalidRequestPayload_missingRequiredField() {
    var bridgeFields =
        new ApiBridgeDefinition(
            ImmutableList.of(
                // Only providing zipCode, missing accountNumber which is required
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    // accountNumber is missing - request validation should fail

    // Run
    var resultApplicantData =
        apiBridgeProcessor
            .callApiBridgeEndpoints(
                originalApplicantData,
                ImmutableMap.of(SampleBridgeAdminName.AccountValidation, bridgeFields))
            .toCompletableFuture()
            .join();

    // Verify original data is returned unchanged since request validation failed
    assertThat(resultApplicantData).isNotNull();
    assertThat(resultApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    // No bridge response should be present since bridge was not called
    assertThat(resultApplicantData.readString(SamplePath.IsValidPath)).isEmpty();
  }

  @Test
  public void run_callApiBridgeEndpoints_invalidRequestPayload_wrongDataType() {
    var bridgeFields =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName),
                // accountNumber expects number but we'll provide a text field
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.TextPath,
                    Scalar.TEXT,
                    SampleExternalName.AccountNumberName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putString(SamplePath.TextPath, "not a number");

    // Run
    var resultApplicantData =
        apiBridgeProcessor
            .callApiBridgeEndpoints(
                originalApplicantData,
                ImmutableMap.of(SampleBridgeAdminName.AccountValidation, bridgeFields))
            .toCompletableFuture()
            .join();

    // Verify original data is returned unchanged since request validation failed
    assertThat(resultApplicantData).isNotNull();
    assertThat(resultApplicantData.readString(SamplePath.AddressZipPath)).hasValue("99999");
    assertThat(resultApplicantData.readString(SamplePath.TextPath)).hasValue("not a number");
    // No bridge response should be present since bridge was not called
    assertThat(resultApplicantData.readString(SamplePath.IsValidPath)).isEmpty();
  }

  @Test
  public void run_callApiBridgeEndpoints_invalidResponsePayload_missingRequiredField() {
    // Mock the bridge service to return a response missing required field
    when(apiBridgeService.bridge(eq("%s%s".formatted(BASE_URL, SampleBridgeUrl.Success)), any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                ErrorAnd.of(
                    new BridgeResponse(
                        CompatibilityLevel.V1,
                        ImmutableMap.of(
                            // Missing isValid field which is required
                            SampleExternalName.AccountNumberName, 1234)))));

    var bridgeFields =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AccountNumberName,
                    Scalar.NUMBER,
                    SampleExternalName.AccountNumberName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putLong(SamplePath.AccountNumberPath, 1234);

    // Run - should throw exception due to invalid response
    assertThatThrownBy(
            () ->
                apiBridgeProcessor
                    .callApiBridgeEndpoints(
                        originalApplicantData,
                        ImmutableMap.of(SampleBridgeAdminName.AccountValidation, bridgeFields))
                    .toCompletableFuture()
                    .join())
        .hasCauseInstanceOf(ApiBridgeProcessingException.class)
        .hasMessageContaining("Response payload does not match schema");
  }

  @Test
  public void run_callApiBridgeEndpoints_invalidResponsePayload_wrongDataType() {
    // Mock the bridge service to return a response with wrong data type
    when(apiBridgeService.bridge(eq("%s%s".formatted(BASE_URL, SampleBridgeUrl.Success)), any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                ErrorAnd.of(
                    new BridgeResponse(
                        CompatibilityLevel.V1,
                        ImmutableMap.of(
                            SampleExternalName.IsValidName,
                            "not a boolean", // Should be boolean
                            SampleExternalName.AccountNumberName,
                            1234)))));

    var bridgeFields =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AccountNumberName,
                    Scalar.NUMBER,
                    SampleExternalName.AccountNumberName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putLong(SamplePath.AccountNumberPath, 1234);

    // Run - should throw exception due to invalid response
    assertThatThrownBy(
            () ->
                apiBridgeProcessor
                    .callApiBridgeEndpoints(
                        originalApplicantData,
                        ImmutableMap.of(SampleBridgeAdminName.AccountValidation, bridgeFields))
                    .toCompletableFuture()
                    .join())
        .hasCauseInstanceOf(ApiBridgeProcessingException.class)
        .hasMessageContaining("Response payload does not match schema");
  }

  @Test
  public void run_callApiBridgeEndpoints_invalidResponsePayload_additionalProperties() {
    // Mock the bridge service to return a response with additional properties
    when(apiBridgeService.bridge(eq("%s%s".formatted(BASE_URL, SampleBridgeUrl.Success)), any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                ErrorAnd.of(
                    new BridgeResponse(
                        CompatibilityLevel.V1,
                        ImmutableMap.of(
                            SampleExternalName.IsValidName,
                            true,
                            SampleExternalName.AccountNumberName,
                            1234,
                            "unexpectedField",
                            "unexpected value")))));

    var bridgeFields =
        new ApiBridgeDefinition(
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AddressName, Scalar.ZIP, SampleExternalName.ZipCodeName),
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.AccountNumberName,
                    Scalar.NUMBER,
                    SampleExternalName.AccountNumberName)),
            ImmutableList.of(
                new ApiBridgeDefinitionItem(
                    SampleQuestionName.IsValidName,
                    Scalar.SELECTION,
                    SampleExternalName.IsValidName)));

    var originalApplicantData = new ApplicantData();
    originalApplicantData.putString(SamplePath.AddressZipPath, "99999");
    originalApplicantData.putLong(SamplePath.AccountNumberPath, 1234);

    // Run - should throw exception due to additional properties not allowed
    assertThatThrownBy(
            () ->
                apiBridgeProcessor
                    .callApiBridgeEndpoints(
                        originalApplicantData,
                        ImmutableMap.of(SampleBridgeAdminName.AccountValidation, bridgeFields))
                    .toCompletableFuture()
                    .join())
        .hasCauseInstanceOf(ApiBridgeProcessingException.class)
        .hasMessageContaining("Response payload does not match schema");
  }

  private static class SampleBridgeAdminName {
    static String AccountValidation = "account-validation-service";
    static String CreditCheck = "credit-check-service";
    static String IncomeVerification = "income-verification-service";
  }

  private static class SampleBridgeUrl {
    static String Success = "/bridge/success";
    static String CreditCheck = "/bridge/credit-check";
    static String IncomeVerification = "/bridge/income-verification";
  }

  private static class SampleExternalName {
    static String ZipCodeName = "zipCode";
    static String IsValidName = "isValid";
    static String AccountNumberName = "accountNumber";
    static String SsnName = "socialSecurityNumber";
    static String DateOfBirthName = "dateOfBirth";
    static String CreditScoreName = "creditScore";
    static String RiskLevelName = "riskLevel";
    static String EmployerNameName = "employerName";
    static String AnnualSalaryName = "annualSalary";
    static String IsVerifiedName = "isVerified";
    static String VerifiedIncomeName = "verifiedIncome";
  }

  private static class SampleQuestionName {
    static String TextPath = "text-question";
    static String AddressName = "address-question";
    static String AccountNumberName = "account-number-question";
    static String IsValidName = "is-valid-question";
    static String SsnName = "ssn-question";
    static String DateOfBirthName = "dob-question";
    static String CreditScoreName = "credit-score-question";
    static String RiskLevelName = "risk-level-question";
    static String EmployerName = "employer-question";
    static String AnnualSalaryName = "salary-question";
    static String IsVerifiedName = "verified-question";
    static String VerifiedIncomeName = "verified-income-question";
  }

  private static class SamplePath {
    static Path TextPath = Path.create("applicant.%s.text".formatted(SampleQuestionName.TextPath));
    static Path AddressZipPath =
        Path.create("applicant.%s.zip".formatted(SampleQuestionName.AddressName));
    static Path AccountNumberPath =
        Path.create("applicant.%s.number".formatted(SampleQuestionName.AccountNumberName));
    static Path IsValidPath =
        Path.create("applicant.%s.selection".formatted(SampleQuestionName.IsValidName));
    static Path SsnPath = Path.create("applicant.%s.text".formatted(SampleQuestionName.SsnName));
    static Path DateOfBirthPath =
        Path.create("applicant.%s.text".formatted(SampleQuestionName.DateOfBirthName));
    static Path CreditScorePath =
        Path.create("applicant.%s.number".formatted(SampleQuestionName.CreditScoreName));
    static Path RiskLevelPath =
        Path.create("applicant.%s.text".formatted(SampleQuestionName.RiskLevelName));
    static Path EmployerNamePath =
        Path.create("applicant.%s.text".formatted(SampleQuestionName.EmployerName));
    static Path AnnualSalaryPath =
        Path.create("applicant.%s.number".formatted(SampleQuestionName.AnnualSalaryName));
    static Path IsVerifiedPath =
        Path.create("applicant.%s.selection".formatted(SampleQuestionName.IsVerifiedName));
    static Path VerifiedIncomePath =
        Path.create("applicant.%s.number".formatted(SampleQuestionName.VerifiedIncomeName));
  }
}
