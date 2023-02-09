package services.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import models.DisplayMode;
import models.Question;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.applicant.question.Scalar;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.types.QuestionDefinition;
import support.CfTestHelpers;
import support.ProgramBuilder;
import support.TestQuestionBank;

public class ProgramDefinitionTest extends ResetPostgres {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(true);

  @Test
  public void createProgramDefinition() {
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .build();
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setCreateTime(Instant.now())
            .setLastModifiedTime(Instant.now())
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .build();

    assertThat(def.id()).isEqualTo(123L);
  }

  @Test
  public void getBlockDefinition_hasValue() {
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .build();
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .build();

    assertThat(program.getBlockDefinitionByIndex(0)).hasValue(blockA);
  }

  @Test
  public void getBlockDefinition_outOfBounds_isEmpty() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .build();

    assertThat(program.getBlockDefinitionByIndex(0)).isEmpty();
  }

  @Test
  public void getProgramQuestionDefinition() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantJugglingNumber())
            .withRequiredCorrectedAddressQuestion(testQuestionBank.applicantAddress())
            .withRequiredQuestion(testQuestionBank.applicantDate())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantKitchenTools())
            .buildDefinition();

    assertThat(
            programDefinition
                .getProgramQuestionDefinition(testQuestionBank.applicantJugglingNumber().id)
                .id())
        .isEqualTo(testQuestionBank.applicantJugglingNumber().id);
  }

  @Test
  public void getProgramQuestionDefinition_notFound() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredCorrectedAddressQuestion(testQuestionBank.applicantAddress())
            .withRequiredQuestion(testQuestionBank.applicantDate())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantKitchenTools())
            .buildDefinition();

    assertThatThrownBy(
            () ->
                programDefinition.getProgramQuestionDefinition(
                    testQuestionBank.applicantJugglingNumber().id))
        .isInstanceOf(ProgramQuestionDefinitionNotFoundException.class);
  }

  @Test
  public void isQuestionUsedInPredicate() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withEligibilityDefinition(
                EligibilityDefinition.builder()
                    .setPredicate(
                        PredicateDefinition.create(
                            PredicateExpressionNode.create(
                                LeafAddressServiceAreaExpressionNode.builder()
                                    .setQuestionId(testQuestionBank.applicantAddress().id)
                                    .setServiceAreaId("seattle")
                                    .build()),
                            PredicateAction.ELIGIBLE_BLOCK))
                    .build())
            .withVisibilityPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.builder()
                            .setQuestionId(testQuestionBank.applicantDate().id)
                            .setScalar(Scalar.DATE)
                            .setOperator(Operator.IS_BEFORE)
                            .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-01-01"))
                            .build()),
                    PredicateAction.HIDE_BLOCK))
            .buildDefinition();

    assertThat(programDefinition.isQuestionUsedInPredicate(testQuestionBank.applicantAddress().id))
        .isTrue();
    assertThat(programDefinition.isQuestionUsedInPredicate(testQuestionBank.applicantDate().id))
        .isTrue();
    assertThat(
            programDefinition.isQuestionUsedInPredicate(
                testQuestionBank.applicantKitchenTools().id))
        .isFalse();
  }

  @Test
  public void hasQuestion_trueIfTheProgramUsesTheQuestion() {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB = testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();

    long programDefinitionId = 123L;
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionA, Optional.of(programDefinitionId)))
            .build();

    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(321L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionB, Optional.of(programDefinitionId)))
            .build();

    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(programDefinitionId)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .build();

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isTrue();
    assertThat(program.hasQuestion(questionC)).isFalse();
  }

  @Test
  public void localizedNameAndDescription() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "Applicant friendly name"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "English description"))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .build();

    assertThat(program.adminName()).isEqualTo("Admin name");
    assertThat(program.localizedName())
        .isEqualTo(LocalizedStrings.of(Locale.US, "Applicant friendly name"));
    assertThat(program.localizedDescription())
        .isEqualTo(LocalizedStrings.of(Locale.US, "English description"));

    assertThatThrownBy(() -> program.localizedName().get(Locale.FRANCE))
        .isInstanceOf(TranslationNotFoundException.class);
    assertThatThrownBy(() -> program.localizedDescription().get(Locale.FRANCE))
        .isInstanceOf(TranslationNotFoundException.class);
    assertThat(program.localizedName().getOrDefault(Locale.FRANCE))
        .isEqualTo("Applicant friendly name");
    assertThat(program.localizedDescription().getOrDefault(Locale.FRANCE))
        .isEqualTo("English description");
  }

  @Test
  public void updateNameAndDescription_replacesExistingValue() throws Exception {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "existing name"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "existing description"))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .build();

    program =
        program.toBuilder()
            .setLocalizedName(program.localizedName().updateTranslation(Locale.US, "new name"))
            .build();
    assertThat(program.localizedName().get(Locale.US)).isEqualTo("new name");

    program =
        program.toBuilder()
            .setLocalizedDescription(
                program.localizedDescription().updateTranslation(Locale.US, "new description"))
            .build();
    assertThat(program.localizedDescription().get(Locale.US)).isEqualTo("new description");
  }

  @Test
  public void getSupportedLocales_noQuestions_returnsOnlyLocalesSupportedByDisplayText() {
    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(Locale.US, "English description", Locale.GERMAN, "test"))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }

  @Test
  public void getSupportedLocales_returnsLocalesSupportedByQuestionsAndText() {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB = testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();

    long programDefinitionId = 123L;
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionA, Optional.of(programDefinitionId)))
            .build();
    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionB, Optional.of(programDefinitionId)))
            .addQuestion(
                ProgramQuestionDefinition.create(questionC, Optional.of(programDefinitionId)))
            .build();
    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(programDefinitionId)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(
                    Locale.US, "English description", Locale.GERMAN, "test", Locale.FRANCE, "test"))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }

  @Test
  public void getAvailablePredicateQuestionDefinitions()
      throws ProgramBlockDefinitionNotFoundException {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB = testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition questionC = testQuestionBank.applicantFile().getQuestionDefinition();
    QuestionDefinition questionD = testQuestionBank.applicantSeason().getQuestionDefinition();

    long programDefinitionId = 123L;
    // To aid readability and reduce errors the block names include the questions that are in them.
    BlockDefinition block1QAQB =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionA, Optional.of(programDefinitionId)))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    questionB,
                    Optional.of(programDefinitionId),
                    /* optional= */ false,
                    /* addressCorrectionEnabled= */ true))
            .build();
    BlockDefinition block2QC =
        BlockDefinition.builder()
            .setId(2L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionC, Optional.of(programDefinitionId)))
            .build();
    BlockDefinition block3QD =
        BlockDefinition.builder()
            .setId(3L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionD, Optional.of(programDefinitionId)))
            .build();
    ProgramDefinition programDefinition =
        ProgramDefinition.builder()
            .setId(programDefinitionId)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(
                    Locale.US, "English description", Locale.GERMAN, "test", Locale.FRANCE, "test"))
            .addBlockDefinition(block1QAQB)
            .addBlockDefinition(block2QC)
            .addBlockDefinition(block3QD)
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .build();

    // block1
    assertThat(
            programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(block1QAQB.id()))
        .isEmpty();
    assertThat(
            programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(block1QAQB.id()))
        .containsExactly(questionA, questionB);
    // block2
    assertThat(programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(block2QC.id()))
        .containsExactly(questionA, questionB);
    // Doesn't include the file upload question, which don't support predicates.
    assertThat(programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(block2QC.id()))
        .isEmpty();
    // block3
    // Doesn't include the file upload question, which don't support predicates.
    assertThat(programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(block3QD.id()))
        .containsExactly(questionA, questionB);
    assertThat(programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(block3QD.id()))
        .containsExactly(questionD);
  }

  @Test
  public void
      getAvailablePredicateQuestionDefinitions_withRepeatedBlocks_onlyIncludesQuestionsWithSameEnumeratorId()
          throws ProgramBlockDefinitionNotFoundException {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionBEnum =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.applicantHouseholdMemberName().getQuestionDefinition();
    QuestionDefinition questionDEnum =
        testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition();
    QuestionDefinition questionE =
        testQuestionBank.applicantHouseholdMemberDaysWorked().getQuestionDefinition();

    long programDefinitionId = 123L;
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionA, Optional.of(programDefinitionId)))
            .build();
    BlockDefinition blockBEnum =
        BlockDefinition.builder()
            .setId(2L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionBEnum, Optional.of(programDefinitionId)))
            .build();
    BlockDefinition blockC =
        BlockDefinition.builder()
            .setId(3L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionC, Optional.of(programDefinitionId)))
            .setEnumeratorId(Optional.of(2L))
            .build();
    BlockDefinition blockDEnum =
        BlockDefinition.builder()
            .setId(4L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionDEnum, Optional.of(programDefinitionId)))
            .setEnumeratorId(Optional.of(2L))
            .build();
    BlockDefinition blockE =
        BlockDefinition.builder()
            .setId(5L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .addQuestion(
                ProgramQuestionDefinition.create(questionE, Optional.of(programDefinitionId)))
            .setEnumeratorId(Optional.of(4L))
            .build();
    ProgramDefinition programDefinition =
        ProgramDefinition.builder()
            .setId(programDefinitionId)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(
                    Locale.US, "English description", Locale.GERMAN, "test", Locale.FRANCE, "test"))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockBEnum)
            .addBlockDefinition(blockC)
            .addBlockDefinition(blockDEnum)
            .addBlockDefinition(blockE)
            .build();

    // blockA (applicantName)
    assertThat(programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(blockA.id()))
        .isEmpty();
    assertThat(programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(blockA.id()))
        .containsExactly(questionA);
    // blockB (applicantHouseholdMembers)
    assertThat(
            programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(blockBEnum.id()))
        .containsExactly(questionA);
    assertThat(
            programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(blockBEnum.id()))
        .isEmpty();
    // blockC (applicantHouseholdMembers.householdMemberName)
    assertThat(programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(blockC.id()))
        .containsExactly(questionA);
    assertThat(programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(blockC.id()))
        .containsExactly(questionC);
    // blockD (applicantHouseholdMembers.householdMemberJobs)
    assertThat(
            programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(blockDEnum.id()))
        .containsExactly(questionA, questionC);
    assertThat(
            programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(blockDEnum.id()))
        .isEmpty();
    // blockE (applicantHouseholdMembers.householdMemberJobs.householdMemberJobIncome)
    assertThat(programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(blockE.id()))
        .containsExactly(questionA, questionC);
    assertThat(programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(blockE.id()))
        .containsExactly(questionE);
  }

  @Test
  public void insertBlockDefinitionInTheRightPlace_repeatedBlock() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build()
            .getProgramDefinition();
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setName("new screen")
            .setDescription("new screen")
            .setId(100L)
            .setEnumeratorId(Optional.of(1L))
            .build();

    ProgramDefinition result =
        programDefinition.insertBlockDefinitionInTheRightPlace(blockDefinition);

    assertThat(result.blockDefinitions()).hasSize(4);
    assertThat(result.getBlockDefinitionByIndex(0).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(0).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMembers().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(2).get().getQuestionCount()).isEqualTo(0);
    assertThat(result.getBlockDefinitionByIndex(3).get().isRepeated()).isFalse();
  }

  @Test
  public void moveBlock_up() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build()
            .getProgramDefinition();

    ProgramDefinition result = programDefinition.moveBlock(3L, ProgramDefinition.Direction.UP);

    assertThat(result.blockDefinitions()).hasSize(3);
    assertThat(result.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMembers().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(2).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(2).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition());
  }

  @Test
  public void moveBlock_down() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build()
            .getProgramDefinition();

    ProgramDefinition result = programDefinition.moveBlock(1L, ProgramDefinition.Direction.DOWN);

    assertThat(result.blockDefinitions()).hasSize(3);
    assertThat(result.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMembers().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(2).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(2).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition());
  }

  @Test
  public void moveBlock_up_withVisibilityPredicate() throws Exception {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    // Trying to move a block with a predicate before the block it depends on throws.
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    predicateQuestion.id,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("yellow"))),
            PredicateAction.SHOW_BLOCK);

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .withBlock()
            .withOptionalQuestion(testQuestionBank.applicantHouseholdMembers())
            .withBlock()
            .withVisibilityPredicate(predicate)
            .build()
            .getProgramDefinition();

    ProgramDefinition result = programDefinition.moveBlock(3L, ProgramDefinition.Direction.UP);

    assertThat(result.getBlockDefinitionByIndex(1)).isPresent();
    assertThat(result.getBlockDefinitionByIndex(1).get().visibilityPredicate()).contains(predicate);
  }

  @Test
  public void moveBlock_up_withVisibilityPredicate_throwsForIllegalMove() {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    // Trying to move a block with a predicate before the block it depends on throws.
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    predicateQuestion.id,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("yellow"))),
            PredicateAction.SHOW_BLOCK);

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .withBlock()
            .withVisibilityPredicate(predicate)
            .build()
            .getProgramDefinition();

    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(() -> programDefinition.moveBlock(2L, ProgramDefinition.Direction.UP))
        .withMessage(
            "This move is not possible - it would move a block condition before the question it"
                + " depends on");
  }

  @Test
  public void moveBlock_up_withEligibilityPredicate() throws Exception {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    // Trying to move a block with a predicate before the block it depends on throws.
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            predicateQuestion.id,
                            Scalar.TEXT,
                            Operator.EQUAL_TO,
                            PredicateValue.of("yellow"))),
                    PredicateAction.SHOW_BLOCK))
            .build();

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .withBlock()
            .withOptionalQuestion(testQuestionBank.applicantHouseholdMembers())
            .withBlock()
            .withEligibilityDefinition(eligibility)
            .build()
            .getProgramDefinition();

    ProgramDefinition result = programDefinition.moveBlock(3L, ProgramDefinition.Direction.UP);

    assertThat(result.getBlockDefinitionByIndex(1)).isPresent();
    assertThat(result.getBlockDefinitionByIndex(1).get().eligibilityDefinition())
        .contains(eligibility);
  }

  @Test
  public void moveBlock_up_withEligibilityPredicate_throwsForIllegalMove() {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    // Trying to move a block with a predicate before the block it depends on throws.
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            predicateQuestion.id,
                            Scalar.TEXT,
                            Operator.EQUAL_TO,
                            PredicateValue.of("yellow"))),
                    PredicateAction.SHOW_BLOCK))
            .build();

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .withBlock()
            .withEligibilityDefinition(eligibility)
            .build()
            .getProgramDefinition();

    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(() -> programDefinition.moveBlock(2L, ProgramDefinition.Direction.UP))
        .withMessage(
            "This move is not possible - it would move a block condition before the question it"
                + " depends on");
  }

  @Test
  public void moveBlock_up_withEligibilityPredicateAndQuestionInSameBlock() throws Exception {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    // Trying to move a block with a predicate before the block it depends on throws.
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            predicateQuestion.id,
                            Scalar.TEXT,
                            Operator.EQUAL_TO,
                            PredicateValue.of("yellow"))),
                    PredicateAction.SHOW_BLOCK))
            .build();

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withOptionalQuestion(testQuestionBank.applicantHouseholdMembers())
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .withEligibilityDefinition(eligibility)
            .build()
            .getProgramDefinition();

    ProgramDefinition result = programDefinition.moveBlock(2L, ProgramDefinition.Direction.UP);

    assertThat(result.getBlockDefinitionByIndex(0)).isPresent();
    assertThat(result.getBlockDefinitionByIndex(0).get().eligibilityDefinition())
        .contains(eligibility);
    assertThat(result.getQuestionDefinition(0, 0).getId()).isEqualTo(predicateQuestion.id);
  }

  @Test
  public void moveBlockDown_throwsForIllegalMove() {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    // Trying to move a block after a block that depends on it throws.
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    predicateQuestion.id,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("yellow"))),
            PredicateAction.SHOW_BLOCK);

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .withBlock()
            .withVisibilityPredicate(predicate)
            .build()
            .getProgramDefinition();

    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(() -> programDefinition.moveBlock(1L, ProgramDefinition.Direction.DOWN))
        .withMessage(
            "This move is not possible - it would move a block condition before the question it"
                + " depends on");
  }

  @Test
  public void hasValidPredicateOrdering() {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    predicateQuestion.id,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("yellow"))),
            PredicateAction.SHOW_BLOCK);

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .withBlock()
            .withVisibilityPredicate(predicate)
            .build()
            .getProgramDefinition();

    assertThat(programDefinition.hasValidPredicateOrdering()).isTrue();

    programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withVisibilityPredicate(predicate)
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .build()
            .getProgramDefinition();

    assertThat(programDefinition.hasValidPredicateOrdering()).isFalse();
  }

  @Test
  public void hasValidPredicateOrdering_returnsFalseIfQuestionsAreInSameBlockAsPredicate() {
    Question predicateQuestion = testQuestionBank.applicantFavoriteColor();
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    predicateQuestion.id,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("yellow"))),
            PredicateAction.SHOW_BLOCK);

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .withVisibilityPredicate(predicate)
            .build()
            .getProgramDefinition();

    assertThat(programDefinition.hasValidPredicateOrdering()).isFalse();
  }

  @Test
  public void getCreateTimeWhenExist() {
    Instant now = Instant.now();
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .setCreateTime(now)
            .build();
    assertThat(def.createTime().get()).isEqualTo(now);
  }

  @Test
  public void getCreateTimeWhenDoesntExist() {
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .build();
    assertThat(def.createTime().isPresent()).isFalse();
  }

  @Test
  public void getLastModifiedTimeWhenExist() {
    Instant now = Instant.now();
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .setLastModifiedTime(now)
            .build();
    assertThat(def.lastModifiedTime().get()).isEqualTo(now);
  }

  @Test
  public void getLastModifiedTimeWhenDoesntExist() {
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setStatusDefinitions(new StatusDefinitions())
            .setDisplayMode(DisplayMode.PUBLIC)
            .build();
    assertThat(def.lastModifiedTime().isPresent()).isFalse();
  }
}
