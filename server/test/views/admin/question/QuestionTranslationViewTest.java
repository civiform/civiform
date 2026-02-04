package views.admin.question;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
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
}
