package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.BadRequestException;
import controllers.CiviFormController;
import controllers.FlashKey;
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
import services.TranslationLocales;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.statuses.OutOfDateStatusesException;
import services.statuses.StatusDefinitions;
import services.statuses.StatusService;
import views.admin.programs.ProgramTranslationView;
import views.components.ToastMessage;

/** Provides methods for updating localizations for a given program. */
public class AdminProgramTranslationsController extends CiviFormController {

  private final ProgramService service;
  private final ProgramTranslationView translationView;
  private final FormFactory formFactory;
  private final TranslationLocales translationLocales;
  private final Optional<Locale> maybeFirstTranslatableLocale;
  private final StatusService statusService;

  @Inject
  public AdminProgramTranslationsController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ProgramService service,
      ProgramTranslationView translationView,
      FormFactory formFactory,
      TranslationLocales translationLocales,
      StatusService statusService) {
    super(profileUtils, versionRepository);
    this.service = checkNotNull(service);
    this.translationView = checkNotNull(translationView);
    this.formFactory = checkNotNull(formFactory);
    this.translationLocales = checkNotNull(translationLocales);
    this.statusService = checkNotNull(statusService);
    this.maybeFirstTranslatableLocale =
        this.translationLocales.translatableLocales().stream().findFirst();
  }

  /**
   * Redirects to the first non-English locale eligible for translations for a program. English
   * translations for program details / statuses are not supported since configuring these values
   * already has separate UI.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result redirectToFirstLocale(Http.Request request, String programName) {
    if (maybeFirstTranslatableLocale.isEmpty()) {
      return redirect(routes.AdminProgramController.index().url())
          .flashing(FlashKey.ERROR, "Translations are not enabled for this configuration");
    }

    return redirect(
        routes.AdminProgramTranslationsController.edit(
                programName, maybeFirstTranslatableLocale.get().toLanguageTag())
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
  public Result edit(Http.Request request, String programName, String locale)
      throws ProgramNotFoundException {
    ProgramDefinition program = getDraftProgramDefinition(programName);
    Optional<Locale> maybeLocaleToEdit = translationLocales.fromLanguageTag(locale);
    Optional<ToastMessage> errorMessage =
        request.flash().get(FlashKey.ERROR).map(m -> ToastMessage.errorNonLocalized(m));
    if (maybeLocaleToEdit.isEmpty()) {
      return redirect(routes.AdminProgramController.index().url())
          .flashing(FlashKey.ERROR, String.format("The %s locale is not supported", locale));
    }
    StatusDefinitions activeStatusDefinitions =
        statusService.lookupActiveStatusDefinitions(programName);
    Locale localeToEdit = maybeLocaleToEdit.get();
    return ok(
        translationView.render(
            request,
            localeToEdit,
            program,
            activeStatusDefinitions,
            ProgramTranslationForm.fromProgram(
                program, localeToEdit, formFactory, activeStatusDefinitions),
            errorMessage));
  }

  private ProgramDefinition getDraftProgramDefinition(String programName) {
    return service
        .getActiveAndDraftPrograms()
        .getDraftProgramDefinition(programName)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format("No draft found for program: \"%s\"", programName)));
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
  public Result update(Http.Request request, String programName, String locale)
      throws ProgramNotFoundException {
    ProgramDefinition program = getDraftProgramDefinition(programName);
    Optional<Locale> maybeLocaleToUpdate = translationLocales.fromLanguageTag(locale);
    StatusDefinitions currentStatuDefnitions =
        statusService.lookupActiveStatusDefinitions(programName);
    if (maybeLocaleToUpdate.isEmpty()) {
      return redirect(routes.AdminProgramController.index().url())
          .flashing(FlashKey.ERROR, String.format("The %s locale is not supported", locale));
    }
    Locale localeToUpdate = maybeLocaleToUpdate.get();

    ImmutableList<Long> blockIds =
        program.blockDefinitions().stream()
            .map(blockDefinition -> blockDefinition.id())
            .collect(ImmutableList.toImmutableList());

    ProgramTranslationForm translationForm =
        ProgramTranslationForm.bindFromRequest(
            request,
            formFactory,
            currentStatuDefnitions.getStatuses().size(),
            program.localizedSummaryImageDescription().isPresent(),
            blockIds);
    // There are two updateLocalization() now, one in ProgramService (which doesn't throw
    // OutOfDateStatusException) and
    // the other one in StatusService which throws it.
    // Hence, one is in try-catch block and the other isn't.

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        service.updateLocalization(
            program.id(), localeToUpdate, translationForm.getUpdateData(blockIds));
    if (result.isError()) {
      ToastMessage errorMessage = ToastMessage.errorNonLocalized(joinErrors(result.getErrors()));
      return ok(
          translationView.render(
              request,
              localeToUpdate,
              program,
              currentStatuDefnitions,
              translationForm,
              Optional.of(errorMessage)));
    }
    final ErrorAnd<StatusDefinitions, CiviFormError> statusUpdate;
    try {
      statusUpdate =
          statusService.updateLocalization(
              program.adminName(), localeToUpdate, translationForm.getUpdateData(blockIds));
    } catch (OutOfDateStatusesException e) {
      return redirect(routes.AdminProgramTranslationsController.edit(programName, locale))
          .flashing(FlashKey.ERROR, e.userFacingMessage());
    }
    if (statusUpdate.isError()) {
      ToastMessage errorMessage =
          ToastMessage.errorNonLocalized(joinErrors(statusUpdate.getErrors()));
      return ok(
          translationView.render(
              request,
              localeToUpdate,
              program,
              currentStatuDefnitions,
              translationForm,
              Optional.of(errorMessage)));
    }

    return ok(
        translationView.render(
            request,
            localeToUpdate,
            program,
            statusUpdate.getResult(),
            translationForm,
            /* message= */ Optional.empty()));
  }
}
