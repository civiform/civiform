package repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.DB;
import io.ebean.Database;
import java.time.Clock;
import javax.inject.Inject;
import models.ProgramModel;
import services.program.ProgramDefinition;

/**
 * Factory for initializing ReportingRepository.
 *
 * <p>Avoids n+1 DB queries when acquiring the public name of a program when loading program
 * reporting statistics.
 */
public final class ReportingRepositoryFactory {
  private final Clock clock;
  private final Database database;
  private final VersionRepository versionRepository;
  private final ProgramRepository programRepository;

  @Inject
  public ReportingRepositoryFactory(
      Clock clock, VersionRepository versionRepository, ProgramRepository programRepository) {
    this.clock = Preconditions.checkNotNull(clock);
    this.database = DB.getDefault();
    this.versionRepository = Preconditions.checkNotNull(versionRepository);
    this.programRepository = Preconditions.checkNotNull(programRepository);
  }

  /**
   * Creating a ReportingRepository object with <code>List&lt;ProgramModel&gt;</code> output as a
   * <code>ImmutableMap</code>.
   */
  public ReportingRepository create() {
    ImmutableList<ProgramModel> listOfPrograms =
        versionRepository.getProgramsForVersion(versionRepository.getActiveVersion());
    ImmutableMap<String, String> programAdminNameToProgramDisplayName =
        programMapBuilder(listOfPrograms);
    return new ReportingRepository(clock, database, programAdminNameToProgramDisplayName);
  }

  private ImmutableMap<String, String> programMapBuilder(
      ImmutableList<ProgramModel> listOfPrograms) {
    ImmutableMap.Builder<String, String> programMapBuilder = new ImmutableMap.Builder<>();
    for (ProgramModel p : listOfPrograms) {
      ProgramDefinition pd = programRepository.getShallowProgramDefinition(p);
      programMapBuilder.put(pd.adminName(), pd.localizedName().getDefault());
    }
    return programMapBuilder.build();
  }
}
