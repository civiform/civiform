package services.openapi.v3;

import static com.google.auto.common.MoreStreams.toImmutableList;
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
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

@RunWith(JUnitParamsRunner.class)
public class OpenApi3SchemaGeneratorTest {
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

    var generator = new OpenApi3SchemaGenerator(settings);
    String actual = generator.createSchema(programDefinition);

    String expected =
        """
openapi: 3.0.1
info:
  title: test-program-admin-name
  description: Test Admin Description
  contact:
    name: CiviForm Technical Support
    email: email123@example.com
  version: "789"
servers:
- url: http://baseUrl/api/v1/admin/programs/test-program-admin-name
- url: https://baseUrl/api/v1/admin/programs/test-program-admin-name
security:
- basicAuth: []
paths:
  /applications:
    get:
      tags:
      - programs
      summary: Export applications
      description: List Applications
      operationId: list_applications
      parameters:
      - name: fromDate
        in: query
        description: "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results\\
          \\ to applications submitted on or after the provided date, in the CiviForm\\
          \\ instance's local time."
        schema:
          type: string
      - name: toDate
        in: query
        description: "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results\\
          \\ to applications submitted on or after the provided date, in the CiviForm\\
          \\ instance's local time."
        schema:
          type: string
      - name: pageSize
        in: query
        description: "A positive integer. Limits the number of results per page. If\\
          \\ pageSize is larger than CiviForm's maximum page size then the maximum\\
          \\ will be used. The default maximum is 1,000 and is configurable."
        schema:
          type: integer
          format: int32
      - name: nextPageToken
        in: query
        description: "An opaque, alphanumeric identifier for a specific page of results.\\
          \\ When included CiviForm will return a page of results corresponding to\\
          \\ the token."
        schema:
          type: string
      responses:
        "200":
          description: For valid requests.
          headers:
            x-next:
              description: A link to the next page of responses
              schema:
                type: string
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/result"
        "400":
          description: Returned if any request parameters fail validation.
        "401":
          description: Returned if the API key is invalid or does not have access
            to the program.
components:
  schemas:
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
              application_id:
                type: integer
                format: int32
              application_note:
                type: string
                nullable: true
              create_time:
                type: string
                format: date-time
              language:
                type: string
                example: en-US
              program_name:
                type: string
                example: program-name-123
              program_version_id:
                type: integer
                format: int32
              revision_state:
                type: string
                example: CURRENT
              status:
                type: string
                nullable: true
              submit_time:
                type: string
                format: date-time
              submitter_type:
                type: string
              ti_email:
                type: string
                nullable: true
              ti_organization:
                type: string
                nullable: true
        nextPageToken:
          type: string
  securitySchemes:
    basicAuth:
      type: http
      scheme: basic
""";

    // Those only print on failure, but make it easier to compare as the test framework only shows a
    // small snippet
    System.out.println("* ACTUAL *************************************");
    System.out.println(actual);
    System.out.println("* EXPECTED *************************************");
    System.out.println(expected);
    System.out.println("**************************************");

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void createSchema_WithSelectionOfQuestions() {

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
        new OpenApiSchemaSettings("baseUrl", "email123@example.com", true);

    var generator = new OpenApi3SchemaGenerator(settings);
    String actual = generator.createSchema(programDefinition);

    String expected =
        """
openapi: 3.0.1
info:
  title: test-program-admin-name
  description: Test Admin Description
  contact:
    name: CiviForm Technical Support
    email: email123@example.com
  version: "789"
servers:
- url: http://baseUrl/api/v1/admin/programs/test-program-admin-name
- url: https://baseUrl/api/v1/admin/programs/test-program-admin-name
security:
- basicAuth: []
paths:
  /applications:
    get:
      tags:
      - programs
      summary: Export applications
      description: List Applications
      operationId: list_applications
      parameters:
      - name: fromDate
        in: query
        description: "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results\\
          \\ to applications submitted on or after the provided date, in the CiviForm\\
          \\ instance's local time."
        schema:
          type: string
      - name: toDate
        in: query
        description: "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results\\
          \\ to applications submitted on or after the provided date, in the CiviForm\\
          \\ instance's local time."
        schema:
          type: string
      - name: pageSize
        in: query
        description: "A positive integer. Limits the number of results per page. If\\
          \\ pageSize is larger than CiviForm's maximum page size then the maximum\\
          \\ will be used. The default maximum is 1,000 and is configurable."
        schema:
          type: integer
          format: int32
      - name: nextPageToken
        in: query
        description: "An opaque, alphanumeric identifier for a specific page of results.\\
          \\ When included CiviForm will return a page of results corresponding to\\
          \\ the token."
        schema:
          type: string
      responses:
        "200":
          description: For valid requests.
          headers:
            x-next:
              description: A link to the next page of responses
              schema:
                type: string
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/result"
        "400":
          description: Returned if any request parameters fail validation.
        "401":
          description: Returned if the API key is invalid or does not have access
            to the program.
components:
  schemas:
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
                      corrected:
                        type: string
                      latitude:
                        type: string
                      line2:
                        type: string
                      longitude:
                        type: string
                      service_area:
                        type: string
                      state:
                        type: string
                      street:
                        type: string
                      well_known_id:
                        type: string
                      zip:
                        type: string
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
                  sample_date_question:
                    type: object
                    properties:
                      question_type:
                        type: string
                      date:
                        type: string
                        format: date
                  sample_dropdown_question:
                    type: object
                    properties:
                      question_type:
                        type: string
                      selection:
                        type: string
                  sample_email_question:
                    type: object
                    properties:
                      question_type:
                        type: string
                      email:
                        type: string
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
                      file_urls:
                        type: array
                        items:
                          type: string
                  sample_id_question:
                    type: object
                    properties:
                      question_type:
                        type: string
                      id:
                        type: string
                  sample_name_question:
                    type: object
                    properties:
                      question_type:
                        type: string
                      first_name:
                        type: string
                      last_name:
                        type: string
                      middle_name:
                        type: string
                      suffix:
                        type: string
                  sample_number_question:
                    type: object
                    properties:
                      question_type:
                        type: string
                      number:
                        type: integer
                        format: int64
                  sample_phone_question:
                    type: object
                    properties:
                      question_type:
                        type: string
                      country_code:
                        type: string
                      phone_number:
                        type: string
                  sample_predicate_date_question:
                    type: object
                    properties:
                      question_type:
                        type: string
                      date:
                        type: string
                        format: date
                  sample_radio_button_question:
                    type: object
                    properties:
                      question_type:
                        type: string
                      selection:
                        type: string
                  sample_text_question:
                    type: object
                    properties:
                      question_type:
                        type: string
                      text:
                        type: string
              application_id:
                type: integer
                format: int32
              application_note:
                type: string
                nullable: true
              create_time:
                type: string
                format: date-time
              language:
                type: string
                example: en-US
              program_name:
                type: string
                example: program-name-123
              program_version_id:
                type: integer
                format: int32
              revision_state:
                type: string
                example: CURRENT
              status:
                type: string
                nullable: true
              submit_time:
                type: string
                format: date-time
              submitter_type:
                type: string
              ti_email:
                type: string
                nullable: true
              ti_organization:
                type: string
                nullable: true
        nextPageToken:
          type: string
  securitySchemes:
    basicAuth:
      type: http
      scheme: basic
""";

    // Those only print on failure, but make it easier to compare as the test framework only shows a
    // small snippet
    System.out.println("* ACTUAL *************************************");
    System.out.println(actual);
    System.out.println("* EXPECTED *************************************");
    System.out.println(expected);
    System.out.println("**************************************");

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
                                  corrected:
                                    type: string
                                  latitude:
                                    type: string
                                  line2:
                                    type: string
                                  longitude:
                                    type: string
                                  service_area:
                                    type: string
                                  state:
                                    type: string
                                  street:
                                    type: string
                                  well_known_id:
                                    type: string
                                  zip:
                                    type: string
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
                                  file_urls:
                                    type: array
                                    items:
                                      type: string
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
                                  last_name:
                                    type: string
                                  middle_name:
                                    type: string
                                  suffix:
                                    type: string
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
                                  phone_number:
                                    type: string
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
        new OpenApiSchemaSettings("baseUrl", "email123@example.com", true);

    var generator = new OpenApi3SchemaGenerator(settings);
    String actual = generator.createSchema(programDefinition);

    String expected =
        """
openapi: 3.0.1
info:
  title: test-program-admin-name
  description: Test Admin Description
  contact:
    name: CiviForm Technical Support
    email: email123@example.com
  version: "789"
servers:
- url: http://baseUrl/api/v1/admin/programs/test-program-admin-name
- url: https://baseUrl/api/v1/admin/programs/test-program-admin-name
security:
- basicAuth: []
paths:
  /applications:
    get:
      tags:
      - programs
      summary: Export applications
      description: List Applications
      operationId: list_applications
      parameters:
      - name: fromDate
        in: query
        description: "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results\\
          \\ to applications submitted on or after the provided date, in the CiviForm\\
          \\ instance's local time."
        schema:
          type: string
      - name: toDate
        in: query
        description: "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits results\\
          \\ to applications submitted on or after the provided date, in the CiviForm\\
          \\ instance's local time."
        schema:
          type: string
      - name: pageSize
        in: query
        description: "A positive integer. Limits the number of results per page. If\\
          \\ pageSize is larger than CiviForm's maximum page size then the maximum\\
          \\ will be used. The default maximum is 1,000 and is configurable."
        schema:
          type: integer
          format: int32
      - name: nextPageToken
        in: query
        description: "An opaque, alphanumeric identifier for a specific page of results.\\
          \\ When included CiviForm will return a page of results corresponding to\\
          \\ the token."
        schema:
          type: string
      responses:
        "200":
          description: For valid requests.
          headers:
            x-next:
              description: A link to the next page of responses
              schema:
                type: string
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/result"
        "400":
          description: Returned if any request parameters fail validation.
        "401":
          description: Returned if the API key is invalid or does not have access
            to the program.
components:
  schemas:
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
              application_note:
                type: string
                nullable: true
              create_time:
                type: string
                format: date-time
              language:
                type: string
                example: en-US
              program_name:
                type: string
                example: program-name-123
              program_version_id:
                type: integer
                format: int32
              revision_state:
                type: string
                example: CURRENT
              status:
                type: string
                nullable: true
              submit_time:
                type: string
                format: date-time
              submitter_type:
                type: string
              ti_email:
                type: string
                nullable: true
              ti_organization:
                type: string
                nullable: true
        nextPageToken:
          type: string
  securitySchemes:
    basicAuth:
      type: http
      scheme: basic
""";

    // Those only print on failure, but make it easier to compare as the test framework only shows a
    // small snippet
    System.out.println("* ACTUAL *************************************");
    System.out.println(actual);
    System.out.println("* EXPECTED *************************************");
    System.out.println(expected);
    System.out.println("**************************************");

    assertThat(actual).isEqualTo(expected);
  }
}
