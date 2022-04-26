package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import support.TestQuestionBank;

public class QuestionDefinitionBuilderTest {

  private static final TestQuestionBank QUESTION_BANK = new TestQuestionBank(false);

  @Test
  public void builder_addsNewTranslations() throws Exception {
    QuestionDefinition question = QUESTION_BANK.applicantName().getQuestionDefinition();

    QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder(question);
    builder.updateQuestionText(Locale.FRENCH, "french");
    builder.updateQuestionHelpText(Locale.FRENCH, "french help");
    question = builder.build();

    assertThat(question.getQuestionText().get(Locale.FRENCH)).isEqualTo("french");
    assertThat(question.getQuestionHelpText().get(Locale.FRENCH)).isEqualTo("french help");
  }

  @Test
  public void builder_overwritesExistingTranslation() throws Exception {
    QuestionDefinition question = QUESTION_BANK.applicantName().getQuestionDefinition();

    QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder(question);
    builder.updateQuestionText(Locale.US, "new text");
    builder.updateQuestionHelpText(Locale.US, "new help text");
    question = builder.build();

    assertThat(question.getQuestionText().get(Locale.US)).isEqualTo("new text");
    assertThat(question.getQuestionHelpText().get(Locale.US)).isEqualTo("new help text");
  }

  @Test
  public void builder_enumeratorWithNull_usesDefaultEntityType() throws Exception {
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.ENUMERATOR)
            .setName("")
            .setDescription("")
            .setEnumeratorId(Optional.of(123L))
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setEntityType(null);

    EnumeratorQuestionDefinition enumerator = (EnumeratorQuestionDefinition) builder.build();
    assertThat(enumerator.getEntityType().isEmpty()).isFalse();
    assertThat(enumerator.getEntityType().getDefault())
        .isEqualTo(EnumeratorQuestionDefinition.DEFAULT_ENTITY_TYPE);
  }

  @Test
  public void builder_emptyEntityType_usesDefaultEntityType() throws Exception {
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.ENUMERATOR)
            .setName("")
            .setDescription("")
            .setEnumeratorId(Optional.of(123L))
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setEntityType(LocalizedStrings.empty());

    EnumeratorQuestionDefinition enumerator = (EnumeratorQuestionDefinition) builder.build();
    assertThat(enumerator.getEntityType().isEmpty()).isFalse();
    assertThat(enumerator.getEntityType().getDefault())
        .isEqualTo(EnumeratorQuestionDefinition.DEFAULT_ENTITY_TYPE);
  }

  @Test
  public void builder_withEnumeratorQuestion_keepsEntityType() throws Exception {
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.ENUMERATOR)
            .setName("")
            .setDescription("")
            .setEnumeratorId(Optional.of(123L))
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setEntityType(LocalizedStrings.withDefaultValue("household member"))
            .build();

    EnumeratorQuestionDefinition enumerator =
        (EnumeratorQuestionDefinition) new QuestionDefinitionBuilder(questionDefinition).build();

    assertThat(enumerator.getEntityType().isEmpty()).isFalse();
    assertThat(enumerator.getEntityType().getDefault()).isEqualTo("household member");
  }
}
