package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.Optional;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.Path;
import services.applicant.ApplicantData;
import services.question.types.QuestionType;
import support.ProgramBuilder;
import support.QuestionAnswerer;

public class JsonExporterTest extends ResetPostgres {
  private Program fakeProgram;
  private ImmutableList<Question> fakeQuestions;

  private void answerQuestion(
      QuestionType questionType,
      Question question,
      ApplicantData applicantDataOne,
      ApplicantData applicantDataTwo) {
    Path answerPath =
        question
            .getQuestionDefinition()
            .getContextualizedPath(Optional.empty(), ApplicantData.APPLICANT_PATH);
    switch (questionType) {
      case ADDRESS:
        QuestionAnswerer.answerAddressQuestion(
            applicantDataOne, answerPath, "street st", "apt 100", "city", "AB", "54321");
        // applicant two did not answer this question.
        break;
      case CHECKBOX:
        QuestionAnswerer.answerMultiSelectQuestion(applicantDataOne, answerPath, 0, 1L);
        QuestionAnswerer.answerMultiSelectQuestion(applicantDataOne, answerPath, 1, 2L);
        // applicant two did not answer this question.
        break;
      case CURRENCY:
        QuestionAnswerer.answerCurrencyQuestion(applicantDataOne, answerPath, "1,234.56");
        break;
      case DATE:
        QuestionAnswerer.answerDateQuestion(applicantDataOne, answerPath, "1980-01-01");
        // applicant two did not answer this question.
        break;
      case DROPDOWN:
        QuestionAnswerer.answerSingleSelectQuestion(applicantDataOne, answerPath, 2L);
        // applicant two did not answer this question.
        break;
      case EMAIL:
        QuestionAnswerer.answerEmailQuestion(applicantDataOne, answerPath, "one@example.com");
        // applicant two did not answer this question.
        break;
      case FILEUPLOAD:
        QuestionAnswerer.answerFileQuestion(applicantDataOne, answerPath, "my-file-key");
        // applicant two did not answer this question.
        break;
      case ID:
        QuestionAnswerer.answerIdQuestion(applicantDataOne, answerPath, "012");
        QuestionAnswerer.answerIdQuestion(applicantDataTwo, answerPath, "123");
        break;
      case NAME:
        QuestionAnswerer.answerNameQuestion(applicantDataOne, answerPath, "Alice", "", "Appleton");
        QuestionAnswerer.answerNameQuestion(applicantDataTwo, answerPath, "Bob", "", "Baker");
        break;
      case NUMBER:
        QuestionAnswerer.answerNumberQuestion(applicantDataOne, answerPath, "123456");
        // applicant two did not answer this question.
        break;
      case RADIO_BUTTON:
        QuestionAnswerer.answerSingleSelectQuestion(applicantDataOne, answerPath, 1L);
        // applicant two did not answer this question.
        break;
      case ENUMERATOR:
        QuestionAnswerer.answerEnumeratorQuestion(
            applicantDataOne, answerPath, ImmutableList.of("item1", "item2"));
        // applicant two did not answer this question.
        break;
      case TEXT:
        QuestionAnswerer.answerTextQuestion(
            applicantDataOne, answerPath, "Some Value \" containing ,,, special characters");
        // applicant two did not answer this question.
        break;
      case STATIC:
        // Do nothing.
        break;
    }
  }

  private void createFakeApplications() {
    Applicant fakeApplicantOne = new Applicant();
    Applicant fakeApplicantTwo = new Applicant();
    testQuestionBank.getSampleQuestionsForAllTypes().entrySet().stream()
        .forEach(
            entry ->
                answerQuestion(
                    entry.getKey(),
                    entry.getValue(),
                    fakeApplicantOne.getApplicantData(),
                    fakeApplicantTwo.getApplicantData()));
    fakeApplicantOne.save();
    fakeApplicantTwo.save();
    new Application(fakeApplicantOne, fakeProgram, LifecycleStage.ACTIVE)
        .setSubmitTimeToNow()
        .save();
    new Application(fakeApplicantOne, fakeProgram, LifecycleStage.OBSOLETE)
        .setSubmitTimeToNow()
        .save();
    new Application(fakeApplicantOne, fakeProgram, LifecycleStage.DRAFT)
        .setSubmitTimeToNow()
        .save();
    new Application(fakeApplicantTwo, fakeProgram, LifecycleStage.ACTIVE)
        .setSubmitTimeToNow()
        .save();
  }

  private void createFakeQuestions() {
    this.fakeQuestions =
        testQuestionBank.getSampleQuestionsForAllTypes().values().stream()
            .sorted(Comparator.comparing(question -> question.getQuestionDefinition().getName()))
            .collect(ImmutableList.toImmutableList());
  }

  public void createFakeProgram() {
    ProgramBuilder fakeProgram = ProgramBuilder.newActiveProgram();
    fakeQuestions.forEach(
        question -> fakeProgram.withBlock().withRequiredQuestion(question).build());

    this.fakeProgram = fakeProgram.build();
  }

  @Before
  public void setup() {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();
  }

  @Test
  public void addingToAray() throws Exception {
    JsonExporter exporter = instanceOf(JsonExporter.class);

    String result = exporter.export(fakeProgram);
    assertThat(result).isEqualTo("[]");
  }
}
