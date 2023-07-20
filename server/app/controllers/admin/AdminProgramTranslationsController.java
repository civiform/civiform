package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.CiviFormController;
import forms.translation.ProgramTranslationForm;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.TranslationLocales;
import services.program.OutOfDateStatusesException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramTranslationView;
import views.components.ToastMessage;

/** Provides methods for updating localizations for a given program. */
public class AdminProgramTranslationsController extends CiviFormController {

  private final ProgramService service;
  private final ProgramTranslationView translationView;
  private final FormFactory formFactory;
  private final TranslationLocales translationLocales;
  private final Optional<Locale> maybeFirstTranslatableLocale;

  @Inject
  public AdminProgramTranslationsController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ProgramService service,
      ProgramTranslationView translationView,
      FormFactory formFactory,
      TranslationLocales translationLocales) {
    super(profileUtils, versionRepository);
    this.service = checkNotNull(service);
    this.translationView = checkNotNull(translationView);
    this.formFactory = checkNotNull(formFactory);
    this.translationLocales = checkNotNull(translationLocales);
    this.maybeFirstTranslatableLocale =
        this.translationLocales.translatableLocales().stream().findFirst();
  }

  /**
   * Redirects to the first non-English locale eligible for translations for a program. English
   * translations for program details / statuses are not supported since configuring these values
   * already has separate UI.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result redirectToFirstLocale(Http.Request request, long programId) {
    if (maybeFirstTranslatableLocale.isEmpty()) {
      return redirect(routes.AdminProgramController.index().url())
          .flashing("error", "Translations are not enabled for this configuration");
    }
    return redirect(
        routes.AdminProgramTranslationsController.edit(
                programId, maybeFirstTranslatableLocale.get().toLanguageTag())
            .url());
  }

  /**
   * Renders an edit form for a program so the admin can update translations for the given locale.
   *
   * @param request the current {@link Http.Request}
   * @param locale the locale to update, as an ISO language tag
   * @return a rendered {@link ProgramTranslationView} pre-populated with any existing translations
   *     for the given locale
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(Http.Request request, long programId, String locale)
      throws ProgramNotFoundException {
    ProgramDefinition program = service.getProgramDefinition(programId);
    Optional<Locale> maybeLocaleToEdit = translationLocales.fromLanguageTag(locale);
    Optional<ToastMessage> errorMessage =
        request.flash().get("error").map(m -> ToastMessage.errorNonLocalized(m));
    if (maybeLocaleToEdit.isEmpty()) {
      return redirect(routes.AdminProgramController.index().url())
          .flashing("error", String.format("The %s locale is not supported", locale));
    }
    Locale localeToEdit = maybeLocaleToEdit.get();
    return ok(
        translationView.render(
            request,
            localeToEdit,
            program,
            ProgramTranslationForm.fromProgram(program, localeToEdit, formFactory),
            errorMessage));
  }

  /**
   * Save updates to a program's localizations.
   *
   * @param request the current {@link Http.Request}
   * @param locale the locale to update, as an ISO language tag
   * @return redirects to the admin's home page if updates were successful; otherwise, renders the
   *     same {@link ProgramTranslationView} with error messages
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(Http.Request request, long programId, String locale)
      throws ProgramNotFoundException {
    ProgramDefinition program = service.getProgramDefinition(programId);
    Optional<Locale> maybeLocaleToUpdate = translationLocales.fromLanguageTag(locale);
    if (maybeLocaleToUpdate.isEmpty()) {
      return redirect(routes.AdminProgramController.index().url())
          .flashing("error", String.format("The %s locale is not supported", locale));
    }
    Locale localeToUpdate = maybeLocaleToUpdate.get();

    ProgramTranslationForm translationForm =
        ProgramTranslationForm.bindFromRequest(
            request, formFactory, program.statusDefinitions().getStatuses().size());

    final ErrorAnd<ProgramDefinition, CiviFormError> result;
    try {
      result =
          service.updateLocalization(program.id(), localeToUpdate, translationForm.getUpdateData());
    } catch (OutOfDateStatusesException e) {
      return redirect(routes.AdminProgramTranslationsController.edit(programId, locale))
          .flashing("error", e.userFacingMessage());
    }
    if (result.isError()) {
      ToastMessage errorMessage = ToastMessage.errorNonLocalized(joinErrors(result.getErrors()));
      return ok(
          translationView.render(
              request, localeToUpdate, program, translationForm, Optional.of(errorMessage)));
    }
    return redirect(routes.AdminProgramController.index().url())
        .flashing(
            "success",
            String.format(
                "Program translations updated for %s",
                localeToUpdate.getDisplayLanguage(LocalizedStrings.DEFAULT_LOCALE)));
  }
}
