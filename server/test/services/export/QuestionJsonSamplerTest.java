package services.export;

import static controllers.dev.seeding.SampleQuestionDefinitions.ADDRESS_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.CHECKBOX_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.CURRENCY_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.DATE_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.EMAIL_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.ENUMERATOR_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.FILE_UPLOAD_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.ID_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.NAME_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.NUMBER_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.PHONE_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.RADIO_BUTTON_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.STATIC_CONTENT_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.TEXT_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.dateEnumeratedQuestionDefinition;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.CfJsonDocumentContext;
import services.question.types.QuestionType;

public class QuestionJsonSamplerTest extends ResetPostgres {

  private QuestionJsonSampler.Factory questionJsonSamplerFactory;

  @Before
  public void setUp() {
    questionJsonSamplerFactory = instanceOf(QuestionJsonSampler.Factory.class);
  }

  @Test
  public void samplesAddressQuestion() {

    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.ADDRESS)
            .getSampleJson(ADDRESS_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_address_question\" : {\n"
                + "      \"zip\" : \"97403\",\n"
                + "      \"city\" : \"Springfield\",\n"
                + "      \"street\" : \"742 Evergreen Terrace\",\n"
                + "      \"latitude\" : \"44.0462\",\n"
                + "      \"well_known_id\" : \"23214\",\n"
                + "      \"service_area\" : \"springfield_county\",\n"
                + "      \"state\" : \"OR\",\n"
                + "      \"line2\" : null,\n"
                + "      \"corrected\" : null,\n"
                + "      \"longitude\" : \"-123.0236\"\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesCurrencyQuestion() {

    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.CURRENCY)
            .getSampleJson(CURRENCY_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_currency_question\" : {\n"
                + "      \"currency_dollars\" : 123.45\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesDateQuestion() {

    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.DATE)
            .getSampleJson(DATE_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_date_question\" : {\n"
                + "      \"date\" : \"2023-01-02\"\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesEmailQuestion() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.EMAIL)
            .getSampleJson(EMAIL_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_email_question\" : {\n"
                + "      \"email\" : \"homer.simpson@springfield.gov\"\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesFileUploadQuestion() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.FILEUPLOAD)
            .getSampleJson(FILE_UPLOAD_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_file_upload_question\" : {\n"
                + "      \"file_key\" :"
                + " \"http://localhost:9000/admin/applicant-files/my-file-key\"\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesIdQuestion() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.ID)
            .getSampleJson(ID_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_id_question\" : {\n"
                + "      \"id\" : \"12345\"\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesMultiSelectQuestion() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.CHECKBOX)
            .getSampleJson(CHECKBOX_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_checkbox_question\" : {\n"
                + "      \"selections\" : [ \"toaster\", \"pepper grinder\" ]\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesNameQuestion() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.NAME)
            .getSampleJson(NAME_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"name\" : {\n"
                + "      \"last_name\" : \"Simpson\",\n"
                + "      \"middle_name\" : \"Jay\",\n"
                + "      \"first_name\" : \"Homer\"\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesNumberQuestion() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.NUMBER)
            .getSampleJson(NUMBER_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_number_question\" : {\n"
                + "      \"number\" : 12321\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesPhoneQuestion() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.PHONE)
            .getSampleJson(PHONE_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_phone_question\" : {\n"
                + "      \"phone_number\" : \"+12143673764\"\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesSingleSelectQuestion() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.RADIO_BUTTON)
            .getSampleJson(RADIO_BUTTON_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_radio_button_question\" : {\n"
                + "      \"selection\" : \"winter (will hide next block)\"\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void samplesTextQuestion() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.TEXT)
            .getSampleJson(TEXT_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_text_question\" : {\n"
                + "      \"text\" : \"I love CiviForm!\"\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void staticQuestions_returnEmpty() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.STATIC)
            .getSampleJson(STATIC_CONTENT_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString()).isEqualTo("{ }");
  }

  // TODO(#4975): update this test once enumerator questions are supported.
  @Test
  public void enumeratorQuestions_returnEmpty() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.ENUMERATOR)
            .getSampleJson(ENUMERATOR_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(json.asPrettyJsonString()).isEqualTo("{ }");
  }

  // TODO(#5238): update this test once enumerated questions are supported.
  @Test
  public void enumeratedQuestions_returnEmpty() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.DATE)
            .getSampleJson(dateEnumeratedQuestionDefinition(1L).withPopulatedTestId());

    assertThat(json.asPrettyJsonString()).isEqualTo("{ }");
  }
}
