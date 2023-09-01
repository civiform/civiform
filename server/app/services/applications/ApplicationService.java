package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.util.Optional;
import javax.inject.Inject;
import models.Application;
import repository.ApplicationRepository;
import services.DeploymentType;
import services.program.ProgramDefinition;

// NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING ApplicationService

/** The service responsible for mediating access to the Application resource. */
public class ApplicationService {

  private final ApplicationRepository applicationRepository;

  @Inject
  ApplicationService(
      ApplicationRepository applicationRepository,
      Config configuration,
      DeploymentType deploymentType) {
    this.applicationRepository = checkNotNull(applicationRepository);

    checkNotNull(configuration);
    checkNotNull(deploymentType);
  }

  /**
   * Retrieves the application with the given ID and validates that it is associated with the given
   * program.
   */
  public Optional<Application> getApplication(long applicationId, ProgramDefinition program) {
    Optional<Application> maybeApplication =
        applicationRepository.getApplication(applicationId).toCompletableFuture().join();
    if (maybeApplication.isEmpty()) {
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
