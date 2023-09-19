package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import models.Application;
import play.libs.concurrent.HttpExecutionContext;
import repository.ApplicationRepository;
import services.program.ProgramDefinition;

/** The service responsible for mediating access to the Application resource. */
public final class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  ApplicationService(
      ApplicationRepository applicationRepository, HttpExecutionContext httpExecutionContext) {
    this.applicationRepository = checkNotNull(applicationRepository);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
  }

  /** Validates the given application is associated with the given program. */
  public Optional<Application> validateApplication(
      Optional<Application> application, ProgramDefinition program) {
    if (application.isEmpty()
        || application.get().getProgramName().isEmpty()
        || !application.get().getProgramName().equals(program.adminName())) {
      return Optional.empty();
    } else {
      return application;
    }
  }

  /**
   * Retrieves the application with the given ID and validates that it is associated with the given
   * program.
   */
  public CompletionStage<Optional<Application>> getApplicationAsync(
      long applicationId, CompletableFuture<ProgramDefinition> program) {
    CompletableFuture<Optional<Application>> maybeApplication =
        applicationRepository.getApplication(applicationId).toCompletableFuture();
    return CompletableFuture.allOf(maybeApplication, program)
        .thenApplyAsync(
            v -> {
              return validateApplication(maybeApplication.join(), program.join());
            },
            httpExecutionContext.current());
  }
}
