package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import io.ebean.DB;
import io.ebean.Database;
import java.time.Clock;
import javax.inject.Inject;

/**
 * Factory for initializing ReportingRepository.
 *
 * <p>Avoids n+1 DB queries when acquiring the public name of a program when loading program
 * reporting statistics.
 */
public class ReportingRepositoryFactory {
  private final Clock clock;
  private final Database database;
  private final VersionRepository versionRepository;

  @Inject
  public ReportingRepositoryFactory(Clock clock, VersionRepository versionRepository) {
    this.clock = checkNotNull(clock);
    this.database = DB.getDefault();
    this.versionRepository = Preconditions.checkNotNull(versionRepository);
  }

  /** Creating a ProgramBlockValidation object with version(DB object) as its member variable */
  public ReportingRepository create() {
    return new ReportingRepository(
        clock, database, versionRepository.getActiveVersion().getPrograms());
  }
}
