package services.apibridge;

import static services.apibridge.ApiBridgeServiceDto.BridgeRequest;
import static services.apibridge.ApiBridgeServiceDto.BridgeResponse;
import static services.apibridge.ApiBridgeServiceDto.CompatibilityLevel;
import static services.apibridge.ApiBridgeServiceDto.DiscoveryResponse;
import static services.apibridge.ApiBridgeServiceDto.Endpoint;
import static services.apibridge.ApiBridgeServiceDto.HealthcheckResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class FakeApiBridgeService implements IApiBridgeService {
  public static class SchemaDatabase {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String RequestJsonSchema =
        """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$id": "https://example.com/schemas/applicant.json",
          "title": "Applicant Information",
          "description": "Schema for housing assistance applicant information",
          "type": "object",
          "properties": {
            "applicantId": {
              "type": "string",
              "description": "Unique identifier for the applicant",
              "pattern": "^APP-\\\\d{3}$"
            },
            "householdSize": {
              "type": "number",
              "description": "Number of people in the household",
              "minimum": 1
            },
            "annualIncome": {
              "type": "number",
              "description": "Annual household income in dollars",
              "minimum": 0
            },
            "currentAddress": {
              "type": "string",
              "description": "Current residential address",
              "minLength": 5
            }
          },
          "required": [
            "applicantId",
            "householdSize",
            "annualIncome",
            "currentAddress"
          ],
          "additionalProperties": false
        }
        """
            .stripIndent();

    public static String ResponseJsonSchema =
        """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$id": "https://civiform.us/schemas/application-status.json",
          "title": "Application Status",
          "description": "Schema for housing application status response",
          "type": "object",
          "properties": {
            "applicationId": {
              "type": "string",
              "description": "Unique identifier for the application",
              "pattern": "^HA-\\\\d{4}-\\\\d{3}$"
            },
            "status": {
              "type": "string",
              "description": "Current status of the application",
              "enum": ["submitted", "processing", "approved", "rejected"]
            },
            "nextSteps": {
              "type": "string",
              "description": "Instructions for what the applicant should do next",
              "minLength": 1
            },
            "processingTimeEstimate": {
              "type": "string",
              "description": "Estimated time for application processing",
              "pattern": "^\\\\d+-\\\\d+\\\\s+\\\\w+\\\\s+\\\\w+$"
            }
          },
          "required": [
            "applicationId",
            "status",
            "nextSteps",
            "processingTimeEstimate"
          ],
          "additionalProperties": false
        }
        """
            .stripIndent();

    public static JsonNode requestSchemaNode() {
      try {
        return mapper.readTree(RequestJsonSchema);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static JsonNode responseSchemaNode() {
      try {
        return mapper.readTree(ResponseJsonSchema);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public CompletionStage<HealthcheckResponse> healthcheck(String hostUri) {
    var response = new HealthcheckResponse(1739599694);

    return CompletableFuture.completedFuture(response);
  }

  public CompletionStage<DiscoveryResponse> discovery(String hostUri) {
    var response =
        new DiscoveryResponse(
            ImmutableMap.of(
                "housing-assistance",
                new Endpoint(
                    CompatibilityLevel.V1,
                    "Submit and process housing assistance applications",
                    "/bridge/housing-assistance",
                    SchemaDatabase.requestSchemaNode(),
                    SchemaDatabase.responseSchemaNode())));

    return CompletableFuture.completedFuture(response);
  }

  public CompletionStage<BridgeResponse> bridge(URI uri, BridgeRequest request) {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    JsonSchema requestSchema =
        factory.getSchema(FakeApiBridgeService.SchemaDatabase.requestSchemaNode());
    ObjectMapper mapper = new ObjectMapper();

    Set<ValidationMessage> validationMessageSet;
    try {
      validationMessageSet =
          requestSchema.validate(
              mapper.readTree(mapper.writeValueAsString(request)).get("payload"));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    if (!validationMessageSet.isEmpty()) {
      var errors =
          validationMessageSet.stream()
              .map(ValidationMessage::getMessage)
              .collect(Collectors.joining(", "));

      throw new RuntimeException("Invalid BridgeRequest data. " + errors);
    }

    var response =
        new BridgeResponse(
            CompatibilityLevel.V1,
            ImmutableMap.of(
                "applicationId", "HA-2024-001",
                "status", "submitted",
                "nextSteps", "Your application is being reviewed",
                "processingTimeEstimate", "5-7 business days"));

    return CompletableFuture.completedFuture(response);
  }
}
