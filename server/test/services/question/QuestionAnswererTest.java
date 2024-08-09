package services.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;

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
  public void answerCurrencyQuestion() {
    Path path = Path.create("applicant.currency_cents");
    QuestionAnswerer.answerCurrencyQuestion(applicantData, path, "2.33");

    assertThat(applicantData.readCurrency(path.join(Scalar.CURRENCY_CENTS))).isPresent();
    assertThat(applicantData.readCurrency(path.join(Scalar.CURRENCY_CENTS)).get().getCents())
        .isEqualTo(233);
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

    assertThat(applicantData.readLongList(path.join(Scalar.SELECTIONS)))
        .contains(ImmutableList.of(5L, 6L));
  }

  @Test
  public void answerNameQuestion() {
    Path path = Path.create("applicant.name");
    QuestionAnswerer.answerNameQuestion(applicantData, path, "first", "middle", "last", "suffix");

    assertThat(applicantData.readString(path.join(Scalar.FIRST_NAME))).contains("first");
    assertThat(applicantData.readString(path.join(Scalar.MIDDLE_NAME))).contains("middle");
    assertThat(applicantData.readString(path.join(Scalar.LAST_NAME))).contains("last");
    assertThat(applicantData.readString(path.join(Scalar.NAME_SUFFIX))).contains("suffix");
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
  public void answerIdQuestion() {
    Path path = Path.create("applicant.id");
    QuestionAnswerer.answerIdQuestion(applicantData, path, "123");

    assertThat(applicantData.readString(path.join(Scalar.ID))).contains("123");
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
