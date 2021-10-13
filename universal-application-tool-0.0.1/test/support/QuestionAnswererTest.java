package support;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;

@RunWith(JUnitParamsRunner.class)
public class QuestionAnswererTest {

  ApplicantData applicantData;

  @Before
  public void freshApplicantData() {
    applicantData = new ApplicantData();
  }

  @Test
  public void answerAddressQuestion() {
    Path path = Path.create("applicant.address");
    QuestionAnswerer.answerAddressQuestion(
        applicantData, path, "street", "line 2", "city", "state", "zip");

    assertThat(applicantData.readString(path.join(Scalar.STREET))).contains("street");
    assertThat(applicantData.readString(path.join(Scalar.CITY))).contains("city");
    assertThat(applicantData.readString(path.join(Scalar.STATE))).contains("state");
  }

  @Test
  @Parameters({
      // Single digit dollars.
      "0, 0", // Zero
      "0.00, 0", // Zero with cents
      "0.45, 45", // Only cents
      "1, 100", // Single dollars
      "1.23, 123", // Single dollars with cents.
      // Large values
      "12345, 1234500",
      "12\\,345, 1234500", // With comma
      "12345.67, 1234567", // With cents.
      "12\\,345.67, 1234567" // With comma and cents.
  })
  public void answerCurrencyQuestion(String dollars, Long cents) {
    Path path = Path.create("applicant.currency_cents");
    QuestionAnswerer.answerFileQuestion(applicantData, path, "file key");

    assertThat(applicantData.readString(path.join(Scalar.FILE_KEY))).contains("file key");
  }

  @Test
  public void answerFileQuestion() {
    Path path = Path.create("applicant.file");
    QuestionAnswerer.answerFileQuestion(applicantData, path, "file key");

    assertThat(applicantData.readString(path.join(Scalar.FILE_KEY))).contains("file key");
  }

  @Test
  public void answerMultiSelectQuestion() {
    Path path = Path.create("applicant.multi");
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, path, 0, 5L);
    QuestionAnswerer.answerMultiSelectQuestion(applicantData, path, 1, 6L);

    assertThat(applicantData.readList(path.join(Scalar.SELECTIONS)))
        .contains(ImmutableList.of(5L, 6L));
  }

  @Test
  public void answerNameQuestion() {
    Path path = Path.create("applicant.name");
    QuestionAnswerer.answerNameQuestion(applicantData, path, "first", "middle", "last");

    assertThat(applicantData.readString(path.join(Scalar.FIRST_NAME))).contains("first");
    assertThat(applicantData.readString(path.join(Scalar.MIDDLE_NAME))).contains("middle");
    assertThat(applicantData.readString(path.join(Scalar.LAST_NAME))).contains("last");
  }

  @Test
  public void answerNumberQuestion() {
    Path path = Path.create("applicant.number");
    QuestionAnswerer.answerNumberQuestion(applicantData, path, 5);

    assertThat(applicantData.readLong(path.join(Scalar.NUMBER))).contains(5L);
  }

  @Test
  public void answerNumberQuestion_string() {
    Path path = Path.create("applicant.number");
    QuestionAnswerer.answerNumberQuestion(applicantData, path, "5");

    assertThat(applicantData.readLong(path.join(Scalar.NUMBER))).contains(5L);
  }

  @Test
  public void answerSingleSelectQuestion() {
    Path path = Path.create("applicant.single");
    QuestionAnswerer.answerSingleSelectQuestion(applicantData, path, 5L);

    assertThat(applicantData.readLong(path.join(Scalar.SELECTION))).contains(5L);
  }

  @Test
  public void answerTextQuestion() {
    Path path = Path.create("applicant.text");
    QuestionAnswerer.answerTextQuestion(applicantData, path, "text");

    assertThat(applicantData.readString(path.join(Scalar.TEXT))).contains("text");
  }

  @Test
  public void addMetadata() {
    Path path = Path.create("applicant.address");
    QuestionAnswerer.addMetadata(applicantData, path, 5L, 12345L);

    assertThat(applicantData.readLong(path.join(Scalar.PROGRAM_UPDATED_IN))).contains(5L);
    assertThat(applicantData.readLong(path.join(Scalar.UPDATED_AT))).contains(12345L);
  }
}
