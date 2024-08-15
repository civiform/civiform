package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.Test;
import services.CiviFormError;
import services.LocalizedStrings;

public class EnumeratorQuestionDefinitionTest {
  @Test
  public void validate_enumeratorQuestion_withEmptyEntityString_returnsErrors() throws Exception {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .build();
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(config, LocalizedStrings.withDefaultValue(""));

    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Enumerator question must have specified entity type"));
  }
}
