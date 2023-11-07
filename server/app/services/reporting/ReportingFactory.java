package services.reporting;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import javax.inject.Inject;
import models.Program;
import repository.VersionRepository;

public final class ReportingFactory {
  private final ImmutableList<Program> listOfPrograms;

  @Inject
  public ReportingFactory(VersionRepository versionRepository) {
    listOfPrograms = Preconditions.checkNotNull(versionRepository).getActiveVersion().getPrograms();
  }

  public Optional<Program> getProgram(String name) {
    return listOfPrograms.stream()
        .filter(p -> p.getProgramDefinition().adminName().equals(name))
        .findAny();
  }
}
