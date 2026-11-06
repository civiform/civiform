package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import durablejobs.DurableJobName;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import models.JobType;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import models.QuestionModel;
import org.junit.Test;
import repository.ProgramRepository;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.StaticContentQuestionDefinition;
import support.ProgramBuilder;
import support.cloud.FakePublicStorageClient;

public class UnusedPublicImagesCleanupJobTest extends ResetPostgres {
  FakePublicStorageClient fakePublicStorageClient = new FakePublicStorageClient();
  ProgramRepository programRepository = instanceOf(ProgramRepository.class);
  ProgramService programService = instanceOf(ProgramService.class);
  QuestionService questionService = instanceOf(QuestionService.class);
  VersionRepository versionRepository = instanceOf(VersionRepository.class);
  PersistedDurableJobModel jobModel =
      new PersistedDurableJobModel(
          DurableJobName.UNUSED_PROGRAM_IMAGES_CLEANUP.toString(),
          JobType.RECURRING,
          Instant.ofEpochMilli(1000));

  private QuestionModel createDraftQuestion(String name) {
    QuestionDefinition definition =
        new StaticContentQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name + "-" + UUID.randomUUID())
                .setDescription("static content description")
                .setQuestionText(LocalizedStrings.withDefaultValue("Static content text"))
                .setQuestionHelpText(LocalizedStrings.withDefaultValue("Static content help text"))
                .build());
    QuestionModel question = new QuestionModel(definition);
    question.addVersion(versionRepository.getDraftVersionOrCreate());
    question.save();
    return question;
  }

  @Test
  public void getPersistedDurableJob_isJobModel() {
    UnusedPublicImagesCleanupJob job =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);

    assertThat(job.getPersistedDurableJob()).isEqualTo(jobModel);
  }

  @Test
  public void run_doesNotDeleteActiveProgramImages() throws ProgramNotFoundException {
    ProgramModel program1 = ProgramBuilder.newDraftProgram("Program #1").build();
    ProgramModel program2 = ProgramBuilder.newDraftProgram("Program #2").build();
    programService.setSummaryImageFileKey(program1.id, "program-summary-image/program-1/test.jpg");
    programService.setSummaryImageFileKey(program2.id, "program-summary-image/program-2/test.jpg");
    versionRepository.publishNewSynchronizedVersion();

    UnusedPublicImagesCleanupJob job =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
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

    UnusedPublicImagesCleanupJob job =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
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

    UnusedPublicImagesCleanupJob job =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
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
    UnusedPublicImagesCleanupJob jobBeforePublish =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobBeforePublish.run();

    // ...verify that the old image is still listed as in-use.
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly("program-summary-image/program-1/active-image.jpg");

    // After publishing the image deletion...
    versionRepository.publishNewSynchronizedVersion();

    UnusedPublicImagesCleanupJob jobAfterPublish =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
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
    UnusedPublicImagesCleanupJob jobBeforePublish =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobBeforePublish.run();

    // ...verify that the old image is still listed as in-use.
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly(
            "program-summary-image/program-1/active-image.jpg",
            "program-summary-image/program-1/new-image.jpg");

    // After publishing the image replacement...
    versionRepository.publishNewSynchronizedVersion();

    UnusedPublicImagesCleanupJob jobAfterPublish =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobAfterPublish.run();

    // ...verify that the old image is no longer listed as in-use (so should be deleted).
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly("program-summary-image/program-1/new-image.jpg");
  }

  @Test
  public void run_doesNotDeleteActiveQuestionImages() throws Exception {
    QuestionModel question1 = createDraftQuestion("question-1");
    QuestionModel question2 = createDraftQuestion("question-2");
    questionService.setImageFileKeyAndDescription(
        question1.id,
        Optional.of("question-image/question-1/test.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "alt text 1");
    questionService.setImageFileKeyAndDescription(
        question2.id,
        Optional.of("question-image/question-2/test.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "alt text 2");
    versionRepository.publishNewSynchronizedVersion();

    UnusedPublicImagesCleanupJob job =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    job.run();

    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly(
            "question-image/question-1/test.jpg", "question-image/question-2/test.jpg");
  }

  @Test
  public void run_doesNotDeleteDraftQuestionImages() throws Exception {
    QuestionModel question1 = createDraftQuestion("question-1");
    QuestionModel question2 = createDraftQuestion("question-2");
    questionService.setImageFileKeyAndDescription(
        question1.id,
        Optional.of("question-image/question-1/test.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "alt text 1");
    questionService.setImageFileKeyAndDescription(
        question2.id,
        Optional.of("question-image/question-2/test.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "alt text 2");

    UnusedPublicImagesCleanupJob job =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    job.run();

    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly(
            "question-image/question-1/test.jpg", "question-image/question-2/test.jpg");
  }

  @Test
  public void run_doesNotDeleteDraftAndActiveImageOfSameQuestion() throws Exception {
    QuestionModel question = createDraftQuestion("question");
    questionService.setImageFileKeyAndDescription(
        question.id,
        Optional.of("question-image/question-1/active-image.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "active alt text");
    versionRepository.publishNewSynchronizedVersion();

    // Update the question in the draft version with a new image without publishing
    questionService.setImageFileKeyAndDescription(
        question.id,
        Optional.of("question-image/question-1/draft-image.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "draft alt text");

    UnusedPublicImagesCleanupJob job =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    job.run();

    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly(
            "question-image/question-1/active-image.jpg",
            "question-image/question-1/draft-image.jpg");
  }

  @Test
  public void run_deletesDeletedQuestionImage_onlyAfterPublish() throws Exception {
    QuestionModel question = createDraftQuestion("question");
    questionService.setImageFileKeyAndDescription(
        question.id,
        Optional.of("question-image/question-1/active-image.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "active alt text");
    versionRepository.publishNewSynchronizedVersion();

    questionService.deleteImageFromQuestion(question.id);

    // Before publishing the image deletion...
    UnusedPublicImagesCleanupJob jobBeforePublish =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobBeforePublish.run();

    // ...verify that the old image is still listed as in-use.
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly("question-image/question-1/active-image.jpg");

    // After publishing the image deletion...
    versionRepository.publishNewSynchronizedVersion();

    UnusedPublicImagesCleanupJob jobAfterPublish =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobAfterPublish.run();

    // ...verify that the old image is no longer listed as in-use (so should be deleted).
    assertThat(fakePublicStorageClient.getLastInUseFileKeys()).isEmpty();
  }

  @Test
  public void run_deletesReplacedQuestionImage_onlyAfterPublish() throws Exception {
    QuestionModel question = createDraftQuestion("question");
    questionService.setImageFileKeyAndDescription(
        question.id,
        Optional.of("question-image/question-1/active-image.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "active alt text");
    versionRepository.publishNewSynchronizedVersion();

    questionService.setImageFileKeyAndDescription(
        question.id,
        Optional.of("question-image/question-1/new-image.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "new alt text");

    // Before publishing the image replacement...
    UnusedPublicImagesCleanupJob jobBeforePublish =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobBeforePublish.run();

    // ...verify that both the active and draft images are listed as in-use.
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly(
            "question-image/question-1/active-image.jpg",
            "question-image/question-1/new-image.jpg");

    // After publishing the image replacement...
    versionRepository.publishNewSynchronizedVersion();

    UnusedPublicImagesCleanupJob jobAfterPublish =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobAfterPublish.run();

    // ...verify that the old image is no longer listed as in-use (so should be deleted).
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly("question-image/question-1/new-image.jpg");
  }

  @Test
  public void run_deletesArchivedQuestionImage_onlyAfterPublish() throws Exception {
    // Keep at least one program in the version so publishing an archived question is valid.
    ProgramBuilder.newDraftProgram("Program").build();
    QuestionModel question = createDraftQuestion("question");
    questionService.setImageFileKeyAndDescription(
        question.id,
        Optional.of("question-image/question-1/active-image.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "active alt text");
    versionRepository.publishNewSynchronizedVersion();

    questionService.archiveQuestion(question.id);

    // Before publishing the question archival...
    UnusedPublicImagesCleanupJob jobBeforePublish =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobBeforePublish.run();

    // ...verify that the image is still listed as in-use while the question is still active.
    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly("question-image/question-1/active-image.jpg");

    // After publishing the question archival...
    versionRepository.publishNewSynchronizedVersion();

    UnusedPublicImagesCleanupJob jobAfterPublish =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    jobAfterPublish.run();

    // ...verify that the archived question's image is no longer listed as in-use.
    assertThat(fakePublicStorageClient.getLastInUseFileKeys()).isEmpty();
  }

  @Test
  public void run_keepsBothProgramAndQuestionImages() throws Exception {
    ProgramModel program = ProgramBuilder.newDraftProgram("Program").build();
    programService.setSummaryImageFileKey(
        program.id, "program-summary-image/program-1/program.jpg");

    QuestionModel question = createDraftQuestion("question");
    questionService.setImageFileKeyAndDescription(
        question.id,
        Optional.of("question-image/question-1/question.jpg"),
        LocalizedStrings.DEFAULT_LOCALE,
        "question alt text");
    versionRepository.publishNewSynchronizedVersion();

    UnusedPublicImagesCleanupJob job =
        new UnusedPublicImagesCleanupJob(fakePublicStorageClient, versionRepository, jobModel);
    job.run();

    assertThat(fakePublicStorageClient.getLastInUseFileKeys())
        .containsExactly(
            "program-summary-image/program-1/program.jpg",
            "question-image/question-1/question.jpg");
  }
}
