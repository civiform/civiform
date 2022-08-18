package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.question.types.QuestionType;

@RunWith(JUnitParamsRunner.class)
public class IconsTest {

  private static final SvgTag TEXT_ICON = Icons.questionTypeSvg(QuestionType.TEXT, 24);
  // TODO(https://github.com/seattle-uat/civiform/issues/395): Implement dropdown rendering.
  private static final EnumSet<QuestionType> TYPES_WITH_DEFAULT_ICON =
      EnumSet.of(QuestionType.TEXT);

  @Test
  @Parameters(method = "handledTypes")
  public void allHandledTypesHaveCustomIcons(QuestionType type) {
    assertThat(Icons.questionTypeSvg(type, 24)).isNotEqualTo(TEXT_ICON);
  }

  private EnumSet<QuestionType> handledTypes() {
    return EnumSet.complementOf(TYPES_WITH_DEFAULT_ICON);
  }

  @Test
  @Parameters(method = "defaultTypes")
  public void unhandledQuestionTypesDefaultToTextIcon(QuestionType type) {
    assertThat(Icons.questionTypeSvg(type, 24)).isEqualTo(TEXT_ICON);
  }

  private EnumSet<QuestionType> defaultTypes() {
    return TYPES_WITH_DEFAULT_ICON;
  }
}
