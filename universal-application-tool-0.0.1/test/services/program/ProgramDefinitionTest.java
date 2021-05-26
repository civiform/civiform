package services.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class ProgramDefinitionTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Test
  public void createProgramDefinition() {
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();
    ProgramDefinition.builder()
        .setId(123L)
        .setAdminName("Admin name")
        .setAdminDescription("Admin description")
        .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
        .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
        .addBlockDefinition(blockA)
        .build();
  }

  @Test
  public void getBlockDefinition_hasValue() {
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
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
            .build();

    assertThat(program.getBlockDefinitionByIndex(0)).isEmpty();
  }

  @Test
  public void hasQuestion_trueIfTheProgramUsesTheQuestion() {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB = testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();

    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionA))
            .build();

    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(321L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionB))
            .build();

    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "The Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "This program is for testing."))
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
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }

  @Test
  public void getSupportedLocales_returnsLocalesSupportedByQuestionsAndText() {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB = testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();

    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionA))
            .build();
    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionB))
            .addQuestion(ProgramQuestionDefinition.create(questionC))
            .build();
    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(
                    Locale.US, "English description", Locale.GERMAN, "test", Locale.FRANCE, "test"))
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
    QuestionDefinition questionC =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    QuestionDefinition questionD = testQuestionBank.applicantSeason().getQuestionDefinition();

    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionA))
            .build();
    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(2L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionB))
            .addQuestion(ProgramQuestionDefinition.create(questionC))
            .build();
    BlockDefinition blockC =
        BlockDefinition.builder()
            .setId(3L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionD))
            .build();
    ProgramDefinition programDefinition =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(
                    Locale.US, "English description", Locale.GERMAN, "test", Locale.FRANCE, "test"))
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .addBlockDefinition(blockC)
            .build();

    // blockA
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(1L)).isEmpty();
    // blockB
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(2L))
        .containsExactly(questionA);
    // blockC
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(3L))
        .containsExactly(questionA, questionB, questionC);
  }

  @Test
  public void
      getAvailablePredicateQuestionDefinitions_withRepeatedBlocks_onlyIncludesQuestionsWithSameEnumeratorId()
          throws ProgramBlockDefinitionNotFoundException {
    QuestionDefinition questionA = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition questionC =
        testQuestionBank.applicantHouseholdMemberName().getQuestionDefinition();
    QuestionDefinition questionD =
        testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition();
    QuestionDefinition questionE =
        testQuestionBank.applicantHouseholdMemberJobIncome().getQuestionDefinition();

    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionA))
            .build();
    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(2L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionB))
            .build();
    BlockDefinition blockC =
        BlockDefinition.builder()
            .setId(3L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionC))
            .build();
    BlockDefinition blockD =
        BlockDefinition.builder()
            .setId(4L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionD))
            .build();
    BlockDefinition blockE =
        BlockDefinition.builder()
            .setId(5L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionE))
            .build();
    ProgramDefinition programDefinition =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(
                LocalizedStrings.of(Locale.US, "Applicant friendly name", Locale.FRANCE, "test"))
            .setLocalizedDescription(
                LocalizedStrings.of(
                    Locale.US, "English description", Locale.GERMAN, "test", Locale.FRANCE, "test"))
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .addBlockDefinition(blockC)
            .addBlockDefinition(blockD)
            .addBlockDefinition(blockE)
            .build();

    // blockA (applicantName)
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(1L)).isEmpty();
    // blockB (applicantHouseholdMembers)
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(2L))
        .containsExactly(questionA);
    // blockC (applicantHouseholdMembers.householdMemberName)
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(3L)).isEmpty();
    // blockD (applicantHouseholdMembers.householdMemberJobs)
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(4L))
        .containsExactly(questionC);
    // blockE (applicantHouseholdMembers.householdMemberJobs.householdMemberJobIncome)
    assertThat(programDefinition.getAvailablePredicateQuestionDefinitions(5L)).isEmpty();
  }
}
