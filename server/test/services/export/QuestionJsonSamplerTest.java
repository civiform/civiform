package services.export;

import static controllers.dev.seeding.SampleQuestionDefinitions.ADDRESS_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.CURRENCY_QUESTION_DEFINITION;
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
  public void samplesEnumeratedDateQuestion() {
    CfJsonDocumentContext json =
        questionJsonSamplerFactory
            .create(QuestionType.DATE)
            .getSampleJson(dateEnumeratedQuestionDefinition(1L).withPopulatedTestId());

    assertThat(json.asPrettyJsonString())
        .isEqualTo(
            "{\n"
                + "  \"application\" : {\n"
                + "    \"sample_enumerated_date_question\" : {\n"
                + "      \"date\" : \"2023-01-02\"\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }
}
