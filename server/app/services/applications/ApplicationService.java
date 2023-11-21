package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApplicationModel;
import repository.ApplicationRepository;

/** The service responsible for mediating access to the Application resource. */
public final class ApplicationService {

  private final ApplicationRepository applicationRepository;

  @Inject
  ApplicationService(ApplicationRepository applicationRepository) {
    this.applicationRepository = checkNotNull(applicationRepository);
  }

  /** Retrieves the application with the given ID. */
  public CompletionStage<Optional<ApplicationModel>> getApplicationAsync(long applicationId) {
    return applicationRepository.getApplication(applicationId);
  }
}
