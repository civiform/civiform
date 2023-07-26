package services;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import models.Version;
import repository.VersionRepository;

/**
 * Factory for creating `ProgramBlockValidation` instances. 
 * <p>Needed because - for every question in the question picker, we will need to check if the question is
 * tombstoned, resulting in n+1 DB queries. To avoid this, we create a factory object injecting a
 * VersionRepository and calling getDraftVersion() only once per request.
 */
public final class ProgramBlockValidationFactory {

  private final VersionRepository versionRepository;

  @Inject
  public ProgramBlockValidationFactory(VersionRepository versionRepository) {
    this.versionRepository = checkNotNull(versionRepository);
  }
  /** Creating a ProgramBlockValidation object with version(DB object) as its member variable */
  public ProgramBlockValidation create() {
    Version version = versionRepository.getDraftVersion();
    return new ProgramBlockValidation(version);
  }
}
