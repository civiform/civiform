package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.ContainerTag;
import java.util.EnumSet;
import junitparams.Parameters;
import org.junit.Test;
import services.question.QuestionType;

public class IconsTest {

  private static final ContainerTag TEXT_ICON = Icons.questionTypeSvg(QuestionType.TEXT, 0);
  private static final EnumSet<QuestionType> TYPES_WITH_DEFAULT_ICON =
      EnumSet.of(QuestionType.TEXT);

  @Test
  @Parameters(method = "handledTypes")
  public void allHandledTypesHaveCustomIcons(QuestionType type) {
    assertThat(Icons.questionTypeSvg(type, 0)).isNotEqualTo(TEXT_ICON);
  }

  private EnumSet<QuestionType> handledTypes() {
    return EnumSet.complementOf(TYPES_WITH_DEFAULT_ICON);
  }

  @Test
  @Parameters(method = "defaultTypes")
  public void unhandledQuestionTypesDefaultToTextIcon(QuestionType type) {
    assertThat(Icons.questionTypeSvg(type, 0)).isEqualTo(TEXT_ICON);
  }

  private EnumSet<QuestionType> defaultTypes() {
    return TYPES_WITH_DEFAULT_ICON;
  }
}
