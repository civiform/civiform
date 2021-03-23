package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.ContainerTag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import services.question.QuestionType;

public class IconsTest {

  private static final ContainerTag TEXT_ICON = Icons.questionTypeSvg(QuestionType.TEXT, 0);

  @ParameterizedTest
  @EnumSource(
      value = QuestionType.class,
      names = {"TEXT"},
      mode = EnumSource.Mode.EXCLUDE)
  public void allHandledTypesHaveCustomIcons(QuestionType type) {
    assertThat(Icons.questionTypeSvg(type, 0)).isNotEqualTo(TEXT_ICON);
  }

  @ParameterizedTest
  @EnumSource(
      value = QuestionType.class,
      names = {"TEXT"})
  public void unhandledQuestionTypesDefaultToTextIcon(QuestionType type) {
    assertThat(Icons.questionTypeSvg(type, 0)).isEqualTo(TEXT_ICON);
  }
}
