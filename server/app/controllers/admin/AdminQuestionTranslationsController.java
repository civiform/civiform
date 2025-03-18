package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.BadRequestException;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.translation.EnumeratorQuestionTranslationForm;
import forms.translation.MultiOptionQuestionTranslationForm;
import forms.translation.QuestionTranslationForm;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import models.ConcurrentUpdateException;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.TranslationLocales;
import services.question.QuestionService;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionTranslationView;
import views.components.ToastMessage;

/** Provides controller methods for editing and updating question translations. */
public class AdminQuestionTranslationsController extends CiviFormController {

  private final QuestionService questionService;
  private final QuestionTranslationView translationView;
  private final FormFactory formFactory;
  private final TranslationLocales translationLocales;
  private final Optional<Locale> maybeFirstTranslatableLocale;

  @Inject
  public AdminQuestionTranslationsController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      QuestionService questionService,
      QuestionTranslationView translationView,
      FormFactory formFactory,
      TranslationLocales translationLocales) {
    super(profileUtils, versionRepository);
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
  public Result redirectToFirstLocale(Http.Request request, String questionName) {
    if (maybeFirstTranslatableLocale.isEmpty()) {
      return redirect(routes.AdminQuestionController.index().url())
          .flashing(FlashKey.ERROR, "Translations are not enabled for this configuration");
    }

    return redirect(
        routes.AdminQuestionTranslationsController.edit(
                questionName, maybeFirstTranslatableLocale.get().toLanguageTag())
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
  public Result edit(Http.Request request, String questionName, String locale) {
    Optional<Locale> maybeLocaleToEdit = translationLocales.fromLanguageTag(locale);
    if (maybeLocaleToEdit.isEmpty()) {
      return redirect(routes.AdminQuestionController.index().url())
          .flashing(FlashKey.ERROR, String.format("The %s locale is not supported", locale));
    }
    Locale localeToEdit = maybeLocaleToEdit.get();

    QuestionDefinition definition = getDraftQuestionDefinition(questionName);
    Optional<ToastMessage> message =
        request.flash().get(FlashKey.CONCURRENT_UPDATE).map(m -> ToastMessage.errorNonLocalized(m));
    if (message.isPresent()) {
      return ok(translationView.renderErrors(request, localeToEdit, definition, message.get()));
    }
    return ok(translationView.render(request, localeToEdit, definition));
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
  public Result update(Http.Request request, String questionName, String locale) {
    Optional<Locale> maybeLocaleToUpdate = translationLocales.fromLanguageTag(locale);
    if (maybeLocaleToUpdate.isEmpty()) {
      return redirect(routes.AdminQuestionController.index().url())
          .flashing(FlashKey.ERROR, String.format("The %s locale is not supported", locale));
    }

    Locale localeToUpdate = maybeLocaleToUpdate.get();
    QuestionDefinition toUpdate = getDraftQuestionDefinition(questionName);

    try {
      QuestionDefinition definitionWithUpdates =
          buildFormFromRequest(request, toUpdate.getQuestionType())
              .builderWithUpdates(toUpdate, localeToUpdate)
              .build();
      ErrorAnd<QuestionDefinition, CiviFormError> result =
          questionService.update(definitionWithUpdates);

      if (result.isError()) {
        ToastMessage message = ToastMessage.errorNonLocalized(joinErrors(result.getErrors()));
        return ok(
            translationView.renderErrors(request, localeToUpdate, definitionWithUpdates, message));
      }
      return ok(translationView.render(request, localeToUpdate, definitionWithUpdates));

    } catch (UnsupportedQuestionTypeException e) {
      return badRequest(e.getMessage());
    } catch (InvalidUpdateException e) {
      return internalServerError(e.getMessage());
    } catch (ConcurrentUpdateException e) {
      // If there was a concurrent update, load a fresh edit form so the admin sees the concurrently
      // made changes.
      return redirect(routes.AdminQuestionTranslationsController.edit(questionName, locale).url())
          .flashing(
              FlashKey.CONCURRENT_UPDATE,
              "The question was updated by another user while the edit page was open in your"
                  + " browser. Please try your edits again.");
    }
  }

  private QuestionDefinition getDraftQuestionDefinition(String questionName) {
    return questionService
        .getReadOnlyQuestionService()
        .toCompletableFuture()
        .join()
        .getActiveAndDraftQuestions()
        .getDraftQuestionDefinition(questionName)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format("No draft found for question: \"%s\"", questionName)));
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
      case NULL_QUESTION: // fallthrough intended
      case NUMBER: // fallthrough intended
      case TEXT: // fallthrough intended
      case PHONE: // fallthrough intended
      default:
        return formFactory.form(QuestionTranslationForm.class).bindFromRequest(request).get();
    }
  }
}
