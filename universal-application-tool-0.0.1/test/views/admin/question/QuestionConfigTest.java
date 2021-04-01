package views.admin.question;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.ContainerTag;
import java.util.EnumSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.question.QuestionType;
import views.admin.questions.QuestionConfig;

@RunWith(JUnitParamsRunner.class)
public class QuestionConfigTest {

  private static final ContainerTag DEFAULT_CONFIG = div();
  // TODO(https://github.com/seattle-uat/civiform/issues/395): Implement dropdown rendering.
  private static final EnumSet<QuestionType> TYPES_WITH_NO_CONFIG =
      EnumSet.of(QuestionType.DROPDOWN, QuestionType.NAME, QuestionType.REPEATER);

  @Test
  @Parameters(method = "handledTypes")
  public void allHandledTypesHaveCustomConfig(QuestionType type) {
    assertThat(QuestionConfig.buildQuestionConfig(type)).isNotEqualTo(DEFAULT_CONFIG);
  }

  private EnumSet<QuestionType> handledTypes() {
    return EnumSet.complementOf(TYPES_WITH_NO_CONFIG);
  }

  @Test
  @Parameters(method = "defaultTypes")
  public void unhandledQuestionTypesDefaultToDefaultConfig(QuestionType type) {
    assertThat(QuestionConfig.buildQuestionConfig(type)).isEqualTo(DEFAULT_CONFIG);
  }

  private EnumSet<QuestionType> defaultTypes() {
    return TYPES_WITH_NO_CONFIG;
  }
}
