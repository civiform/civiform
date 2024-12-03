package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Optional;
import models.ApplicantModel;
import models.ApplicationModel;
import models.JobType;
import models.LifecycleStage;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import org.junit.Test;
import repository.ResetPostgres;
import services.applicant.ApplicantData;
import services.applicant.RepeatedEntity;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramQuestionDefinition;
import services.question.QuestionAnswerer;
import services.question.types.EnumeratorQuestionDefinition;
import support.ProgramBuilder;

public class CopyFileKeyForMultipleFileUploadTest extends ResetPostgres {
  @Test
  public void run_copiesFileKey() {
    ProgramModel program =
        ProgramBuilder.newActiveProgram("program")
            .withBlock("block1")
            .withOptionalQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();
    ProgramQuestionDefinition programFileUpload =
        ProgramQuestionDefinition.create(
            testQuestionBank.fileUploadApplicantFile().getQuestionDefinition(),
            Optional.of(program.id));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicantQuestion applicantFileUploadQuestion =
        new ApplicantQuestion(
            programFileUpload, applicant, applicant.getApplicantData(), Optional.empty());

    QuestionAnswerer.answerFileQuestion(
        applicant.getApplicantData(),
        applicantFileUploadQuestion.getContextualizedPath(),
        "applicantFile.jpg");
    applicant.save();

    runJob();

    applicant.refresh();
    ApplicantQuestion latestProgramFileUpload =
        new ApplicantQuestion(
            programFileUpload, applicant, applicant.getApplicantData(), Optional.empty());
    assertOldAndNewDataEquals(latestProgramFileUpload, "applicantFile.jpg");
  }

  @Test
  public void run_doesntModifyUnansweredQuestions() {
    ProgramModel program =
        ProgramBuilder.newActiveProgram("program")
            .withBlock("block1")
            .withOptionalQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();
    ProgramQuestionDefinition programFileUpload =
        ProgramQuestionDefinition.create(
            testQuestionBank.fileUploadApplicantFile().getQuestionDefinition(),
            Optional.of(program.id));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicantData applicantDataBeforeJob = applicant.getApplicantData();

    runJob();

    applicant.refresh();
    ApplicantQuestion latestProgramFileUpload =
        new ApplicantQuestion(
            programFileUpload, applicant, applicant.getApplicantData(), Optional.empty());
    assertOldAndNewDataUnanswered(latestProgramFileUpload);

    assertThat(applicant.getApplicantData().asJsonString())
        .isEqualTo(applicantDataBeforeJob.asJsonString());
  }

  @Test
  public void run_doesntOverwriteNewFormat() {
    ProgramModel program =
        ProgramBuilder.newActiveProgram("program")
            .withBlock("block1")
            .withOptionalQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();
    ProgramQuestionDefinition programFileUpload =
        ProgramQuestionDefinition.create(
            testQuestionBank.fileUploadApplicantFile().getQuestionDefinition(),
            Optional.of(program.id));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();

    // This shouldn't happen anywhere but a staging environment, but make sure that if we have
    // existing data in the new format, we don't overwrite it.
    ApplicantQuestion applicantFileUploadQuestion =
        new ApplicantQuestion(
            programFileUpload, applicant, applicant.getApplicantData(), Optional.empty());
    QuestionAnswerer.answerFileQuestion(
        applicant.getApplicantData(),
        applicantFileUploadQuestion.getContextualizedPath(),
        "oldData.jpg");
    QuestionAnswerer.answerFileQuestionWithMultipleUpload(
        applicant.getApplicantData(),
        applicantFileUploadQuestion.getContextualizedPath(),
        ImmutableList.of("newData.jpg"));

    applicant.save();

    runJob();

    applicant.refresh();
    ApplicantQuestion latestProgramFileUpload =
        new ApplicantQuestion(
            programFileUpload, applicant, applicant.getApplicantData(), Optional.empty());
    assertThat(latestProgramFileUpload.createFileUploadQuestion().getFileKeyValue().get())
        .isEqualTo("oldData.jpg");
    assertThat(latestProgramFileUpload.createFileUploadQuestion().getFileKeyListValue().get())
        .containsExactly("newData.jpg");
  }

  @Test
  public void run_copiesFileKeyForNestedEnumerators() {
    ProgramModel program =
        ProgramBuilder.newActiveProgram("program")
            .withBlock("household")
            .withOptionalQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
            .withRepeatedBlock("jobs")
            .withOptionalQuestion(testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs())
            .withRepeatedBlock("fileuploads")
            .withOptionalQuestion(testQuestionBank.fileUploadRepeatedHouseholdMemberFile())
            .build();

    ProgramQuestionDefinition programEnumerator =
        ProgramQuestionDefinition.create(
            testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition(),
            Optional.of(program.id));
    ProgramQuestionDefinition programNestedEnumerator =
        ProgramQuestionDefinition.create(
            testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs().getQuestionDefinition(),
            Optional.of(program.id));
    ProgramQuestionDefinition programFileUpload =
        ProgramQuestionDefinition.create(
            testQuestionBank.fileUploadApplicantFile().getQuestionDefinition(),
            Optional.of(program.id));

    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();

    ApplicantQuestion enumeratorQuestion =
        new ApplicantQuestion(
            programEnumerator, applicant, applicant.getApplicantData(), Optional.empty());
    ApplicantQuestion nestedEnumeratorQuestion =
        new ApplicantQuestion(
            programNestedEnumerator, applicant, applicant.getApplicantData(), Optional.empty());

    // Answer enumerator question
    QuestionAnswerer.answerEnumeratorQuestion(
        applicant.getApplicantData(),
        enumeratorQuestion.getContextualizedPath(),
        ImmutableList.of("enumerator"));

    // Answer nested enumerator
    RepeatedEntity enumeratorEntity =
        RepeatedEntity.createRepeatedEntities(
                (EnumeratorQuestionDefinition) enumeratorQuestion.getQuestionDefinition(),
                Optional.empty(),
                applicant.getApplicantData())
            .get(0);

    ApplicantQuestion enumeratorEntityQuestion =
        new ApplicantQuestion(
            programNestedEnumerator,
            applicant,
            applicant.getApplicantData(),
            Optional.of(enumeratorEntity));
    QuestionAnswerer.answerEnumeratorQuestion(
        applicant.getApplicantData(),
        enumeratorEntityQuestion.getContextualizedPath(),
        ImmutableList.of("nested"));

    // Answer repeated file question.
    RepeatedEntity nestedEntity =
        enumeratorEntity
            .createNestedRepeatedEntities(
                (EnumeratorQuestionDefinition) nestedEnumeratorQuestion.getQuestionDefinition(),
                Optional.empty(),
                applicant.getApplicantData())
            .get(0);

    ApplicantQuestion nestedFiledUploadEntityQuestion =
        new ApplicantQuestion(
            programFileUpload, applicant, applicant.getApplicantData(), Optional.of(nestedEntity));

    QuestionAnswerer.answerFileQuestion(
        applicant.getApplicantData(),
        nestedFiledUploadEntityQuestion.getContextualizedPath(),
        "entity1File.jpg");

    applicant.save();

    runJob();

    applicant.refresh();

    RepeatedEntity latestEnumeratorEntity =
        RepeatedEntity.createRepeatedEntities(
                (EnumeratorQuestionDefinition) enumeratorQuestion.getQuestionDefinition(),
                Optional.empty(),
                applicant.getApplicantData())
            .get(0);

    RepeatedEntity latestNestedEntity =
        enumeratorEntity
            .createNestedRepeatedEntities(
                (EnumeratorQuestionDefinition) nestedEnumeratorQuestion.getQuestionDefinition(),
                Optional.empty(),
                applicant.getApplicantData())
            .get(0);
    ApplicantQuestion latestFileUpload =
        new ApplicantQuestion(
            programFileUpload,
            applicant,
            applicant.getApplicantData(),
            Optional.of(latestNestedEntity));

    assertOldAndNewDataEquals(latestFileUpload, "entity1File.jpg");
  }

  @Test
  public void
      run_copiesFileKeyForApplicantAndApplicationsWithEnumeratorAndNonEnumeratorQuestions() {
    // Create programs
    ProgramModel program1 =
        ProgramBuilder.newActiveProgram("program")
            .withBlock("block1")
            .withOptionalQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();
    ProgramModel program2 =
        ProgramBuilder.newActiveProgram("program2")
            .withBlock("block1")
            .withOptionalQuestion(testQuestionBank.fileUploadApplicantFile())
            .withBlock("repeated")
            .withOptionalQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
            .withRepeatedBlock("fileUploadRepeated")
            .withOptionalQuestion(testQuestionBank.fileUploadRepeatedHouseholdMemberFile())
            .build();

    ProgramQuestionDefinition program1FileUpload =
        ProgramQuestionDefinition.create(
            testQuestionBank.fileUploadApplicantFile().getQuestionDefinition(),
            Optional.of(program1.id));
    ProgramQuestionDefinition program2Enumerator =
        ProgramQuestionDefinition.create(
            testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition(),
            Optional.of(program2.id));
    ProgramQuestionDefinition program2FileUploadRepeated =
        ProgramQuestionDefinition.create(
            testQuestionBank.fileUploadRepeatedHouseholdMemberFile().getQuestionDefinition(),
            Optional.of(program2.id));

    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicantQuestion applicantFileUploadQuestion =
        new ApplicantQuestion(
            program1FileUpload, applicant, applicant.getApplicantData(), Optional.empty());
    ApplicantQuestion applicantEnumeratorQuestion =
        new ApplicantQuestion(
            program2Enumerator, applicant, applicant.getApplicantData(), Optional.empty());

    // Answer first file upload question.
    QuestionAnswerer.answerFileQuestion(
        applicant.getApplicantData(),
        applicantFileUploadQuestion.getContextualizedPath(),
        "applicantFile.jpg");

    // Answer enumerator question
    QuestionAnswerer.answerEnumeratorQuestion(
        applicant.getApplicantData(),
        applicantEnumeratorQuestion.getContextualizedPath(),
        ImmutableList.of("entity1", "entity2", "entity3"));

    // Answer repeated file question.
    ImmutableList<RepeatedEntity> applicantEntities =
        RepeatedEntity.createRepeatedEntities(
            (EnumeratorQuestionDefinition) program2Enumerator.getQuestionDefinition(),
            Optional.empty(),
            applicant.getApplicantData());
    ApplicantQuestion applicantEntity1FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicant.getApplicantData(),
            Optional.of(applicantEntities.get(0)));
    ApplicantQuestion applicantEntity3FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicant.getApplicantData(),
            Optional.of(applicantEntities.get(2)));
    QuestionAnswerer.answerFileQuestion(
        applicant.getApplicantData(),
        applicantEntity1FileUpload.getContextualizedPath(),
        "entity1File.jpg");
    QuestionAnswerer.answerFileQuestion(
        applicant.getApplicantData(),
        applicantEntity3FileUpload.getContextualizedPath(),
        "entity3File.jpg");

    applicant.save();

    ApplicationModel applicantApplicationProgram1 =
        new ApplicationModel(applicant, program1, LifecycleStage.ACTIVE);
    applicantApplicationProgram1.setApplicantData(applicant.getApplicantData());

    applicantApplicationProgram1.save();

    ApplicationModel applicantApplicationProgram2 =
        new ApplicationModel(applicant, program1, LifecycleStage.ACTIVE);
    applicantApplicationProgram2.setApplicantData(applicant.getApplicantData());

    applicantApplicationProgram2.save();

    runJob();

    applicant.refresh();
    applicantApplicationProgram1.refresh();
    applicantApplicationProgram2.refresh();

    // Assert applicant's data is correct, redefining applicant questions to get the latest data.
    ApplicantQuestion latestProgram1FileUpload =
        new ApplicantQuestion(
            program1FileUpload, applicant, applicant.getApplicantData(), Optional.empty());
    ImmutableList<RepeatedEntity> latestEntities =
        RepeatedEntity.createRepeatedEntities(
            (EnumeratorQuestionDefinition) program2Enumerator.getQuestionDefinition(),
            Optional.empty(),
            applicant.getApplicantData());
    ApplicantQuestion latestApplicantEntity1FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicant.getApplicantData(),
            Optional.of(latestEntities.get(0)));
    ApplicantQuestion latestApplicantEntity2FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicant.getApplicantData(),
            Optional.of(latestEntities.get(1)));
    ApplicantQuestion latestApplicantEntity3FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicant.getApplicantData(),
            Optional.of(latestEntities.get(2)));

    assertOldAndNewDataEquals(latestProgram1FileUpload, "applicantFile.jpg");
    assertOldAndNewDataEquals(latestApplicantEntity1FileUpload, "entity1File.jpg");
    assertOldAndNewDataUnanswered(latestApplicantEntity2FileUpload);
    assertOldAndNewDataEquals(latestApplicantEntity3FileUpload, "entity3File.jpg");

    // Same asserts, but now for the submitted application.
    ApplicantQuestion applicationProgram1FileUpload =
        new ApplicantQuestion(
            program1FileUpload,
            applicant,
            applicantApplicationProgram1.getApplicantData(),
            Optional.empty());
    ImmutableList<RepeatedEntity> applicationEntities =
        RepeatedEntity.createRepeatedEntities(
            (EnumeratorQuestionDefinition) program2Enumerator.getQuestionDefinition(),
            Optional.empty(),
            applicantApplicationProgram2.getApplicantData());
    ApplicantQuestion applicationEntity1FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicantApplicationProgram2.getApplicantData(),
            Optional.of(applicationEntities.get(0)));
    ApplicantQuestion applicationEntity2FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicantApplicationProgram2.getApplicantData(),
            Optional.of(applicationEntities.get(1)));
    ApplicantQuestion applicationEntity3FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicantApplicationProgram2.getApplicantData(),
            Optional.of(applicationEntities.get(2)));

    assertOldAndNewDataEquals(applicationProgram1FileUpload, "applicantFile.jpg");
    assertOldAndNewDataEquals(applicationEntity1FileUpload, "entity1File.jpg");
    assertOldAndNewDataUnanswered(applicationEntity2FileUpload);
    assertOldAndNewDataEquals(applicationEntity3FileUpload, "entity3File.jpg");

    // Just to be thorough, let's make sure the enumerator data is also in the program1 application
    // (even though it's not used)
    ImmutableList<RepeatedEntity> program1ApplicationEntities =
        RepeatedEntity.createRepeatedEntities(
            (EnumeratorQuestionDefinition) program2Enumerator.getQuestionDefinition(),
            Optional.empty(),
            applicantApplicationProgram1.getApplicantData());
    ApplicantQuestion program1ApplicationEntity1FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicantApplicationProgram1.getApplicantData(),
            Optional.of(program1ApplicationEntities.get(0)));
    ApplicantQuestion program1ApplicationEntity2FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicantApplicationProgram1.getApplicantData(),
            Optional.of(program1ApplicationEntities.get(1)));
    ApplicantQuestion program1ApplicationEntity3FileUpload =
        new ApplicantQuestion(
            program2FileUploadRepeated,
            applicant,
            applicantApplicationProgram1.getApplicantData(),
            Optional.of(program1ApplicationEntities.get(2)));

    assertOldAndNewDataEquals(program1ApplicationEntity1FileUpload, "entity1File.jpg");
    assertOldAndNewDataUnanswered(program1ApplicationEntity2FileUpload);
    assertOldAndNewDataEquals(program1ApplicationEntity3FileUpload, "entity3File.jpg");
  }

  @Test
  public void run_copiesFileKeyForSingleApplicantWithMultipleApplications() {
    ProgramModel program =
        ProgramBuilder.newActiveProgram("program")
            .withBlock("block1")
            .withOptionalQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();

    ProgramQuestionDefinition programFileUpload =
        ProgramQuestionDefinition.create(
            testQuestionBank.fileUploadApplicantFile().getQuestionDefinition(),
            Optional.of(program.id));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicantQuestion applicantFileUploadQuestion =
        new ApplicantQuestion(
            programFileUpload, applicant, applicant.getApplicantData(), Optional.empty());

    QuestionAnswerer.answerFileQuestion(
        applicant.getApplicantData(),
        applicantFileUploadQuestion.getContextualizedPath(),
        "application1.jpg");
    applicant.save();
    applicant.refresh();

    ApplicationModel application1 =
        new ApplicationModel(applicant, program, LifecycleStage.OBSOLETE);
    application1.setApplicantData(applicant.getApplicantData());
    application1.save();

    QuestionAnswerer.answerFileQuestion(
        applicant.getApplicantData(), applicantFileUploadQuestion.getContextualizedPath(), "");
    applicant.save();
    applicant.refresh();

    ApplicationModel application2 = new ApplicationModel(applicant, program, LifecycleStage.ACTIVE);
    application2.setApplicantData(applicant.getApplicantData());
    application2.save();

    QuestionAnswerer.answerFileQuestion(
        applicant.getApplicantData(),
        applicantFileUploadQuestion.getContextualizedPath(),
        "application3.jpg");
    applicant.save();
    applicant.refresh();

    ApplicationModel application3 = new ApplicationModel(applicant, program, LifecycleStage.DRAFT);
    application3.setApplicantData(applicant.getApplicantData());
    application3.save();

    runJob();

    applicant.refresh();
    application1.refresh();
    application2.refresh();
    application3.refresh();
    assertOldAndNewDataEquals(
        new ApplicantQuestion(
            programFileUpload, applicant, application1.getApplicantData(), Optional.empty()),
        "application1.jpg");
    assertOldAndNewDataUnanswered(
        new ApplicantQuestion(
            programFileUpload, applicant, application2.getApplicantData(), Optional.empty()));
    assertOldAndNewDataEquals(
        new ApplicantQuestion(
            programFileUpload, applicant, application3.getApplicantData(), Optional.empty()),
        "application3.jpg");
    assertOldAndNewDataEquals(
        new ApplicantQuestion(
            programFileUpload, applicant, applicant.getApplicantData(), Optional.empty()),
        "application3.jpg");
  }

  @Test
  public void run_copiesFileKeyForMultipleApplicants() {
    ProgramModel program =
        ProgramBuilder.newActiveProgram("program")
            .withBlock("block1")
            .withOptionalQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();

    ProgramQuestionDefinition programFileUpload =
        ProgramQuestionDefinition.create(
            testQuestionBank.fileUploadApplicantFile().getQuestionDefinition(),
            Optional.of(program.id));
    ApplicantModel applicant1 = resourceCreator.insertApplicantWithAccount();
    ApplicantQuestion applicant1FileUploadQuestion =
        new ApplicantQuestion(
            programFileUpload, applicant1, applicant1.getApplicantData(), Optional.empty());

    QuestionAnswerer.answerFileQuestion(
        applicant1.getApplicantData(),
        applicant1FileUploadQuestion.getContextualizedPath(),
        "applicant1.jpg");
    applicant1.save();
    applicant1.refresh();

    ApplicationModel applicant1Application =
        new ApplicationModel(applicant1, program, LifecycleStage.ACTIVE);
    applicant1Application.setApplicantData(applicant1.getApplicantData());
    applicant1Application.save();

    ApplicantModel applicant2 = resourceCreator.insertApplicantWithAccount();
    ApplicantQuestion applicant2FileUploadQuestion =
        new ApplicantQuestion(
            programFileUpload, applicant2, applicant2.getApplicantData(), Optional.empty());

    QuestionAnswerer.answerFileQuestion(
        applicant2.getApplicantData(),
        applicant2FileUploadQuestion.getContextualizedPath(),
        "applicant2.jpg");
    applicant2.save();
    applicant2.refresh();

    ApplicationModel applicant2Application =
        new ApplicationModel(applicant2, program, LifecycleStage.ACTIVE);
    applicant2Application.setApplicantData(applicant2.getApplicantData());
    applicant2Application.save();

    runJob();

    applicant1.refresh();
    applicant1Application.refresh();
    applicant2Application.refresh();
    applicant2.refresh();
    assertOldAndNewDataEquals(
        new ApplicantQuestion(
            programFileUpload, applicant1, applicant1.getApplicantData(), Optional.empty()),
        "applicant1.jpg");
    assertOldAndNewDataEquals(
        new ApplicantQuestion(
            programFileUpload, applicant2, applicant2.getApplicantData(), Optional.empty()),
        "applicant2.jpg");

    assertOldAndNewDataEquals(
        new ApplicantQuestion(
            programFileUpload,
            applicant1,
            applicant1Application.getApplicantData(),
            Optional.empty()),
        "applicant1.jpg");
    assertOldAndNewDataEquals(
        new ApplicantQuestion(
            programFileUpload,
            applicant2,
            applicant2Application.getApplicantData(),
            Optional.empty()),
        "applicant2.jpg");
  }

  private static void assertOldAndNewDataUnanswered(ApplicantQuestion question) {
    assertThat(question.createFileUploadQuestion().getFileKeyValue()).isEmpty();
    assertThat(question.createFileUploadQuestion().getFileKeyListValue()).isEmpty();
  }

  private static void assertOldAndNewDataEquals(ApplicantQuestion question, String fileName) {
    assertThat(question.createFileUploadQuestion().getFileKeyValue().get()).isEqualTo(fileName);
    assertThat(question.createFileUploadQuestion().getFileKeyListValue().get())
        .containsExactly(fileName);
  }

  private void runJob() {
    CopyFileKeyForMultipleFileUpload job =
        new CopyFileKeyForMultipleFileUpload(
            new PersistedDurableJobModel("fake-job", JobType.RUN_ONCE, Instant.now()));

    job.run();
  }
}
