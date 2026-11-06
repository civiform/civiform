package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import durablejobs.DurableJob;
import java.util.Optional;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import models.QuestionModel;
import repository.VersionRepository;
import services.cloud.PublicStorageClient;

/** A job that removes unused program and question images from public cloud storage. */
public final class UnusedPublicImagesCleanupJob extends DurableJob {
  private final PublicStorageClient publicStorageClient;
  private final VersionRepository versionRepository;
  private final PersistedDurableJobModel persistedDurableJob;

  public UnusedPublicImagesCleanupJob(
      PublicStorageClient publicStorageClient,
      VersionRepository versionRepository,
      PersistedDurableJobModel persistedDurableJob) {
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.versionRepository = checkNotNull(versionRepository);
    this.persistedDurableJob = checkNotNull(persistedDurableJob);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    // All program and question images currently used in all active & draft versions are the
    // files we should keep.
    ImmutableSet.Builder<String> inUseImageFileKeys = ImmutableSet.builder();
    addProgramFileKeysToList(
        inUseImageFileKeys,
        versionRepository.getProgramsForVersion(versionRepository.getActiveVersion()));
    addProgramFileKeysToList(
        inUseImageFileKeys,
        versionRepository.getProgramsForVersion(versionRepository.getDraftVersion()));
    addQuestionFileKeysToList(
        inUseImageFileKeys,
        versionRepository.getQuestionsForVersion(versionRepository.getActiveVersion()));
    addQuestionFileKeysToList(
        inUseImageFileKeys,
        versionRepository.getQuestionsForVersion(versionRepository.getDraftVersion()));

    publicStorageClient.prunePublicFileStorage(inUseImageFileKeys.build());
  }

  private void addProgramFileKeysToList(
      ImmutableSet.Builder<String> fileKeys, ImmutableList<ProgramModel> programs) {
    for (ProgramModel program : programs) {
      Optional<String> fileKey = program.getProgramDefinition().summaryImageFileKey();
      fileKey.ifPresent(fileKeys::add);
    }
  }

  private void addQuestionFileKeysToList(
      ImmutableSet.Builder<String> fileKeys, ImmutableList<QuestionModel> questions) {
    for (QuestionModel question : questions) {
      Optional<String> fileKey = question.getQuestionDefinition().getImageFileKey();
      fileKey.ifPresent(fileKeys::add);
    }
  }
}
