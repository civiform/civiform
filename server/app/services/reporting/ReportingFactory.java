package services.reporting;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import javax.inject.Inject;
import models.Program;
import models.Version;
import repository.VersionRepository;

public final class ReportingFactory {
  private final Version activeVersion;
  private final ImmutableList<Program> listOfPrograms;

  @Inject
  public ReportingFactory(VersionRepository versionRepository) {
    activeVersion = Preconditions.checkNotNull(versionRepository).getActiveVersion();
    listOfPrograms = activeVersion.getPrograms();
  }

  public Optional<Program> getProgram(String name) {
    return listOfPrograms.stream()
        .filter(p -> p.getProgramDefinition().adminName().equals(name))
        .findAny();
  }
}
