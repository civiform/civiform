package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

import models.Application;
import play.libs.concurrent.HttpExecutionContext;
import repository.ApplicationRepository;
import services.program.ProgramDefinition;

/** The service responsible for mediating access to the Application resource. */
public final class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  ApplicationService(ApplicationRepository applicationRepository,HttpExecutionContext httpExecutionContext) {
    this.applicationRepository = checkNotNull(applicationRepository);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
  }

  /**
   * Retrieves the application with the given ID and validates that it is associated with the given
   * program.
   */
  public CompletableFuture<Optional<Application>> getApplication(
      long applicationId, CompletableFuture<ProgramDefinition> program) {
    CompletableFuture<Optional<Application>> maybeApplication =
      applicationRepository.getApplication(applicationId).toCompletableFuture();
    return CompletableFuture.allOf(maybeApplication, program)
      .thenComposeAsync(
        applicationMaybe -> {
          try {
            if(maybeApplication.get().isEmpty() || !program.get().adminName().equals(maybeApplication.get().get().getProgram().getProgramDefinition().adminName()))
            {
              return supplyAsync(() -> Optional.empty());
            }
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          } catch (ExecutionException e) {
            throw new RuntimeException(e);
          }
          return maybeApplication;
          },httpExecutionContext.current());
  }
}
