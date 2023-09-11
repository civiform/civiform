package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.swing.text.html.Option;

import models.Application;
import repository.ApplicationRepository;
import services.program.ProgramDefinition;

/** The service responsible for mediating access to the Application resource. */
public final class ApplicationService {

  private final ApplicationRepository applicationRepository;

  @Inject
  ApplicationService(ApplicationRepository applicationRepository) {
    this.applicationRepository = checkNotNull(applicationRepository);
  }

  /**
   * Retrieves the application with the given ID and validates that it is associated with the given
   * program.
   */
  public CompletionStage<Optional<Application>> getApplication(
      long applicationId, CompletableFuture<ProgramDefinition> program) {
    CompletableFuture<Optional<Application>> maybeApplication =
        applicationRepository.getApplication(applicationId).toCompletableFuture();
    return CompletableFuture.allOf(maybeApplication, program)
      .thenApplyAsync(
        application -> {
          Optional<Application> maybeApplication1 = maybeApplication.get();
          Application application = maybeApplication1.get();
          ProgramDefinition program1 = program.get();
          if (program1.adminName().isEmpty()
            || !application
            .getProgram()
            .getProgramDefinition()
            .adminName()
            .equals(program1.adminName())) {
            return CompletableFuture.completedFuture(Optional.empty());
          }
          return CompletableFuture.completedFuture(Optional.of(application));
        }
      )
      .exceptionally(
        ex -> {
          return CompletableFuture.completedFuture(Optional.empty());
        }
      );
//    if (maybeApplication.isEmpty()) {
//      return CompletableFuture.completedFuture(Optional.empty());
//    }
//    Application application = maybeApplication.get();
//    if (program.adminName().isEmpty()
//        || !application
//            .getProgram()
//            .getProgramDefinition()
//            .adminName()
//            .equals(program.adminName())) {
//      return CompletableFuture.completedFuture(Optional.empty());
//    }
//    return CompletableFuture.completedFuture(Optional.of(application));
//  }
}
