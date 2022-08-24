package views.questiontypes;

import static j2html.TagCreator.document;
import static j2html.TagCreator.html;
import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionType;
import views.AwsFileUploadViewStrategy;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

@RunWith(JUnitParamsRunner.class)
public class ApplicantQuestionRendererFactoryTest {

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
  private final ApplicantQuestionRendererParams params =
      ApplicantQuestionRendererParams.builder()
          .setMessages(messages)
          .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
          .build();

  @Test
  @Parameters(source = QuestionType.class)
  public void rendererExistsForAllTypes(QuestionType type) throws UnsupportedQuestionTypeException {
    ApplicantQuestionRendererFactory factory =
        new ApplicantQuestionRendererFactory(new AwsFileUploadViewStrategy());

    ApplicantQuestionRenderer sampleRenderer = factory.getSampleRenderer(type);

    DivTag content = sampleRenderer.render(params);
    String renderedContent = document(html(content));
    assertThat(renderedContent).contains("Sample question text");
    assertThat(renderedContent).doesNotContain("help text");
  }
}
