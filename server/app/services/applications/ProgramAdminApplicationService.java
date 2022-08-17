package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import java.util.Optional;
import models.Application;
import repository.ApplicationRepository;
import services.program.ProgramDefinition;

/** The service responsible for mediating a program admin's access to the Application resource. */
public final class ProgramAdminApplicationService {
  private final ApplicationRepository applicationRepository;

  @Inject
  ProgramAdminApplicationService(ApplicationRepository applicationRepository) {
    this.applicationRepository = checkNotNull(applicationRepository);
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
}
