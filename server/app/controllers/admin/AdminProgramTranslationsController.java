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
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramTranslationView;

/** Provides methods for updating localizations for a given program. */
public class AdminProgramTranslationsController extends CiviFormController {

  private final ProgramService service;
  private final ProgramTranslationView translationView;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramTranslationsController(
      ProgramService service, ProgramTranslationView translationView, FormFactory formFactory) {
    this.service = checkNotNull(service);
    this.translationView = checkNotNull(translationView);
    this.formFactory = checkNotNull(formFactory);
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
    Locale localeToEdit = Locale.forLanguageTag(locale);
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
    Form<ProgramTranslationForm> translationForm = formFactory.form(ProgramTranslationForm.class);
    if (translationForm.hasErrors()) {
      return badRequest();
    }
    ProgramTranslationForm translations = translationForm.bindFromRequest(request).get();
    Locale updatedLocale = Locale.forLanguageTag(locale);
    String displayName = translations.getDisplayName();
    String displayDescription = translations.getDisplayDescription();

    try {
      ErrorAnd<ProgramDefinition, CiviFormError> result =
          service.updateLocalization(program.id(), updatedLocale, displayName, displayDescription);
      if (result.isError()) {
        String errorMessage = joinErrors(result.getErrors());
        return ok(
            translationView.render(
                request,
                updatedLocale,
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
