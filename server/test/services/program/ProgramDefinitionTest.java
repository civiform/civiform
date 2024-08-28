package services.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.ProgramAcls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import models.DisplayMode;
import models.QuestionModel;
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
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
            .build();
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setCreateTime(Instant.now())
            .setLastModifiedTime(Instant.now())
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
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
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
            .build();
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();

    assertThat(program.getBlockDefinitionByIndex(0)).isEmpty();
  }

  @Test
  public void getProgramQuestionDefinition() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.numberApplicantJugglingNumber())
            .withRequiredCorrectedAddressQuestion(testQuestionBank.addressApplicantAddress())
            .withRequiredQuestion(testQuestionBank.dateApplicantBirthdate())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.checkboxApplicantKitchenTools())
            .buildDefinition();

    assertThat(
            programDefinition
                .getProgramQuestionDefinition(testQuestionBank.numberApplicantJugglingNumber().id)
                .id())
        .isEqualTo(testQuestionBank.numberApplicantJugglingNumber().id);
  }

  @Test
  public void getProgramQuestionDefinition_notFound() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredCorrectedAddressQuestion(testQuestionBank.addressApplicantAddress())
            .withRequiredQuestion(testQuestionBank.dateApplicantBirthdate())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.checkboxApplicantKitchenTools())
            .buildDefinition();

    assertThatThrownBy(
            () ->
                programDefinition.getProgramQuestionDefinition(
                    testQuestionBank.numberApplicantJugglingNumber().id))
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
                                LeafAddressServiceAreaExpressionNode.create(
                                    testQuestionBank.addressApplicantAddress().id,
                                    "seattle",
                                    Operator.IN_SERVICE_AREA)),
                            PredicateAction.ELIGIBLE_BLOCK))
                    .build())
            .withVisibilityPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.builder()
                            .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                            .setScalar(Scalar.DATE)
                            .setOperator(Operator.IS_BEFORE)
                            .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-01-01"))
                            .build()),
                    PredicateAction.HIDE_BLOCK))
            .buildDefinition();

    assertThat(
            programDefinition.isQuestionUsedInPredicate(
                testQuestionBank.addressApplicantAddress().id))
        .isTrue();
    assertThat(
            programDefinition.isQuestionUsedInPredicate(
                testQuestionBank.dateApplicantBirthdate().id))
        .isTrue();
    assertThat(
            programDefinition.isQuestionUsedInPredicate(
                testQuestionBank.checkboxApplicantKitchenTools().id))
        .isFalse();

    // program has eligibility enabled
    assertThat(programDefinition.hasEligibilityEnabled()).isTrue();
  }

  @Test
  public void hasQuestion_trueIfTheProgramUsesTheQuestion() {
    QuestionDefinition questionA = testQuestionBank.nameApplicantName().getQuestionDefinition();
    QuestionDefinition questionB =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition();

    long programDefinitionId = 123L;
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(questionA, Optional.of(programDefinitionId)))
            .build();

    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(321L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isTrue();
    assertThat(program.hasQuestion(questionC)).isFalse();
    assertThat(program.getQuestionIdsInProgram())
        .containsOnly(questionA.getId(), questionB.getId());
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
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
    HashSet<Long> tiGroups = new HashSet<>();
    tiGroups.add(1L);
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "existing name"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "existing description"))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls(tiGroups))
            .setCategories(ImmutableList.of())
            .build();

    program =
        program.toBuilder()
            .setLocalizedName(program.localizedName().updateTranslation(Locale.US, "new name"))
            .build();
    assertThat(program.localizedName().get(Locale.US)).isEqualTo("new name");
    assertThat(program.acls().getTiProgramViewAcls()).containsOnly(1L);

    program =
        program.toBuilder()
            .setLocalizedDescription(
                program.localizedDescription().updateTranslation(Locale.US, "new description"))
            .build();
    assertThat(program.localizedDescription().get(Locale.US)).isEqualTo("new description");
    assertThat(program.acls().getTiProgramViewAcls()).containsOnly(1L);
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }

  @Test
  public void getSupportedLocales_returnsLocalesSupportedByQuestionsAndText() {
    QuestionDefinition questionA = testQuestionBank.nameApplicantName().getQuestionDefinition();
    QuestionDefinition questionB =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition();

    long programDefinitionId = 123L;
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(questionA, Optional.of(programDefinitionId)))
            .build();
    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }

  @Test
  public void getAvailablePredicateQuestionDefinitions()
      throws ProgramBlockDefinitionNotFoundException {
    QuestionDefinition questionA = testQuestionBank.nameApplicantName().getQuestionDefinition();
    QuestionDefinition questionB =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.fileUploadApplicantFile().getQuestionDefinition();
    QuestionDefinition questionD =
        testQuestionBank.radioApplicantFavoriteSeason().getQuestionDefinition();

    long programDefinitionId = 123L;
    // To aid readability and reduce errors the block names include the questions that are in them.
    BlockDefinition block1QAQB =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
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
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(questionC, Optional.of(programDefinitionId)))
            .build();
    BlockDefinition block3QD =
        BlockDefinition.builder()
            .setId(3L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
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
  public void getAvailablePredicateQuestionDefinitions_doesNotDependOnBlockIdOrder()
      throws ProgramBlockDefinitionNotFoundException {
    QuestionDefinition questionA = testQuestionBank.nameApplicantName().getQuestionDefinition();
    QuestionDefinition questionB =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.fileUploadApplicantFile().getQuestionDefinition();
    QuestionDefinition questionD =
        testQuestionBank.radioApplicantFavoriteSeason().getQuestionDefinition();

    long programDefinitionId = 123L;
    // The blocks have their IDs set to non-consecutive numbers. This mimics blocks being re-ordered
    // and deleted.
    BlockDefinition block1QAQB =
        BlockDefinition.builder()
            .setId(2L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
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
            .setId(5L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(questionC, Optional.of(programDefinitionId)))
            .build();
    BlockDefinition block3QD =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
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
    QuestionDefinition questionA = testQuestionBank.nameApplicantName().getQuestionDefinition();
    QuestionDefinition questionBEnum =
        testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.nameRepeatedApplicantHouseholdMemberName().getQuestionDefinition();
    QuestionDefinition questionDEnum =
        testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs().getQuestionDefinition();
    QuestionDefinition questionE =
        testQuestionBank
            .numberNestedRepeatedApplicantHouseholdMemberDaysWorked()
            .getQuestionDefinition();

    long programDefinitionId = 123L;
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(questionA, Optional.of(programDefinitionId)))
            .build();
    BlockDefinition blockBEnum =
        BlockDefinition.builder()
            .setId(2L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(questionBEnum, Optional.of(programDefinitionId)))
            .build();
    BlockDefinition blockC =
        BlockDefinition.builder()
            .setId(3L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(questionC, Optional.of(programDefinitionId)))
            .setEnumeratorId(Optional.of(2L))
            .build();
    BlockDefinition blockDEnum =
        BlockDefinition.builder()
            .setId(4L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(questionDEnum, Optional.of(programDefinitionId)))
            .setEnumeratorId(Optional.of(2L))
            .build();
    BlockDefinition blockE =
        BlockDefinition.builder()
            .setId(5L)
            .setName("Screen Name")
            .setDescription("Screen Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen Name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen Description"))
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockBEnum)
            .addBlockDefinition(blockC)
            .addBlockDefinition(blockDEnum)
            .addBlockDefinition(blockE)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
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
  public void hasEligibilityEnabled_correctlyReturnsFalse() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs())
            .build()
            .getProgramDefinition();

    // program does not have eligibility enabled
    assertThat(programDefinition.hasEligibilityEnabled()).isFalse();
  }

  @Test
  public void insertBlockDefinitionInTheRightPlace_repeatedBlock() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build()
            .getProgramDefinition();
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setName("new screen")
            .setDescription("new screen")
            .setLocalizedName(LocalizedStrings.withDefaultValue("new screen"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("new screen"))
            .setId(100L)
            .setEnumeratorId(Optional.of(1L))
            .build();

    ProgramDefinition result =
        programDefinition.insertBlockDefinitionInTheRightPlace(blockDefinition);

    assertThat(result.blockDefinitions()).hasSize(4);
    assertThat(result.getBlockDefinitionByIndex(0).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(0).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(
            testQuestionBank
                .enumeratorNestedApplicantHouseholdMemberJobs()
                .getQuestionDefinition());
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
            .withRequiredQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build()
            .getProgramDefinition();

    ProgramDefinition result = programDefinition.moveBlock(3L, ProgramDefinition.Direction.UP);

    assertThat(result.blockDefinitions()).hasSize(3);
    assertThat(result.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(2).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(2).get().getQuestionDefinition(0))
        .isEqualTo(
            testQuestionBank
                .enumeratorNestedApplicantHouseholdMemberJobs()
                .getQuestionDefinition());
  }

  @Test
  public void moveBlock_down() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build()
            .getProgramDefinition();

    ProgramDefinition result = programDefinition.moveBlock(1L, ProgramDefinition.Direction.DOWN);

    assertThat(result.blockDefinitions()).hasSize(3);
    assertThat(result.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(1).get().isRepeated()).isFalse();
    assertThat(result.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition());
    assertThat(result.getBlockDefinitionByIndex(2).get().isEnumerator()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(result.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(1L);
    assertThat(result.getBlockDefinitionByIndex(2).get().getQuestionDefinition(0))
        .isEqualTo(
            testQuestionBank
                .enumeratorNestedApplicantHouseholdMemberJobs()
                .getQuestionDefinition());
  }

  @Test
  public void moveBlock_up_withVisibilityPredicate() throws Exception {
    QuestionModel predicateQuestion = testQuestionBank.textApplicantFavoriteColor();
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
            .withOptionalQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
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
    QuestionModel predicateQuestion = testQuestionBank.textApplicantFavoriteColor();
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
    QuestionModel predicateQuestion = testQuestionBank.textApplicantFavoriteColor();
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
            .withOptionalQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
            .withBlock()
            .withEligibilityDefinition(eligibility)
            .build()
            .getProgramDefinition();

    ProgramDefinition result = programDefinition.moveBlock(3L, ProgramDefinition.Direction.UP);

    assertThat(result.getBlockDefinitionByIndex(1)).isPresent();
    assertThat(result.getBlockDefinitionByIndex(1).get().eligibilityDefinition())
        .contains(eligibility);

    // program has eligibility enabled
    assertThat(programDefinition.hasEligibilityEnabled()).isTrue();
  }

  @Test
  public void moveBlock_up_withEligibilityPredicate_throwsForIllegalMove() {
    QuestionModel predicateQuestion = testQuestionBank.textApplicantFavoriteColor();
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
    QuestionModel predicateQuestion = testQuestionBank.textApplicantFavoriteColor();
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
            .withOptionalQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
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

    // program has eligibility enabled
    assertThat(programDefinition.hasEligibilityEnabled()).isTrue();
  }

  @Test
  public void moveBlockDown_throwsForIllegalMove() {
    QuestionModel predicateQuestion = testQuestionBank.textApplicantFavoriteColor();
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
    QuestionModel predicateQuestion = testQuestionBank.textApplicantFavoriteColor();
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
    QuestionModel predicateQuestion = testQuestionBank.textApplicantFavoriteColor();
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .setCreateTime(now)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .setLastModifiedTime(now)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();
    assertThat(def.lastModifiedTime().isPresent()).isFalse();
  }

  @Test
  public void getLocalizedSummaryImageDescription_whenExists() {
    LocalizedStrings description = LocalizedStrings.of(Locale.US, "summary image description");
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .setLocalizedSummaryImageDescription(Optional.of(description))
            .build();

    assertThat(def.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(def.localizedSummaryImageDescription().get()).isEqualTo(description);
  }

  @Test
  public void getLocalizedSummaryImageDescription_whenDoesntExist() {
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();

    assertThat(def.localizedSummaryImageDescription().isPresent()).isFalse();
  }

  @Test
  public void getLocalizedSummaryImageDescription_replacesExistingValue()
      throws TranslationNotFoundException {
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .setLocalizedSummaryImageDescription(
                Optional.of(LocalizedStrings.of(Locale.US, "first image description")))
            .build();

    def =
        def.toBuilder()
            .setLocalizedSummaryImageDescription(
                Optional.of(
                    def.localizedSummaryImageDescription()
                        .get()
                        .updateTranslation(Locale.US, "new image description")))
            .build();

    assertThat(def.localizedSummaryImageDescription().get().get(Locale.US))
        .isEqualTo("new image description");
  }

  @Test
  public void getSummaryImageFileKey_whenExists() {
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .setSummaryImageFileKey(Optional.of("program-summary-image/program-123/fileKey.png"))
            .build();

    assertThat(def.summaryImageFileKey().isPresent()).isTrue();
    assertThat(def.summaryImageFileKey().get())
        .isEqualTo("program-summary-image/program-123/fileKey.png");
  }

  @Test
  public void getSummaryImageFileKey_whenDoesntExist() {
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();

    assertThat(def.summaryImageFileKey().isPresent()).isFalse();
  }

  @Test
  public void getSummaryImageFileKey_replacesExistingValue() {
    ProgramDefinition def =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .setSummaryImageFileKey(Optional.of("program-summary-image/program-123/fileKey.png"))
            .build();

    def =
        def.toBuilder()
            .setSummaryImageFileKey(Optional.of("program-summary-image/program-123/newFileKey.png"))
            .build();

    assertThat(def.summaryImageFileKey().get())
        .isEqualTo("program-summary-image/program-123/newFileKey.png");
  }

  @Test
  public void serializeThenDeserialize_allFieldsPresent() throws JsonProcessingException {
    QuestionDefinition questionA = testQuestionBank.nameApplicantName().getQuestionDefinition();
    QuestionDefinition questionB =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition();

    long programDefinitionId = 654L;
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block A")
            .setDescription("Block A Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block A"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block A Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    questionA,
                    Optional.of(programDefinitionId),
                    /* optional= */ false,
                    /* addressCorrectionEnabled= */ false))
            .build();

    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(321L)
            .setName("Block B")
            .setDescription("Block B Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block B"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block B Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    questionB,
                    Optional.of(programDefinitionId),
                    /* optional= */ false,
                    /* addressCorrectionEnabled= */ true))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    questionC,
                    Optional.of(programDefinitionId),
                    /* optional= */ true,
                    /* addressCorrectionEnabled= */ false))
            .build();

    ProgramDefinition programDefinition =
        ProgramDefinition.builder()
            // The following fields *should* be included in the serialization
            .setId(programDefinitionId)
            .setAdminName("serialize-test")
            .setAdminDescription("Test program for serialization")
            .setLocalizedName(
                LocalizedStrings.of(
                    Locale.US,
                    "US name",
                    Locale.FRENCH,
                    "French name",
                    Locale.ITALIAN,
                    "Italian name"))
            .setLocalizedDescription(
                LocalizedStrings.of(
                    Locale.US, "US description", Locale.GERMAN, "German description"))
            .setLocalizedConfirmationMessage(
                LocalizedStrings.of(Locale.US, "US message", Locale.GERMAN, "German message"))
            .setLocalizedSummaryImageDescription(
                Optional.of(
                    LocalizedStrings.of(
                        Locale.US,
                        "US summary image description",
                        Locale.ITALIAN,
                        "Italian summary image description")))
            .setExternalLink("external.link")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.COMMON_INTAKE_FORM)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls(ImmutableSet.of(987L, 65L, 4321L)))
            .setCategories(ImmutableList.of())
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            // The following fields should *not* be included in the serialization
            .setSummaryImageFileKey(Optional.of("program-summary-image/program-123/fileKey.png"))
            .setCreateTime(Instant.now())
            .setLastModifiedTime(Instant.now())
            .build();

    ObjectMapper objectMapper =
        instanceOf(ObjectMapper.class)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module());

    String serializedProgramDefinition = objectMapper.writeValueAsString(programDefinition);
    ProgramDefinition result =
        objectMapper.readValue(serializedProgramDefinition, ProgramDefinition.class);

    // Assert that the fields that should have been parsed were parsed correctly.
    assertThat(result.id()).isEqualTo(654L);
    assertThat(result.adminName()).isEqualTo("serialize-test");
    assertThat(result.adminDescription()).isEqualTo("Test program for serialization");
    assertThat(result.localizedName())
        .isEqualTo(
            LocalizedStrings.of(
                Locale.US,
                "US name",
                Locale.FRENCH,
                "French name",
                Locale.ITALIAN,
                "Italian name"));
    assertThat(result.localizedDescription())
        .isEqualTo(
            LocalizedStrings.of(Locale.US, "US description", Locale.GERMAN, "German description"));
    assertThat(result.localizedConfirmationMessage())
        .isEqualTo(LocalizedStrings.of(Locale.US, "US message", Locale.GERMAN, "German message"));
    assertThat(result.localizedSummaryImageDescription())
        .hasValue(
            LocalizedStrings.of(
                Locale.US,
                "US summary image description",
                Locale.ITALIAN,
                "Italian summary image description"));
    assertThat(result.externalLink()).isEqualTo("external.link");
    assertThat(result.displayMode()).isEqualTo(DisplayMode.PUBLIC);
    assertThat(result.programType()).isEqualTo(ProgramType.COMMON_INTAKE_FORM);
    assertThat(result.eligibilityIsGating()).isTrue();
    assertThat(result.acls().getTiProgramViewAcls()).containsExactlyInAnyOrder(987L, 65L, 4321L);

    // Assert that the block definitions were parsed correctly.
    // Note: A BlockDefinition contains a list of ProgramQuestionDefinitions, which specify the
    // questions included in the block. ProgramQuestionDefinitions are serialized into JSON and
    // stored in our database as that JSON string. When we serialize, we specifically exclude
    // the `programDefinitionId` and `questionDefinition` fields. When it gets deserialized,
    // we expect those fields to be missing.
    BlockDefinition expectedBlockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block A")
            .setDescription("Block A Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block A"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block A Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    questionA.getId(),
                    /* optional= */ false,
                    /* addressCorrectionEnabled= */ false))
            .build();
    BlockDefinition expectedBlockB =
        BlockDefinition.builder()
            .setId(321L)
            .setName("Block B")
            .setDescription("Block B Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block B"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block B Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    questionB.getId(), /* optional= */ false, /* addressCorrectionEnabled= */ true))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    questionC.getId(), /* optional= */ true, /* addressCorrectionEnabled= */ false))
            .build();
    assertThat(result.blockDefinitions()).containsExactly(expectedBlockA, expectedBlockB);

    // Assert that the fields that should not have been parsed were not included.
    assertThat(result.summaryImageFileKey()).isEmpty();
    assertThat(result.createTime()).isEmpty();
    assertThat(result.lastModifiedTime()).isEmpty();
  }

  @Test
  public void serializeThenDeserialize_withEnumeratorQuestion() throws JsonProcessingException {
    long programDefinitionId = 654L;

    QuestionDefinition mainEnumQ =
        testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition qForMainEnum =
        testQuestionBank.nameRepeatedApplicantHouseholdMemberName().getQuestionDefinition();
    QuestionDefinition nestedEnumQ =
        testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs().getQuestionDefinition();
    QuestionDefinition qForNestedEnum =
        testQuestionBank
            .numberNestedRepeatedApplicantHouseholdMemberDaysWorked()
            .getQuestionDefinition();

    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(100L)
            .setName("Block A Name")
            .setDescription("Block A Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block A"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block A Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(mainEnumQ, Optional.of(programDefinitionId)))
            .build();
    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(200L)
            .setName("Block B Name")
            .setDescription("Block B Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block B"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block B Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(qForMainEnum, Optional.of(programDefinitionId)))
            .setEnumeratorId(Optional.of(100L))
            .build();
    BlockDefinition blockC =
        BlockDefinition.builder()
            .setId(300L)
            .setName("Block C Name")
            .setDescription("Block C Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block C"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block C Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(nestedEnumQ, Optional.of(programDefinitionId)))
            .setEnumeratorId(Optional.of(100L))
            .build();
    BlockDefinition blockD =
        BlockDefinition.builder()
            .setId(400L)
            .setName("Block D Name")
            .setDescription("Block D Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block D"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block D Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(qForNestedEnum, Optional.of(programDefinitionId)))
            .setEnumeratorId(Optional.of(300L))
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
            .setDisplayMode(DisplayMode.PUBLIC)
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .addBlockDefinition(blockC)
            .addBlockDefinition(blockD)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();

    ObjectMapper objectMapper =
        instanceOf(ObjectMapper.class)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module());

    String serializedProgramDefinition = objectMapper.writeValueAsString(programDefinition);
    ProgramDefinition result =
        objectMapper.readValue(serializedProgramDefinition, ProgramDefinition.class);

    // Assert that the block definitions were parsed correctly to include the enumerator IDs.
    BlockDefinition expectedBlockA =
        BlockDefinition.builder()
            .setId(100L)
            .setName("Block A Name")
            .setDescription("Block A Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block A"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block A Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    mainEnumQ.getId(),
                    /* optional= */ false,
                    /* addressCorrectionEnabled= */ false))
            .setEnumeratorId(Optional.empty())
            .build();
    BlockDefinition expectedBlockB =
        BlockDefinition.builder()
            .setId(200L)
            .setName("Block B Name")
            .setDescription("Block B Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block B"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block B Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    qForMainEnum.getId(),
                    /* optional= */ false,
                    /* addressCorrectionEnabled= */ false))
            .setEnumeratorId(Optional.of(100L))
            .build();
    BlockDefinition expectedBlockC =
        BlockDefinition.builder()
            .setId(300L)
            .setName("Block C Name")
            .setDescription("Block C Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block C"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block C Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    nestedEnumQ.getId(),
                    /* optional= */ false,
                    /* addressCorrectionEnabled= */ false))
            .setEnumeratorId(Optional.of(100L))
            .build();
    BlockDefinition expectedBlockD =
        BlockDefinition.builder()
            .setId(400L)
            .setName("Block D Name")
            .setDescription("Block D Description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Block D"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Block D Description"))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    qForNestedEnum.getId(),
                    /* optional= */ false,
                    /* addressCorrectionEnabled= */ false))
            .setEnumeratorId(Optional.of(300L))
            .build();
    assertThat(result.blockDefinitions())
        .containsExactly(expectedBlockA, expectedBlockB, expectedBlockC, expectedBlockD);
  }
}
