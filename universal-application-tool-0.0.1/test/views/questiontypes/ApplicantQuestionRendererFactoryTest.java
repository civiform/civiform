package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import services.question.QuestionType;
import services.question.UnsupportedQuestionTypeException;

public class ApplicantQuestionRendererFactoryTest {

  @ParameterizedTest
  @EnumSource(QuestionType.class)
  public void rendererExistsForAllTypes(QuestionType type) throws UnsupportedQuestionTypeException {
    ApplicantQuestionRendererFactory factory = new ApplicantQuestionRendererFactory();

    ApplicantQuestionRenderer renderer = factory.getSampleRenderer(type);

    assertThat(renderer.render()).isInstanceOf(Tag.class);
  }
}
