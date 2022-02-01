package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.QuestionRepository;
import repository.VersionRepository;
import repository.WithPostgresContainer;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class AdminQuestionTranslationsControllerTest extends WithPostgresContainer {

  private Version draftVersion;
  private TestQuestionBank questionBank;
  private QuestionRepository questionRepository;
  private AdminQuestionTranslationsController controller;

  @Before
  public void setup() {
    questionBank = new TestQuestionBank(true);
    questionRepository = instanceOf(QuestionRepository.class);
    controller = instanceOf(AdminQuestionTranslationsController.class);
    // Create a new draft version.
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    draftVersion = versionRepository.getDraftVersion();
  }

  @Test
  public void edit_rendersForm() {
    Question question = questionBank.applicantName();

    Result result =
        controller
            .edit(addCSRFToken(fakeRequest()).build(), question.id, "en-US")
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("Manage Question Translations", "English", "Spanish", "what is your name?");
  }

  @Test
  public void edit_questionNotFound_returnsNotFound() {
    Result result =
        controller
            .edit(addCSRFToken(fakeRequest()).build(), 1000L, "en-US")
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_addsNewLocalesAndRedirects() throws TranslationNotFoundException {
    Question question = createDraftQuestion();
    Http.RequestBuilder requestBuilder =
        fakeRequest()
            .bodyForm(ImmutableMap.of("questionText", "french", "questionHelpText", "french help"));

    Result result =
        controller
            .update(addCSRFToken(requestBuilder).build(), question.id, "fr")
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    QuestionDefinition updatedQuestion =
        questionRepository
            .lookupQuestion(question.id)
            .toCompletableFuture()
            .join()
            .get()
            .getQuestionDefinition();
    assertThat(updatedQuestion.getQuestionText().get(Locale.FRENCH)).isEqualTo("french");
    assertThat(updatedQuestion.getQuestionHelpText().get(Locale.FRENCH)).isEqualTo("french help");
  }

  @Test
  public void update_updatesExistingLocalesAndRedirects() throws TranslationNotFoundException {
    Question question = createDraftQuestion();
    Http.RequestBuilder requestBuilder =
        fakeRequest()
            .bodyForm(ImmutableMap.of("questionText", "new", "questionHelpText", "new help"));

    Result result =
        controller
            .update(addCSRFToken(requestBuilder).build(), question.id, "en-US")
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    QuestionDefinition updatedQuestion =
        questionRepository
            .lookupQuestion(question.id)
            .toCompletableFuture()
            .join()
            .get()
            .getQuestionDefinition();
    assertThat(updatedQuestion.getQuestionText().get(Locale.US)).isEqualTo("new");
    assertThat(updatedQuestion.getQuestionHelpText().get(Locale.US)).isEqualTo("new help");
  }

  @Test
  public void update_questionNotFound_returnsNotFound() {
    Result result =
        controller
            .update(addCSRFToken(fakeRequest()).build(), 1000L, "en-US")
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_validationErrors_rendersEditFormWithMessage() {
    Question question = createDraftQuestion();
    Http.RequestBuilder requestBuilder =
        fakeRequest().bodyForm(ImmutableMap.of("questionText", "", "questionHelpText", ""));

    Result result =
        controller
            .update(addCSRFToken(requestBuilder).build(), question.id, "en-US")
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("Manage Question Translations", "Question text cannot be blank");
  }

  /** Creates a draft question, since only draft questions are editable. */
  private Question createDraftQuestion() {
    QuestionDefinition definition =
        new NameQuestionDefinition(
            "applicant name",
            Optional.empty(),
            "name of applicant",
            LocalizedStrings.of(Locale.US, "what is your name?"),
            LocalizedStrings.of(Locale.US, "help text"));
    Question question = new Question(definition);
    question.addVersion(draftVersion);
    question.save();
    return question;
  }
}
