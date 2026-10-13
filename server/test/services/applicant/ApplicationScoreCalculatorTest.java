package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import models.ApplicantModel;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.ObjectMapperSingleton;
import services.Path;
import services.applicant.predicate.JsonPathPredicateGeneratorFactory;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.QuestionAnswerer;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import support.ProgramBuilder;

public class ApplicationScoreCalculatorTest extends ResetPostgres {

  private static final ObjectMapper mapper = ObjectMapperSingleton.instance();

  private JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory;
  private ApplicationScoreCalculator calculator;
  private ApplicantModel applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    jsonPathPredicateGeneratorFactory = instanceOf(JsonPathPredicateGeneratorFactory.class);
    calculator = new ApplicationScoreCalculator();
    applicant = new ApplicantModel();
    applicantData = new ApplicantData();
  }

  /** Computes scores from the applicant data's answers against the program, as the service does. */
  private ApplicationScores calculate(ProgramDefinition program) {
    return calculator.calculate(
        new ReadOnlyApplicantProgramService(
            jsonPathPredicateGeneratorFactory, applicant, applicantData, program));
  }

  /** Computes scores and writes them back to the applicant data, as submit does end to end. */
  private void calculateAndApply(ProgramDefinition program) {
    calculate(program).applyTo(applicantData);
  }

  private static ImmutableList<QuestionOption> scoredOptions() {
    return ImmutableList.of(
        option(1L, 0L, "ten_and_a_half", Optional.of(10.5)),
        option(2L, 1L, "unscored", Optional.empty()),
        option(3L, 2L, "minus_four_and_a_quarter", Optional.of(-4.25)),
        option(4L, 3L, "zero", Optional.of(0.0)));
  }

  private static QuestionOption option(
      long id, long displayOrder, String adminName, Optional<Double> score) {
    return QuestionOption.create(
        id,
        displayOrder,
        adminName,
        LocalizedStrings.of(Locale.US, adminName),
        /* displayInAnswerOptions= */ Optional.of(true),
        score);
  }

  private QuestionDefinition saveMultiOptionQuestion(
      String name, MultiOptionQuestionType type, ImmutableList<QuestionOption> options) {
    return saveMultiOptionQuestion(name, type, options, Optional.empty());
  }

  private QuestionDefinition saveMultiOptionQuestion(
      String name,
      MultiOptionQuestionType type,
      ImmutableList<QuestionOption> options,
      Optional<Long> enumeratorId) {
    QuestionDefinitionConfig.Builder config =
        QuestionDefinitionConfig.builder()
            .setName(name)
            .setDescription(name)
            .setQuestionText(LocalizedStrings.of(Locale.US, name + "?"))
            .setQuestionHelpText(LocalizedStrings.empty());
    enumeratorId.ifPresent(config::setEnumeratorId);
    return testQuestionBank
        .maybeSave(
            new MultiOptionQuestionDefinition(config.build(), options, type), LifecycleStage.ACTIVE)
        .getQuestionDefinition();
  }

  private static Path questionPath(QuestionDefinition question) {
    return ApplicantData.APPLICANT_PATH.join(question.getQuestionPathSegment());
  }

  private ProgramDefinition programWith(QuestionDefinition... questions) {
    ProgramBuilder builder = ProgramBuilder.newActiveProgram("score calc program");
    ProgramBuilder.BlockBuilder block = builder.withBlock();
    for (QuestionDefinition question : questions) {
      block = block.withRequiredQuestionDefinition(question);
    }
    return block.buildDefinition();
  }

  @Test
  public void calculate_returnsScoresWithoutWritingToApplicantData() {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion(
            "value dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    QuestionDefinition checkbox =
        saveMultiOptionQuestion(
            "value checkbox", MultiOptionQuestionType.CHECKBOX, scoredOptions());
    ProgramDefinition program = programWith(dropdown, checkbox);
    QuestionAnswerer.answerSingleSelectQuestion(applicantData, questionPath(dropdown), 1L);
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, questionPath(checkbox), 0, 3L);
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, questionPath(checkbox), 1, 2L);
    String applicantDataBefore = applicantData.asJsonString();

    ApplicationScores scores = calculate(program);

    assertThat(scores.singleSelectScores()).containsExactly(entry(questionPath(dropdown), 10.5));
    assertThat(scores.checkboxScores())
        .containsExactly(
            entry(questionPath(checkbox), ImmutableList.of(Optional.of(-4.25), Optional.empty())));
    assertThat(scores.total()).isEqualTo(6.25);
    // Calculation is pure; only applyTo writes.
    assertThat(applicantData.asJsonString()).isEqualTo(applicantDataBefore);
  }

  @Test
  public void calculateAndApply_singleSelect_scoredOption_writesScoreAndTotal() {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion(
            "scored dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    ProgramDefinition program = programWith(dropdown);
    Path path = questionPath(dropdown);
    QuestionAnswerer.answerSingleSelectQuestion(applicantData, path, 1L);

    calculateAndApply(program);

    assertThat(applicantData.readDouble(ApplicantData.scorePath(path))).hasValue(10.5);
    assertThat(applicantData.readDouble(ApplicationScores.TOTAL_SCORE_PATH)).hasValue(10.5);
  }

  @Test
  public void calculateAndApply_singleSelect_unscoredOption_writesNoKeyButZeroTotal() {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion(
            "unscored dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    ProgramDefinition program = programWith(dropdown);
    Path path = questionPath(dropdown);
    QuestionAnswerer.answerSingleSelectQuestion(applicantData, path, 2L);

    calculateAndApply(program);

    assertThat(applicantData.hasPath(ApplicantData.scorePath(path))).isFalse();
    // A zero-sum result still persists 0: presence marks that scoring was applied.
    assertThat(applicantData.readDouble(ApplicationScores.TOTAL_SCORE_PATH)).hasValue(0.0);
  }

  @Test
  public void calculateAndApply_unansweredQuestions_writeNoPerQuestionKeys() {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion(
            "blank dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    QuestionDefinition checkbox =
        saveMultiOptionQuestion(
            "blank checkbox", MultiOptionQuestionType.CHECKBOX, scoredOptions());
    ProgramDefinition program = programWith(dropdown, checkbox);

    calculateAndApply(program);

    assertThat(applicantData.hasPath(ApplicantData.scorePath(questionPath(dropdown)))).isFalse();
    assertThat(applicantData.hasPath(ApplicantData.scoresPath(questionPath(checkbox)))).isFalse();
    assertThat(applicantData.readDouble(ApplicationScores.TOTAL_SCORE_PATH)).hasValue(0.0);
  }

  @Test
  public void calculateAndApply_checkbox_mixedSelections_writesAlignedNullableArray() {
    QuestionDefinition checkbox =
        saveMultiOptionQuestion(
            "mixed checkbox", MultiOptionQuestionType.CHECKBOX, scoredOptions());
    ProgramDefinition program = programWith(checkbox);
    Path path = questionPath(checkbox);
    // Stored order: scored, unscored, unknown id, duplicate of first, scored-negative.
    long[] selections = {3L, 2L, 999L, 3L, 1L};
    for (int i = 0; i < selections.length; i++) {
      QuestionAnswerer.answerMultiSelectQuestion(applicantData, path, i, selections[i]);
    }
    String selectionsBefore =
        applicantData.readLongList(path.join("selections")).orElseThrow().toString();

    calculateAndApply(program);

    // Same length, stored order preserved; unscored/unknown/duplicate occurrences are null.
    assertThat(applicantData.readNullableDoubleList(ApplicantData.scoresPath(path)))
        .hasValue(Arrays.asList(-4.25, null, null, null, 10.5));
    // Only the first occurrence of the duplicated id contributes.
    assertThat(applicantData.readDouble(ApplicationScores.TOTAL_SCORE_PATH)).hasValue(6.25);
    // The stored selections answer is byte-identical.
    assertThat(applicantData.readLongList(path.join("selections")).orElseThrow().toString())
        .isEqualTo(selectionsBefore);
  }

  @Test
  public void calculateAndApply_checkbox_zeroScoredOption_contributesExplicitZero() {
    QuestionDefinition checkbox =
        saveMultiOptionQuestion("zero checkbox", MultiOptionQuestionType.CHECKBOX, scoredOptions());
    ProgramDefinition program = programWith(checkbox);
    Path path = questionPath(checkbox);
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, path, 0, 4L);

    calculateAndApply(program);

    assertThat(applicantData.readNullableDoubleList(ApplicantData.scoresPath(path)))
        .hasValue(Arrays.asList(0.0));
    assertThat(applicantData.readDouble(ApplicationScores.TOTAL_SCORE_PATH)).hasValue(0.0);
  }

  @Test
  public void calculateAndApply_decimalScores_totalUsesExactDecimalArithmetic() {
    QuestionDefinition checkbox =
        saveMultiOptionQuestion(
            "decimal checkbox",
            MultiOptionQuestionType.CHECKBOX,
            ImmutableList.of(
                option(1L, 0L, "tenth", Optional.of(0.1)),
                option(2L, 1L, "fifth", Optional.of(0.2))));
    ProgramDefinition program = programWith(checkbox);
    Path path = questionPath(checkbox);
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, path, 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, path, 1, 2L);

    calculateAndApply(program);

    // Naive double addition would persist 0.30000000000000004; BigDecimal accumulation over the
    // entered values persists the exact decimal sum.
    assertThat(applicantData.readDouble(ApplicationScores.TOTAL_SCORE_PATH)).hasValue(0.3);
  }

  @Test
  public void calculateAndApply_yesNoQuestion_neverScores() {
    // A Yes/No question whose stored options somehow carry scores must still not score.
    QuestionDefinition yesNo =
        saveMultiOptionQuestion(
            "scored yes no",
            MultiOptionQuestionType.YES_NO,
            ImmutableList.of(
                option(1L, 0L, "yes", Optional.of(100.0)),
                option(0L, 1L, "no", Optional.of(50.0))));
    ProgramDefinition program = programWith(yesNo);
    Path path = questionPath(yesNo);
    QuestionAnswerer.answerSingleSelectQuestion(applicantData, path, 1L);

    calculateAndApply(program);

    assertThat(applicantData.hasPath(ApplicantData.scorePath(path))).isFalse();
    assertThat(applicantData.readDouble(ApplicationScores.TOTAL_SCORE_PATH)).hasValue(0.0);
  }

  @Test
  public void calculateAndApply_repeatedQuestion_scoresEachInstance() {
    QuestionDefinition enumerator =
        testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition repeatedDropdown =
        saveMultiOptionQuestion(
            "repeated scored dropdown",
            MultiOptionQuestionType.DROPDOWN,
            scoredOptions(),
            Optional.of(enumerator.getId()));
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram("repeated scoring program")
            .withBlock()
            .withRequiredQuestionDefinition(enumerator)
            .withRepeatedBlock()
            .withRequiredQuestionDefinition(repeatedDropdown)
            .buildDefinition();

    Path enumeratorPath = ApplicantData.APPLICANT_PATH.join(enumerator.getQuestionPathSegment());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantData, enumeratorPath, ImmutableList.of("first", "second"));
    Path firstInstance = enumeratorPath.atIndex(0).join(repeatedDropdown.getQuestionPathSegment());
    Path secondInstance = enumeratorPath.atIndex(1).join(repeatedDropdown.getQuestionPathSegment());
    QuestionAnswerer.answerSingleSelectQuestion(applicantData, firstInstance, 1L);
    QuestionAnswerer.answerSingleSelectQuestion(applicantData, secondInstance, 3L);

    calculateAndApply(program);

    assertThat(applicantData.readDouble(ApplicantData.scorePath(firstInstance))).hasValue(10.5);
    assertThat(applicantData.readDouble(ApplicantData.scorePath(secondInstance))).hasValue(-4.25);
    assertThat(applicantData.readDouble(ApplicationScores.TOTAL_SCORE_PATH)).hasValue(6.25);
  }

  @Test
  public void calculateAndApply_hiddenBlock_staleAnswersDoNotCount() {
    QuestionDefinition color =
        testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition();
    QuestionDefinition dropdown =
        saveMultiOptionQuestion(
            "gated dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    QuestionDefinition checkbox =
        saveMultiOptionQuestion(
            "gated checkbox", MultiOptionQuestionType.CHECKBOX, scoredOptions());
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram("hidden block scoring program")
            .withBlock()
            .withRequiredQuestionDefinition(color)
            .withBlock()
            .withVisibilityPredicate(hideBlockWhenColorIs(color, "blue"))
            .withRequiredQuestionDefinition(dropdown)
            .withRequiredQuestionDefinition(checkbox)
            .buildDefinition();
    // The applicant answered the scored block, then changed the color answer to one that hides it.
    QuestionAnswerer.answerSingleSelectQuestion(applicantData, questionPath(dropdown), 1L);
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, questionPath(checkbox), 0, 3L);
    QuestionAnswerer.answerTextQuestion(applicantData, questionPath(color), "blue");

    calculateAndApply(program);

    assertThat(applicantData.hasPath(ApplicantData.scorePath(questionPath(dropdown)))).isFalse();
    assertThat(applicantData.hasPath(ApplicantData.scoresPath(questionPath(checkbox)))).isFalse();
    assertThat(applicantData.readDouble(ApplicationScores.TOTAL_SCORE_PATH)).hasValue(0.0);
  }

  @Test
  public void calculateAndApply_shownBlock_answersCount() {
    QuestionDefinition color =
        testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition();
    QuestionDefinition dropdown =
        saveMultiOptionQuestion(
            "shown dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram("shown block scoring program")
            .withBlock()
            .withRequiredQuestionDefinition(color)
            .withBlock()
            .withVisibilityPredicate(hideBlockWhenColorIs(color, "blue"))
            .withRequiredQuestionDefinition(dropdown)
            .buildDefinition();
    QuestionAnswerer.answerTextQuestion(applicantData, questionPath(color), "red");
    QuestionAnswerer.answerSingleSelectQuestion(applicantData, questionPath(dropdown), 1L);

    calculateAndApply(program);

    assertThat(applicantData.readDouble(ApplicantData.scorePath(questionPath(dropdown))))
        .hasValue(10.5);
    assertThat(applicantData.readDouble(ApplicationScores.TOTAL_SCORE_PATH)).hasValue(10.5);
  }

  private static PredicateDefinition hideBlockWhenColorIs(QuestionDefinition color, String value) {
    return PredicateDefinition.create(
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                color.getId(), Scalar.TEXT, Operator.EQUAL_TO, PredicateValue.of(value))),
        PredicateAction.HIDE_BLOCK);
  }

  @Test
  public void calculateAndApply_onlyAddsSiblingScoreKeys_preservesExistingAnswers()
      throws Exception {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion(
            "subtree dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    QuestionDefinition checkbox =
        saveMultiOptionQuestion(
            "subtree checkbox", MultiOptionQuestionType.CHECKBOX, scoredOptions());
    ProgramDefinition program = programWith(dropdown, checkbox);
    QuestionAnswerer.answerSingleSelectQuestion(applicantData, questionPath(dropdown), 1L);
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, questionPath(checkbox), 0, 999L);
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, questionPath(checkbox), 1, 3L);
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, questionPath(checkbox), 2, 3L);
    JsonNode applicantSubtreeBefore =
        mapper.readTree(applicantData.asJsonString()).get("applicant");

    calculateAndApply(program);

    JsonNode after = mapper.readTree(applicantData.asJsonString());
    // Enrichment adds only a score key inside each scored question and the root total_score.
    // Removing those must restore the applicant subtree exactly, including checkbox order, unknown
    // ids, and duplicate ids.
    ObjectNode applicantAfter = ((ObjectNode) after.get("applicant")).deepCopy();
    JsonNode removedScore =
        ((ObjectNode) applicantAfter.get(questionPath(dropdown).keyName())).remove("score");
    JsonNode removedScores =
        ((ObjectNode) applicantAfter.get(questionPath(checkbox).keyName())).remove("scores");
    assertThat(removedScore).isNotNull();
    assertThat(removedScores).isNotNull();
    assertThat(applicantAfter).isEqualTo(applicantSubtreeBefore);
    assertThat(after.has("total_score")).isTrue();
  }
}
