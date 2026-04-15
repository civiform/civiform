package repository;

import static org.assertj.core.api.Assertions.assertThat;

import auth.StoredFileAcls;
import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.util.List;
import models.StoredFileModel;
import org.junit.Before;
import org.junit.Test;
import services.cloud.ApplicantFileNameFormatter;
import support.ProgramBuilder;

public class StoredFileRepositoryTest extends ResetPostgres {

  private StoredFileRepository repo;
  private StoredFileModel file;

  @Before
  public void setUp() {
    repo = instanceOf(StoredFileRepository.class);
    file = new StoredFileModel().setName("file name");
  }

  @Test
  public void insert() {
    repo.insert(file).toCompletableFuture().join();

    long id = file.id;
    file = repo.lookupFile(id).toCompletableFuture().join().get();
    assertThat(file.id).isEqualTo(id);
    assertThat(file.getName()).isEqualTo("file name");
  }

  @Test
  public void update_aclChangesArePersisted() {
    file.save();
    file.getAcls()
        .addProgramToReaders(ProgramBuilder.newDraftProgram("program-one").buildDefinition());

    repo.update(file).toCompletableFuture().join();

    file = repo.lookupFile(file.id).toCompletableFuture().join().get();
    assertThat(file.getAcls().getProgramReadAcls()).containsOnly("program-one");
  }

  @Test
  public void lookupFiles() {
    file.save();
    var fileTwo = new StoredFileModel().setName("file-two");
    fileTwo.save();

    List<StoredFileModel> result =
        repo.lookupFiles(ImmutableList.of(file.getName(), fileTwo.getName()))
            .toCompletableFuture()
            .join();

    assertThat(result).containsOnly(file, fileTwo);
  }

  @Test
  public void lookupFile() {
    file.save();

    StoredFileModel result = repo.lookupFile(file.getName()).toCompletableFuture().join().get();

    assertThat(result).isEqualTo(file);
  }

  @Test
  public void lookupFilesByApplicant() {
    // Verify only Applicant 1 is returned and does not substring match on 10.
    long applicantId = 1L;
    long supersetApplicantId = 10L;

    StoredFileModel applicantFile1 =
        new StoredFileModel()
            .setName(
                ApplicantFileNameFormatter.formatFileUploadQuestionFilename(
                    applicantId, /* programId= */ 20L, /* blockId= */ "5"));
    applicantFile1.save();

    StoredFileModel applicantFile2 =
        new StoredFileModel()
            .setName(
                ApplicantFileNameFormatter.formatFileUploadQuestionFilename(
                    applicantId, /* programId= */ 30L, /* blockId= */ "6"));
    applicantFile2.save();

    StoredFileModel supersetApplicantFile =
        new StoredFileModel()
            .setName(
                ApplicantFileNameFormatter.formatFileUploadQuestionFilename(
                    supersetApplicantId, /* programId= */ 20L, /* blockId= */ "5"));
    supersetApplicantFile.save();

    List<StoredFileModel> result =
        repo.lookupFilesByApplicant(applicantId).toCompletableFuture().join();

    assertThat(result).containsExactlyInAnyOrder(applicantFile1, applicantFile2);
  }

  @Test
  public void lookupFilesByApplicant_noFiles_returnsEmpty() {
    List<StoredFileModel> result =
        repo.lookupFilesByApplicant(/* applicantId= */ 99L).toCompletableFuture().join();

    assertThat(result).isEmpty();
  }

  /**
   * This is a regression test to ensure that files created before the migration to stored file ACLs
   * can be successfully loaded.
   */
  @Test
  public void lookFile_fileHasDefaultAclsValue_doesNotThrowException() {
    var fileName = "default_acls_value_file";
    Database database = DB.getDefault();
    database
        .sqlUpdate("INSERT INTO files(name, acls) VALUES (:name, '{}')")
        .setParameter("name", fileName)
        .execute();

    StoredFileModel result = repo.lookupFile(fileName).toCompletableFuture().join().get();

    assertThat(result.getName()).isEqualTo(fileName);
    assertThat(result.getAcls()).isInstanceOf(StoredFileAcls.class);
  }
}
