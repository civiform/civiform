package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.Tag;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.question.QuestionType;
import services.question.UnsupportedQuestionTypeException;

@RunWith(JUnitParamsRunner.class)
public class ApplicantQuestionRendererFactoryTest {

  @Test
  @Parameters(source = QuestionType.class)
  public void rendererExistsForAllTypes(QuestionType type) throws UnsupportedQuestionTypeException {
    ApplicantQuestionRendererFactory factory = new ApplicantQuestionRendererFactory();

    ApplicantQuestionRenderer renderer = factory.getSampleRenderer(type);

    assertThat(renderer.render()).isInstanceOf(Tag.class);
  }
}
