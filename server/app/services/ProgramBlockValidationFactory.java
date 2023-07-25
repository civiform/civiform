package services;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import models.Version;
import repository.VersionRepository;

public final class ProgramBlockValidationFactory {

  private final VersionRepository versionRepository;

  @Inject
  public ProgramBlockValidationFactory(VersionRepository versionRepository) {
    this.versionRepository = checkNotNull(versionRepository);
  }

  public ProgramBlockValidation create() {
    Version version = versionRepository.getDraftVersion();
    return new ProgramBlockValidation(version);
  }
}
