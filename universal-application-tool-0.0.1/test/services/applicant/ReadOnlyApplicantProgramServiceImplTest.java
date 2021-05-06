package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.Path;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.ScalarType;
import support.ProgramBuilder;
import support.QuestionAnswerer;

public class ReadOnlyApplicantProgramServiceImplTest extends WithPostgresContainer {

  private QuestionDefinition nameQuestion;
  private QuestionDefinition colorQuestion;
  private QuestionDefinition addressQuestion;
  private ApplicantData applicantData;
  private ProgramDefinition programDefinition;

  @Before
  public void setUp() {
    applicantData = new ApplicantData();
    nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    colorQuestion = testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    addressQuestion = testQuestionBank.applicantAddress().getQuestionDefinition();
    programDefinition =
        ProgramBuilder.newDraftProgram("My Program")
            .withLocalizedName(Locale.GERMAN, "Mein Programm")
            .withBlock("Block one")
            .withQuestionDefinition(nameQuestion)
            .withBlock("Block two")
            .withQuestionDefinition(colorQuestion)
            .withQuestionDefinition(addressQuestion)
            .buildDefinition();
  }

  @Test
  public void getProgramTitle_returnsProgramTitleInDefaultLocale() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    assertThat(subject.getProgramTitle()).isEqualTo("My Program");
  }

  @Test
  public void getProgramTitle_returnsProgramTitleForPreferredLocale() {
    applicantData.setPreferredLocale(Locale.GERMAN);
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    assertThat(subject.getProgramTitle()).isEqualTo("Mein Programm");
  }

  @Test
  public void getAllBlocks_includesPreviouslyCompletedBlocks() {
    // Answer first block in a separate program
    answerNameQuestion(programDefinition.id() + 1);

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);
    ImmutableList<Block> allBlocks = subject.getAllBlocks();

    assertThat(allBlocks).hasSize(2);
    assertThat(allBlocks.get(0).getName()).isEqualTo("Block one");
    assertThat(allBlocks.get(1).getName()).isEqualTo("Block two");
  }

  @Test
  public void getAllBlocks_includesRepeatedBlocks() {
    programDefinition =
        ProgramBuilder.newActiveProgram()
            .withBlock("name")
            .withQuestion(testQuestionBank.applicantName())
            .withBlock("address")
            .withQuestion(testQuestionBank.applicantAddress())
            .withBlock("enumeration - household members")
            .withQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock("repeated - household members name")
            .withQuestion(testQuestionBank.applicantHouseholdMemberName())
            .withAnotherRepeatedBlock("repeated - household members jobs")
            .withQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .withRepeatedBlock("deeply repeated - household members jobs income")
            .withQuestion(testQuestionBank.applicantHouseholdMemberJobIncome())
            .buildDefinition();

    // Add repeated entities to applicant data
    Path enumerationPath =
        ApplicantData.APPLICANT_PATH.join(testQuestionBank.applicantHouseholdMembers().getQuestionDefinition().getQuestionPathSegment());
    applicantData.putString(enumerationPath.atIndex(0).join(Scalar.ENTITY_NAME), "first entity");
    applicantData.putString(enumerationPath.atIndex(1).join(Scalar.ENTITY_NAME), "second entity");
    applicantData.putString(enumerationPath.atIndex(2).join(Scalar.ENTITY_NAME), "third entity");
    Path deepEnumerationPath = enumerationPath.atIndex(2).join(
        testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition().getQuestionPathSegment());
    applicantData.putString(deepEnumerationPath.atIndex(0).join(Scalar.ENTITY_NAME), "nested first job");
    applicantData.putString(deepEnumerationPath.atIndex(1).join(Scalar.ENTITY_NAME), "nested second job");

    ReadOnlyApplicantProgramService service =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    ImmutableList<Block> blocks = service.getAllBlocks();

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
    questionPath = deepEnumerationPath.atIndex(0).join("household_members_jobs_income");
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
    questionPath = deepEnumerationPath.atIndex(1).join("household_members_jobs_income");
    assertThat(blocks.get(10).getQuestions().get(0).getContextualizedScalars())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                questionPath.join(Scalar.NUMBER),
                ScalarType.LONG,
                questionPath.join(Scalar.PROGRAM_UPDATED_IN),
                ScalarType.LONG,
                questionPath.join(Scalar.UPDATED_AT),
                ScalarType.LONG));
    assertThat(blocks.get(10).getEntityNames()).containsExactlyEntriesOf(ImmutableMap.of(
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition(),
        "third entity",
        testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition(),
        "nested second job"));
  }

  @Test
  public void getInProgressBlocks_getsTheApplicantSpecificBlocksForTheProgram() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);
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
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);
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
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);
    ImmutableList<Block> blockList = subject.getInProgressBlocks();

    assertThat(blockList).isEmpty();
  }

  @Test
  public void getInProgressBlocks_includesBlocksThatWereCompletedInThisProgram() {
    // Answer block 1 questions in this program session
    answerNameQuestion(programDefinition.id());

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);
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
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    ImmutableList<Block> blockList = subject.getInProgressBlocks();

    // Block 1 should still be there
    Block block = blockList.get(0);
    assertThat(block.getName()).isEqualTo("Block one");

    // Block 2 should still be there, even though it was partially completed by another program.
    assertThat(blockList).hasSize(2);
  }

  @Test
  public void getBlock_blockExists_returnsTheBlock() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    Optional<Block> maybeBlock = subject.getBlock("1");

    assertThat(maybeBlock).isPresent();
    assertThat(maybeBlock.get().getId()).isEqualTo("1");
  }

  @Test
  public void getBlock_blockNotInList_returnsEmpty() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    Optional<Block> maybeBlock = subject.getBlock("111");

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getBlockAfter_thereExistsABlockAfter_returnsTheBlockAfterTheGivenBlock() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    Optional<Block> maybeBlock = subject.getBlockAfter("1");

    assertThat(maybeBlock).isPresent();
    assertThat(maybeBlock.get().getId()).isEqualTo("2");
  }

  @Test
  public void getBlockAfter_argIsLastBlock_returnsEmpty() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    Optional<Block> maybeBlock = subject.getBlockAfter("321");

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
                .addLocalizedName(Locale.US, "The Program")
                .addLocalizedDescription(Locale.US, "This program is for testing.")
                .build());

    Optional<Block> maybeBlock = subject.getBlockAfter("321");

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getFirstIncompleteBlock_firstIncompleteBlockReturned() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    Optional<Block> maybeBlock = subject.getFirstIncompleteBlock();

    assertThat(maybeBlock).isNotEmpty();
    assertThat(maybeBlock.get().getName()).isEqualTo("Block one");
  }

  @Test
  public void getFirstIncompleteBlock_returnsFirstIncompleteIfFirstBlockCompleted() {
    // Answer the first block in this program - it will still be in getInProgressBlocks;
    answerNameQuestion(programDefinition.id());

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    assertThat(subject.getInProgressBlocks().get(0).getName()).isEqualTo("Block one");

    Optional<Block> maybeBlock = subject.getFirstIncompleteBlock();

    assertThat(maybeBlock).isNotEmpty();
    assertThat(maybeBlock.get().getName()).isEqualTo("Block two");
  }

  @Test
  public void preferredLanguageSupported_returnsTrueForDefaults() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);
    assertThat(subject.preferredLanguageSupported()).isTrue();
  }

  @Test
  public void preferredLanguageSupported_returnsFalseForUnsupportedLang() {
    applicantData.setPreferredLocale(Locale.CHINESE);

    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    assertThat(subject.preferredLanguageSupported()).isFalse();
  }

  @Test
  public void getSummaryData_returnsCompletedData() {
    answerNameQuestion(programDefinition.id());
    answerColorQuestion(programDefinition.id());
    answerAddressQuestion(programDefinition.id());
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    ImmutableList<AnswerData> result = subject.getSummaryData();

    assertEquals(3, result.size());
    assertThat(result.get(0).answerText()).isEqualTo("Alice Middle Last");
    assertThat(result.get(1).answerText()).isEqualTo("mauve");
    assertThat(result.get(2).answerText()).isEqualTo("123 Rhode St.\nSeattle, WA, 12345");
  }

  @Test
  public void getSummaryData_returnsWithEmptyData() {
    ReadOnlyApplicantProgramService subject =
        new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);

    ImmutableList<AnswerData> result = subject.getSummaryData();

    assertEquals(3, result.size());
    assertThat(result.get(0).answerText()).isEqualTo("");
    assertThat(result.get(1).answerText()).isEqualTo("-");
    assertThat(result.get(2).answerText()).isEqualTo("");
  }

  private void answerNameQuestion(long programId) {
    Path path = Path.create("applicant.applicant_name");
    QuestionAnswerer.answerNameQuestion(applicantData, path, "Alice", "Middle", "Last");
    QuestionAnswerer.addMetadata(applicantData, path, programId, 12345L);
  }

  private void answerColorQuestion(long programId) {
    Path path = Path.create("applicant.applicant_favorite_color");
    QuestionAnswerer.answerTextQuestion(applicantData, path, "mauve");
    QuestionAnswerer.addMetadata(applicantData, path, programId, 12345L);
  }

  private void answerAddressQuestion(long programId) {
    Path path = Path.create("applicant.applicant_address");
    QuestionAnswerer.answerAddressQuestion(
        applicantData, path, "123 Rhode St.", "", "Seattle", "WA", "12345");
    QuestionAnswerer.addMetadata(applicantData, path, programId, 12345L);
  }
}
