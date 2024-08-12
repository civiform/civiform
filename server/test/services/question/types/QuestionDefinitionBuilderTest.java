package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import support.TestQuestionBank;

public class QuestionDefinitionBuilderTest {

  private static final TestQuestionBank QUESTION_BANK = new TestQuestionBank(false);
  private QuestionDefinitionBuilder builder;
  private QuestionDefinitionBuilder applicantNameBuilder;

  @Before
  public void setup() {
    builder =
        new QuestionDefinitionBuilder()
            .setName("")
            .setDescription("")
            .setQuestionType(QuestionType.TEXT)
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty());
    applicantNameBuilder =
        new QuestionDefinitionBuilder(QUESTION_BANK.nameApplicantName().getQuestionDefinition());
  }

  @Test
  public void builder_addsNewTranslations() throws Exception {
    applicantNameBuilder.updateQuestionText(Locale.FRENCH, "french");
    applicantNameBuilder.updateQuestionHelpText(Locale.FRENCH, "french help");
    QuestionDefinition questionDefinition = applicantNameBuilder.build();

    assertThat(questionDefinition.getQuestionText().get(Locale.FRENCH)).isEqualTo("french");
    assertThat(questionDefinition.getQuestionHelpText().get(Locale.FRENCH))
        .isEqualTo("french help");
  }

  @Test
  public void builder_overwritesExistingTranslation() throws Exception {
    applicantNameBuilder.updateQuestionText(Locale.US, "new text");
    applicantNameBuilder.updateQuestionHelpText(Locale.US, "new help text");
    QuestionDefinition questionDefinition = applicantNameBuilder.build();

    assertThat(questionDefinition.getQuestionText().get(Locale.US)).isEqualTo("new text");
    assertThat(questionDefinition.getQuestionHelpText().get(Locale.US)).isEqualTo("new help text");
  }

  @Test
  public void builder_enumeratorWithNull_usesDefaultEntityType() throws Exception {
    builder
        .setQuestionType(QuestionType.ENUMERATOR)
        .setEnumeratorId(Optional.of(123L))
        .setEntityType(null);

    EnumeratorQuestionDefinition enumerator = (EnumeratorQuestionDefinition) builder.build();
    assertThat(enumerator.getEntityType().isEmpty()).isFalse();
    assertThat(enumerator.getEntityType().getDefault())
        .isEqualTo(EnumeratorQuestionDefinition.DEFAULT_ENTITY_TYPE);
  }

  @Test
  public void builder_emptyEntityType_usesDefaultEntityType() throws Exception {
    builder
        .setQuestionType(QuestionType.ENUMERATOR)
        .setEnumeratorId(Optional.of(123L))
        .setEntityType(LocalizedStrings.empty());

    EnumeratorQuestionDefinition enumerator = (EnumeratorQuestionDefinition) builder.build();
    assertThat(enumerator.getEntityType().isEmpty()).isFalse();
    assertThat(enumerator.getEntityType().getDefault())
        .isEqualTo(EnumeratorQuestionDefinition.DEFAULT_ENTITY_TYPE);
  }

  @Test
  public void builder_withEnumeratorQuestion_keepsEntityType() throws Exception {
    QuestionDefinition questionDefinition =
        builder
            .setQuestionType(QuestionType.ENUMERATOR)
            .setEnumeratorId(Optional.of(123L))
            .setEntityType(LocalizedStrings.withDefaultValue("household member"))
            .build();

    EnumeratorQuestionDefinition enumerator =
        (EnumeratorQuestionDefinition) new QuestionDefinitionBuilder(questionDefinition).build();

    assertThat(enumerator.getEntityType().isEmpty()).isFalse();
    assertThat(enumerator.getEntityType().getDefault()).isEqualTo("household member");
  }

  @Test
  public void getLastModifiedTimeWhenExists() throws Exception {
    Instant now = Instant.now();
    QuestionDefinition questionDefinition = builder.setLastModifiedTime(Optional.of(now)).build();
    assertThat(questionDefinition.getLastModifiedTime()).isEqualTo(Optional.of(now));
  }

  @Test
  public void getLastModifiedTimeWhenDoesNotExist() throws Exception {
    QuestionDefinition questionDefinition = builder.build();
    assertThat(questionDefinition.getLastModifiedTime()).isEmpty();
  }

  @Test
  public void setUniversal_setsTheUniversalField() throws Exception {
    QuestionDefinition questionDefinition = builder.setUniversal(true).build();
    assertThat(questionDefinition.isUniversal()).isTrue();
  }
}
