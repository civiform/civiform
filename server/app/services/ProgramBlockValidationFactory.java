package services;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import models.Version;
import repository.VersionRepository;

/**
 * Helper class for creating a ProgramBlockValidation Object.The reason this factory class is needed
 * because - for every question in the question picker, we will need to check if the question is
 * tombstoned, resulting in n+1 DB queries. To avoid it, we create a factory object injecting a
 * VersionRepository and calling getDraftVersion() only once during the object creation.
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
