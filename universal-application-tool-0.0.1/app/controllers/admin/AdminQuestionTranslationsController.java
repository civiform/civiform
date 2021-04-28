package controllers.admin;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.QuestionTranslationForm;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import services.CiviFormError;
import services.ErrorAnd;
import services.question.QuestionService;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import views.admin.questions.QuestionTranslationView;

/** Provides controller methods for editing and updating question translations. */
public class AdminQuestionTranslationsController extends CiviFormController {

  private final HttpExecutionContext httpExecutionContext;
  private final QuestionService questionService;
  private final QuestionTranslationView translationView;
  private final FormFactory formFactory;

  @Inject
  public AdminQuestionTranslationsController(
      HttpExecutionContext httpExecutionContext,
      QuestionService questionService,
      QuestionTranslationView translationView,
      FormFactory formFactory) {
    this.httpExecutionContext = httpExecutionContext;
    this.questionService = questionService;
    this.translationView = translationView;
    this.formFactory = formFactory;
  }

  /**
   * Renders an edit form for admins to add or update translations for the question in the given
   * locale.
   *
   * @param request the current {@link Http.Request}
   * @param id the ID of the question to update
   * @param locale the locale to update, as an ISO language tag
   * @return a rendered {@link QuestionTranslationView} pre-populated with any existing translations
   *     for the given locale
   */
  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> edit(Http.Request request, long id, String locale) {
    return questionService
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyQuestionService -> {
              try {
                QuestionDefinition definition = readOnlyQuestionService.getQuestionDefinition(id);
                Locale localeToEdit = Locale.forLanguageTag(locale);
                return ok(
                    translationView.render(
                        request,
                        id,
                        localeToEdit,
                        definition.maybeGetQuestionText(localeToEdit),
                        definition.maybeGetQuestionHelpText(localeToEdit),
                        Optional.empty()));
              } catch (QuestionNotFoundException e) {
                return notFound(e.getMessage());
              }
            },
            httpExecutionContext.current());
  }

  /**
   * Save updates to a question's localizations.
   *
   * @param request the current {@link Http.Request}
   * @param id the ID of the question to update
   * @param locale the locale to update, as an ISO language tag
   * @return redirects to the admin's home page if updates were successful; otherwise, renders the
   *     same {@link QuestionTranslationView} with error messages
   */
  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> update(Http.Request request, long id, String locale) {
    Form<QuestionTranslationForm> translationForm = formFactory.form(QuestionTranslationForm.class);
    if (translationForm.hasErrors()) {
      return supplyAsync(Results::badRequest);
    }

    QuestionTranslationForm translations = translationForm.bindFromRequest(request).get();
    Locale updatedLocale = Locale.forLanguageTag(locale);
    String questionText = translations.getQuestionText();
    String questionHelpText = translations.getQuestionHelpText();

    return questionService
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyQuestionService -> {
              try {
                QuestionDefinition toUpdate = readOnlyQuestionService.getQuestionDefinition(id);
                QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder(toUpdate);
                builder.updateQuestionText(updatedLocale, questionText);
                builder.updateQuestionHelpText(updatedLocale, questionHelpText);
                ErrorAnd<QuestionDefinition, CiviFormError> result =
                    questionService.update(builder.build());

                if (result.isError()) {
                  String errorMessage = joinErrors(result.getErrors());
                  return ok(
                      translationView.render(
                          request,
                          id,
                          updatedLocale,
                          Optional.of(questionText),
                          Optional.of(questionHelpText),
                          Optional.of(errorMessage)));
                }

                return redirect(routes.QuestionController.index().url());

              } catch (QuestionNotFoundException e) {
                return notFound(e.getMessage());
              } catch (UnsupportedQuestionTypeException e) {
                return badRequest(e.getMessage());
              } catch (InvalidUpdateException e) {
                return internalServerError(e.getMessage());
              }
            });
  }
}
