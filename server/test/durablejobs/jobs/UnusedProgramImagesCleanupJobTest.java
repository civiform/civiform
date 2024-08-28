package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import durablejobs.DurableJobName;
import java.time.Instant;
import models.JobType;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import org.junit.Test;
import repository.ProgramRepository;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import support.ProgramBuilder;
import support.cloud.FakePublicStorageClient;

public class UnusedProgramImagesCleanupJobTest extends ResetPostgres {
  FakePublicStorageClient fakePublicStorageClient = new FakePublicStorageClient();
  ProgramRepository programRepository = instanceOf(ProgramRepository.class);
  ProgramService programService = instanceOf(ProgramService.class);
  VersionRepository versionRepository = instanceOf(VersionRepository.class);
  PersistedDurableJobModel jobModel =
      new PersistedDurableJobModel(
          DurableJobName.UNUSED_PROGRAM_IMAGES_CLEANUP.toString(),
          JobType.RECURRING,
          Instant.ofEpochMilli(1000));

  @Test
  public void getPersistedDurableJob_isJobModel() {
    UnusedProgramImagesCleanupJob job =
        new UnusedProgramImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);

    assertThat(job.getPersistedDurableJob()).isEqualTo(jobModel);
  }

  @Test
  public void run_doesNotDeleteActiveProgramImages() throws ProgramNotFoundException {
    ProgramModel program1 = ProgramBuilder.newDraftProgram("Program #1").build();
    ProgramModel program2 = ProgramBuilder.newDraftProgram("Program #2").build();
    programService.setSummaryImageFileKey(program1.id, "program-summary-image/program-1/test.jpg");
    programService.setSummaryImageFileKey(program2.id, "program-summary-image/program-2/test.jpg");
    versionRepository.publishNewSynchronizedVersion();

    UnusedProgramImagesCleanupJob job =
        new UnusedProgramImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    job.run();

    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly(
            "program-summary-image/program-1/test.jpg", "program-summary-image/program-2/test.jpg");
  }

  @Test
  public void run_doesNotDeleteDraftProgramImages() throws ProgramNotFoundException {
    ProgramModel program1 = ProgramBuilder.newDraftProgram("Program #1").build();
    ProgramModel program2 = ProgramBuilder.newDraftProgram("Program #2").build();
    programService.setSummaryImageFileKey(program1.id, "program-summary-image/program-1/test.jpg");
    programService.setSummaryImageFileKey(program2.id, "program-summary-image/program-2/test.jpg");
    // Don't publish the version so that the programs remain as drafts

    UnusedProgramImagesCleanupJob job =
        new UnusedProgramImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    job.run();

    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly(
            "program-summary-image/program-1/test.jpg", "program-summary-image/program-2/test.jpg");
  }

  @Test
  public void run_doesNotDeleteDraftAndActiveImageOfSameProgram() throws ProgramNotFoundException {
    // Create a program in the Active state with a program image
    ProgramModel program = ProgramBuilder.newDraftProgram("Program").build();
    programService.setSummaryImageFileKey(
        program.id, "program-summary-image/program-1/active-image.jpg");
    versionRepository.publishNewSynchronizedVersion();

    // Edit that program to have a new program image but don't publish it, so that a single
    // program has different images in its Draft and Active versions.
    ProgramModel draftProgram = programRepository.createOrUpdateDraft(program);
    programService.setSummaryImageFileKey(
        draftProgram.id, "program-summary-image/program-1/draft-image.jpg");

    UnusedProgramImagesCleanupJob job =
        new UnusedProgramImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    job.run();

    // Verify that the in-use keys include both the Active program image and the Draft program
    // image.
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly(
            "program-summary-image/program-1/active-image.jpg",
            "program-summary-image/program-1/draft-image.jpg");
  }

  @Test
  public void run_deletesDeletedImage_onlyAfterPublish() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("Program").build();
    programService.setSummaryImageFileKey(
        program.id, "program-summary-image/program-1/active-image.jpg");
    versionRepository.publishNewSynchronizedVersion();

    ProgramModel draftProgram = programRepository.createOrUpdateDraft(program);
    programService.deleteSummaryImageFileKey(draftProgram.id);

    // Before publishing the image deletion...
    UnusedProgramImagesCleanupJob jobBeforePublish =
        new UnusedProgramImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobBeforePublish.run();

    // ...verify that the old image is still listed as in-use.
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly("program-summary-image/program-1/active-image.jpg");

    // After publishing the image deletion...
    versionRepository.publishNewSynchronizedVersion();

    UnusedProgramImagesCleanupJob jobAfterPublish =
        new UnusedProgramImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobAfterPublish.run();

    // ...verify that the old image is no longer listed as in-use (so should be deleted).
    assertThat(fakePublicStorageClient.getLastInUseFileKeys()).isEmpty();
  }

  @Test
  public void run_deletesReplacedImage_onlyAfterPublish() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("Program").build();
    programService.setSummaryImageFileKey(
        program.id, "program-summary-image/program-1/active-image.jpg");
    versionRepository.publishNewSynchronizedVersion();

    ProgramModel draftProgram = programRepository.createOrUpdateDraft(program);
    programService.setSummaryImageFileKey(
        draftProgram.id, "program-summary-image/program-1/new-image.jpg");

    // Before publishing the image replacement...
    UnusedProgramImagesCleanupJob jobBeforePublish =
        new UnusedProgramImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobBeforePublish.run();

    // ...verify that the old image is still listed as in-use.
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly(
            "program-summary-image/program-1/active-image.jpg",
            "program-summary-image/program-1/new-image.jpg");

    // After publishing the image replacement...
    versionRepository.publishNewSynchronizedVersion();

    UnusedProgramImagesCleanupJob jobAfterPublish =
        new UnusedProgramImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobAfterPublish.run();

    // ...verify that the old image is no longer listed as in-use (so should be deleted).
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly("program-summary-image/program-1/new-image.jpg");
  }
}
