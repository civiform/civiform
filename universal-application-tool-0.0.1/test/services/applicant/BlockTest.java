package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.util.Optional;
import org.junit.Test;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.ScalarType;
import services.question.types.TextQuestionDefinition;
import support.QuestionAnswerer;
import support.TestQuestionBank;

public class BlockTest {

  private static final long UNUSED_PROGRAM_ID = 1L;

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  private static final NameQuestionDefinition NAME_QUESTION =
      (NameQuestionDefinition) testQuestionBank.applicantName().getQuestionDefinition();
  private static final TextQuestionDefinition COLOR_QUESTION =
      (TextQuestionDefinition) testQuestionBank.applicantFavoriteColor().getQuestionDefinition();

  @Test
  public void createNewBlock() {
    BlockDefinition definition =
        BlockDefinition.builder().setId(123L).setName("name").setDescription("description").build();
    Block block = new Block("1", definition, new ApplicantData(), Optional.empty());
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
            new Block("1", definition, new ApplicantData(), Optional.empty()),
            new Block("1", definition, new ApplicantData(), Optional.empty()))
        .addEqualityGroup(
            new Block("2", definition, new ApplicantData(), Optional.empty()),
            new Block("2", definition, new ApplicantData(), Optional.empty()))
        .addEqualityGroup(
            new Block(
                "1",
                definition.toBuilder()
                    .addQuestion(ProgramQuestionDefinition.create(question))
                    .build(),
                new ApplicantData(),
                Optional.empty()),
            new Block(
                "1",
                definition.toBuilder()
                    .addQuestion(ProgramQuestionDefinition.create(question))
                    .build(),
                new ApplicantData(),
                Optional.empty()))
        .addEqualityGroup(
            new Block("1", definition, applicant, Optional.empty()),
            new Block("1", definition, applicant, Optional.empty()))
        .testEquals();
  }

  @Test
  public void getQuestions_returnsCorrectApplicantQuestions() {
    BlockDefinition definition = setUpBlockWithQuestions();
    ApplicantData applicantData = new ApplicantData();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    ImmutableList<ApplicantQuestion> expected =
        ImmutableList.of(
            new ApplicantQuestion(NAME_QUESTION, applicantData, ApplicantData.APPLICANT_PATH),
            new ApplicantQuestion(COLOR_QUESTION, applicantData, ApplicantData.APPLICANT_PATH));
    assertThat(block.getQuestions()).containsExactlyElementsOf(expected);
  }

  @Test
  public void getScalarType_returnsAllScalarTypes() {
    BlockDefinition definition = setUpBlockWithQuestions();
    ApplicantData applicantData = new ApplicantData();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(
            block.getScalarType(
                ApplicantData.APPLICANT_PATH.join("applicant_name").join(Scalar.FIRST_NAME)))
        .contains(ScalarType.STRING);
    assertThat(
            block.getScalarType(
                ApplicantData.APPLICANT_PATH.join("applicant_name").join(Scalar.MIDDLE_NAME)))
        .contains(ScalarType.STRING);
    assertThat(
            block.getScalarType(
                ApplicantData.APPLICANT_PATH.join("applicant_name").join(Scalar.LAST_NAME)))
        .contains(ScalarType.STRING);
    assertThat(
            block.getScalarType(
                ApplicantData.APPLICANT_PATH
                    .join("applicant_name")
                    .join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(ScalarType.LONG);
    assertThat(
            block.getScalarType(
                ApplicantData.APPLICANT_PATH.join("applicant_name").join(Scalar.UPDATED_AT)))
        .contains(ScalarType.LONG);
    assertThat(
            block.getScalarType(
                ApplicantData.APPLICANT_PATH.join("applicant_favorite_color").join(Scalar.TEXT)))
        .contains(ScalarType.STRING);
    assertThat(
            block.getScalarType(
                ApplicantData.APPLICANT_PATH
                    .join("applicant_favorite_color")
                    .join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(ScalarType.LONG);
    assertThat(
            block.getScalarType(
                ApplicantData.APPLICANT_PATH
                    .join("applicant_favorite_color")
                    .join(Scalar.UPDATED_AT)))
        .contains(ScalarType.LONG);
  }

  @Test
  public void getScalarType_forNonExistentPath_returnsEmpty() {
    BlockDefinition definition = setUpBlockWithQuestions();
    ApplicantData applicantData = new ApplicantData();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.getScalarType(Path.create("fake.path"))).isEmpty();
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/377): Add more tests for
  // hasErrors once question validation is implemented for at least one type.
  @Test
  public void hasErrors_returnsFalseIfBlockHasNoQuestions() {
    BlockDefinition definition =
        BlockDefinition.builder().setId(123L).setName("name").setDescription("description").build();
    Block block = new Block("1", definition, new ApplicantData(), Optional.empty());

    assertThat(block.hasErrors()).isFalse();
  }

  @Test
  public void hasErrors_returnsFalseIfQuestionsHaveNoErrors() {
    BlockDefinition definition = setUpBlockWithQuestions();
    Block block = new Block("1", definition, new ApplicantData(), Optional.empty());

    assertThat(block.hasErrors()).isFalse();
  }

  @Test
  public void isComplete_returnsTrueForBlockWithNoQuestions() {
    BlockDefinition definition =
        BlockDefinition.builder().setId(123L).setName("name").setDescription("description").build();
    Block block = new Block("1", definition, new ApplicantData(), Optional.empty());

    assertThat(block.isCompleteWithoutErrors()).isTrue();
  }

  @Test
  public void isComplete_returnsFalseIfMultipleQuestionsNotAnswered() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    // No questions filled in yet.
    assertThat(block.isCompleteWithoutErrors()).isFalse();
  }

  @Test
  public void isComplete_returnsFalseIfOneQuestionNotAnswered() {
    ApplicantData applicantData = new ApplicantData();
    // Fill in one of the questions.
    answerColorQuestion(applicantData, UNUSED_PROGRAM_ID);
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isCompleteWithoutErrors()).isFalse();
  }

  @Test
  public void isComplete_returnsTrueIfAllQuestionsAnswered() {
    ApplicantData applicantData = new ApplicantData();
    // Fill in all questions.
    answerNameQuestion(applicantData, UNUSED_PROGRAM_ID);
    answerColorQuestion(applicantData, UNUSED_PROGRAM_ID);
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isCompleteWithoutErrors()).isTrue();
  }

  @Test
  public void isComplete_outsideChangesToApplicantData_updatesCompletionCheck() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isCompleteWithoutErrors()).isFalse();

    // Complete the block.
    answerNameQuestion(applicantData, UNUSED_PROGRAM_ID);
    answerColorQuestion(applicantData, UNUSED_PROGRAM_ID);
    assertThat(block.isCompleteWithoutErrors()).isTrue();
  }

  @Test
  public void wasCompletedInProgram_returnsFalse() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.wasCompletedInProgram(1L)).isFalse();
  }

  @Test
  public void wasCompletedInProgram_returnsFalseIfQuestionsCompletedInDifferentProgram() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());
    // Answer questions in different program.
    answerNameQuestion(applicantData, 567L);
    answerColorQuestion(applicantData, 567L);

    assertThat(block.wasCompletedInProgram(1L)).isFalse();
  }

  @Test
  public void wasCompletedInProgram_returnsFalseIfOnlyOneQuestionAnswered() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());
    answerNameQuestion(applicantData, 1L);

    assertThat(block.wasCompletedInProgram(1L)).isFalse();
  }

  @Test
  public void wasCompletedInProgram_returnsTrueIfQuestionsCompletedInGivenProgram() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());
    answerNameQuestion(applicantData, 22L);
    answerColorQuestion(applicantData, 22L);

    assertThat(block.wasCompletedInProgram(22L)).isTrue();
  }

  @Test
  public void wasCompletedInProgram_returnsTrueIfSomeQuestionsCompletedInGivenProgram() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());
    answerNameQuestion(applicantData, 100L);
    answerColorQuestion(applicantData, 200L);

    assertThat(block.wasCompletedInProgram(200L)).isTrue();
  }

  @Test
  public void isEnumerator_isTrue() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("")
            .setDescription("")
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantHouseholdMembers().getQuestionDefinition()))
            .build();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isEnumerator()).isTrue();
  }

  @Test
  public void isEnumerator_isFalse() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isEnumerator()).isFalse();
  }

  @Test
  public void getEnumeratorQuestion() {
    ApplicantData applicantData = new ApplicantData();
    QuestionDefinition enumeratorQuestionDefinition =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    BlockDefinition definition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("")
            .setDescription("")
            .addQuestion(ProgramQuestionDefinition.create(enumeratorQuestionDefinition))
            .build();
    Block block = new Block("1", definition, applicantData, Optional.empty());

    ApplicantQuestion enumeratorQuestion = block.getEnumeratorQuestion();

    assertThat(enumeratorQuestion.getQuestionDefinition()).isEqualTo(enumeratorQuestionDefinition);
  }

  @Test
  public void isFileUpload_isTrue() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("")
            .setDescription("")
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantFile().getQuestionDefinition()))
            .build();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isFileUpload()).isTrue();
  }

  @Test
  public void isFileUpload_isFalse() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isFileUpload()).isFalse();
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

  private static void answerNameQuestion(ApplicantData data, long programId) {
    Path path = Path.create("applicant.applicant_name");
    QuestionAnswerer.answerNameQuestion(data, path, "Alice", "P.", "Walker");
    QuestionAnswerer.addMetadata(data, path, programId, 12345L);
  }

  private static void answerColorQuestion(ApplicantData data, long programId) {
    Path path = Path.create("applicant.applicant_favorite_color");
    QuestionAnswerer.answerTextQuestion(data, path, "maroon");
    QuestionAnswerer.addMetadata(data, path, programId, 12345L);
  }
}
