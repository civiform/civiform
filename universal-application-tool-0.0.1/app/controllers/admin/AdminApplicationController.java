package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.collect.ImmutableList;
import forms.ProgramForm;
import javax.inject.Inject;
import models.Application;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramApplicationListView;
import views.admin.programs.ProgramEditView;
import views.admin.programs.ProgramIndexView;
import views.admin.programs.ProgramNewOneView;

/** Controller for handling methods for admins managing program definitions. */
public class AdminApplicationController extends Controller {

  private final ProgramService service;
  private final ProgramApplicationListView applicationListView;

  @Inject
  public AdminApplicationController(
      ProgramService service,
      ProgramApplicationListView applicationListView
      ) {
    this.service = checkNotNull(service);
    this.applicationListView = checkNotNull(applicationListView);
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result downloadAll(long programId) {
    throw new UnsupportedOperationException("Not yet implemented.");
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result download(long applicationId) {
    throw new UnsupportedOperationException("Not yet implemented.");
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result answerList(long programId) {
    try {
      ImmutableList<Application> applications = service.getProgramApplications(programId);
      return ok(applicationListView.render(programId, applications));
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }
  }
}
