package services.reporting;

import models.Program;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.VersionRepository;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public final class ReportingFactoryTest extends ResetPostgres {

  private VersionRepository versionRepository;
  private ReportingFactory reportingFactory;

  @Before
  public void setUp() {
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void getProgram() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String programName = "program";
    resourceCreator.insertDraftProgram(programName);
    version.refresh();
    versionRepository.publishNewSynchronizedVersion();
    reportingFactory = new ReportingFactory(versionRepository);
    Optional<Program> result = reportingFactory.getProgram(programName);

    assertThat(result.isPresent()).isTrue();
    assertThat(result.get().getProgramDefinition().adminName()).isEqualTo(programName);
  }
}
