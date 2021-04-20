package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.Tag;
import java.util.EnumSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionType;

@RunWith(JUnitParamsRunner.class)
public class ApplicantQuestionRendererFactoryTest {

  // TODO(https://github.com/seattle-uat/civiform/issues/405): Change this to just use
  // @Parameters(source = QuestionType.class) once RepeatedQuestionDefinition exists.
  @Test
  @Parameters(method = "types")
  public void rendererExistsForAllTypes(QuestionType type) throws UnsupportedQuestionTypeException {
    ApplicantQuestionRendererFactory factory = new ApplicantQuestionRendererFactory();

    ApplicantQuestionRenderer renderer = factory.getSampleRenderer(type);

    assertThat(renderer.render()).isInstanceOf(Tag.class);
  }

  private EnumSet<QuestionType> types() {
    return EnumSet.complementOf(EnumSet.of(QuestionType.REPEATER));
  }
}
