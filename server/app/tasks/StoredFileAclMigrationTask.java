package tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import io.ebean.DB;
import io.ebean.Database;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import models.Application;
import models.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.ApplicationRepository;
import repository.StoredFileRepository;
import services.applicant.ReadOnlyApplicantProgramServiceImpl;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/**
 * Migrates {@link StoredFile} to ensure they have correct {@link auth.StoredFileAcls} by looping
 * through all submitted applications, inspecting their files, and adding ACLs if they are missing
 * for the program the application is associated with. This task is idempotent and can be run
 * multiple times.
 *
 * <p>This can be deleted after the data in Seattle production has been migrated.
 */
public class StoredFileAclMigrationTask implements Runnable {
  private final ApplicationRepository applicationRepository;
  private final String baseUrl;
  private final Database database;
  private final ProgramService programService;
  private final StoredFileRepository storedFileRepository;

  private static final Logger LOGGER = LoggerFactory.getLogger(StoredFileAclMigrationTask.class);

  @Inject
  public StoredFileAclMigrationTask(
      ApplicationRepository applicationRepository,
      Config configuration,
      ProgramService programService,
      StoredFileRepository storedFileRepository) {
    this.applicationRepository = checkNotNull(applicationRepository);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.database = DB.getDefault();
    this.programService = checkNotNull(programService);
    this.storedFileRepository = checkNotNull(storedFileRepository);
  }

  @Override
  public void run() {
    // Using a counter class here because Java does not allow lambdas to mutate variables
    // declared in a parent scope.
    var counter = new Counter();

    HashMap<Long, ProgramDefinition> programDefinitions = new HashMap<>();

    applicationRepository.forEachSubmittedApplication(
        (Application application) -> {
          try {
            addAclsToApplicationFiles(counter, programDefinitions, application);
          } catch (ProgramNotFoundException | RuntimeException e) {
            counter.incrementErrorCount();
            LOGGER.error(String.format("StoredFileMigrationError: %s", e.toString()));
          }
        });

    LOGGER.info(
        String.format(
            "StoredFileMigrationCompleted: migrated %d files, caught %d exceptions",
            counter.getUpdatedFileCount(), counter.getErrorCount()));
  }

  private void addAclsToApplicationFiles(
      Counter counter, HashMap<Long, ProgramDefinition> programDefinitions, Application application)
      throws ProgramNotFoundException {
    Long programId = application.getProgram().id;
    if (!programDefinitions.containsKey(programId)) {
      programDefinitions.put(programId, programService.getProgramDefinition(programId));
    }
    ProgramDefinition programDefinition = programDefinitions.get(programId);

    var service =
        new ReadOnlyApplicantProgramServiceImpl(
            application.getApplicantData(), programDefinition, baseUrl);

    List<StoredFile> files =
        storedFileRepository.lookupFiles(service.getStoredFileKeys()).toCompletableFuture().join();

    files.forEach(
        file -> {
          if (!file.getAcls().hasProgramReadPermission(programDefinition)) {
            file.getAcls().addProgramToReaders(programDefinition);
            database.update(file);
            counter.incrementUpdatedFileCount();
          }
        });
  }

  private static class Counter {
    private int updatedFileCount = 0;
    private int errorCount = 0;

    public void incrementUpdatedFileCount() {
      updatedFileCount++;
    }

    public int getUpdatedFileCount() {
      return updatedFileCount;
    }

    public void incrementErrorCount() {
      errorCount++;
    }

    public int getErrorCount() {
      return errorCount;
    }
  }
}
