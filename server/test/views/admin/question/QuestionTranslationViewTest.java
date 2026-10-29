package views.admin.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.TranslationLocales;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.admin.AdminLayoutFactory;
import views.admin.questions.QuestionTranslationView;

public class QuestionTranslationViewTest extends ResetPostgres {

  private QuestionTranslationView questionTranslationView;

  @Before
  public void setup() {
    questionTranslationView = instanceOf(QuestionTranslationView.class);
  }

  @Test
  public void render_yesNoQuestion_showsPreTranslatedNoticeAndNoInputs() throws Exception {
    QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder();
    builder.setName("yes-no-q");
    builder.setDescription("desc");
    builder.setQuestionText(LocalizedStrings.withDefaultValue("help"));
    builder.setQuestionHelpText(LocalizedStrings.withDefaultValue("admin-description"));
    builder.setQuestionType(QuestionType.YES_NO);
    builder.setId(1L);
    var questionDefinition = builder.build();

    Http.Request request = fakeRequestBuilder().build();

    Locale locale = Locale.ENGLISH;

    Content content = questionTranslationView.render(request, locale, questionDefinition);
    String renderedHtml = content.body();

    assertThat(renderedHtml).contains("Yes/No question options are pre-translated.");

    assertThat(renderedHtml).doesNotContain("name=\"yes\"");
    assertThat(renderedHtml).doesNotContain("name=\"no\"");
    assertThat(renderedHtml).doesNotContain("name=\"maybe\"");
    assertThat(renderedHtml).doesNotContain("name=\"not-sure\"");

    assertThat(renderedHtml).contains("<textarea");
    assertThat(renderedHtml).contains("name=\"questionText\"");
    assertThat(renderedHtml).contains("name=\"questionHelpText\"");
  }

  @Test
  public void render_questionWithImage_featureEnabled_showsImageDescriptionInput() throws Exception {
    SettingsManifest settingsManifest = mock(SettingsManifest.class);
    when(settingsManifest.getImagesInQuestionFeatureEnabled(any())).thenReturn(true);
    QuestionTranslationView view =
        new QuestionTranslationView(
            instanceOf(AdminLayoutFactory.class),
            instanceOf(TranslationLocales.class),
            settingsManifest);

    QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder();
    builder.setName("image-q");
    builder.setDescription("desc");
    builder.setQuestionText(LocalizedStrings.withDefaultValue("help"));
    builder.setQuestionHelpText(LocalizedStrings.withDefaultValue("admin-description"));
    builder.setQuestionType(QuestionType.STATIC);
    builder.setImageFileKey("test-image-key");
    builder.setLocalizedImageDescription(LocalizedStrings.withDefaultValue("default alt text"));
    builder.setId(1L);
    var questionDefinition = builder.build();

    Http.Request request = fakeRequestBuilder().build();
    Content content = view.render(request, Locale.forLanguageTag("es-US"), questionDefinition);
    String renderedHtml = content.body();

    assertThat(renderedHtml).contains("name=\"imageDescription\"");
    assertThat(renderedHtml).contains("Question image description");
  }

  @Test
  public void render_questionWithoutImage_doesNotShowImageDescriptionInput() throws Exception {
    QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder();
    builder.setName("no-image-q");
    builder.setDescription("desc");
    builder.setQuestionText(LocalizedStrings.withDefaultValue("help"));
    builder.setQuestionHelpText(LocalizedStrings.withDefaultValue("admin-description"));
    builder.setQuestionType(QuestionType.STATIC);
    builder.setId(1L);
    var questionDefinition = builder.build();

    Http.Request request = fakeRequestBuilder().build();
    Content content =
        questionTranslationView.render(request, Locale.forLanguageTag("es-US"), questionDefinition);
    String renderedHtml = content.body();

    assertThat(renderedHtml).doesNotContain("name=\"imageDescription\"");
  }
}
