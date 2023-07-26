package services.export;

import static controllers.dev.seeding.SampleQuestionDefinitions.ALL_SAMPLE_QUESTION_DEFINITIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import repository.ResetPostgres;
import services.CfJsonDocumentContext;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

@RunWith(MockitoJUnitRunner.class)
public class ProgramJsonSamplerTest extends ResetPostgres {

  private static final Stream<QuestionDefinition> ALL_SAMPLE_QUESTION_DEFINITIONS_WITH_IDS_STREAM =
      ALL_SAMPLE_QUESTION_DEFINITIONS.stream().map(QuestionDefinition::withPopulatedTestId);

  private ProgramJsonSampler programJsonSampler;

  /**
   * We need to mock the AutoValue in order to mock on streamQuestionDefinitions(), and this is
   * substantially simpler than seeding the complex ProgramDefinition AutoValue with data.
   */
  @SuppressWarnings("DoNotMockAutoValue")
  @Mock
  private ProgramDefinition mockProgramDefinition;

  @Before
  public void setUp() {
    programJsonSampler = instanceOf(ProgramJsonSampler.class);

    when(mockProgramDefinition.streamQuestionDefinitions())
        .thenReturn(ALL_SAMPLE_QUESTION_DEFINITIONS_WITH_IDS_STREAM);
  }

  @Test
  public void samplesFullProgram() {
    CfJsonDocumentContext json = programJsonSampler.getSampleJson(mockProgramDefinition);

    String allJsonSamples =
        "{\n"
            + "  \"application\" : {\n"
            + "    \"name\" : {\n"
            + "      \"first_name\" : \"Homer\",\n"
            + "      \"last_name\" : \"Simpson\",\n"
            + "      \"middle_name\" : \"Jay\"\n"
            + "    },\n"
            + "    \"sample_address_question\" : {\n"
            + "      \"city\" : \"Springfield\",\n"
            + "      \"corrected\" : null,\n"
            + "      \"latitude\" : \"44.0462\",\n"
            + "      \"line2\" : null,\n"
            + "      \"longitude\" : \"-123.0236\",\n"
            + "      \"service_area\" : \"springfield_county\",\n"
            + "      \"state\" : \"OR\",\n"
            + "      \"street\" : \"742 Evergreen Terrace\",\n"
            + "      \"well_known_id\" : \"23214\",\n"
            + "      \"zip\" : \"97403\"\n"
            + "    },\n"
            + "    \"sample_checkbox_question\" : {\n"
            + "      \"selections\" : [ \"toaster\", \"pepper grinder\" ]\n"
            + "    },\n"
            + "    \"sample_currency_question\" : {\n"
            + "      \"currency_dollars\" : 123.45\n"
            + "    },\n"
            + "    \"sample_date_question\" : {\n"
            + "      \"date\" : \"2023-01-02\"\n"
            + "    },\n"
            + "    \"sample_dropdown_question\" : {\n"
            + "      \"selection\" : \"chocolate\"\n"
            + "    },\n"
            + "    \"sample_email_question\" : {\n"
            + "      \"email\" : \"homer.simpson@springfield.gov\"\n"
            + "    },\n"
            + "    \"sample_file_upload_question\" : {\n"
            + "      \"file_key\" : \"http://localhost:9000/admin/applicant-files/my-file-key\"\n"
            + "    },\n"
            + "    \"sample_id_question\" : {\n"
            + "      \"id\" : \"12345\"\n"
            + "    },\n"
            + "    \"sample_number_question\" : {\n"
            + "      \"number\" : 12321\n"
            + "    },\n"
            + "    \"sample_phone_question\" : {\n"
            + "      \"phone_number\" : \"+12143673764\"\n"
            + "    },\n"
            + "    \"sample_predicate_date_question\" : {\n"
            + "      \"date\" : \"2023-01-02\"\n"
            + "    },\n"
            + "    \"sample_radio_button_question\" : {\n"
            + "      \"selection\" : \"winter (will hide next block)\"\n"
            + "    },\n"
            + "    \"sample_text_question\" : {\n"
            + "      \"text\" : \"I love CiviForm!\"\n"
            + "    }\n"
            + "  }\n"
            + "}";

    assertThat(json.asPrettyJsonString()).isEqualTo(allJsonSamples);
  }
}
