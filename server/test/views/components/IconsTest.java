package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.question.types.QuestionType;
import views.style.BaseStyles;

@RunWith(JUnitParamsRunner.class)
public class IconsTest {

  private static final SvgTag TEXT_ICON = Icons.questionTypeSvg(QuestionType.TEXT);
  private static final SvgTag TEXT_ICON_WITH_ID = Icons.questionTypeSvgWithId(QuestionType.TEXT);
  // TODO(https://github.com/seattle-uat/civiform/issues/395): Implement dropdown rendering.
  private static final EnumSet<QuestionType> TYPES_WITH_DEFAULT_ICON =
      EnumSet.of(QuestionType.TEXT);

  @Test
  @Parameters(method = "handledTypes")
  public void allHandledTypesHaveCustomIcons(QuestionType type) {
    assertThat(Icons.questionTypeSvg(type)).isNotEqualTo(TEXT_ICON);
  }

  @Test
  @Parameters(method = "handledTypes")
  public void allHandledTypesHaveCustomIconsWithId(QuestionType type) {
    assertThat(Icons.questionTypeSvgWithId(type)).isNotEqualTo(TEXT_ICON_WITH_ID);
  }

  private EnumSet<QuestionType> handledTypes() {
    return EnumSet.complementOf(TYPES_WITH_DEFAULT_ICON);
  }

  @Test
  @Parameters(method = "defaultTypes")
  public void unhandledQuestionTypesDefaultToTextIcon(QuestionType type) {
    assertThat(Icons.questionTypeSvg(type)).isEqualTo(TEXT_ICON);
  }

  @Test
  @Parameters(method = "defaultTypes")
  public void unhandledQuestionTypesDefaultToTextIconWithId(QuestionType type) {
    assertThat(Icons.questionTypeSvgWithId(type)).isEqualTo(TEXT_ICON_WITH_ID);
  }

  private EnumSet<QuestionType> defaultTypes() {
    return TYPES_WITH_DEFAULT_ICON;
  }

  @Test
  public void setColor_overridesDefaultColor() {
    SvgTag svgWithColor =
        Icons.setColor(Icons.svg(Icons.MARKDOWN), BaseStyles.FORM_LABEL_TEXT_COLOR);
    assertThat(svgWithColor.render()).contains(BaseStyles.FORM_LABEL_TEXT_COLOR);
    assertThat(svgWithColor.render()).doesNotContain("currentColor");
  }
}
