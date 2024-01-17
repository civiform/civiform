package durablejobs.jobs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import durablejobs.DurableJob;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import models.VersionModel;
import org.opensaml.xmlsec.encryption.Public;
import org.opensaml.xmlsec.signature.P;
import repository.VersionRepository;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/** A job that removes old program images that are no longer used. */
public final class UnusedProgramImagesCleanupJob extends DurableJob {
    private final PublicStorageClient publicStorageClient;
    private final VersionRepository versionRepository;
    private final PersistedDurableJobModel persistedDurableJob;

    public UnusedProgramImagesCleanupJob(PublicStorageClient publicStorageClient, VersionRepository versionRepository, PersistedDurableJobModel persistedDurableJob) {
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
        // Find all the program images currently used in all active & draft programs -- these are the files we should keep.
        ImmutableSet.Builder<String> validProgramImageFileKeys = ImmutableSet.builder();
        addFileKeysToList(validProgramImageFileKeys, versionRepository.getProgramsForVersion(versionRepository.getActiveVersion()));
        addFileKeysToList(validProgramImageFileKeys, versionRepository.getProgramsForVersion(versionRepository.getDraftVersion()));

        // Find all the program images currently in cloud storage.
        List<String> unusedProgramImages = new ArrayList<>(publicStorageClient.listPublicFiles());
        unusedProgramImages.removeAll(validProgramImageFileKeys.build());

        unusedProgramImages.forEach(publicStorageClient::deletePublicFile);
    }

    private void addFileKeysToList(ImmutableSet.Builder<String> fileKeys, ImmutableList<ProgramModel> programs) {
        for (ProgramModel program : programs) {
            Optional<String> fileKey = program.getProgramDefinition().summaryImageFileKey();
            fileKey.ifPresent(fileKeys::add);
        }
    }
}
