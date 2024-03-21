package services.openapi.v2;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import controllers.dev.seeding.SampleQuestionDefinitions;
import java.util.Optional;
import java.util.stream.Stream;
import models.DisplayMode;
import org.junit.Test;
import services.LocalizedStrings;
import services.openapi.OpenApiSchemaSettings;
import services.openapi.v2.serializers.OpenApiSerializationAsserter;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.program.StatusDefinitions;
import services.question.types.QuestionDefinition;

public class Swagger2SchemaGeneratorTest extends OpenApiSerializationAsserter {
  /** All members of this class that are of type {@link QuestionDefinition}. */
  public static final ImmutableList<QuestionDefinition> ALL_SAMPLE_QUESTION_DEFINITIONS =
      ImmutableList.of(
          SampleQuestionDefinitions.ADDRESS_QUESTION_DEFINITION,
          SampleQuestionDefinitions.CHECKBOX_QUESTION_DEFINITION,
          SampleQuestionDefinitions.CURRENCY_QUESTION_DEFINITION,
          SampleQuestionDefinitions.DATE_PREDICATE_QUESTION_DEFINITION,
          SampleQuestionDefinitions.DATE_QUESTION_DEFINITION,
          SampleQuestionDefinitions.DROPDOWN_QUESTION_DEFINITION,
          SampleQuestionDefinitions.EMAIL_QUESTION_DEFINITION,
          SampleQuestionDefinitions.ENUMERATOR_QUESTION_DEFINITION,
          SampleQuestionDefinitions.FILE_UPLOAD_QUESTION_DEFINITION,
          SampleQuestionDefinitions.ID_QUESTION_DEFINITION,
          SampleQuestionDefinitions.NAME_QUESTION_DEFINITION,
          SampleQuestionDefinitions.NUMBER_QUESTION_DEFINITION,
          SampleQuestionDefinitions.PHONE_QUESTION_DEFINITION,
          SampleQuestionDefinitions.RADIO_BUTTON_QUESTION_DEFINITION,
          SampleQuestionDefinitions.STATIC_CONTENT_QUESTION_DEFINITION,
          SampleQuestionDefinitions.TEXT_QUESTION_DEFINITION);

  private static final Stream<QuestionDefinition> ALL_SAMPLE_QUESTION_DEFINITIONS_WITH_IDS_STREAM =
      ALL_SAMPLE_QUESTION_DEFINITIONS.stream().map(QuestionDefinition::withPopulatedTestId);

  @Test
  public void canSerialize() {

    StatusDefinitions possibleProgramStatuses =
        new StatusDefinitions(
            ImmutableList.of(
                StatusDefinitions.Status.builder()
                    .setStatusText("Pending Review")
                    .setDefaultStatus(Optional.of(true))
                    .setLocalizedStatusText(LocalizedStrings.empty())
                    .setLocalizedEmailBodyText(Optional.empty())
                    .build()));

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
            .setStatusDefinitions(possibleProgramStatuses)
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
        new YamlFormatter()
            .appendLine("swagger: \"2.0\"")
            .appendLine("basePath: /api/v1/admin/programs/test-program-admin-name")
            .appendLine("host: baseUrl")
            .appendLine("info:")
            .appendLine("  title: test-program-admin-name")
            .appendLine("  version: \"789\"")
            .appendLine("  description: Test Admin Description")
            .appendLine("  contact:")
            .appendLine("    name: CiviForm Technical Support")
            .appendLine("    email: email123@example.com")
            .appendLine("schemes:")
            .appendLine("  - http")
            .appendLine("  - https")
            .appendLine("security:")
            .appendLine("  - basicAuth: []")
            .appendLine("securityDefinitions:")
            .appendLine("  basicAuth:")
            .appendLine("    type: basic")
            .appendLine("paths:")
            .appendLine("  /applications:")
            .appendLine("    get:")
            .appendLine("      summary: Export applications")
            .appendLine("      operationId: get_applications")
            .appendLine("      description: Get Applications")
            .appendLine("      parameters:")
            .appendLine("        - in: query")
            .appendLine("          name: fromDate")
            .appendLine("          type: string")
            .appendLine("          required: false")
            .appendLine(
                "          description: An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits"
                    + " results to applications submitted on or after the provided date.")
            .appendLine("        - in: query")
            .appendLine("          name: toDate")
            .appendLine("          type: string")
            .appendLine("          required: false")
            .appendLine(
                "          description: An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits"
                    + " results to applications submitted before the provided date.")
            .appendLine("        - in: query")
            .appendLine("          name: pageSize")
            .appendLine("          type: integer")
            .appendLine("          required: false")
            .appendLine(
                "          description: \"A positive integer. Limits the number of results per"
                    + " page. If pageSize is larger than CiviForm's maximum page size then the"
                    + " maximum will be used. The default maximum is 1,000 and is configurable.\"")
            .appendLine("        - in: query")
            .appendLine("          name: nextPageToken")
            .appendLine("          type: string")
            .appendLine("          required: false")
            .appendLine(
                "          description: \"An opaque, alphanumeric identifier for a specific page"
                    + " of results. When included CiviForm will return a page of results"
                    + " corresponding to the token.\"")
            .appendLine("      produces:")
            .appendLine("        - application/json")
            .appendLine("      responses:")
            .appendLine("        \"200\":")
            .appendLine("          description: For valid requests.")
            .appendLine("          headers:")
            .appendLine("            x-next:")
            .appendLine("              type: string")
            .appendLine("              description: A link to the next page of responses")
            .appendLine("          schema:")
            .appendLine("            $ref: '#/definitions/result'")
            .appendLine("        \"400\":")
            .appendLine(
                "          description: Returned if any request parameters fail validation.")
            .appendLine("        \"401\":")
            .appendLine(
                "          description: Returned if the API key is invalid or does not have access"
                    + " to the program.")
            .appendLine("      tags:")
            .appendLine("        - programs")
            .appendLine("definitions:")
            .appendLine("  result:")
            .appendLine("    type: object")
            .appendLine("    properties:")
            .appendLine("      payload:")
            .appendLine("        type: array")
            .appendLine("        items:")
            .appendLine("          type: object")
            .appendLine("          properties:")
            .appendLine("            applicant_id:")
            .appendLine("              type: integer")
            .appendLine("              format: int32")
            .appendLine("            application:")
            .appendLine("              type: object")
            .appendLine("              properties:")
            .appendLine("                name:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    first_name:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                    last_name:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                    middle_name:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_address_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    city:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                    corrected:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                    latitude:")
            .appendLine("                      type: number")
            .appendLine("                      format: double")
            .appendLine("                      x-nullable: true")
            .appendLine("                    line2:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                    longitude:")
            .appendLine("                      type: number")
            .appendLine("                      format: double")
            .appendLine("                      x-nullable: true")
            .appendLine("                    service_area:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                    state:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                    street:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                    well_known_id:")
            .appendLine("                      type: integer")
            .appendLine("                      format: int64")
            .appendLine("                      x-nullable: true")
            .appendLine("                    zip:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_checkbox_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    selections:")
            .appendLine("                      type: array")
            .appendLine("                      items:")
            .appendLine("                        type: object")
            .appendLine("                sample_currency_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    currency_cents:")
            .appendLine("                      type: string")
            .appendLine("                      format: double")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_date_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    date:")
            .appendLine("                      type: string")
            .appendLine("                      format: date")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_dropdown_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    selection:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_email_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    email:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_enumerator_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    entities:")
            .appendLine("                      type: array")
            .appendLine("                      items:")
            .appendLine("                        type: object")
            .appendLine("                sample_file_upload_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    file_key:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                    original_file_name:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_id_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    id:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_number_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    number:")
            .appendLine("                      type: integer")
            .appendLine("                      format: int64")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_phone_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    country_code:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                    phone_number:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_predicate_date_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    date:")
            .appendLine("                      type: string")
            .appendLine("                      format: date")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_radio_button_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    selection:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("                sample_text_question:")
            .appendLine("                  type: object")
            .appendLine("                  properties:")
            .appendLine("                    question_type:")
            .appendLine("                      type: string")
            .appendLine("                    text:")
            .appendLine("                      type: string")
            .appendLine("                      x-nullable: true")
            .appendLine("            application_id:")
            .appendLine("              type: integer")
            .appendLine("              format: int32")
            .appendLine("            create_time:")
            .appendLine("              type: string")
            .appendLine("            language:")
            .appendLine("              type: string")
            .appendLine("            program_name:")
            .appendLine("              type: string")
            .appendLine("            program_version_id:")
            .appendLine("              type: integer")
            .appendLine("              format: int32")
            .appendLine("            status:")
            .appendLine("              type: string")
            .appendLine("            submit_time:")
            .appendLine("              type: string")
            .appendLine("            submitter_email:")
            .appendLine("              type: string")
            .appendLine("      nextPageToken:")
            .appendLine("        type: string")
            .toString();

    assertThat(actual).isEqualTo(expected);
  }
}
