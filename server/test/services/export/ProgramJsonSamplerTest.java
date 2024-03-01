package services.export;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static controllers.dev.seeding.SampleQuestionDefinitions.ALL_SAMPLE_QUESTION_DEFINITIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static services.export.JsonPrettifier.asPrettyJsonString;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.stream.Stream;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.program.StatusDefinitions;
import services.question.types.QuestionDefinition;

@RunWith(MockitoJUnitRunner.class)
public class ProgramJsonSamplerTest extends ResetPostgres {

  private static final Stream<QuestionDefinition> ALL_SAMPLE_QUESTION_DEFINITIONS_WITH_IDS_STREAM =
      ALL_SAMPLE_QUESTION_DEFINITIONS.stream().map(QuestionDefinition::withPopulatedTestId);

  private ProgramJsonSampler programJsonSampler;

  private ProgramDefinition programDefinition;

  @Before
  public void setUp() {
    programJsonSampler = instanceOf(ProgramJsonSampler.class);

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

    programDefinition =
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
  }

  @Test
  public void samplesFullProgram() {
    String json = programJsonSampler.getSampleJson(programDefinition);

    String expectedJson =
        "{\n"
            + "  \"nextPageToken\" : null,\n"
            + "  \"payload\" : [ {\n"
            + "    \"applicant_id\" : 123,\n"
            + "    \"application\" : {\n"
            + "      \"name\" : {\n"
            + "        \"first_name\" : \"Homer\",\n"
            + "        \"last_name\" : \"Simpson\",\n"
            + "        \"middle_name\" : \"Jay\",\n"
            + "        \"question_type\" : \"NAME\"\n"
            + "      },\n"
            + "      \"sample_address_question\" : {\n"
            + "        \"city\" : \"Springfield\",\n"
            + "        \"corrected\" : \"Corrected\",\n"
            + "        \"latitude\" : \"44.0462\",\n"
            + "        \"line2\" : null,\n"
            + "        \"longitude\" : \"-123.0236\",\n"
            + "        \"question_type\" : \"ADDRESS\",\n"
            + "        \"service_area\" :"
            + " \"springfield_county_InArea_1709069741,portland_NotInArea_1709069741\",\n"
            + "        \"state\" : \"OR\",\n"
            + "        \"street\" : \"742 Evergreen Terrace\",\n"
            + "        \"well_known_id\" : \"4326\",\n"
            + "        \"zip\" : \"97403\"\n"
            + "      },\n"
            + "      \"sample_checkbox_question\" : {\n"
            + "        \"question_type\" : \"MULTI_SELECT\",\n"
            + "        \"selections\" : [ \"toaster\", \"pepper_grinder\" ]\n"
            + "      },\n"
            + "      \"sample_currency_question\" : {\n"
            + "        \"currency_dollars\" : 123.45,\n"
            + "        \"question_type\" : \"CURRENCY\"\n"
            + "      },\n"
            + "      \"sample_date_question\" : {\n"
            + "        \"date\" : \"2023-01-02\",\n"
            + "        \"question_type\" : \"DATE\"\n"
            + "      },\n"
            + "      \"sample_dropdown_question\" : {\n"
            + "        \"question_type\" : \"SINGLE_SELECT\",\n"
            + "        \"selection\" : \"chocolate\"\n"
            + "      },\n"
            + "      \"sample_email_question\" : {\n"
            + "        \"email\" : \"homer.simpson@springfield.gov\",\n"
            + "        \"question_type\" : \"EMAIL\"\n"
            + "      },\n"
            + "      \"sample_file_upload_question\" : {\n"
            + "        \"file_key\" :"
            + " \"http://localhost:9000/admin/applicant-files/my-file-key\",\n"
            + "        \"question_type\" : \"FILE_UPLOAD\"\n"
            + "      },\n"
            + "      \"sample_id_question\" : {\n"
            + "        \"id\" : \"12345\",\n"
            + "        \"question_type\" : \"ID\"\n"
            + "      },\n"
            + "      \"sample_number_question\" : {\n"
            + "        \"number\" : 12321,\n"
            + "        \"question_type\" : \"NUMBER\"\n"
            + "      },\n"
            + "      \"sample_phone_question\" : {\n"
            + "        \"phone_number\" : \"+12143673764\",\n"
            + "        \"question_type\" : \"PHONE\"\n"
            + "      },\n"
            + "      \"sample_predicate_date_question\" : {\n"
            + "        \"date\" : \"2023-01-02\",\n"
            + "        \"question_type\" : \"DATE\"\n"
            + "      },\n"
            + "      \"sample_radio_button_question\" : {\n"
            + "        \"question_type\" : \"SINGLE_SELECT\",\n"
            + "        \"selection\" : \"winter\"\n"
            + "      },\n"
            + "      \"sample_text_question\" : {\n"
            + "        \"question_type\" : \"TEXT\",\n"
            + "        \"text\" : \"I love CiviForm!\"\n"
            + "      }\n"
            + "    },\n"
            + "    \"application_id\" : 456,\n"
            + "    \"create_time\" : \"2023/05/25 1:46:15 PM PDT\",\n"
            + "    \"language\" : \"en-US\",\n"
            + "    \"program_name\" : \"test-program-admin-name\",\n"
            + "    \"program_version_id\" : 789,\n"
            + "    \"revision_state\" : \"CURRENT\",\n"
            + "    \"status\" : \"Pending Review\",\n"
            + "    \"submit_time\" : \"2023/05/26 1:46:15 PM PDT\",\n"
            + "    \"submitter_type\" : \"APPLICANT\",\n"
            + "    \"ti_email\" : null,\n"
            + "    \"ti_organization\" : null\n"
            + "  } ]\n"
            + "}";

    assertThat(asPrettyJsonString(json)).isEqualTo(expectedJson);
  }
}
