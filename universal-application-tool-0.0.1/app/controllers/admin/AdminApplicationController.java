package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import javax.inject.Inject;
import models.Application;
import org.pac4j.play.java.Secure;
import play.mvc.Controller;
import play.mvc.Result;
import repository.ApplicationRepository;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramApplicationListView;
import views.admin.programs.ProgramApplicationView;

/** Controller for admins viewing responses to programs. */
public class AdminApplicationController extends Controller {

  private final ProgramService service;
  private final ApplicationRepository applicationRepository;
  private final ProgramApplicationListView applicationListView;
  private final ProgramApplicationView applicationView;

  @Inject
  public AdminApplicationController(
      ProgramService service,
      ProgramApplicationListView applicationListView,
      ProgramApplicationView applicationView,
      ApplicationRepository applicationRepository) {
    this.service = checkNotNull(service);
    this.applicationListView = checkNotNull(applicationListView);
    this.applicationView = checkNotNull(applicationView);
    this.applicationRepository = checkNotNull(applicationRepository);
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result downloadAll(long programId) {
    throw new UnsupportedOperationException("Not yet implemented.");
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result download(long programId, long applicationId) {
    throw new UnsupportedOperationException("Not yet implemented.");
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result view(long programId, long applicationId) {
    Optional<Application> applicationMaybe =
        this.applicationRepository.getApplication(applicationId).toCompletableFuture().join();
    if (!applicationMaybe.isPresent()) {
      return notFound(String.format("Application %d does not exist.", applicationId));
    }
    try {
      return ok(applicationView.render(programId, applicationMaybe.get()));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program %d does not exit.", programId));
    }
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
