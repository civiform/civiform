package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import repository.VersionRepository;

/** Utility class for checking Request parameters. */
final class RequestChecker {
  private final VersionRepository versionRepository;

  @Inject
  public RequestChecker(VersionRepository versionRepository) {
    this.versionRepository = checkNotNull(versionRepository);
  }

  /**
   * Verifies programId is associated with the Draft version and throws {@code
   * NotChangeableException} if not.
   */
  void throwIfProgramNotDraft(Long programId) {
    if (versionRepository.isDraftProgram(programId)) {
      return;
    }
    throw new NotChangeableException(String.format("Program %d is not a Draft.", programId));
  }

  /**
   * Verifies programId is associated with the Active version and throws {@code
   * NotViewableException} if not.
   */
  void throwIfProgramNotActive(Long programId) {
    if (versionRepository.isActiveProgram(programId)) {
      return;
    }
    throw new NotViewableException(String.format("Program %d is not Active.", programId));
  }
}
