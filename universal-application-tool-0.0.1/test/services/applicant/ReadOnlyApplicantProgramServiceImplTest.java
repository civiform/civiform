package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.Path;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
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
        applicantData, path, "123 Rhode St.", "Seattle", "WA", "12345");
    QuestionAnswerer.addMetadata(applicantData, path, programId, 12345L);
  }
}
