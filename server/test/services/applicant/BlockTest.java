package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;
import services.LocalizedStrings;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.ScalarType;
import services.question.types.StaticContentQuestionDefinition;
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
  private static final StaticContentQuestionDefinition STATIC_QUESTION =
      new StaticContentQuestionDefinition(
          OptionalLong.of(123L),
          "more info about something",
          Optional.empty(),
          "Shows more info to the applicant",
          LocalizedStrings.of(Locale.US, "This is more info"),
          LocalizedStrings.of(Locale.US, ""));

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
    assertThat(block.isAnsweredWithoutErrors()).isTrue();
  }

  @Test
  public void equalsAndHashCode() {
    BlockDefinition definition =
        BlockDefinition.builder().setId(123L).setName("name").setDescription("description").build();
    QuestionDefinition question = NAME_QUESTION;
    ApplicantData applicant = new ApplicantData();
    applicant.putString(Path.create("applicant.hello"), "world");
    long programDefinitionId = 1L;

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
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            question, Optional.of(programDefinitionId)))
                    .build(),
                new ApplicantData(),
                Optional.empty()),
            new Block(
                "1",
                definition.toBuilder()
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            question, Optional.of(programDefinitionId)))
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
            new ApplicantQuestion(NAME_QUESTION, applicantData, Optional.empty()),
            new ApplicantQuestion(COLOR_QUESTION, applicantData, Optional.empty()));
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
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();
    // Both questions are required. Fill out an answer.
    answerColorQuestion(applicantData, UNUSED_PROGRAM_ID);
    answerNameQuestion(applicantData, UNUSED_PROGRAM_ID);
    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.hasErrors()).isFalse();
  }

  @Test
  public void hasErrors_returnsTrueIfApplicantDataHasInvalidUpdates() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();
    // Both questions are required. Fill out an answer.
    answerColorQuestion(applicantData, UNUSED_PROGRAM_ID);
    answerNameQuestion(applicantData, UNUSED_PROGRAM_ID);
    applicantData.setFailedUpdates(
        ImmutableMap.of(Path.create("applicant.applicant_favorite_color"), "invalid_input"));
    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.hasErrors()).isTrue();
  }

  @Test
  public void isAnswered_returnsTrueForBlockWithNoQuestions() {
    BlockDefinition definition =
        BlockDefinition.builder().setId(123L).setName("name").setDescription("description").build();
    Block block = new Block("1", definition, new ApplicantData(), Optional.empty());

    assertThat(block.isAnsweredWithoutErrors()).isTrue();
    assertThat(block.containsStatic()).isFalse();
  }

  @Test
  public void isAnswered_returnsFalseIfMultipleQuestionsNotAnswered() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    // No questions filled in yet.
    assertThat(block.isAnsweredWithoutErrors()).isFalse();
    assertThat(block.containsStatic()).isFalse();
  }

  @Test
  public void isAnswered_returnsFalseIfOneQuestionNotAnswered() {
    ApplicantData applicantData = new ApplicantData();
    // Fill in one of the questions.
    answerColorQuestion(applicantData, UNUSED_PROGRAM_ID);
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isAnsweredWithoutErrors()).isFalse();
  }

  @Test
  public void isAnswered_returnsTrueIfAllQuestionsAnswered() {
    ApplicantData applicantData = new ApplicantData();
    // Fill in all questions.
    answerNameQuestion(applicantData, UNUSED_PROGRAM_ID);
    answerColorQuestion(applicantData, UNUSED_PROGRAM_ID);
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isAnsweredWithoutErrors()).isTrue();
  }

  @Test
  public void isAnswered_returnsTrueIsAllQuestionsAnswered_includesStatic() {
    ApplicantData applicantData = new ApplicantData();
    // Fill in all questions.
    answerNameQuestion(applicantData, UNUSED_PROGRAM_ID);
    answerColorQuestion(applicantData, UNUSED_PROGRAM_ID);
    BlockDefinition definition =
        BlockDefinition.builder()
            .setId(20L)
            .setName("")
            .setDescription("")
            .addQuestion(ProgramQuestionDefinition.create(NAME_QUESTION, Optional.empty()))
            .addQuestion(ProgramQuestionDefinition.create(COLOR_QUESTION, Optional.empty()))
            .addQuestion(ProgramQuestionDefinition.create(STATIC_QUESTION, Optional.empty()))
            .build();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isAnsweredWithoutErrors()).isTrue();
    assertThat(block.containsStatic()).isTrue();
  }

  @Test
  public void isAnswered_onlyStatic() {
    ApplicantData applicantData = new ApplicantData();
    // Fill in all questions.
    answerNameQuestion(applicantData, UNUSED_PROGRAM_ID);
    answerColorQuestion(applicantData, UNUSED_PROGRAM_ID);
    BlockDefinition definition =
        BlockDefinition.builder()
            .setId(20L)
            .setName("")
            .setDescription("")
            .addQuestion(ProgramQuestionDefinition.create(STATIC_QUESTION, Optional.empty()))
            .build();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isAnsweredWithoutErrors()).isTrue();
    assertThat(block.containsStatic()).isTrue();
  }

  @Test
  public void isAnswered_outsideChangesToApplicantData_updatesCompletionCheck() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.isAnsweredWithoutErrors()).isFalse();

    // Complete the block.
    answerNameQuestion(applicantData, UNUSED_PROGRAM_ID);
    answerColorQuestion(applicantData, UNUSED_PROGRAM_ID);
    assertThat(block.isAnsweredWithoutErrors()).isTrue();
  }

  @Test
  public void wasAnsweredInProgram_returnsFalse() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());

    assertThat(block.wasAnsweredInProgram(1L)).isFalse();
  }

  @Test
  public void wasAnsweredInProgram_returnsFalseIfQuestionsCompletedInDifferentProgram() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());
    // Answer questions in different program.
    answerNameQuestion(applicantData, 567L);
    answerColorQuestion(applicantData, 567L);

    assertThat(block.wasAnsweredInProgram(1L)).isFalse();
  }

  @Test
  public void wasAnsweredInProgram_returnsFalseIfOnlyOneQuestionAnswered() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());
    answerNameQuestion(applicantData, 1L);

    assertThat(block.wasAnsweredInProgram(1L)).isFalse();
  }

  @Test
  public void wasAnsweredInProgram_returnsTrueIfQuestionsCompletedInGivenProgram() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());
    answerNameQuestion(applicantData, 22L);
    answerColorQuestion(applicantData, 22L);

    assertThat(block.wasAnsweredInProgram(22L)).isTrue();
  }

  @Test
  public void wasAnsweredInProgram_returnsTrueIfSomeQuestionsCompletedInGivenProgram() {
    ApplicantData applicantData = new ApplicantData();
    BlockDefinition definition = setUpBlockWithQuestions();

    Block block = new Block("1", definition, applicantData, Optional.empty());
    answerNameQuestion(applicantData, 100L);
    answerColorQuestion(applicantData, 200L);

    assertThat(block.wasAnsweredInProgram(200L)).isTrue();
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
                    testQuestionBank.applicantHouseholdMembers().getQuestionDefinition(),
                    Optional.empty()))
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
            .addQuestion(
                ProgramQuestionDefinition.create(enumeratorQuestionDefinition, Optional.empty()))
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
                    testQuestionBank.applicantFile().getQuestionDefinition(), Optional.empty()))
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

  @Test
  public void hasRequiredQuestionsThatAreSkippedInCurrentProgram_returnsTrue() {
    long programId = 5L;
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(20L)
            .setName("")
            .setDescription("")
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                    Optional.of(programId)))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                    Optional.of(programId)))
            .build();
    ApplicantData applicantData = new ApplicantData();
    Block block = new Block("id", blockDefinition, applicantData, Optional.empty());

    block.getQuestions().stream()
        .map(ApplicantQuestion::getContextualizedPath)
        .forEach(path -> QuestionAnswerer.addMetadata(applicantData, path, programId, 1L));

    assertThat(block.hasRequiredQuestionsThatAreSkippedInCurrentProgram()).isTrue();
  }

  @Test
  public void
      hasRequiredQuestionsThatAreSkippedInCurrentProgram_questionsAnsweredInAnotherProgram_returnsFalse() {
    long programId = 5L;
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(20L)
            .setName("")
            .setDescription("")
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                    Optional.of(programId)))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                    Optional.of(programId)))
            .build();
    ApplicantData applicantData = new ApplicantData();
    Block block = new Block("id", blockDefinition, applicantData, Optional.empty());

    block.getQuestions().stream()
        .map(ApplicantQuestion::getContextualizedPath)
        .forEach(path -> QuestionAnswerer.addMetadata(applicantData, path, programId + 1, 1L));

    assertThat(block.hasRequiredQuestionsThatAreSkippedInCurrentProgram()).isFalse();
  }

  @Test
  public void isCompleteInProgramWithoutErrors_withSkippedOptionalQuestions_isTrue() {
    ApplicantData applicantData = new ApplicantData();
    long programId = 5L;
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantJugglingNumber()
                .getQuestionDefinition()
                .getQuestionPathSegment());
    QuestionAnswerer.addMetadata(applicantData, questionPath, programId, 0L);
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                Optional.of(programId))
            .setOptional(true);
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("name")
            .setDescription("desc")
            .addQuestion(pqd)
            .build();

    Block block = new Block("id", blockDefinition, applicantData, Optional.empty());

    assertThat(block.isCompletedInProgramWithoutErrors()).isTrue();
  }

  @Test
  public void
      hasRequiredQuestionsThatAreSkippedInCurrentProgram_withOptionalQuestions_returnsFalse() {
    long programId = 5L;
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(20L)
            .setName("")
            .setDescription("")
            .addQuestion(
                ProgramQuestionDefinition.create(
                        testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                        Optional.of(programId))
                    .setOptional(true))
            .addQuestion(
                ProgramQuestionDefinition.create(
                        testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                        Optional.of(programId))
                    .setOptional(true))
            .build();
    ApplicantData applicantData = new ApplicantData();
    Block block = new Block("id", blockDefinition, applicantData, Optional.empty());

    assertThat(block.hasRequiredQuestionsThatAreSkippedInCurrentProgram()).isFalse();
  }

  @Test
  public void
      hasRequiredQuestionsThatAreSkippedInCurrentProgram_withAnsweredQuestions_returnsFalse() {
    long programId = 5L;
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(20L)
            .setName("")
            .setDescription("")
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                    Optional.of(programId)))
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                    Optional.of(programId)))
            .build();
    ApplicantData applicantData = new ApplicantData();
    Block block = new Block("id", blockDefinition, applicantData, Optional.empty());

    QuestionAnswerer.answerNumberQuestion(
        applicantData, block.getQuestions().get(0).getContextualizedPath(), "5");
    QuestionAnswerer.answerTextQuestion(
        applicantData, block.getQuestions().get(1).getContextualizedPath(), "brown");

    assertThat(block.hasRequiredQuestionsThatAreSkippedInCurrentProgram()).isFalse();
  }

  @Test
  public void
      isCompleteInProgramWithoutErrors_withSkippedOptionalQuestionsInWrongProgram_isFalse() {
    ApplicantData applicantData = new ApplicantData();
    long programId = 5L;
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantJugglingNumber()
                .getQuestionDefinition()
                .getQuestionPathSegment());
    QuestionAnswerer.addMetadata(applicantData, questionPath, programId + 1, 0L);
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                Optional.of(programId))
            .setOptional(true);
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("name")
            .setDescription("desc")
            .addQuestion(pqd)
            .build();

    Block block = new Block("id", blockDefinition, applicantData, Optional.empty());

    assertThat(block.isCompletedInProgramWithoutErrors()).isFalse();
  }

  @Test
  public void isCompleteInProgramWithoutErrors_withRequiredSkippedQuestions_isFalse() {
    ApplicantData applicantData = new ApplicantData();
    long programId = 5L;
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantJugglingNumber()
                .getQuestionDefinition()
                .getQuestionPathSegment());
    QuestionAnswerer.addMetadata(applicantData, questionPath, programId, 0L);
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
            testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
            Optional.of(programId));
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("name")
            .setDescription("desc")
            .addQuestion(pqd)
            .build();

    Block block = new Block("id", blockDefinition, applicantData, Optional.empty());

    assertThat(block.isCompletedInProgramWithoutErrors()).isFalse();
  }

  private static BlockDefinition setUpBlockWithQuestions() {
    return BlockDefinition.builder()
        .setId(20L)
        .setName("")
        .setDescription("")
        .addQuestion(ProgramQuestionDefinition.create(NAME_QUESTION, Optional.empty()))
        .addQuestion(ProgramQuestionDefinition.create(COLOR_QUESTION, Optional.empty()))
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
