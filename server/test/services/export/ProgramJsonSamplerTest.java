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
                .setLocalizedName(LocalizedStrings.withDefaultValue("Test Block Definition"))
                .setLocalizedDescription(
                    LocalizedStrings.withDefaultValue("Test Block Description"))
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
        """
{
  "nextPageToken" : null,
  "payload" : [ {
    "applicant_id" : 123,
    "application" : {
      "name" : {
        "first_name" : "Homer",
        "last_name" : "Simpson",
        "middle_name" : "Jay",
        "question_type" : "NAME"
      },
      "sample_address_question" : {
        "city" : "Springfield",
        "corrected" : "Corrected",
        "latitude" : "44.0462",
        "line2" : null,
        "longitude" : "-123.0236",
        "question_type" : "ADDRESS",
        "service_area" : "springfieldCounty_InArea_1709069741,portland_NotInArea_1709069741",
        "state" : "OR",
        "street" : "742 Evergreen Terrace",
        "well_known_id" : "4326",
        "zip" : "97403"
      },
      "sample_checkbox_question" : {
        "question_type" : "MULTI_SELECT",
        "selections" : [ "toaster", "pepper_grinder" ]
      },
      "sample_currency_question" : {
        "currency_dollars" : 123.45,
        "question_type" : "CURRENCY"
      },
      "sample_date_question" : {
        "date" : "2023-01-02",
        "question_type" : "DATE"
      },
      "sample_dropdown_question" : {
        "question_type" : "SINGLE_SELECT",
        "selection" : "chocolate"
      },
      "sample_email_question" : {
        "email" : "homer.simpson@springfield.gov",
        "question_type" : "EMAIL"
      },
      "sample_file_upload_question" : {
        "file_key" : "http://localhost:9000/admin/applicant-files/my-file-key",
        "question_type" : "FILE_UPLOAD"
      },
      "sample_id_question" : {
        "id" : "12345",
        "question_type" : "ID"
      },
      "sample_number_question" : {
        "number" : 12321,
        "question_type" : "NUMBER"
      },
      "sample_phone_question" : {
        "phone_number" : "+12143673764",
        "question_type" : "PHONE"
      },
      "sample_predicate_date_question" : {
        "date" : "2023-01-02",
        "question_type" : "DATE"
      },
      "sample_radio_button_question" : {
        "question_type" : "SINGLE_SELECT",
        "selection" : "winter"
      },
      "sample_text_question" : {
        "question_type" : "TEXT",
        "text" : "I love CiviForm!"
      }
    },
    "application_id" : 456,
    "create_time" : "2023-05-25T13:46:15-07:00",
    "language" : "en-US",
    "program_name" : "test-program-admin-name",
    "program_version_id" : 789,
    "revision_state" : "CURRENT",
    "status" : "Pending Review",
    "submit_time" : "2023-05-26T13:46:15-07:00",
    "submitter_type" : "APPLICANT",
    "ti_email" : null,
    "ti_organization" : null
  } ]
}""";

    assertThat(asPrettyJsonString(json)).isEqualTo(expectedJson);
  }
}
