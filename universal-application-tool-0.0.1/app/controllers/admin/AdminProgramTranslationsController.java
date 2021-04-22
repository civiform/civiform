package controllers.admin;

import static play.mvc.Results.badRequest;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.ProgramTranslationForm;
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

public class AdminProgramTranslationsController extends CiviFormController {

  private final ProgramService service;
  private final ProgramTranslationView translationView;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramTranslationsController(
      ProgramService service, ProgramTranslationView translationView, FormFactory formFactory) {
    this.service = service;
    this.translationView = translationView;
    this.formFactory = formFactory;
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(Http.Request request, long id) {
    return ok(translationView.render(request, id, Optional.empty()));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Http.Request request, long id) {
    Form<ProgramTranslationForm> translationForm = formFactory.form(ProgramTranslationForm.class);
    if (translationForm.hasErrors()) {
      return badRequest();
    }
    ProgramTranslationForm translations = translationForm.bindFromRequest(request).get();

    try {
      ErrorAnd<ProgramDefinition, CiviFormError> result =
          service.updateLocalization(
              id,
              translations.getLocale(),
              translations.getDisplayName(),
              translations.getDisplayDescription());
      if (result.isError()) {
        String errorMessage = joinErrors(result.getErrors());
        return ok(translationView.render(request, id, Optional.of(errorMessage)));
      }
      return redirect(routes.AdminProgramController.index().url());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", id));
    }
  }
}
