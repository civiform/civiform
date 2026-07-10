package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.applicant.question.Scalar;
import services.program.EligibilityDefinition;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.ActiveAndDraftQuestions;
import services.question.QuestionService;
import support.ProgramBuilder;

public class QuestionBankCsvExporterTest extends ResetPostgres {

  private static final CSVFormat DEFAULT_FORMAT = CSVFormat.DEFAULT.builder().setHeader().get();

  private QuestionBankCsvExporter exporter;
  private QuestionService questionService;

  @Before
  public void setUp() {
    exporter = instanceOf(QuestionBankCsvExporter.class);
    questionService = instanceOf(QuestionService.class);
  }

  // ---------------------------------------------------------------------------
  // Headers
  // ---------------------------------------------------------------------------

  @Test
  public void export_headers_matchExpectedColumns() throws IOException {
    testQuestionBank.textApplicantFavoriteColor();

    String csv = exporter.export(getActiveAndDraftQuestions());
    List<CSVRecord> records = parseRecords(csv);

    assertThat(records.get(0).getParser().getHeaderNames())
        .containsExactly(
            "Admin ID",
            "Question Text",
            "Question Help Text",
            "Admin Description",
            "Question Type",
            "Answer Options",
            "Universal",
            "Eligibility Question",
            "Programs",
            "Status",
            "Last Modified");
  }

  @Test
  public void export_emptyQuestionBank_producesHeaderRowOnly() throws IOException {
    String csv = exporter.export(getActiveAndDraftQuestions());
    List<CSVRecord> records = parseRecords(csv);

    assertThat(records).isEmpty();
    assertThat(csv).startsWith("Admin ID,");
  }

  // ---------------------------------------------------------------------------
  // Question Type column
  // ---------------------------------------------------------------------------

  @Test
  public void export_questionType_address() throws IOException {
    testQuestionBank.addressApplicantAddress();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Admin ID")).isEqualTo("applicant address");
    assertThat(records.get(0).get("Question Type")).isEqualTo("Address");
  }

  @Test
  public void export_questionType_text() throws IOException {
    testQuestionBank.textApplicantFavoriteColor();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Admin ID")).isEqualTo("applicant favorite color");
    assertThat(records.get(0).get("Question Type")).isEqualTo("Text");
  }

  @Test
  public void export_questionType_dropdown() throws IOException {
    testQuestionBank.dropdownApplicantIceCream();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Admin ID")).isEqualTo("applicant ice cream");
    assertThat(records.get(0).get("Question Type")).isEqualTo("Dropdown");
  }

  @Test
  public void export_questionType_checkbox() throws IOException {
    testQuestionBank.checkboxApplicantKitchenTools();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Admin ID")).isEqualTo("kitchen tools");
    assertThat(records.get(0).get("Question Type")).isEqualTo("Checkbox");
  }

  @Test
  public void export_questionType_radio() throws IOException {
    testQuestionBank.radioApplicantFavoriteSeason();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Admin ID")).isEqualTo("applicant favorite season");
    assertThat(records.get(0).get("Question Type")).isEqualTo("Radio Button");
  }

  // ---------------------------------------------------------------------------
  // Answer Options column
  // ---------------------------------------------------------------------------

  @Test
  public void export_answerOptions_emptyForNonMultiOptionTypes() throws IOException {
    testQuestionBank.addressApplicantAddress();
    testQuestionBank.textApplicantFavoriteColor();
    testQuestionBank.numberApplicantJugglingNumber();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    // All three of: address, favorite color, juggling number — sorted alphabetically
    // "applicant address", "applicant favorite color", "applicant juggling number"
    assertThat(records).hasSize(3);
    for (CSVRecord record : records) {
      assertThat(record.get("Answer Options"))
          .as("Expected no answer options for %s", record.get("Admin ID"))
          .isEmpty();
    }
  }

  @Test
  public void export_answerOptions_dropdownOptionsSemicolonSeparated() throws IOException {
    testQuestionBank.dropdownApplicantIceCream();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Answer Options"))
        .isEqualTo("Chocolate; Strawberry; Vanilla; Coffee");
  }

  @Test
  public void export_answerOptions_checkboxOptionsSemicolonSeparated() throws IOException {
    testQuestionBank.checkboxApplicantKitchenTools();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Answer Options"))
        .isEqualTo("Toaster; Pepper Grinder; Garlic Press");
  }

  @Test
  public void export_answerOptions_radioOptionsSemicolonSeparated() throws IOException {
    testQuestionBank.radioApplicantFavoriteSeason();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Answer Options")).isEqualTo("Winter; Spring; Summer; Fall");
  }

  // ---------------------------------------------------------------------------
  // Programs column
  // ---------------------------------------------------------------------------

  @Test
  public void export_programs_emptyWhenQuestionNotUsedInAnyProgram() throws IOException {
    testQuestionBank.textApplicantFavoriteColor();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Programs")).isEmpty();
  }

  @Test
  public void export_programs_showsActiveProgramName() throws IOException {
    var colorQuestion = testQuestionBank.textApplicantFavoriteColor();

    ProgramBuilder.newActiveProgram("color-program")
        .withBlock("Screen 1")
        .withRequiredQuestion(colorQuestion)
        .build();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Admin ID")).isEqualTo("applicant favorite color");
    assertThat(records.get(0).get("Programs")).isEqualTo("color-program");
  }

  @Test
  public void export_programs_showsMultipleProgramNamesSemicolonSeparated() throws IOException {
    var colorQuestion = testQuestionBank.textApplicantFavoriteColor();

    ProgramBuilder.newActiveProgram("alpha-program")
        .withBlock("Screen 1")
        .withRequiredQuestion(colorQuestion)
        .build();
    ProgramBuilder.newActiveProgram("beta-program")
        .withBlock("Screen 1")
        .withRequiredQuestion(colorQuestion)
        .build();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    // Programs are sorted alphabetically and separated by "; "
    assertThat(records.get(0).get("Programs")).isEqualTo("alpha-program; beta-program");
  }

  // ---------------------------------------------------------------------------
  // Eligibility Question column
  // ---------------------------------------------------------------------------

  @Test
  public void export_eligibilityQuestion_noWhenNotUsedInAnyProgram() throws IOException {
    testQuestionBank.textApplicantFavoriteColor();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Eligibility Question")).isEqualTo("No");
  }

  @Test
  public void export_eligibilityQuestion_noWhenUsedInProgramWithoutEligibilityPredicate()
      throws IOException {
    var colorQuestion = testQuestionBank.textApplicantFavoriteColor();

    ProgramBuilder.newActiveProgram("regular-program")
        .withBlock("Screen 1")
        .withRequiredQuestion(colorQuestion)
        .build();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Admin ID")).isEqualTo("applicant favorite color");
    assertThat(records.get(0).get("Eligibility Question")).isEqualTo("No");
  }

  @Test
  public void export_eligibilityQuestion_yesWhenUsedInEligibilityPredicate() throws IOException {
    var colorQuestion = testQuestionBank.textApplicantFavoriteColor();

    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            colorQuestion.id,
                            Scalar.TEXT,
                            Operator.EQUAL_TO,
                            PredicateValue.of("blue"))),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();

    ProgramBuilder.newActiveProgram("eligibility-program")
        .withBlock("Screen 1")
        .withRequiredQuestion(colorQuestion)
        .withEligibilityDefinition(eligibilityDef)
        .build();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    assertThat(records).hasSize(1);
    assertThat(records.get(0).get("Admin ID")).isEqualTo("applicant favorite color");
    assertThat(records.get(0).get("Eligibility Question")).isEqualTo("Yes");
  }

  @Test
  public void export_eligibilityQuestion_onlyYesForQuestionsActuallyInEligibilityPredicate()
      throws IOException {
    // Two questions in the same program: only color is in the eligibility predicate.
    var colorQuestion = testQuestionBank.textApplicantFavoriteColor();
    var addressQuestion = testQuestionBank.addressApplicantAddress();

    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            colorQuestion.id,
                            Scalar.TEXT,
                            Operator.EQUAL_TO,
                            PredicateValue.of("blue"))),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();

    // Block 1: address (no eligibility). Block 2: color (has eligibility).
    ProgramBuilder.newActiveProgram("mixed-program")
        .withBlock("Screen 1")
        .withRequiredQuestion(addressQuestion)
        .withBlock("Screen 2")
        .withRequiredQuestion(colorQuestion)
        .withEligibilityDefinition(eligibilityDef)
        .build();

    List<CSVRecord> records = parseRecords(exporter.export(getActiveAndDraftQuestions()));

    // Sorted: "applicant address", "applicant favorite color"
    assertThat(records).hasSize(2);
    assertThat(records.get(0).get("Admin ID")).isEqualTo("applicant address");
    assertThat(records.get(0).get("Eligibility Question")).isEqualTo("No");

    assertThat(records.get(1).get("Admin ID")).isEqualTo("applicant favorite color");
    assertThat(records.get(1).get("Eligibility Question")).isEqualTo("Yes");
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private ActiveAndDraftQuestions getActiveAndDraftQuestions() {
    return questionService.getReadOnlyQuestionServiceSync().getActiveAndDraftQuestions();
  }

  private static List<CSVRecord> parseRecords(String csv) throws IOException {
    try (CSVParser parser = CSVParser.parse(new StringReader(csv), DEFAULT_FORMAT)) {
      return parser.getRecords();
    }
  }
}
