package views.questiontypes;

import static j2html.TagCreator.document;
import static j2html.TagCreator.html;
import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.ConfigFactory;
import controllers.applicant.ApplicantRoutes;
import j2html.tags.specialized.DivTag;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import support.cloud.FakeApplicantStorageClient;
import views.applicant.ApplicantFileUploadRenderer;
import views.fileupload.GenericS3FileUploadViewStrategy;
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
    // A null question type is not allowed to be created and won't show in the list
    if (type == QuestionType.NULL_QUESTION) {
      return;
    }

    var applicantRoutes = new ApplicantRoutes();

    ApplicantQuestionRendererFactory factory =
        new ApplicantQuestionRendererFactory(
            new ApplicantFileUploadRenderer(
                new GenericS3FileUploadViewStrategy(),
                applicantRoutes,
                new FakeApplicantStorageClient(),
                new SettingsManifest(ConfigFactory.parseMap(ImmutableMap.of()))));

    ApplicantQuestionRenderer sampleRenderer = factory.getSampleRenderer(type);

    DivTag content = sampleRenderer.render(params);
    String renderedContent = document(html(content));
    assertThat(renderedContent).contains("Sample question text");
    assertThat(renderedContent).doesNotContain("help text");
  }

  @Test
  @Parameters(source = QuestionType.class)
  public void compositeQuestionsUseFieldset(QuestionType type)
      throws UnsupportedQuestionTypeException {
    // A null question type is not allowed to be created and won't show in the list
    if (type == QuestionType.NULL_QUESTION) {
      return;
    }

    var applicantRoutes = new ApplicantRoutes();

    // Multi-input questions should be wrapped in fieldsets for screen reader users.
    ApplicantQuestionRendererFactory factory =
        new ApplicantQuestionRendererFactory(
            new ApplicantFileUploadRenderer(
                new GenericS3FileUploadViewStrategy(),
                applicantRoutes,
                new FakeApplicantStorageClient(),
                new SettingsManifest(ConfigFactory.parseMap(ImmutableMap.of()))));

    ApplicantQuestionRenderer sampleRenderer = factory.getSampleRenderer(type);

    DivTag content = sampleRenderer.render(params);
    String renderedContent = document(html(content));
    switch (type) {
      case ADDRESS:
      case CHECKBOX:
      case ENUMERATOR:
      case NAME:
      case RADIO_BUTTON:
        assertThat(renderedContent).contains("fieldset");
        break;
      case CURRENCY:
      case DATE:
      case DROPDOWN:
      case EMAIL:
      case FILEUPLOAD:
      case ID:
      case NUMBER:
      case PHONE:
      case STATIC:
      case TEXT:
        assertThat(renderedContent).doesNotContain("fieldset");
        break;
        // This is here because errorprone doesn't like that it was missing
      case NULL_QUESTION:
        break;
    }
  }
}
