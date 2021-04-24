package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.program.BlockDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;
import support.QuestionAnswerer;
import support.TestQuestionBank;

public class BlockTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  private static final NameQuestionDefinition NAME_QUESTION =
      (NameQuestionDefinition) testQuestionBank.applicantName().getQuestionDefinition();
  private static final Path NAME_QUESTION_PATH = Path.create("applicant.applicant_name");

  private static final TextQuestionDefinition COLOR_QUESTION =
      (TextQuestionDefinition) testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
  private static final Path COLOR_QUESTION_PATH = Path.create("applicant.applicant_favorite_color");

  @Test
  public void createNewBlock() {
    BlockDefinition definition =
        BlockDefinition.builder().setId(123L).setName("name").setDescription("description").build();
    Block block = new Block(1L, definition, new ApplicantData());
    assertThat(block.getId()).isEqualTo("1");
    assertThat(block.getName()).isEqualTo("name");
    assertThat(block.getDescription()).isEqualTo("description");
    assertThat(block.getQuestions()).isEmpty();
    assertThat(block.hasErrors()).isFalse();
    assertThat(block.isCompleteWithoutErrors()).isTrue();
  }

  @Test
  public void equalsAndHashCode() {
    BlockDefinition definition =
        BlockDefinition.builder().setId(123L).setName("name").setDescription("description").build();
    QuestionDefinition question = NAME_QUESTION;
    ApplicantData applicant = new ApplicantData();
    applicant.putString(Path.create("applicant.hello"), "world");

    new EqualsTester()
        .addEqualityGroup(
            new Block(1L, definition, new ApplicantData()),
            new Block(1L, definition, new ApplicantData()))
        .addEqualityGroup(
            new Block(2L, definition, new ApplicantData()),
            new Block(2L, definition, new ApplicantData()))
        .addEqualityGroup(
            new Block(
                1L,
                definition.toBuilder()
                    .addQuestion(ProgramQuestionDefinition.create(question))
                    .build(),
                new ApplicantData()),
            new Block(
                1L,
                definition.toBuilder()
                    .addQuestion(ProgramQuestionDefinition.create(question))
                    .build(),
                new ApplicantData()))
        .addEqualityGroup(
            new Block(1L, definition, applicant), new Block(1L, definition, applicant))
        .testEquals();
  }

  @Test
  public void getQuestions_returnsCorrectApplicantQuestions() {
    BlockDefinition definition = setUpBlockWithQuestions();
    ApplicantData applicantData = new ApplicantData();

    Block block = new Block(1L, definition, applicantData);

    ImmutableList<ApplicantQuestion> expected =
        ImmutableList.of(
            new ApplicantQuestion(NAME_QUESTION, applicantData),
            new ApplicantQuestion(COLOR_QUESTION, applicantData));
    assertThat(block.getQuestions()).containsExactlyElementsOf(expected);
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/377): Add more tests for
  // hasErrors once question validation is implemented for at least one type.
  @Test
  public void hasErrors_returnsFalseIfBlockHasNoQuestions() {
    BlockDefinition definition =
        BlockDefinition.builder().setId(123L).setName("name").setDescription("description").build();
    Block block = new Block(1L, definition, new ApplicantData());

    assertThat(block.hasErrors()).isFalse();
  }

  @Test
  public void hasErrors_returnsFalseIfQuestionsHaveNoErrors() {
    BlockDefinition definition = setUpBlockWithQuestions();
    Block block = new Block(1L, definition, new ApplicantData());

    assertThat(block.hasErrors()).isFalse();
  }

  @Test
  public void isComplete_returnsTrueForBlockWithNoQuestions() {
    BlockDefinition definition =
        BlockDefinition.builder().setId(123L).setName("name").setDescription("description").build();
    Block block = new Block(1L, definition, new ApplicantData());

    assertThat(block.isCompleteWithoutErrors()).isTrue();
  }

  @Test
  public void isComplete_returnsFalseIfMultipleQuestionsNotAnswered() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block(1L, definition, applicantData);

    // No questions filled in yet.
    assertThat(block.isCompleteWithoutErrors()).isFalse();
  }

  @Test
  public void isComplete_returnsFalseIfOneQuestionNotAnswered() {
    ApplicantData applicantData = new ApplicantData();
    // Fill in one of the questions.
    answerColorQuestion(applicantData);
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block(1L, definition, applicantData);

    assertThat(block.isCompleteWithoutErrors()).isFalse();
  }

  @Test
  public void isComplete_returnsTrueIfAllQuestionsAnswered() {
    ApplicantData applicantData = new ApplicantData();
    // Fill in all questions.
    answerNameQuestion(applicantData);
    answerColorQuestion(applicantData);
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block(1L, definition, applicantData);

    assertThat(block.isCompleteWithoutErrors()).isTrue();
  }

  @Test
  public void isComplete_outsideChangesToApplicantData_updatesCompletionCheck() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block(1L, definition, applicantData);

    assertThat(block.isCompleteWithoutErrors()).isFalse();

    // Complete the block.
    answerNameQuestion(applicantData);
    answerColorQuestion(applicantData);
    assertThat(block.isCompleteWithoutErrors()).isTrue();
  }

  @Test
  public void wasCompletedInProgram_returnsFalse() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block(1L, definition, applicantData);

    assertThat(block.wasCompletedInProgram(1L)).isFalse();
  }

  @Test
  public void wasCompletedInProgram_returnsFalseIfQuestionsCompletedInDifferentProgram() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block(1L, definition, applicantData);
    // Answer questions in different program.
    answerNameQuestion(applicantData, 567L);
    answerColorQuestion(applicantData, 567L);

    assertThat(block.wasCompletedInProgram(1L)).isFalse();
  }

  @Test
  public void wasCompletedInProgram_returnsFalseIfOnlyOneQuestionAnswered() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block(1L, definition, applicantData);
    answerNameQuestion(applicantData, 1L);

    assertThat(block.wasCompletedInProgram(1L)).isFalse();
  }

  @Test
  public void wasCompletedInProgram_returnsTrueIfQuestionsCompletedInGivenProgram() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block(1L, definition, applicantData);
    answerNameQuestion(applicantData, 22L);
    answerColorQuestion(applicantData, 22L);

    assertThat(block.wasCompletedInProgram(22L)).isTrue();
  }

  @Test
  public void wasCompletedInProgram_returnsFalseIfSomeQuestionsCompletedInDifferentProgram() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block(1L, definition, applicantData);
    answerNameQuestion(applicantData, 100L);
    answerColorQuestion(applicantData, 200L);

    assertThat(block.wasCompletedInProgram(200L)).isFalse();
  }

  private static BlockDefinition setUpBlockWithQuestions() {
    return BlockDefinition.builder()
        .setId(20L)
        .setName("")
        .setDescription("")
        .addQuestion(ProgramQuestionDefinition.create(NAME_QUESTION))
        .addQuestion(ProgramQuestionDefinition.create(COLOR_QUESTION))
        .build();
  }

  private static void answerNameQuestion(ApplicantData data) {
    answerNameQuestion(data, 1L);
  }

  private static void answerNameQuestion(ApplicantData data, long programId) {
    QuestionAnswerer.answerNameQuestion(data, NAME_QUESTION_PATH, "Alice", "P.", "Walker");
    QuestionAnswerer.addMetadata(data, NAME_QUESTION_PATH, programId, 12345L);
  }

  private static void answerColorQuestion(ApplicantData data) {
    answerColorQuestion(data, 1L);
  }

  private static void answerColorQuestion(ApplicantData data, long programId) {
    QuestionAnswerer.answerTextQuestion(data, COLOR_QUESTION_PATH, "maroon");
    QuestionAnswerer.addMetadata(data, COLOR_QUESTION_PATH, programId, 12345L);
  }
}
