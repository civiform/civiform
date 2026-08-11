package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

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
import services.program.ProgramDefinition;
import services.question.QuestionAnswerer;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import support.ProgramBuilder;

public class ApplicationScoreCalculatorTest extends ResetPostgres {

  private static final ObjectMapper mapper = ObjectMapperSingleton.instance();

  private ApplicationScoreCalculator calculator;
  private ApplicantModel applicant;
  private ApplicantData snapshot;

  @Before
  public void setUp() {
    calculator = new ApplicationScoreCalculator(instanceOf(JsonPathPredicateGeneratorFactory.class));
    applicant = new ApplicantModel();
    snapshot = new ApplicantData();
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
            new MultiOptionQuestionDefinition(config.build(), options, type),
            LifecycleStage.ACTIVE)
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
  public void enrich_singleSelect_scoredOption_writesScoreAndTotal() {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion("scored dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    ProgramDefinition program = programWith(dropdown);
    Path path = questionPath(dropdown);
    QuestionAnswerer.answerSingleSelectQuestion(snapshot, path, 1L);

    calculator.enrich(applicant, snapshot, program);

    assertThat(snapshot.readDouble(ApplicationScoreMetadata.scorePath(path))).hasValue(10.5);
    assertThat(snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())).hasValue(10.5);
  }

  @Test
  public void enrich_singleSelect_negativeScore_contributesNegative() {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion("negative dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    ProgramDefinition program = programWith(dropdown);
    Path path = questionPath(dropdown);
    QuestionAnswerer.answerSingleSelectQuestion(snapshot, path, 3L);

    calculator.enrich(applicant, snapshot, program);

    assertThat(snapshot.readDouble(ApplicationScoreMetadata.scorePath(path))).hasValue(-4.25);
    assertThat(snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())).hasValue(-4.25);
  }

  @Test
  public void enrich_singleSelect_unscoredOption_writesNoKeyButZeroTotal() {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion("unscored dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    ProgramDefinition program = programWith(dropdown);
    Path path = questionPath(dropdown);
    QuestionAnswerer.answerSingleSelectQuestion(snapshot, path, 2L);

    calculator.enrich(applicant, snapshot, program);

    assertThat(snapshot.hasPath(ApplicationScoreMetadata.scorePath(path))).isFalse();
    // A zero-sum enrichment still persists 0: presence marks that scoring was applied.
    assertThat(snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())).hasValue(0.0);
  }

  @Test
  public void enrich_unansweredQuestions_writeNoPerQuestionKeys() {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion("blank dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    QuestionDefinition checkbox =
        saveMultiOptionQuestion("blank checkbox", MultiOptionQuestionType.CHECKBOX, scoredOptions());
    ProgramDefinition program = programWith(dropdown, checkbox);

    calculator.enrich(applicant, snapshot, program);

    assertThat(snapshot.hasPath(ApplicationScoreMetadata.scorePath(questionPath(dropdown))))
        .isFalse();
    assertThat(snapshot.hasPath(ApplicationScoreMetadata.scoresPath(questionPath(checkbox))))
        .isFalse();
    assertThat(snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())).hasValue(0.0);
  }

  @Test
  public void enrich_checkbox_mixedSelections_writesAlignedNullableArray() {
    QuestionDefinition checkbox =
        saveMultiOptionQuestion("mixed checkbox", MultiOptionQuestionType.CHECKBOX, scoredOptions());
    ProgramDefinition program = programWith(checkbox);
    Path path = questionPath(checkbox);
    // Stored order: scored, unscored, unknown id, duplicate of first, scored-negative.
    long[] selections = {3L, 2L, 999L, 3L, 1L};
    for (int i = 0; i < selections.length; i++) {
      QuestionAnswerer.answerMultiSelectQuestion(snapshot, path, i, selections[i]);
    }
    String selectionsBefore =
        snapshot.readLongList(path.join("selections")).orElseThrow().toString();

    calculator.enrich(applicant, snapshot, program);

    // Same length, stored order preserved; unscored/unknown/duplicate occurrences are null.
    assertThat(snapshot.readNullableDoubleList(ApplicationScoreMetadata.scoresPath(path)))
        .hasValue(Arrays.asList(-4.25, null, null, null, 10.5));
    // Only the first occurrence of the duplicated id contributes.
    assertThat(snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())).hasValue(6.25);
    // The stored selections answer is byte-identical.
    assertThat(snapshot.readLongList(path.join("selections")).orElseThrow().toString())
        .isEqualTo(selectionsBefore);
  }

  @Test
  public void enrich_checkbox_zeroScoredOption_contributesExplicitZero() {
    QuestionDefinition checkbox =
        saveMultiOptionQuestion("zero checkbox", MultiOptionQuestionType.CHECKBOX, scoredOptions());
    ProgramDefinition program = programWith(checkbox);
    Path path = questionPath(checkbox);
    QuestionAnswerer.answerMultiSelectQuestion(snapshot, path, 0, 4L);

    calculator.enrich(applicant, snapshot, program);

    assertThat(snapshot.readNullableDoubleList(ApplicationScoreMetadata.scoresPath(path)))
        .hasValue(Arrays.asList(0.0));
    assertThat(snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())).hasValue(0.0);
  }

  @Test
  public void enrich_decimalScores_totalUsesExactDecimalArithmetic() {
    QuestionDefinition checkbox =
        saveMultiOptionQuestion(
            "decimal checkbox",
            MultiOptionQuestionType.CHECKBOX,
            ImmutableList.of(
                option(1L, 0L, "tenth", Optional.of(0.1)),
                option(2L, 1L, "fifth", Optional.of(0.2))));
    ProgramDefinition program = programWith(checkbox);
    Path path = questionPath(checkbox);
    QuestionAnswerer.answerMultiSelectQuestion(snapshot, path, 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(snapshot, path, 1, 2L);

    calculator.enrich(applicant, snapshot, program);

    // Naive double addition would persist 0.30000000000000004; BigDecimal accumulation over the
    // entered values persists the exact decimal sum.
    assertThat(snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())).hasValue(0.3);
  }

  @Test
  public void enrich_yesNoQuestion_neverScores() {
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
    QuestionAnswerer.answerSingleSelectQuestion(snapshot, path, 1L);

    calculator.enrich(applicant, snapshot, program);

    assertThat(snapshot.hasPath(ApplicationScoreMetadata.scorePath(path))).isFalse();
    assertThat(snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())).hasValue(0.0);
  }

  @Test
  public void enrich_duplicateContextualizedPaths_countedOnce() {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion("dup dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    // A malformed program containing the same question in two blocks.
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram("duplicate question program")
            .withBlock()
            .withRequiredQuestionDefinition(dropdown)
            .withBlock()
            .withRequiredQuestionDefinition(dropdown)
            .buildDefinition();
    Path path = questionPath(dropdown);
    QuestionAnswerer.answerSingleSelectQuestion(snapshot, path, 1L);

    calculator.enrich(applicant, snapshot, program);

    assertThat(snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())).hasValue(10.5);
  }

  @Test
  public void enrich_repeatedQuestion_scoresEachInstance() {
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

    Path enumeratorPath =
        ApplicantData.APPLICANT_PATH.join(enumerator.getQuestionPathSegment());
    QuestionAnswerer.answerEnumeratorQuestion(
        snapshot, enumeratorPath, ImmutableList.of("first", "second"));
    Path firstInstance =
        enumeratorPath.atIndex(0).join(repeatedDropdown.getQuestionPathSegment());
    Path secondInstance =
        enumeratorPath.atIndex(1).join(repeatedDropdown.getQuestionPathSegment());
    QuestionAnswerer.answerSingleSelectQuestion(snapshot, firstInstance, 1L);
    QuestionAnswerer.answerSingleSelectQuestion(snapshot, secondInstance, 3L);

    calculator.enrich(applicant, snapshot, program);

    assertThat(snapshot.readDouble(ApplicationScoreMetadata.scorePath(firstInstance)))
        .hasValue(10.5);
    assertThat(snapshot.readDouble(ApplicationScoreMetadata.scorePath(secondInstance)))
        .hasValue(-4.25);
    assertThat(snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())).hasValue(6.25);
  }

  @Test
  public void enrich_onlyAddsSiblingScoreKeys_preservesExistingAnswers() throws Exception {
    QuestionDefinition dropdown =
        saveMultiOptionQuestion("subtree dropdown", MultiOptionQuestionType.DROPDOWN, scoredOptions());
    QuestionDefinition checkbox =
        saveMultiOptionQuestion("subtree checkbox", MultiOptionQuestionType.CHECKBOX, scoredOptions());
    ProgramDefinition program = programWith(dropdown, checkbox);
    QuestionAnswerer.answerSingleSelectQuestion(snapshot, questionPath(dropdown), 1L);
    QuestionAnswerer.answerMultiSelectQuestion(snapshot, questionPath(checkbox), 0, 999L);
    QuestionAnswerer.answerMultiSelectQuestion(snapshot, questionPath(checkbox), 1, 3L);
    QuestionAnswerer.answerMultiSelectQuestion(snapshot, questionPath(checkbox), 2, 3L);
    JsonNode applicantSubtreeBefore = mapper.readTree(snapshot.asJsonString()).get("applicant");

    calculator.enrich(applicant, snapshot, program);

    JsonNode after = mapper.readTree(snapshot.asJsonString());
    // Enrichment adds exactly a score key inside each scored question object and the root
    // total_score. Removing those additions must restore the applicant subtree byte for byte,
    // proving nothing else changed — including checkbox order, unknown ids, and duplicate ids.
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
