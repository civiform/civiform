package services.openapi.v2;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.dev.seeding.SampleQuestionDefinitions;
import java.util.Locale;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import models.ApplicationStep;
import models.CategoryModel;
import models.DisplayMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.LocalizedStrings;
import services.openapi.OpenApiSchemaSettings;
import services.openapi.v2.serializers.OpenApiSerializationAsserter;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.types.QuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class Swagger2SchemaGeneratorTest extends OpenApiSerializationAsserter {
  private static final Stream<QuestionDefinition> ALL_SAMPLE_QUESTION_DEFINITIONS_WITH_IDS_STREAM =
      SampleQuestionDefinitions.ALL_SAMPLE_QUESTION_DEFINITIONS.stream()
          .map(QuestionDefinition::withPopulatedTestId);

  @Test
  public void createSchema_withNoPrograms() {
    ImmutableList<BlockDefinition> blockDefinitions =
        ImmutableList.of(
            BlockDefinition.builder()
                .setId(135L)
                .setName("Test Block Definition")
                .setDescription("Test Block Description")
                .setLocalizedName(LocalizedStrings.builder().build())
                .setLocalizedDescription(LocalizedStrings.builder().build())
                .build());

    ProgramDefinition programDefinition =
        ProgramDefinition.builder()
            .setId(789L)
            .setAdminName("test-program-admin-name")
            .setAdminDescription("Test Admin Description")
            .setExternalLink("https://mytestlink.gov")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(false)
            .setAcls(new ProgramAcls())
            .setBlockDefinitions(blockDefinitions)
            .setApplicationSteps(
                ImmutableList.<ApplicationStep>builder()
                    .add(new ApplicationStep("step-1-title", "step-1-description"))
                    .build())
            .setCategories(
                ImmutableList.<CategoryModel>builder()
                    .add(new CategoryModel(ImmutableMap.<Locale, String>builder().build()))
                    .build())
            .build();

    OpenApiSchemaSettings settings =
        new OpenApiSchemaSettings("baseUrl", "email123@example.com", true);

    var generator = new Swagger2SchemaGenerator(settings);
    String actual = generator.createSchema(programDefinition);

    String expected =
        """
---
swagger: "2.0"
info:
  description: "Test Admin Description"
  version: "789"
  title: "test-program-admin-name"
  contact:
    name: "CiviForm Technical Support"
    email: "email123@example.com"
host: "baseUrl"
basePath: "/api/v1/admin/programs/test-program-admin-name"
schemes:
- "http"
- "https"
security:
- basicAuth: []
paths:
  /applications:
    get:
      tags:
      - "programs"
      summary: "Export applications"
      description: "List Applications"
      operationId: "list_applications"
      produces:
      - "application/json"
      parameters:
      - name: "fromDate"
        in: "query"
        description: "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results\\
          \\ to applications submitted on or after the provided date, in the CiviForm\\
          \\ instance's local time."
        required: false
        type: "string"
      - name: "toDate"
        in: "query"
        description: "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results\\
          \\ to applications submitted on or after the provided date, in the CiviForm\\
          \\ instance's local time."
        required: false
        type: "string"
      - name: "pageSize"
        in: "query"
        description: "A positive integer. Limits the number of results per page. If\\
          \\ pageSize is larger than CiviForm's maximum page size then the maximum\\
          \\ will be used. The default maximum is 1,000 and is configurable."
        required: false
        type: "integer"
      - name: "nextPageToken"
        in: "query"
        description: "An opaque, alphanumeric identifier for a specific page of results.\\
          \\ When included CiviForm will return a page of results corresponding to\\
          \\ the token."
        required: false
        type: "string"
      responses:
        "200":
          description: "For valid requests."
          headers:
            x-next:
              type: "string"
              description: "A link to the next page of responses"
          schema:
            $ref: "#/definitions/result"
        "400":
          description: "Returned if any request parameters fail validation."
        "401":
          description: "Returned if the API key is invalid or does not have access\\
            \\ to the program."
securityDefinitions:
  basicAuth:
    type: "basic"
definitions:
  result:
    type: "object"
    properties:
      payload:
        type: "array"
        items:
          type: "object"
          properties:
            applicant_id:
              type: "integer"
              format: "int32"
            application:
              type: "object"
            application_id:
              type: "integer"
              format: "int32"
            create_time:
              type: "string"
              format: "date-time"
            language:
              type: "string"
            program_name:
              type: "string"
            program_version_id:
              type: "integer"
              format: "int32"
            revision_state:
              type: "string"
            status:
              type: "string"
              x-nullable: true
            submit_time:
              type: "string"
              format: "date-time"
            submitter_type:
              type: "string"
            ti_email:
              type: "string"
              x-nullable: true
            ti_organization:
              type: "string"
              x-nullable: true
      nextPageToken:
        type: "string"
""";

    System.out.println("**************************************");
    System.out.println(actual);
    System.out.println("**************************************");
    System.out.println(expected);
    System.out.println("**************************************");

    assertThat(actual).isEqualTo(expected);
  }
}
