package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.program.StatusDefinitions;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.types.QuestionDefinition;
import services.question.types.ScalarType;
import support.ProgramBuilder;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class ReadOnlyApplicantProgramServiceImplTest extends ResetPostgres {
  private static final String FAKE_BASE_URL = "http://fake-base-url";

  private QuestionDefinition nameQuestion;
  private QuestionDefinition colorQuestion;
  private QuestionDefinition addressQuestion;
  private QuestionDefinition staticQuestion;
  private ApplicantData applicantData;
  private ProgramDefinition programDefinition;

  @Before
  public void setUp() {
    applicantData = new ApplicantData();
    nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    colorQuestion = testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    addressQuestion = testQuestionBank.applicantAddress().getQuestionDefinition();
    staticQuestion = testQuestionBank.staticContent().getQuestionDefinition();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withLocalizedName(Locale.GERMAN, "Mein Programm")
            .withBlock("Block one")
            .withRequiredQuestionDefinition(nameQuestion)
            .withBlock("Block two")
            .withRequiredQuestionDefinition(colorQuestion)
            .withRequiredQuestionDefinition(addressQuestion)
            .buildDefinition();
  }

  @Test
  public void getProgramTitle_returnsProgramTitleInDefaultLocale() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    assertThat(subject.getProgramTitle()).isEqualTo("My Program");
  }

  @Test
  public void getProgramTitle_returnsProgramTitleForPreferredLocale() {
    applicantData.setPreferredLocale(Locale.GERMAN);
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    assertThat(subject.getProgramTitle()).isEqualTo("Mein Programm");
  }

  @Test
  public void getStoredFileKeys_includesAnsweredFileQuestions() {
    QuestionDefinition fileQuestionDefinition =
        testQuestionBank.applicantFile().getQuestionDefinition();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withLocalizedName(Locale.GERMAN, "Mein Programm")
            .withBlock("Block one")
            .withBlock("file-one")
            .withRequiredQuestionDefinition(fileQuestionDefinition)
            .buildDefinition();

    QuestionAnswerer.answerFileQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(fileQuestionDefinition.getQuestionPathSegment()),
        "file-key");

    ReadOnlyApplicantProgramService service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    assertThat(service.getStoredFileKeys()).containsExactly("file-key");
  }

  @Test
  public void getStoredFileKeys_doesNotIncludeUnansweredFileQuestions() {
    QuestionDefinition fileQuestionDefinition =
        testQuestionBank.applicantFile().getQuestionDefinition();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withLocalizedName(Locale.GERMAN, "Mein Programm")
            .withBlock("Block one")
            .withBlock("file-one")
            .withRequiredQuestionDefinition(fileQuestionDefinition)
            .buildDefinition();

    ReadOnlyApplicantProgramService service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    assertThat(service.getStoredFileKeys()).isEmpty();
  }

  @Test
  public void getAllBlocks_includesPreviouslyCompletedBlocks() {
    ProgramDefinition programDefinitionWithStatic =
        ProgramBuilder.newDraftProgram("My Program")
            .withLocalizedName(Locale.GERMAN, "Mein Programm")
            .withBlock("Block one")
            .withRequiredQuestionDefinition(nameQuestion)
            .withBlock("Block two")
            .withRequiredQuestionDefinition(colorQuestion)
            .withRequiredQuestionDefinition(addressQuestion)
            .withBlock("Block three")
            .withRequiredQuestionDefinition(staticQuestion)
            .buildDefinition();
    // Answer first block in a separate program
    answerNameQuestion(programDefinitionWithStatic.id() + 1);

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(
            applicantData, programDefinitionWithStatic, FAKE_BASE_URL);
    ImmutableList<Block> allBlocks = subject.getAllActiveBlocks();

    assertThat(allBlocks).hasSize(3);
    assertThat(allBlocks.get(0).getName()).isEqualTo("Block one");
    assertThat(allBlocks.get(1).getName()).isEqualTo("Block two");
    assertThat(allBlocks.get(2).getName()).isEqualTo("Block three");
  }

  @Test
  public void getAllBlocks_onlyStaticBlock() {
    ProgramDefinition programDefinitionWithStatic =
        ProgramBuilder.newDraftProgram("My Program")
            .withLocalizedName(Locale.GERMAN, "Mein Programm")
            .withBlock("Block one")
            .withRequiredQuestionDefinition(staticQuestion)
            .buildDefinition();

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(
            applicantData, programDefinitionWithStatic, FAKE_BASE_URL);
    ImmutableList<Block> allBlocks = subject.getAllActiveBlocks();

    assertThat(allBlocks).hasSize(1);
    assertThat(allBlocks.get(0).getName()).isEqualTo("Block one");

    ImmutableList<Block> inProgressBlocks = subject.getInProgressBlocks();

    assertThat(inProgressBlocks).hasSize(1);
    assertThat(inProgressBlocks.get(0).getName()).isEqualTo("Block one");

    Optional<Block> firstIncompleteBlock = subject.getFirstIncompleteOrStaticBlock();
    Optional<Block> firstIncompleteExcludingStatic =
        subject.getFirstIncompleteBlockExcludingStatic();

    assertThat(firstIncompleteBlock.isPresent()).isTrue();
    assertThat(firstIncompleteBlock.get().getName()).isEqualTo("Block one");
    assertThat(firstIncompleteExcludingStatic.isPresent()).isFalse();
  }

  @Test
  public void getAllBlocks_doesNotIncludeBlocksThatAreHidden() {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    colorQuestion.getId(),
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("blue"))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock() // Previous block with color question
            .withRequiredQuestionDefinition(colorQuestion)
            .withBlock() // Block with predicate
            .withVisibilityPredicate(predicate)
            .withRequiredQuestionDefinition(
                addressQuestion) // Include a question that has not been answered
            .buildDefinition();

    // Answer predicate question so that the block should be hidden
    answerColorQuestion(program.id(), "blue");

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);
    ImmutableList<Block> allBlocks = subject.getAllActiveBlocks();

    assertThat(allBlocks).hasSize(1);
  }

  @Test
  public void getAllBlocks_IncludesBlocksThatAreNotHidden() {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    colorQuestion.getId(),
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("blue"))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock() // Previous block with color question
            .withRequiredQuestionDefinition(colorQuestion)
            .withBlock() // Block with predicate
            .withVisibilityPredicate(predicate)
            .withRequiredQuestionDefinition(
                addressQuestion) // Include a question that has not been answered
            .buildDefinition();

    // Answer predicate question so that the block should be hidden
    answerColorQuestion(program.id(), "red");

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);
    ImmutableList<Block> allBlocks = subject.getAllActiveBlocks();

    assertThat(allBlocks).hasSize(2);
  }

  @Test
  public void getAllBlocks_doesNotIncludeRepeatedEntitiesThatAreHidden() {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    colorQuestion.getId(),
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("blue"))),
            PredicateAction.SHOW_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock("visibility question")
            .withRequiredQuestionDefinition(colorQuestion)
            .withBlock("enumeration - household members")
            .withVisibilityPredicate(predicate)
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock("repeated - household members name")
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberName())
            .withAnotherRepeatedBlock("repeated - household members jobs")
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .withRepeatedBlock("deeply repeated - household members number days worked")
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberDaysWorked())
            .buildDefinition();

    // Answer predicate question so that the block should be visible
    answerColorQuestion(program.id(), "blue");

    // Add repeated entities to applicant data
    Path enumerationPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantHouseholdMembers()
                .getQuestionDefinition()
                .getQuestionPathSegment());
    applicantData.putString(enumerationPath.atIndex(0).join(Scalar.ENTITY_NAME), "first entity");
    applicantData.putString(enumerationPath.atIndex(1).join(Scalar.ENTITY_NAME), "second entity");
    applicantData.putString(enumerationPath.atIndex(2).join(Scalar.ENTITY_NAME), "third entity");
    Path deepEnumerationPath =
        enumerationPath
            .atIndex(2)
            .join(
                testQuestionBank
                    .applicantHouseholdMemberJobs()
                    .getQuestionDefinition()
                    .getQuestionPathSegment());
    applicantData.putString(
        deepEnumerationPath.atIndex(0).join(Scalar.ENTITY_NAME), "nested first job");
    applicantData.putString(
        deepEnumerationPath.atIndex(1).join(Scalar.ENTITY_NAME), "nested second job");

    ReadOnlyApplicantProgramService service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);
    ImmutableList<Block> blocks = service.getAllActiveBlocks();

    assertThat(blocks).as("The block count when the Predicate evluates to visible").hasSize(10);

    // Answer predicate question so that the enum should no longer be visible
    answerColorQuestion(program.id(), "red");
    ReadOnlyApplicantProgramService serviceWhenHidden =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);
    ImmutableList<Block> blocksWhenHidden = serviceWhenHidden.getAllActiveBlocks();

    assertThat(blocks).isNotEqualTo(blocksWhenHidden);
    assertThat(blocksWhenHidden)
        .as("The block count when the Predicate evluates to hidden")
        .hasSize(1);
  }

  @Test
  public void getAllBlocks_includesBlockWithStaticEvenWhenOthersAreAnswered() {
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock("Block one") // Previous block with color question
            .withRequiredQuestionDefinition(colorQuestion)
            .withRequiredQuestionDefinition(staticQuestion)
            .withBlock("Block two") // Block required skipped question
            .withRequiredQuestionDefinition(addressQuestion)
            .buildDefinition();

    // Answer color question
    answerColorQuestion(program.id(), "blue");

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);

    ImmutableList<Block> allBlocks = subject.getAllActiveBlocks();
    Optional<Block> maybeBlock = subject.getFirstIncompleteOrStaticBlock();
    Optional<Block> maybeBlockExcludeStatic = subject.getFirstIncompleteBlockExcludingStatic();

    assertThat(allBlocks).hasSize(2);
    assertThat(maybeBlock).isNotEmpty();
    assertThat(maybeBlock.get().getName()).isEqualTo("Block one");
    assertThat(maybeBlockExcludeStatic).isNotEmpty();
    assertThat(maybeBlockExcludeStatic.get().getName()).isEqualTo("Block two");
  }

  @Test
  public void getAllBlocks_includesRepeatedBlocks() {
    programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock("name")
            .withRequiredQuestion(testQuestionBank.applicantName())
            .withBlock("address")
            .withRequiredQuestion(testQuestionBank.applicantAddress())
            .withBlock("enumeration - household members")
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock("repeated - household members name")
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberName())
            .withAnotherRepeatedBlock("repeated - household members jobs")
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .withRepeatedBlock("deeply repeated - household members number days worked")
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberDaysWorked())
            .buildDefinition();

    // Add repeated entities to applicant data
    Path enumerationPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantHouseholdMembers()
                .getQuestionDefinition()
                .getQuestionPathSegment());
    applicantData.putString(enumerationPath.atIndex(0).join(Scalar.ENTITY_NAME), "first entity");
    applicantData.putString(enumerationPath.atIndex(1).join(Scalar.ENTITY_NAME), "second entity");
    applicantData.putString(enumerationPath.atIndex(2).join(Scalar.ENTITY_NAME), "third entity");
    Path deepEnumerationPath =
        enumerationPath
            .atIndex(2)
            .join(
                testQuestionBank
                    .applicantHouseholdMemberJobs()
                    .getQuestionDefinition()
                    .getQuestionPathSegment());
    applicantData.putString(
        deepEnumerationPath.atIndex(0).join(Scalar.ENTITY_NAME), "nested first job");
    applicantData.putString(
        deepEnumerationPath.atIndex(1).join(Scalar.ENTITY_NAME), "nested second job");

    ReadOnlyApplicantProgramService service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    ImmutableList<Block> blocks = service.getAllActiveBlocks();

    assertThat(blocks).hasSize(11);

    assertThat(blocks.get(3).getId()).isEqualTo("4-0");
    Path questionPath = enumerationPath.atIndex(0).join("household_members_name");
    assertThat(blocks.get(3).getQuestions().get(0).getContextualizedScalars())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                questionPath.join(Scalar.FIRST_NAME), ScalarType.STRING,
                questionPath.join(Scalar.MIDDLE_NAME), ScalarType.STRING,
                questionPath.join(Scalar.LAST_NAME), ScalarType.STRING,
                questionPath.join(Scalar.PROGRAM_UPDATED_IN), ScalarType.LONG,
                questionPath.join(Scalar.UPDATED_AT), ScalarType.LONG));

    assertThat(blocks.get(4).getId()).isEqualTo("5-0");
    assertThat(blocks.get(4).isEnumerator()).isTrue();

    assertThat(blocks.get(5).getId()).isEqualTo("4-1");
    questionPath = enumerationPath.atIndex(1).join("household_members_name");
    assertThat(blocks.get(5).getQuestions().get(0).getContextualizedScalars())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                questionPath.join(Scalar.FIRST_NAME),
                ScalarType.STRING,
                questionPath.join(Scalar.MIDDLE_NAME),
                ScalarType.STRING,
                questionPath.join(Scalar.LAST_NAME),
                ScalarType.STRING,
                questionPath.join(Scalar.PROGRAM_UPDATED_IN),
                ScalarType.LONG,
                questionPath.join(Scalar.UPDATED_AT),
                ScalarType.LONG));

    assertThat(blocks.get(6).getId()).isEqualTo("5-1");
    assertThat(blocks.get(6).isEnumerator()).isTrue();

    assertThat(blocks.get(7).getId()).isEqualTo("4-2");
    questionPath = enumerationPath.atIndex(2).join("household_members_name");
    assertThat(blocks.get(7).getQuestions().get(0).getContextualizedScalars())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                questionPath.join(Scalar.FIRST_NAME),
                ScalarType.STRING,
                questionPath.join(Scalar.MIDDLE_NAME),
                ScalarType.STRING,
                questionPath.join(Scalar.LAST_NAME),
                ScalarType.STRING,
                questionPath.join(Scalar.PROGRAM_UPDATED_IN),
                ScalarType.LONG,
                questionPath.join(Scalar.UPDATED_AT),
                ScalarType.LONG));

    assertThat(blocks.get(8).getId()).isEqualTo("5-2");
    assertThat(blocks.get(8).isEnumerator()).isTrue();

    assertThat(blocks.get(9).getId()).isEqualTo("6-2-0");
    questionPath = deepEnumerationPath.atIndex(0).join("household_members_days_worked");
    assertThat(blocks.get(9).getQuestions().get(0).getContextualizedScalars())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                questionPath.join(Scalar.NUMBER),
                ScalarType.LONG,
                questionPath.join(Scalar.PROGRAM_UPDATED_IN),
                ScalarType.LONG,
                questionPath.join(Scalar.UPDATED_AT),
                ScalarType.LONG));

    assertThat(blocks.get(10).getId()).isEqualTo("6-2-1");
    questionPath = deepEnumerationPath.atIndex(1).join("household_members_days_worked");
    assertThat(blocks.get(10).getQuestions().get(0).getContextualizedScalars())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                questionPath.join(Scalar.NUMBER),
                ScalarType.LONG,
                questionPath.join(Scalar.PROGRAM_UPDATED_IN),
                ScalarType.LONG,
                questionPath.join(Scalar.UPDATED_AT),
                ScalarType.LONG));

    RepeatedEntity repeatedEntity = blocks.get(10).getRepeatedEntity().get();
    assertThat(repeatedEntity.index()).isEqualTo(1);
    assertThat(repeatedEntity.entityName()).isEqualTo("nested second job");
    assertThat(repeatedEntity.parent().get().index()).isEqualTo(2);
    assertThat(repeatedEntity.parent().get().entityName()).isEqualTo("third entity");
  }

  @Test
  public void getInProgressBlocks_getsTheApplicantSpecificBlocksForTheProgram() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    ImmutableList<Block> blockList = subject.getInProgressBlocks();

    assertThat(blockList).hasSize(2);
    Block block = blockList.get(0);
    assertThat(block.getName()).isEqualTo("Block one");
  }

  @Test
  public void getInProgressBlocks_doesNotIncludeCompleteBlocks() {
    // Answer block one questions
    answerNameQuestion(programDefinition.id() + 1);

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    ImmutableList<Block> blockList = subject.getInProgressBlocks();

    assertThat(blockList).hasSize(1);
    assertThat(blockList.get(0).getName()).isEqualTo("Block two");
  }

  @Test
  public void getInProgressBlocks_returnsEmptyListIfAllBlocksCompletedInAnotherProgram() {
    // Answer all questions for a different program.
    answerNameQuestion(programDefinition.id() + 1);
    answerColorQuestion(programDefinition.id() + 1);
    answerAddressQuestion(programDefinition.id() + 1);

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    ImmutableList<Block> blockList = subject.getInProgressBlocks();

    assertThat(blockList).isEmpty();
  }

  @Test
  public void getInProgressBlocks_includesBlocksThatWereCompletedInThisProgram() {
    // Answer block 1 questions in this program session
    answerNameQuestion(programDefinition.id());

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    ImmutableList<Block> blockList = subject.getInProgressBlocks();

    // Block 1 should still be there
    assertThat(blockList).hasSize(2);
    Block block = blockList.get(0);
    assertThat(block.getName()).isEqualTo("Block one");
  }

  @Test
  public void getInProgressBlocks_includesBlocksThatWerePartiallyCompletedInAnotherProgram() {
    // Answer one of block 2 questions in another program
    answerAddressQuestion(programDefinition.id() + 1);

    // Answer the other block 2 questions in this program session
    answerColorQuestion(programDefinition.id());

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    ImmutableList<Block> blockList = subject.getInProgressBlocks();

    // Block 1 should still be there
    Block block = blockList.get(0);
    assertThat(block.getName()).isEqualTo("Block one");

    // Block 2 should still be there, even though it was partially completed by another program.
    assertThat(blockList).hasSize(2);
  }

  @Test
  public void getInProgressBlocks_handlesBlockWithShowBlockActionPredicate() {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    colorQuestion.getId(),
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("blue"))),
            PredicateAction.SHOW_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock() // Previous block with color question
            .withRequiredQuestionDefinition(colorQuestion)
            .withBlock() // Block with predicate
            .withVisibilityPredicate(predicate)
            .withRequiredQuestionDefinition(
                addressQuestion) // Include a question that has not been answered
            .buildDefinition();

    // Answer "blue" to the question - the predicate is true, so we should show the block.
    answerColorQuestion(program.id(), "blue");
    ReadOnlyApplicantProgramService service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);
    assertThat(service.getInProgressBlocks()).hasSize(2);

    // Answer "green" to the question - the predicate is now false, so we should not show the block.
    answerColorQuestion(program.id(), "green");
    service = new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);
    assertThat(service.getInProgressBlocks()).hasSize(1);
  }

  @Test
  public void getInProgressBlocks_handlesBlockWithHideBlockActionPredicate() {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    colorQuestion.getId(),
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("blue"))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestionDefinition(colorQuestion)
            .withBlock()
            .withVisibilityPredicate(predicate)
            .withRequiredQuestionDefinition(
                addressQuestion) // Include a skipped question so the block is incomplete
            .buildDefinition();

    // Answer "blue" to the question - the predicate is true, so we should hide the block.
    answerColorQuestion(program.id(), "blue");
    ReadOnlyApplicantProgramServiceImpl service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);
    assertThat(service.getInProgressBlocks()).hasSize(1);

    // Answer "green" to the question - the predicate is now false, so we should show the block.
    answerColorQuestion(program.id(), "green");
    service = new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);
    assertThat(service.getInProgressBlocks()).hasSize(2);
  }

  @Test
  public void getInProgressBlocks_predicateAnswerUndefined_includesBlockInList() {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    colorQuestion.getId(),
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("blue"))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock() // Block is completed
            .withRequiredQuestionDefinition(nameQuestion)
            .withBlock() // Block incomplete; this is what predicate is based on
            .withRequiredQuestionDefinition(colorQuestion)
            .withBlock()
            .withVisibilityPredicate(predicate)
            .withRequiredQuestionDefinition(
                addressQuestion) // Include a skipped question so the block is incomplete
            .buildDefinition();

    // The color question is not answered yet - we should default to show the block that uses the
    // color question in a predicate.
    answerNameQuestion(program.id());
    ReadOnlyApplicantProgramServiceImpl service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);
    assertThat(service.getInProgressBlocks()).hasSize(3);
  }

  @Test
  public void getEligibilityQuestionsForProgram_returnsQuestionWithEligibilityPredicate() {
    // Create an eligibility condition with number question == 5.
    QuestionDefinition numberQuestionDefinition =
        testQuestionBank.applicantJugglingNumber().getQuestionDefinition();
    PredicateDefinition numberPredicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    numberQuestionDefinition.getId(),
                    Scalar.NUMBER,
                    Operator.EQUAL_TO,
                    PredicateValue.of("5"))),
            PredicateAction.SHOW_BLOCK);

    EligibilityDefinition eligibilityDefinition =
        EligibilityDefinition.builder().setPredicate(numberPredicate).build();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withBlock("Block one")
            .withRequiredQuestionDefinition(numberQuestionDefinition)
            .withEligibilityDefinition(eligibilityDefinition)
            .buildDefinition();

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    ImmutableList<ApplicantQuestion> eligibilityQuestions = subject.getActiveEligibilityQuestions();

    // The number question should be in the list of eligibility questions.
    assertThat(eligibilityQuestions).hasSize(1);
    assertThat(eligibilityQuestions.stream().findFirst().get().getQuestionDefinition())
        .isEqualTo(testQuestionBank.applicantJugglingNumber().getQuestionDefinition());
  }

  @Test
  public void getActiveAndCompletedInProgramBlockCount_withSkippedOptional() {
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock() // Block is completed
            .withQuestionDefinition(nameQuestion, true)
            .withBlock() // Block incomplete; this is what predicate is based on
            .withQuestionDefinition(colorQuestion, false)
            .buildDefinition();
    QuestionAnswerer.addMetadata(
        applicantData,
        nameQuestion.getContextualizedPath(Optional.empty(), ApplicantData.APPLICANT_PATH),
        program.id(),
        0L);

    ReadOnlyApplicantProgramServiceImpl service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);

    assertThat(service.getActiveAndCompletedInProgramBlockCount()).isEqualTo(1);
  }

  @Test
  public void getActiveAndCompletedInProgramBlockCount_withSkippedOptionalInDifferentProgram() {
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock() // Block is completed
            .withQuestionDefinition(nameQuestion, true)
            .withBlock() // Block incomplete; this is what predicate is based on
            .withQuestionDefinition(colorQuestion, false)
            .buildDefinition();
    QuestionAnswerer.addMetadata(
        applicantData,
        nameQuestion.getContextualizedPath(Optional.empty(), ApplicantData.APPLICANT_PATH),
        program.id() + 1,
        0L);

    ReadOnlyApplicantProgramServiceImpl service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);

    assertThat(service.getActiveAndCompletedInProgramBlockCount()).isEqualTo(0);
  }

  @Test
  public void getActiveAndCompletedInProgramBlockCount_withRequiredSkipped() {
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock() // Block is completed
            .withQuestionDefinition(nameQuestion, false)
            .withBlock() // Block incomplete; this is what predicate is based on
            .withQuestionDefinition(colorQuestion, false)
            .buildDefinition();
    QuestionAnswerer.addMetadata(
        applicantData,
        nameQuestion.getContextualizedPath(Optional.empty(), ApplicantData.APPLICANT_PATH),
        program.id(),
        0L);

    ReadOnlyApplicantProgramServiceImpl service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, program, FAKE_BASE_URL);

    assertThat(service.getActiveAndCompletedInProgramBlockCount()).isEqualTo(0);
  }

  @Test
  public void getBlock_blockExists_returnsTheBlock() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    Optional<Block> maybeBlock = subject.getBlock("1");

    assertThat(maybeBlock).isPresent();
    assertThat(maybeBlock.get().getId()).isEqualTo("1");
  }

  @Test
  public void getBlock_blockNotInList_returnsEmpty() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    Optional<Block> maybeBlock = subject.getBlock("111");

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getBlockAfter_thereExistsABlockAfter_returnsTheBlockAfterTheGivenBlock() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    Optional<Block> maybeBlock = subject.getInProgressBlockAfter("1");

    assertThat(maybeBlock).isPresent();
    assertThat(maybeBlock.get().getId()).isEqualTo("2");
  }

  @Test
  public void getBlockAfter_argIsLastBlock_returnsEmpty() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    Optional<Block> maybeBlock = subject.getInProgressBlockAfter("321");

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getBlockAfter_emptyBlocks_returnsEmpty() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(
            new Applicant().getApplicantData(),
            ProgramDefinition.builder()
                .setId(123L)
                .setAdminName("Admin program name")
                .setAdminDescription("Admin description")
                .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
                .setLocalizedDescription(
                    LocalizedStrings.of(Locale.US, "This program is for testing."))
                .setExternalLink("")
                .setStatusDefinitions(new StatusDefinitions())
                .setDisplayMode(DisplayMode.PUBLIC)
                .setProgramType(ProgramType.DEFAULT)
                .build(),
            FAKE_BASE_URL);

    Optional<Block> maybeBlock = subject.getInProgressBlockAfter("321");

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getFirstIncompleteBlock_firstIncompleteBlockReturned() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    Optional<Block> maybeBlock = subject.getFirstIncompleteOrStaticBlock();

    assertThat(maybeBlock).isNotEmpty();
    assertThat(maybeBlock.get().getName()).isEqualTo("Block one");
  }

  @Test
  public void getFirstIncompleteBlock_returnsFirstIncompleteIfFirstBlockCompleted() {
    // Answer the first block in this program - it will still be in getInProgressBlocks;
    answerNameQuestion(programDefinition.id());

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    assertThat(subject.getInProgressBlocks().get(0).getName()).isEqualTo("Block one");

    Optional<Block> maybeBlock = subject.getFirstIncompleteOrStaticBlock();

    assertThat(maybeBlock).isNotEmpty();
    assertThat(maybeBlock.get().getName()).isEqualTo("Block two");
  }

  @Test
  public void preferredLanguageSupported_returnsTrueForDefaults() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    assertThat(subject.preferredLanguageSupported()).isTrue();
  }

  @Test
  public void preferredLanguageSupported_returnsFalseForUnsupportedLang() {
    applicantData.setPreferredLocale(Locale.CHINESE);

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    assertThat(subject.preferredLanguageSupported()).isFalse();
  }

  @Test
  public void getSummaryData_returnsCompletedData() {
    // Create a program with lots of questions
    QuestionDefinition singleSelectQuestionDefinition =
        testQuestionBank.applicantSeason().getQuestionDefinition();
    QuestionDefinition multiSelectQuestionDefinition =
        testQuestionBank.applicantKitchenTools().getQuestionDefinition();
    QuestionDefinition fileQuestionDefinition =
        testQuestionBank.applicantFile().getQuestionDefinition();
    QuestionDefinition enumeratorQuestionDefinition =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition repeatedQuestionDefinition =
        testQuestionBank.applicantHouseholdMemberName().getQuestionDefinition();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withLocalizedName(Locale.GERMAN, "Mein Programm")
            .withBlock("Block one")
            .withRequiredQuestionDefinitions(
                ImmutableList.of(
                    nameQuestion,
                    colorQuestion,
                    addressQuestion,
                    singleSelectQuestionDefinition,
                    multiSelectQuestionDefinition))
            .withBlock("file")
            .withRequiredQuestionDefinition(fileQuestionDefinition)
            .withBlock("enumerator")
            .withRequiredQuestionDefinition(enumeratorQuestionDefinition)
            .withRepeatedBlock("repeated")
            .withRequiredQuestionDefinition(repeatedQuestionDefinition)
            .buildDefinition();
    answerNameQuestion(programDefinition.id());
    answerColorQuestion(programDefinition.id());
    answerAddressQuestion(programDefinition.id());

    // Answer the questions
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(singleSelectQuestionDefinition.getQuestionPathSegment()),
        1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(multiSelectQuestionDefinition.getQuestionPathSegment()),
        0,
        1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(multiSelectQuestionDefinition.getQuestionPathSegment()),
        1,
        2L);
    QuestionAnswerer.answerFileQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(fileQuestionDefinition.getQuestionPathSegment()),
        "file-key");
    Path enumeratorPath =
        ApplicantData.APPLICANT_PATH.join(enumeratorQuestionDefinition.getQuestionPathSegment());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantData, enumeratorPath, ImmutableList.of("enum one", "enum two"));
    QuestionAnswerer.answerNameQuestion(
        applicantData,
        enumeratorPath.atIndex(0).join(repeatedQuestionDefinition.getQuestionPathSegment()),
        "first",
        "middle",
        "last");
    QuestionAnswerer.answerNameQuestion(
        applicantData,
        enumeratorPath.atIndex(1).join(repeatedQuestionDefinition.getQuestionPathSegment()),
        "foo",
        "bar",
        "baz");

    // Test the summary data
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    ImmutableList<AnswerData> result = subject.getSummaryData();

    assertEquals(9, result.size());
    assertThat(result.get(0).answerText()).isEqualTo("Alice Middle Last");
    assertThat(result.get(1).answerText()).isEqualTo("mauve");
    assertThat(result.get(2).answerText()).isEqualTo("123 Rhode St.\nSeattle, WA 12345");

    // Check single and multi select answers
    assertThat(result.get(3).questionIndex()).isEqualTo(3);
    assertThat(result.get(3).scalarAnswersInDefaultLocale())
        .containsExactly(
            new AbstractMap.SimpleEntry<>(
                ApplicantData.APPLICANT_PATH
                    .join(singleSelectQuestionDefinition.getQuestionPathSegment())
                    .join(Scalar.SELECTION),
                "winter"));
    assertThat(result.get(4).questionIndex()).isEqualTo(4);
    assertThat(result.get(4).scalarAnswersInDefaultLocale())
        .containsExactly(
            new AbstractMap.SimpleEntry<>(
                ApplicantData.APPLICANT_PATH
                    .join(multiSelectQuestionDefinition.getQuestionPathSegment())
                    .join(Scalar.SELECTIONS),
                "[toaster, pepper grinder]"));

    // check file answer
    assertThat(result.get(5).questionIndex()).isEqualTo(0);
    assertThat(result.get(5).scalarAnswersInDefaultLocale())
        .containsExactly(
            new AbstractMap.SimpleEntry<>(
                ApplicantData.APPLICANT_PATH
                    .join(fileQuestionDefinition.getQuestionPathSegment())
                    .join(Scalar.FILE_KEY),
                String.format(
                    "%s/admin/programs/%d/files/%s",
                    FAKE_BASE_URL, programDefinition.id(), "file-key")));

    // Check enumerator and repeated answers
    assertThat(result.get(6).questionIndex()).isEqualTo(0);
    assertThat(result.get(6).scalarAnswersInDefaultLocale())
        .containsExactly(new AbstractMap.SimpleEntry<>(enumeratorPath, "enum one\nenum two"));
    assertThat(result.get(7).questionIndex()).isEqualTo(0);
    assertThat(result.get(7).scalarAnswersInDefaultLocale())
        .containsExactlyEntriesOf(
            ImmutableMap.of(
                enumeratorPath
                    .atIndex(0)
                    .join(repeatedQuestionDefinition.getQuestionPathSegment())
                    .join(Scalar.FIRST_NAME),
                "first",
                enumeratorPath
                    .atIndex(0)
                    .join(repeatedQuestionDefinition.getQuestionPathSegment())
                    .join(Scalar.MIDDLE_NAME),
                "middle",
                enumeratorPath
                    .atIndex(0)
                    .join(repeatedQuestionDefinition.getQuestionPathSegment())
                    .join(Scalar.LAST_NAME),
                "last"));
    assertThat(result.get(8).questionIndex()).isEqualTo(0);
    assertThat(result.get(8).scalarAnswersInDefaultLocale())
        .containsExactlyEntriesOf(
            ImmutableMap.of(
                enumeratorPath
                    .atIndex(1)
                    .join(repeatedQuestionDefinition.getQuestionPathSegment())
                    .join(Scalar.FIRST_NAME),
                "foo",
                enumeratorPath
                    .atIndex(1)
                    .join(repeatedQuestionDefinition.getQuestionPathSegment())
                    .join(Scalar.MIDDLE_NAME),
                "bar",
                enumeratorPath
                    .atIndex(1)
                    .join(repeatedQuestionDefinition.getQuestionPathSegment())
                    .join(Scalar.LAST_NAME),
                "baz"));

    for (int i = 0; i < result.size(); ++i) {
      assertThat(result.get(i).isEligible())
          .withFailMessage("Result index %d should be eligible", i)
          .isTrue();
    }
  }

  @Test
  public void getSummaryData_returnsLinkForUploadedFile() {
    // Create a program with a fileupload question and a non-fileupload question
    QuestionDefinition fileUploadQuestionDefinition =
        testQuestionBank.applicantFile().getQuestionDefinition();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withBlock("Block one")
            .withRequiredQuestionDefinition(nameQuestion)
            .withRequiredQuestionDefinition(fileUploadQuestionDefinition)
            .buildDefinition();
    // Answer the questions
    answerNameQuestion(programDefinition.id());
    QuestionAnswerer.answerFileQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(fileUploadQuestionDefinition.getQuestionPathSegment()),
        "test-file-key");

    // Test the summary data
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    ImmutableList<AnswerData> result = subject.getSummaryData();

    assertEquals(2, result.size());
    // Non-fileupload question does not have a file key
    assertThat(result.get(0).encodedFileKey()).isEmpty();
    // Fileupload question has a file key
    assertThat(result.get(1).encodedFileKey()).isNotEmpty();
  }

  @Test
  @Parameters({"5, true", "1, false"})
  public void getSummaryData_isEligible(long answer, boolean expectedResult) {
    // Create a program with an eligibility condition == 5 and answer it with different values.
    QuestionDefinition numberQuestionDefinition =
        testQuestionBank.applicantJugglingNumber().getQuestionDefinition();
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    numberQuestionDefinition.getId(),
                    Scalar.NUMBER,
                    Operator.EQUAL_TO,
                    PredicateValue.of("5"))),
            PredicateAction.SHOW_BLOCK);

    EligibilityDefinition eligibilityDefinition =
        EligibilityDefinition.builder().setPredicate(predicate).build();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withBlock("Block one")
            .withRequiredQuestionDefinition(numberQuestionDefinition)
            .withEligibilityDefinition(eligibilityDefinition)
            .buildDefinition();

    // Answer the questions
    answerNameQuestion(programDefinition.id());
    QuestionAnswerer.answerNumberQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(numberQuestionDefinition.getQuestionPathSegment()),
        answer);

    // Test the summary data
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    ImmutableList<AnswerData> result = subject.getSummaryData();

    assertEquals(1, result.size());
    assertThat(result.get(0).isEligible()).isEqualTo(expectedResult);
  }

  @Test
  public void getSummaryData_returnsWithEmptyData() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    ImmutableList<AnswerData> result = subject.getSummaryData();

    assertThat(result).hasSize(3);
    assertThat(result.get(0).answerText()).isEqualTo("");
    assertThat(result.get(1).answerText()).isEqualTo("-");
    assertThat(result.get(2).answerText()).isEqualTo("");
  }

  @Test
  public void getBlockIndex() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);

    assertThat(subject.getBlockIndex("1")).isEqualTo(0);
    assertThat(subject.getBlockIndex("2")).isEqualTo(1);
    assertThat(subject.getBlockIndex("not a real block id")).isEqualTo(-1);
  }

  @Test
  @Parameters({"5, true", "1, false"})
  public void getIsApplicationEligible(long answer, boolean expectedResult) {
    // Create a program with an eligibility condition == 5 and answer it with different values.
    QuestionDefinition numberQuestionDefinition =
        testQuestionBank.applicantJugglingNumber().getQuestionDefinition();
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    numberQuestionDefinition.getId(),
                    Scalar.NUMBER,
                    Operator.EQUAL_TO,
                    PredicateValue.of("5"))),
            PredicateAction.SHOW_BLOCK);

    EligibilityDefinition eligibilityDefinition =
        EligibilityDefinition.builder().setPredicate(predicate).build();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withBlock("Block one")
            .withRequiredQuestionDefinition(numberQuestionDefinition)
            .withEligibilityDefinition(eligibilityDefinition)
            .buildDefinition();

    // Answer the questions
    answerNameQuestion(programDefinition.id());
    QuestionAnswerer.answerNumberQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(numberQuestionDefinition.getQuestionPathSegment()),
        answer);

    // Test the summary data
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    assertThat(subject.isApplicationEligible()).isEqualTo(expectedResult);
  }

  @Test
  @Parameters({"5, blue, false", "5, purple, true", "1, blue, true"})
  public void getIsApplicationNotEligible(
      long numberAnswer, String textAnswer, boolean expectedResult) {
    // Create a program with one block that has an eligibility condition == 5
    // and another with eligibility condition == "blue" answer it with different values.

    QuestionDefinition numberQuestionDefinition =
        testQuestionBank.applicantJugglingNumber().getQuestionDefinition();
    PredicateDefinition numberPredicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    numberQuestionDefinition.getId(),
                    Scalar.NUMBER,
                    Operator.EQUAL_TO,
                    PredicateValue.of("5"))),
            PredicateAction.SHOW_BLOCK);

    QuestionDefinition colorQuestionDefinition =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    PredicateDefinition colorPredicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    colorQuestionDefinition.getId(),
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("blue"))),
            PredicateAction.HIDE_BLOCK);

    EligibilityDefinition numberEligibilityDefinition =
        EligibilityDefinition.builder().setPredicate(numberPredicate).build();
    EligibilityDefinition colorEligibilityDefinition =
        EligibilityDefinition.builder().setPredicate(colorPredicate).build();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withBlock("Block one")
            .withRequiredQuestionDefinition(numberQuestionDefinition)
            .withEligibilityDefinition(numberEligibilityDefinition)
            .withBlock("Block two")
            .withRequiredQuestionDefinition(colorQuestionDefinition)
            .withEligibilityDefinition(colorEligibilityDefinition)
            .buildDefinition();

    // Answer the questions
    answerNameQuestion(programDefinition.id());
    QuestionAnswerer.answerNumberQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(numberQuestionDefinition.getQuestionPathSegment()),
        numberAnswer);
    QuestionAnswerer.answerTextQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(colorQuestionDefinition.getQuestionPathSegment()),
        textAnswer);

    // Test the summary data
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    assertThat(subject.isApplicationNotEligible()).isEqualTo(expectedResult);
  }

  @Test
  @Parameters({"5, false", "1, true"})
  public void getIsApplicationNotEligible_oneBlockUnanswered(
      long numberAnswer, boolean expectedResult) {
    // Create a program with one block that has an eligibility condition == 5
    // and another with eligibility condition == "blue" answer only the first
    // block with different values.

    QuestionDefinition numberQuestionDefinition =
        testQuestionBank.applicantJugglingNumber().getQuestionDefinition();
    PredicateDefinition numberPredicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    numberQuestionDefinition.getId(),
                    Scalar.NUMBER,
                    Operator.EQUAL_TO,
                    PredicateValue.of("5"))),
            PredicateAction.SHOW_BLOCK);

    QuestionDefinition colorQuestionDefinition =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    PredicateDefinition colorPredicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    colorQuestionDefinition.getId(),
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("blue"))),
            PredicateAction.HIDE_BLOCK);

    EligibilityDefinition numberEligibilityDefinition =
        EligibilityDefinition.builder().setPredicate(numberPredicate).build();
    EligibilityDefinition colorEligibilityDefinition =
        EligibilityDefinition.builder().setPredicate(colorPredicate).build();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withBlock("Block one")
            .withRequiredQuestionDefinition(numberQuestionDefinition)
            .withEligibilityDefinition(numberEligibilityDefinition)
            .withBlock("Block two")
            .withRequiredQuestionDefinition(colorQuestionDefinition)
            .withEligibilityDefinition(colorEligibilityDefinition)
            .buildDefinition();

    // Answer the questions
    answerNameQuestion(programDefinition.id());
    QuestionAnswerer.answerNumberQuestion(
        applicantData,
        ApplicantData.APPLICANT_PATH.join(numberQuestionDefinition.getQuestionPathSegment()),
        numberAnswer);

    // Test the summary data
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition, FAKE_BASE_URL);
    assertThat(subject.isApplicationNotEligible()).isEqualTo(expectedResult);
  }

  private void answerNameQuestion(long programId) {
    Path path = Path.create("applicant.applicant_name");
    QuestionAnswerer.answerNameQuestion(applicantData, path, "Alice", "Middle", "Last");
    QuestionAnswerer.addMetadata(applicantData, path, programId, 12345L);
  }

  private void answerColorQuestion(long programId) {
    answerColorQuestion(programId, "mauve");
  }

  private void answerColorQuestion(long programId, String color) {
    Path path = Path.create("applicant.applicant_favorite_color");
    QuestionAnswerer.answerTextQuestion(applicantData, path, "mauve");
    QuestionAnswerer.answerTextQuestion(applicantData, path, color);
    QuestionAnswerer.addMetadata(applicantData, path, programId, 12345L);
  }

  private void answerAddressQuestion(long programId) {
    Path path = Path.create("applicant.applicant_address");
    QuestionAnswerer.answerAddressQuestion(
        applicantData, path, "123 Rhode St.", "", "Seattle", "WA", "12345");
    QuestionAnswerer.addMetadata(applicantData, path, programId, 12345L);
  }
}
