package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.OptionalLong;
import org.junit.Test;
import services.Path;
import services.program.BlockDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.TextQuestionDefinition;

public class BlockTest {

  @Test
  public void create() {
    BlockDefinition definition =
        BlockDefinition.builder().setId(123L).setName("name").setDescription("description").build();
    Block block = new Block(1L, definition, new ApplicantData());
    assertThat(block.getId()).isEqualTo(1L);
    assertThat(block.getName()).isEqualTo("name");
    assertThat(block.getDescription()).isEqualTo("description");
    assertThat(block.getQuestions()).isEmpty();
    assertThat(block.hasErrors()).isFalse();
    assertThat(block.isCompleteWithoutErrors()).isTrue();
  }

  @Test
  public void getQuestions_returnsCorrectApplicantQuestions() {
    QuestionDefinition name =
        new NameQuestionDefinition(
            OptionalLong.of(1L),
            1L,
            "name",
            Path.empty(),
            "",
            ImmutableMap.of(),
            ImmutableMap.of());
    QuestionDefinition text =
        new TextQuestionDefinition(
            OptionalLong.of(2L), 1L, "", Path.empty(), "", ImmutableMap.of(), ImmutableMap.of());
    BlockDefinition definition =
        BlockDefinition.builder()
            .setId(20L)
            .setName("")
            .setDescription("")
            .addQuestion(ProgramQuestionDefinition.create(name))
            .addQuestion(ProgramQuestionDefinition.create(text))
            .build();
    ApplicantData applicantData = new ApplicantData();

    Block block = new Block(1L, definition, applicantData);

    ImmutableList<ApplicantQuestion> expected =
        ImmutableList.of(
            new ApplicantQuestion(name, applicantData), new ApplicantQuestion(text, applicantData));
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
    applicantData.putString(Path.create("applicant.color"), "maroon");
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block(1L, definition, applicantData);

    assertThat(block.isCompleteWithoutErrors()).isFalse();
  }

  @Test
  public void isComplete_returnsTrueIfAllQuestionsAnswered() {
    ApplicantData applicantData = new ApplicantData();
    // Fill in all questions.
    applicantData.putString(Path.create("applicant.color"), "maroon");
    applicantData.putString(Path.create("applicant.name"), "Alice");
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
    applicantData.putString(Path.create("applicant.color"), "maroon");
    applicantData.putString(Path.create("applicant.name"), "Alice");
    assertThat(block.isCompleteWithoutErrors()).isTrue();
  }

  private static BlockDefinition setUpBlockWithQuestions() {
    Path namePath = Path.create("applicant.name");
    QuestionDefinition name =
        new TextQuestionDefinition(
            OptionalLong.of(1L), 1L, "", namePath, "", ImmutableMap.of(), ImmutableMap.of());
    Path colorPath = Path.create("applicant.color");
    QuestionDefinition color =
        new TextQuestionDefinition(
            OptionalLong.of(2L), 1L, "", colorPath, "", ImmutableMap.of(), ImmutableMap.of());
    return BlockDefinition.builder()
        .setId(20L)
        .setName("")
        .setDescription("")
        .addQuestion(ProgramQuestionDefinition.create(name))
        .addQuestion(ProgramQuestionDefinition.create(color))
        .build();
  }
}
