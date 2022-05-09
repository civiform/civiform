package controllers.admin;

import javax.inject.Inject;
import repository.VersionRepository;

/** Utility class for checking Request parameters. */
class RequestChecker {
  private VersionRepository versionRepository;

  @Inject
  public RequestChecker(VersionRepository versionRepository) {
    this.versionRepository = versionRepository;
  }

  /**
   * Verifies programId is associated with the Draft version and throws {@code
   * NotChangeableException} if not.
   */
  void throwIfNotDraft(Long programId) {
    if (versionRepository.isDraftProgram(programId)) {
      return;
    }
    throw new NotChangeableException(String.format("Program %d is not a Draft.", programId));
  }
}
