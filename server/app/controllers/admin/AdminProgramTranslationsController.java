package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.translation.ProgramTranslationForm;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
import services.TranslationHelper;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramTranslationView;

/** Provides methods for updating localizations for a given program. */
public class AdminProgramTranslationsController extends CiviFormController {

  private final ProgramService service;
  private final ProgramTranslationView translationView;
  private final FormFactory formFactory;
  private final TranslationHelper translationHelper;
  private final Optional<Locale> maybeFirstLocaleForTranslations;

  @Inject
  public AdminProgramTranslationsController(
      ProgramService service,
      ProgramTranslationView translationView,
      FormFactory formFactory,
      TranslationHelper translationHelper) {
    this.service = checkNotNull(service);
    this.translationView = checkNotNull(translationView);
    this.formFactory = checkNotNull(formFactory);
    this.translationHelper = checkNotNull(translationHelper);
    this.maybeFirstLocaleForTranslations =
        this.translationHelper.localesForTranslation().stream().findFirst();
  }

  /**
   * Redirects to the first non-English locale eligible for translations for a a program.
   *
   * @param request the current {@link Http.Request}
   * @param id the ID of the program to update
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result redirectToFirstLocale(Http.Request request, long id) {
    if (maybeFirstLocaleForTranslations.isEmpty()) {
      return redirect(routes.AdminProgramController.index().url())
          .flashing("error", "Translations are not enabled for this configuration");
    }
    return redirect(
        routes.AdminProgramTranslationsController.edit(
                id, maybeFirstLocaleForTranslations.get().toLanguageTag())
            .url());
  }

  /**
   * Renders an edit form for a program so the admin can update translations for the given locale.
   *
   * @param request the current {@link Http.Request}
   * @param id the ID of the program to update
   * @param locale the locale to update, as an ISO language tag
   * @return a rendered {@link ProgramTranslationView} pre-populated with any existing translations
   *     for the given locale
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(Http.Request request, long id, String locale) throws ProgramNotFoundException {
    ProgramDefinition program = service.getProgramDefinition(id);
    Optional<Locale> maybeLocaleToEdit = translationHelper.getSupportedLocale(locale);
    if (maybeLocaleToEdit.isEmpty()) {
      return redirect(routes.AdminProgramController.index().url())
          .flashing("error", String.format("Unsupported locale: %s", locale));
    }
    Locale localeToEdit = maybeLocaleToEdit.get();
    return ok(
        translationView.render(
            request,
            localeToEdit,
            program,
            program.localizedName().maybeGet(localeToEdit),
            program.localizedDescription().maybeGet(localeToEdit),
            Optional.empty()));
  }

  /**
   * Save updates to a program's localizations.
   *
   * @param request the current {@link Http.Request}
   * @param id the ID of the program to update
   * @param locale the locale to update, as an ISO language tag
   * @return redirects to the admin's home page if updates were successful; otherwise, renders the
   *     same {@link ProgramTranslationView} with error messages
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(Http.Request request, long id, String locale)
      throws ProgramNotFoundException {
    ProgramDefinition program = service.getProgramDefinition(id);
    Optional<Locale> maybeLocaleToUpdate = translationHelper.getSupportedLocale(locale);
    if (maybeLocaleToUpdate.isEmpty()) {
      return redirect(routes.AdminProgramController.index().url())
          .flashing("error", String.format("Unsupported locale: %s", locale));
    }
    Locale localeToUpdate = maybeLocaleToUpdate.get();

    Form<ProgramTranslationForm> translationForm = formFactory.form(ProgramTranslationForm.class);
    if (translationForm.hasErrors()) {
      return badRequest();
    }
    ProgramTranslationForm translations = translationForm.bindFromRequest(request).get();
    String displayName = translations.getDisplayName();
    String displayDescription = translations.getDisplayDescription();

    try {
      ErrorAnd<ProgramDefinition, CiviFormError> result =
          service.updateLocalization(program.id(), localeToUpdate, displayName, displayDescription);
      if (result.isError()) {
        String errorMessage = joinErrors(result.getErrors());
        return ok(
            translationView.render(
                request,
                localeToUpdate,
                program,
                Optional.of(displayName),
                Optional.of(displayDescription),
                Optional.of(errorMessage)));
      }
      return redirect(routes.AdminProgramController.index().url());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", id));
    }
  }
}
