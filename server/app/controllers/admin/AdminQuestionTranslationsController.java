package controllers.admin;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.translation.EnumeratorQuestionTranslationForm;
import forms.translation.MultiOptionQuestionTranslationForm;
import forms.translation.QuestionTranslationForm;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
import services.question.QuestionService;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
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
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> edit(Http.Request request, long id, String locale) {
    return questionService
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyQuestionService -> {
              try {
                QuestionDefinition definition = readOnlyQuestionService.getQuestionDefinition(id);
                Locale localeToEdit = Locale.forLanguageTag(locale);
                return ok(translationView.render(request, localeToEdit, definition));
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
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> update(Http.Request request, long id, String locale) {
    Locale updatedLocale = Locale.forLanguageTag(locale);

    return questionService
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyQuestionService -> {
              try {
                QuestionDefinition toUpdate = readOnlyQuestionService.getQuestionDefinition(id);
                QuestionTranslationForm form =
                    buildFormFromRequest(request, toUpdate.getQuestionType());
                QuestionDefinition definitionWithUpdates =
                    form.builderWithUpdates(toUpdate, updatedLocale).build();

                ErrorAnd<QuestionDefinition, CiviFormError> result =
                    questionService.update(definitionWithUpdates);

                if (result.isError()) {
                  String errorMessage = joinErrors(result.getErrors());
                  return ok(
                      translationView.renderErrors(
                          request, updatedLocale, definitionWithUpdates, errorMessage));
                }

                return redirect(routes.AdminQuestionController.index().url());

              } catch (QuestionNotFoundException e) {
                return notFound(e.getMessage());
              } catch (UnsupportedQuestionTypeException e) {
                return badRequest(e.getMessage());
              } catch (InvalidUpdateException e) {
                return internalServerError(e.getMessage());
              }
            },
            httpExecutionContext.current());
  }

  private QuestionTranslationForm buildFormFromRequest(Http.Request request, QuestionType type) {
    switch (type) {
      case CHECKBOX: // fallthrough intended
      case DROPDOWN: // fallthrough intended
      case RADIO_BUTTON:
        return formFactory
            .form(MultiOptionQuestionTranslationForm.class)
            .bindFromRequest(request)
            .get();
      case ENUMERATOR:
        return formFactory
            .form(EnumeratorQuestionTranslationForm.class)
            .bindFromRequest(request)
            .get();
      case ADDRESS: // fallthrough intended
      case CURRENCY: // fallthrough intended
      case FILEUPLOAD: // fallthrough intended
      case NAME: // fallthrough intended
      case NUMBER: // fallthrough intended
      case TEXT: // fallthrough intended
      default:
        return formFactory.form(QuestionTranslationForm.class).bindFromRequest(request).get();
    }
  }
}
