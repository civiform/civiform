package services.openapi.v2;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.dev.seeding.SampleQuestionDefinitions;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
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
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

@RunWith(JUnitParamsRunner.class)
public class Swagger2SchemaGeneratorTest extends OpenApiSerializationAsserter {
  private static final Stream<QuestionDefinition> ALL_SAMPLE_QUESTION_DEFINITIONS_WITH_IDS_STREAM =
      SampleQuestionDefinitions.ALL_SAMPLE_QUESTION_DEFINITIONS.stream()
          .map(QuestionDefinition::withPopulatedTestId);

  @Test
  public void canSerializeWithSelectionOfQuestions() {

    ImmutableList<BlockDefinition> blockDefinitions =
        ImmutableList.of(
            BlockDefinition.builder()
                .setId(135L)
                .setName("Test Block Definition")
                .setDescription("Test Block Description")
                .setProgramQuestionDefinitions(
                    ALL_SAMPLE_QUESTION_DEFINITIONS_WITH_IDS_STREAM
                        .map(
                            questionDefinition ->
                                ProgramQuestionDefinition.create(
                                    questionDefinition, Optional.empty()))
                        .collect(toImmutableList()))
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
        OpenApiSchemaSettings.builder()
            .setBaseUrl("baseUrl")
            .setItEmailAddress("email123@example.com")
            .setAllowHttpScheme(true)
            .build();

    Swagger2SchemaGenerator generator = new Swagger2SchemaGenerator(settings);
    String actual = generator.createSchema(programDefinition);

    String expected =
        """
swagger: "2.0"
basePath: /api/v1/admin/programs/test-program-admin-name
host: baseUrl
info:
  title: test-program-admin-name
  version: "789"
  description: Test Admin Description
  contact:
    name: CiviForm Technical Support
    email: email123@example.com
schemes:
  - http
  - https
security:
  - basicAuth: []
securityDefinitions:
  basicAuth:
    type: basic
paths:
  /applications:
    get:
      summary: Export applications
      operationId: list_applications
      description: List Applications
      parameters:
        - in: query
          name: fromDate
          type: string
          required: false
          description: An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results to applications submitted on or after the provided date.
        - in: query
          name: toDate
          type: string
          required: false
          description: An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results to applications submitted before the provided date.
        - in: query
          name: pageSize
          type: integer
          required: false
          description: "A positive integer. Limits the number of results per page. If pageSize is larger than CiviForm's maximum page size then the maximum will be used. The default maximum is 1,000 and is configurable."
        - in: query
          name: nextPageToken
          type: string
          required: false
          description: "An opaque, alphanumeric identifier for a specific page of results. When included CiviForm will return a page of results corresponding to the token."
      produces:
        - application/json
      responses:
        "200":
          description: For valid requests.
          headers:
            x-next:
              type: string
              description: A link to the next page of responses
          schema:
            $ref: "#/definitions/result"
        "400":
          description: Returned if any request parameters fail validation.
        "401":
          description: Returned if the API key is invalid or does not have access to the program.
      tags:
        - programs
definitions:
  result:
    type: object
    properties:
      payload:
        type: array
        items:
          type: object
          properties:
            applicant_id:
              type: integer
              format: int32
            application:
              type: object
              properties:
                sample_address_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    city:
                      type: string
                      x-nullable: true
                    corrected:
                      type: string
                      x-nullable: true
                    latitude:
                      type: string
                      x-nullable: true
                    line2:
                      type: string
                      x-nullable: true
                    longitude:
                      type: string
                      x-nullable: true
                    service_area:
                      type: string
                      x-nullable: true
                    state:
                      type: string
                      x-nullable: true
                    street:
                      type: string
                      x-nullable: true
                    well_known_id:
                      type: string
                      x-nullable: true
                    zip:
                      type: string
                      x-nullable: true
                sample_checkbox_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    selections:
                      type: array
                      items:
                        type: string
                sample_currency_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    currency_dollars:
                      type: number
                      format: double
                      x-nullable: true
                sample_date_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    date:
                      type: string
                      format: date
                      x-nullable: true
                sample_dropdown_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    selection:
                      type: string
                      x-nullable: true
                sample_email_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    email:
                      type: string
                      x-nullable: true
                sample_enumerator_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    entities:
                      type: array
                      items:
                        type: object
                        properties:
                          entity_name:
                            type: string
                sample_file_upload_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    file_key:
                      type: string
                      x-nullable: true
                    file_urls:
                      type: array
                      items:
                        type: object
                sample_id_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    id:
                      type: string
                      x-nullable: true
                sample_name_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    first_name:
                      type: string
                      x-nullable: true
                    last_name:
                      type: string
                      x-nullable: true
                    middle_name:
                      type: string
                      x-nullable: true
                    suffix:
                      type: string
                      x-nullable: true
                sample_number_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    number:
                      type: integer
                      format: int64
                      x-nullable: true
                sample_phone_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    country_code:
                      type: string
                      x-nullable: true
                    phone_number:
                      type: string
                      x-nullable: true
                sample_predicate_date_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    date:
                      type: string
                      format: date
                      x-nullable: true
                sample_radio_button_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    selection:
                      type: string
                      x-nullable: true
                sample_text_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    text:
                      type: string
                      x-nullable: true
            application_id:
              type: integer
              format: int32
            create_time:
              type: string
              format: date-time
            language:
              type: string
            program_name:
              type: string
            program_version_id:
              type: integer
              format: int32
            revision_state:
              type: string
            status:
              type: string
              x-nullable: true
            submit_time:
              type: string
              format: date-time
            submitter_type:
              type: string
            ti_email:
              type: string
              x-nullable: true
            ti_organization:
              type: string
              x-nullable: true
      nextPageToken:
        type: string
""";

    assertThat(actual).isEqualTo(expected);
  }

  public record Data(QuestionType questionType, String questionSchema) {
    @Override
    public String toString() {
      return questionType.toString();
    }
  }

  public static ImmutableList<Data> getData() {
    return ImmutableList.of(
        new Data(
            QuestionType.ADDRESS,
            """
                sample_address_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    city:
                      type: string
                      x-nullable: true
                    corrected:
                      type: string
                      x-nullable: true
                    latitude:
                      type: string
                      x-nullable: true
                    line2:
                      type: string
                      x-nullable: true
                    longitude:
                      type: string
                      x-nullable: true
                    service_area:
                      type: string
                      x-nullable: true
                    state:
                      type: string
                      x-nullable: true
                    street:
                      type: string
                      x-nullable: true
                    well_known_id:
                      type: string
                      x-nullable: true
                    zip:
                      type: string
                      x-nullable: true
"""),
        new Data(
            QuestionType.CHECKBOX,
            """
                sample_checkbox_question:
                  type: object
                  properties:
                    question_type:
                      type: string
                    selections:
                      type: array
                      items:
                        type: string
"""),
        new Data(
            QuestionType.CURRENCY,
            """
                            sample_currency_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                currency_dollars:
                                  type: number
                                  format: double
                                  x-nullable: true
            """),
        new Data(
            QuestionType.DATE,
            """
                            sample_date_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                date:
                                  type: string
                                  format: date
                                  x-nullable: true
            """),
        new Data(
            QuestionType.DROPDOWN,
            """
                            sample_dropdown_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                selection:
                                  type: string
                                  x-nullable: true
            """),
        new Data(
            QuestionType.EMAIL,
            """
                            sample_email_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                email:
                                  type: string
                                  x-nullable: true
            """),
        new Data(
            QuestionType.ENUMERATOR,
            """
                            sample_enumerator_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                entities:
                                  type: array
                                  items:
                                    type: object
                                    properties:
                                      entity_name:
                                        type: string
            """),
        new Data(
            QuestionType.FILEUPLOAD,
            """
                            sample_file_upload_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                file_key:
                                  type: string
                                  x-nullable: true
                                file_urls:
                                  type: array
                                  items:
                                    type: object
            """),
        new Data(
            QuestionType.ID,
            """
                            sample_id_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                id:
                                  type: string
                                  x-nullable: true
            """),
        new Data(
            QuestionType.NAME,
            """
                            sample_name_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                first_name:
                                  type: string
                                  x-nullable: true
                                last_name:
                                  type: string
                                  x-nullable: true
                                middle_name:
                                  type: string
                                  x-nullable: true
                                suffix:
                                  type: string
                                  x-nullable: true
            """),
        new Data(
            QuestionType.NUMBER,
            """
                            sample_number_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                number:
                                  type: integer
                                  format: int64
                                  x-nullable: true
            """),
        new Data(
            QuestionType.RADIO_BUTTON,
            """
                            sample_radio_button_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                selection:
                                  type: string
                                  x-nullable: true
            """),
        new Data(
            QuestionType.TEXT,
            """
                            sample_text_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                text:
                                  type: string
                                  x-nullable: true
            """),
        new Data(
            QuestionType.PHONE,
            """
                            sample_phone_question:
                              type: object
                              properties:
                                question_type:
                                  type: string
                                country_code:
                                  type: string
                                  x-nullable: true
                                phone_number:
                                  type: string
                                  x-nullable: true
            """));
  }

  @Test
  @Parameters(method = "getData")
  @TestCaseName("canSerialize[QuestionType.{0}]")
  public void canSerialize(Data data) {
    ImmutableList<ProgramQuestionDefinition> programQuestionDefinition =
        ImmutableList.of(
            ProgramQuestionDefinition.create(
                SampleQuestionDefinitions.getQuestionDefinitionWithTestId(data.questionType()),
                Optional.empty()));

    ImmutableList<BlockDefinition> blockDefinitions =
        ImmutableList.of(
            BlockDefinition.builder()
                .setId(135L)
                .setName("Test Block Definition")
                .setDescription("Test Block Description")
                .setProgramQuestionDefinitions(programQuestionDefinition)
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
        OpenApiSchemaSettings.builder()
            .setBaseUrl("baseUrl")
            .setItEmailAddress("email123@example.com")
            .setAllowHttpScheme(true)
            .build();

    Swagger2SchemaGenerator generator = new Swagger2SchemaGenerator(settings);
    String actual = generator.createSchema(programDefinition);

    String expected =
        """
swagger: "2.0"
basePath: /api/v1/admin/programs/test-program-admin-name
host: baseUrl
info:
  title: test-program-admin-name
  version: "789"
  description: Test Admin Description
  contact:
    name: CiviForm Technical Support
    email: email123@example.com
schemes:
  - http
  - https
security:
  - basicAuth: []
securityDefinitions:
  basicAuth:
    type: basic
paths:
  /applications:
    get:
      summary: Export applications
      operationId: list_applications
      description: List Applications
      parameters:
        - in: query
          name: fromDate
          type: string
          required: false
          description: An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results to applications submitted on or after the provided date.
        - in: query
          name: toDate
          type: string
          required: false
          description: An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results to applications submitted before the provided date.
        - in: query
          name: pageSize
          type: integer
          required: false
          description: "A positive integer. Limits the number of results per page. If pageSize is larger than CiviForm's maximum page size then the maximum will be used. The default maximum is 1,000 and is configurable."
        - in: query
          name: nextPageToken
          type: string
          required: false
          description: "An opaque, alphanumeric identifier for a specific page of results. When included CiviForm will return a page of results corresponding to the token."
      produces:
        - application/json
      responses:
        "200":
          description: For valid requests.
          headers:
            x-next:
              type: string
              description: A link to the next page of responses
          schema:
            $ref: "#/definitions/result"
        "400":
          description: Returned if any request parameters fail validation.
        "401":
          description: Returned if the API key is invalid or does not have access to the program.
      tags:
        - programs
definitions:
  result:
    type: object
    properties:
      payload:
        type: array
        items:
          type: object
          properties:
            applicant_id:
              type: integer
              format: int32
            application:
              type: object
              properties:
"""
            + data.questionSchema()
            + """
            application_id:
              type: integer
              format: int32
            create_time:
              type: string
              format: date-time
            language:
              type: string
            program_name:
              type: string
            program_version_id:
              type: integer
              format: int32
            revision_state:
              type: string
            status:
              type: string
              x-nullable: true
            submit_time:
              type: string
              format: date-time
            submitter_type:
              type: string
            ti_email:
              type: string
              x-nullable: true
            ti_organization:
              type: string
              x-nullable: true
      nextPageToken:
        type: string
""";

    assertThat(actual).isEqualTo(expected);
  }
}
