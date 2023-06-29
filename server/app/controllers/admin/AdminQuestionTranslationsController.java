package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.CiviFormController;
import forms.translation.EnumeratorQuestionTranslationForm;
import forms.translation.MultiOptionQuestionTranslationForm;
import forms.translation.QuestionTranslationForm;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.TranslationLocales;
import services.question.QuestionService;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionTranslationView;
import views.components.ToastMessage;

/** Provides controller methods for editing and updating question translations. */
public class AdminQuestionTranslationsController extends CiviFormController {

  private final HttpExecutionContext httpExecutionContext;
  private final QuestionService questionService;
  private final QuestionTranslationView translationView;
  private final FormFactory formFactory;
  private final TranslationLocales translationLocales;
  private final Optional<Locale> maybeFirstTranslatableLocale;

  @Inject
  public AdminQuestionTranslationsController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      HttpExecutionContext httpExecutionContext,
      QuestionService questionService,
      QuestionTranslationView translationView,
      FormFactory formFactory,
      TranslationLocales translationLocales) {
    super(profileUtils, versionRepository);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.questionService = checkNotNull(questionService);
    this.translationView = checkNotNull(translationView);
    this.formFactory = checkNotNull(formFactory);
    this.translationLocales = checkNotNull(translationLocales);
    this.maybeFirstTranslatableLocale =
        this.translationLocales.translatableLocales().stream().findFirst();
  }

  /**
   * Redirects to the first non-English locale eligible for translations for a question. English
   * translations for question details are not supported since configuring these values already has
   * separate UI.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result redirectToFirstLocale(Http.Request request, long questionId) {
    if (maybeFirstTranslatableLocale.isEmpty()) {
      return redirect(routes.AdminQuestionController.index().url())
          .flashing("error", "Translations are not enabled for this configuration");
    }
    return redirect(
        routes.AdminQuestionTranslationsController.edit(
                questionId, maybeFirstTranslatableLocale.get().toLanguageTag())
            .url());
  }

  /**
   * Renders an edit form for admins to add or update translations for the question in the given
   * locale.
   *
   * @param request the current {@link Http.Request}
   * @param locale the locale to update, as an ISO language tag
   * @return a rendered {@link QuestionTranslationView} pre-populated with any existing translations
   *     for the given locale
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> edit(Http.Request request, long questionId, String locale) {
    Optional<Locale> maybeLocaleToEdit = translationLocales.fromLanguageTag(locale);
    if (maybeLocaleToEdit.isEmpty()) {
      return CompletableFuture.completedFuture(
          redirect(routes.AdminQuestionController.index().url())
              .flashing("error", String.format("The %s locale is not supported", locale)));
    }
    Locale localeToEdit = maybeLocaleToEdit.get();

    return questionService
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyQuestionService -> {
              try {
                QuestionDefinition definition =
                    readOnlyQuestionService.getQuestionDefinition(questionId);
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
   * @param locale the locale to update, as an ISO language tag
   * @return redirects to the admin's home page if updates were successful; otherwise, renders the
   *     same {@link QuestionTranslationView} with error messages
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> update(Http.Request request, long questionId, String locale) {
    Optional<Locale> maybeLocaleToUpdate = translationLocales.fromLanguageTag(locale);
    if (maybeLocaleToUpdate.isEmpty()) {
      return CompletableFuture.completedFuture(
          redirect(routes.AdminQuestionController.index().url())
              .flashing("error", String.format("The %s locale is not supported", locale)));
    }
    Locale localeToUpdate = maybeLocaleToUpdate.get();

    return questionService
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyQuestionService -> {
              try {
                QuestionDefinition toUpdate =
                    readOnlyQuestionService.getQuestionDefinition(questionId);
                QuestionTranslationForm form =
                    buildFormFromRequest(request, toUpdate.getQuestionType());
                QuestionDefinition definitionWithUpdates =
                    form.builderWithUpdates(toUpdate, localeToUpdate).build();

                ErrorAnd<QuestionDefinition, CiviFormError> result =
                    questionService.update(definitionWithUpdates);

                if (result.isError()) {
                  ToastMessage message =
                      ToastMessage.errorNonLocalized(joinErrors(result.getErrors()));
                  return ok(
                      translationView.renderErrors(
                          request, localeToUpdate, definitionWithUpdates, message));
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
      case PHONE: // fallthrough intended
      default:
        return formFactory.form(QuestionTranslationForm.class).bindFromRequest(request).get();
    }
  }
}
