package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import durablejobs.DurableJob;
import java.util.Optional;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import repository.VersionRepository;
import services.cloud.PublicStorageClient;

/** A job that removes unused program images from cloud storage. */
public final class UnusedProgramImagesCleanupJob extends DurableJob {
  private final PublicStorageClient publicStorageClient;
  private final VersionRepository versionRepository;
  private final PersistedDurableJobModel persistedDurableJob;

  public UnusedProgramImagesCleanupJob(
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
    // All program images currently used in all active & draft programs are the
    // files we should keep.
    ImmutableSet.Builder<String> inUseProgramImageFileKeys = ImmutableSet.builder();
    addFileKeysToList(
        inUseProgramImageFileKeys,
        versionRepository.getProgramsForVersion(versionRepository.getActiveVersion()));
    addFileKeysToList(
        inUseProgramImageFileKeys,
        versionRepository.getProgramsForVersion(versionRepository.getDraftVersion()));

    publicStorageClient.prunePublicFileStorage(inUseProgramImageFileKeys.build());
  }

  private void addFileKeysToList(
      ImmutableSet.Builder<String> fileKeys, ImmutableList<ProgramModel> programs) {
    for (ProgramModel program : programs) {
      Optional<String> fileKey = program.getProgramDefinition().summaryImageFileKey();
      fileKey.ifPresent(fileKeys::add);
    }
  }
}
