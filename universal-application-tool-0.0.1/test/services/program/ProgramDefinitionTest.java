package services.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Test;
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
        .addLocalizedName(Locale.US, "The Program")
        .addLocalizedDescription(Locale.US, "This program is for testing.")
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
            .addLocalizedName(Locale.US, "The Program")
            .addLocalizedDescription(Locale.US, "This program is for testing.")
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
            .addLocalizedName(Locale.US, "The Program")
            .addLocalizedDescription(Locale.US, "This program is for testing.")
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
            .addLocalizedName(Locale.US, "The Program")
            .addLocalizedDescription(Locale.US, "This program is for testing.")
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
            .addLocalizedName(Locale.US, "Applicant friendly name")
            .addLocalizedDescription(Locale.US, "English description")
            .build();

    assertThat(program.adminName()).isEqualTo("Admin name");
    assertThat(program.localizedName())
        .isEqualTo(ImmutableMap.of(Locale.US, "Applicant friendly name"));
    assertThat(program.localizedDescription())
        .isEqualTo(ImmutableMap.of(Locale.US, "English description"));

    assertThatThrownBy(() -> program.getLocalizedName(Locale.FRANCE))
        .isInstanceOf(TranslationNotFoundException.class);
    assertThatThrownBy(() -> program.getLocalizedDescription(Locale.FRANCE))
        .isInstanceOf(TranslationNotFoundException.class);
    assertThat(program.getLocalizedNameOrDefault(Locale.FRANCE))
        .isEqualTo("Applicant friendly name");
    assertThat(program.getLocalizedDescriptionOrDefault(Locale.FRANCE))
        .isEqualTo("English description");
  }

  @Test
  public void localizedNameAndDescription_cannotAddSameLocaleTwice() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .addLocalizedName(Locale.US, "Applicant friendly name")
            .addLocalizedDescription(Locale.US, "English description")
            .build();

    assertThatThrownBy(
            () -> program.toBuilder().addLocalizedName(Locale.US, "this already exists").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Multiple entries with same key");
    assertThatThrownBy(
            () ->
                program.toBuilder()
                    .addLocalizedDescription(Locale.US, "this already exists")
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Multiple entries with same key");
  }

  @Test
  public void updateNameAndDescription_replacesExistingValue() throws TranslationNotFoundException {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .addLocalizedName(Locale.US, "existing name")
            .addLocalizedDescription(Locale.US, "existing description")
            .build();

    program =
        program.toBuilder()
            .updateLocalizedName(program.localizedName(), Locale.US, "new name")
            .build();
    assertThat(program.getLocalizedName(Locale.US)).isEqualTo("new name");

    program =
        program.toBuilder()
            .updateLocalizedDescription(
                program.localizedDescription(), Locale.US, "new description")
            .build();
    assertThat(program.getLocalizedDescription(Locale.US)).isEqualTo("new description");
  }

  @Test
  public void getSupportedLocales_noQuestions_returnsOnlyLocalesSupportedByDisplayText() {
    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(123L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .addLocalizedName(Locale.US, "Applicant friendly name")
            .addLocalizedName(Locale.FRANCE, "test")
            .addLocalizedDescription(Locale.US, "English description")
            .addLocalizedDescription(Locale.GERMAN, "test")
            .setLifecycleStage(LifecycleStage.ACTIVE)
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
            .addLocalizedName(Locale.US, "Applicant friendly name")
            .addLocalizedName(Locale.FRANCE, "test")
            .addLocalizedDescription(Locale.US, "English description")
            .addLocalizedDescription(Locale.GERMAN, "test")
            .addLocalizedDescription(Locale.FRANCE, "test")
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }
}
