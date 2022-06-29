package tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import models.Applicant;
import models.Program;
import models.StoredFile;
import org.junit.Before;
import org.junit.Test;
import repository.ApplicationRepository;
import repository.ResetPostgres;
import services.applicant.ApplicantData;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;
import support.QuestionAnswerer;

public class StoredFileAclMigrationTaskTest extends ResetPostgres {

  private ApplicationRepository applicationRepository;
  private StoredFileAclMigrationTask storedFileAclMigrationTask;
  private static String fileKey = "file-key";

  @Before
  public void setUp() {
    applicationRepository = instanceOf(ApplicationRepository.class);
    storedFileAclMigrationTask = instanceOf(StoredFileAclMigrationTask.class);
  }

  @Test
  public void run() {
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    QuestionDefinition fileQuestionDefinition =
        testQuestionBank.applicantFile().getQuestionDefinition();
    Program program =
        ProgramBuilder.newDraftProgram("My Program")
            .withLocalizedName(Locale.GERMAN, "Mein Programm")
            .withBlock("Block one")
            .withBlock("file-one")
            .withRequiredQuestionDefinition(fileQuestionDefinition)
            .build();

    QuestionAnswerer.answerFileQuestion(
        applicant.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(fileQuestionDefinition.getQuestionPathSegment()),
        fileKey);
    applicant.save();

    var storedFile = new StoredFile().setName(fileKey);
    storedFile.save();

    applicationRepository
        .submitApplication(applicant, program, Optional.empty())
        .toCompletableFuture()
        .join();

    storedFile.refresh();
    assertThat(storedFile.getAcls().hasProgramReadPermission(program.getProgramDefinition()))
        .isFalse();

    storedFileAclMigrationTask.run();

    storedFile.refresh();
    assertThat(storedFile.getAcls().hasProgramReadPermission(program.getProgramDefinition()))
        .isTrue();
  }
}
