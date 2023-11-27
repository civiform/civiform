package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import java.time.Clock;
import javax.inject.Inject;

/**
 * Factory for initializing ReportingRepository.
 *
 * Avoids n+1 DB queries when acquiring the public name of a program when loading
 * program reporting statistics.
 */
public class ReportingRepositoryFactory {
  private final Clock clock;
  private final VersionRepository versionRepository;

  @Inject
  public ReportingRepositoryFactory(Clock clock, VersionRepository versionRepository) {
    this.clock = checkNotNull(clock);
    this.versionRepository = Preconditions.checkNotNull(versionRepository);
  }

  /** Creating a ProgramBlockValidation object with version(DB object) as its member variable */
  public ReportingRepository create() {
    return new ReportingRepository(clock, versionRepository.getActiveVersion().getPrograms());
  }
}
