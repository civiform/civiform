package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import controllers.BadRequestException;
import java.util.Locale;
import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.QuestionRepository;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

public class AdminQuestionTranslationsControllerTest extends ResetPostgres {

  private static final Locale ES_LOCALE = Locale.forLanguageTag("es-US");

  private static final String ENGLISH_QUESTION_TEXT = "english question text";
  private static final String ENGLISH_QUESTION_HELP_TEXT = "english question help text";
  private static final String SPANISH_QUESTION_TEXT = "spanish question text";
  private static final String SPANISH_QUESTION_HELP_TEXT = "spanish question help text";

  private Version draftVersion;
  private QuestionRepository questionRepository;
  private AdminQuestionTranslationsController controller;

  @Before
  public void setup() {
    questionRepository = instanceOf(QuestionRepository.class);
    controller = instanceOf(AdminQuestionTranslationsController.class);
    // Create a new draft version.
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    draftVersion = versionRepository.getDraftVersionOrCreate();
  }

  @Test
  public void edit_defaultLocaleRedirectsWithError() {
    Question question = createDraftQuestionEnglishAndSpanish();

    Result result =
        controller.edit(
            addCSRFToken(fakeRequest()).build(),
            question.getQuestionDefinition().getName(),
            "en-US");

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminQuestionController.index().url());
    assertThat(result.flash().get("error")).isPresent();
    assertThat(result.flash().get("error").get()).isEqualTo("The en-US locale is not supported");
  }

  @Test
  public void edit_rendersForm_otherLocale() throws UnsupportedQuestionTypeException {
    Question question = createDraftQuestionEnglishAndSpanish();

    Result result =
        controller.edit(
            addCSRFToken(fakeRequest()).build(),
            question.getQuestionDefinition().getName(),
            "es-US");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(
            String.format(
                "Manage Question Translations: %s", question.getQuestionDefinition().getName()),
            "Spanish",
            SPANISH_QUESTION_TEXT,
            SPANISH_QUESTION_HELP_TEXT);
    assertThat(contentAsString(result))
        .contains("English text:", ENGLISH_QUESTION_TEXT, ENGLISH_QUESTION_HELP_TEXT);
  }

  @Test
  public void edit_questionNotFound_returnsNotFound() {
    assertThatThrownBy(
            () ->
                controller.edit(
                    addCSRFToken(fakeRequest()).build(), "non-existent question name", "es-US"))
        .hasMessage("No draft found for question: \"non-existent question name\"")
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void update_addsNewLocalesAndRedirects() throws TranslationNotFoundException {
    Question question = createDraftQuestionEnglishOnly();
    Http.RequestBuilder requestBuilder =
        fakeRequest()
            .bodyForm(
                ImmutableMap.of(
                    "questionText",
                    "updated spanish question text",
                    "questionHelpText",
                    "updated spanish help text"));

    Result result =
        controller.update(
            addCSRFToken(requestBuilder).build(),
            question.getQuestionDefinition().getName(),
            "es-US");

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition updatedQuestion =
        questionRepository
            .lookupQuestion(question.id)
            .toCompletableFuture()
            .join()
            .get()
            .getQuestionDefinition();
    assertThat(updatedQuestion.getQuestionText().get(ES_LOCALE))
        .isEqualTo("updated spanish question text");
    assertThat(updatedQuestion.getQuestionHelpText().get(ES_LOCALE))
        .isEqualTo("updated spanish help text");
  }

  @Test
  public void update_updatesExistingLocalesAndRedirects()
      throws TranslationNotFoundException, UnsupportedQuestionTypeException {
    Question question = createDraftQuestionEnglishAndSpanish();
    Http.RequestBuilder requestBuilder =
        fakeRequest()
            .bodyForm(
                ImmutableMap.of(
                    "questionText",
                    "updated spanish question text",
                    "questionHelpText",
                    "updated spanish question help text"));

    Result result =
        controller.update(
            addCSRFToken(requestBuilder).build(),
            question.getQuestionDefinition().getName(),
            "es-US");

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition updatedQuestion =
        questionRepository
            .lookupQuestion(question.id)
            .toCompletableFuture()
            .join()
            .get()
            .getQuestionDefinition();
    assertThat(updatedQuestion.getQuestionText().get(ES_LOCALE))
        .isEqualTo("updated spanish question text");
    assertThat(updatedQuestion.getQuestionHelpText().get(ES_LOCALE))
        .isEqualTo("updated spanish question help text");
  }

  @Test
  public void update_questionNotFound_returnsNotFound() {
    assertThatThrownBy(
            () ->
                controller.update(
                    addCSRFToken(fakeRequest()).build(), "non-existent question name", "es-US"))
        .hasMessage("No draft found for question: \"non-existent question name\"")
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void update_validationErrors_rendersEditFormWithMessage()
      throws UnsupportedQuestionTypeException {
    Question question = createDraftQuestionEnglishAndSpanish();
    Http.RequestBuilder requestBuilder =
        fakeRequest().bodyForm(ImmutableMap.of("questionText", "", "questionHelpText", ""));

    Result result =
        controller.update(
            addCSRFToken(requestBuilder).build(),
            question.getQuestionDefinition().getName(),
            "es-US");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(
            String.format(
                "Manage Question Translations: %s", question.getQuestionDefinition().getName()),
            "Question text cannot be blank");
  }

  private Question createDraftQuestionEnglishOnly() {
    QuestionDefinition definition =
        new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant name")
                .setDescription("name of applicant")
                .setQuestionText(LocalizedStrings.withDefaultValue(ENGLISH_QUESTION_TEXT))
                .setQuestionHelpText(LocalizedStrings.withDefaultValue(ENGLISH_QUESTION_HELP_TEXT))
                .build());
    Question question = new Question(definition);
    // Only draft questions are editable.
    question.addVersion(draftVersion);
    question.save();
    return question;
  }

  private Question createDraftQuestionEnglishAndSpanish() {
    QuestionDefinition definition =
        new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant name")
                .setDescription("name of applicant")
                .setQuestionText(
                    LocalizedStrings.withDefaultValue(ENGLISH_QUESTION_TEXT)
                        .updateTranslation(ES_LOCALE, SPANISH_QUESTION_TEXT))
                .setQuestionHelpText(
                    LocalizedStrings.withDefaultValue(ENGLISH_QUESTION_HELP_TEXT)
                        .updateTranslation(ES_LOCALE, SPANISH_QUESTION_HELP_TEXT))
                .build());
    Question question = new Question(definition);
    // Only draft questions are editable.
    question.addVersion(draftVersion);
    question.save();
    return question;
  }
}
