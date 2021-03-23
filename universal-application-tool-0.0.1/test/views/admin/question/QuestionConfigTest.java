package views.admin.question;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.ContainerTag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import services.question.QuestionType;
import views.admin.questions.QuestionConfig;
import views.components.Icons;

public class QuestionConfigTest {

  private static final ContainerTag DEFAULT_CONFIG = div();

  @ParameterizedTest
  @EnumSource(
      value = QuestionType.class,
      names = {"REPEATER", "NAME"},
      mode = EnumSource.Mode.EXCLUDE)
  public void allHandledTypesHaveCustomConfig(QuestionType type) {
    assertThat(QuestionConfig.buildQuestionConfig(type)).isNotEqualTo(DEFAULT_CONFIG);
  }

  @ParameterizedTest
  @EnumSource(
      value = QuestionType.class,
      names = {"REPEATER", "NAME"})
  public void unhandledQuestionTypesDefaultToDefaultConfig(QuestionType type) {
    assertThat(Icons.questionTypeSvg(type, 0)).isEqualTo(DEFAULT_CONFIG);
  }
}
