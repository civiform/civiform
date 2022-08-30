package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import java.util.Optional;
import models.Account;
import models.Application;
import models.ApplicationEvent;
import repository.ApplicationEventRepository;
import repository.ApplicationRepository;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;
import services.program.ProgramDefinition;

/** The service responsible for mediating a program admin's access to the Application resource. */
public final class ProgramAdminApplicationService {
  private final ApplicationRepository applicationRepository;
  private final ApplicationEventRepository eventRepository;

  @Inject
  ProgramAdminApplicationService(
      ApplicationRepository applicationRepository, ApplicationEventRepository eventRepository) {
    this.applicationRepository = checkNotNull(applicationRepository);
    this.eventRepository = checkNotNull(eventRepository);
  }

  /**
   * Retrieves the application with the given ID and validates that it is associated with the given
   * program.
   */
  public Optional<Application> getApplication(long applicationId, ProgramDefinition program) {
    Optional<Application> maybeApplication =
        applicationRepository.getApplication(applicationId).toCompletableFuture().join();
    if (!maybeApplication.isPresent()) {
      return Optional.empty();
    }
    Application application = maybeApplication.get();
    if (program.adminName().isEmpty()
        || !application
            .getProgram()
            .getProgramDefinition()
            .adminName()
            .equals(program.adminName())) {
      return Optional.empty();
    }
    return Optional.of(application);
  }

  public void setStatus(Application application, StatusEvent newStatus, Account admin) {
    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(newStatus)
            .build();
    ApplicationEvent event =
        new ApplicationEvent(
            application, admin, ApplicationEventDetails.Type.STATUS_CHANGE, details);
    eventRepository.insertSync(event);
  }
}
