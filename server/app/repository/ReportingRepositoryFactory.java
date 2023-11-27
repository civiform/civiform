package repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import models.ProgramModel;

import javax.inject.Inject;

import java.time.Clock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory for initializing ReportingRepository.
 */
public class ReportingRepositoryFactory {
  private final Clock clock;
  private final ImmutableList<ProgramModel> listOfPrograms;

  @Inject
  public ReportingRepositoryFactory(Clock clock,
    VersionRepository versionRepository) {
    this.clock = checkNotNull(clock);
    listOfPrograms = Preconditions.checkNotNull(versionRepository).getActiveVersion().getPrograms();
  }
  /** Creating a ProgramBlockValidation object with version(DB object) as its member variable */
  public ReportingRepository create() {
    return new ReportingRepository(clock, listOfPrograms);
  }
}
