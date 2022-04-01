package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import models.Application;
import models.Program;
import org.junit.Test;
import services.CfJsonDocumentContext;
import services.Path;

public class JsonExporterTest extends AbstractExporterTest {

  @Test
  public void testAllQuestionTypesWithoutEnumerators() throws Exception {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString = exporter.export(fakeProgram.getProgramDefinition());
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertLengthOf(3);
    testApplicationTopLevelAnswers(fakeProgram, resultAsserter, applicationOne, 2);
    resultAsserter.assertValueAtPath(2, ".applicant_name.first_name", "Alice");
    resultAsserter.assertNullValueAtPath(2, ".applicant_name.middle_name");
    resultAsserter.assertValueAtPath(2, ".applicant_name.last_name", "Appleton");
    resultAsserter.assertValueAtPath(2, ".applicant_birth_date.date", "01/01/1980");
    resultAsserter.assertValueAtPath(2, ".applicant_email_address.email", "one@example.com");
    resultAsserter.assertValueAtPath(2, ".applicant_address.zip", "54321");
    resultAsserter.assertValueAtPath(2, ".applicant_address.city", "city");
    resultAsserter.assertValueAtPath(2, ".applicant_address.street", "street st");
    resultAsserter.assertValueAtPath(2, ".applicant_address.state", "AB");
    resultAsserter.assertValueAtPath(2, ".applicant_address.line2", "apt 100");
    resultAsserter.assertValueAtPath(
        2, ".applicant_favorite_color.text", "Some Value \" containing ,,, special characters");
    resultAsserter.assertValueAtPath(2, ".applicant_monthly_income.currency_cents", 123456);
    resultAsserter.assertValueAtPath(
        2,
        ".applicant_file.file_key",
        "http://localhost:9000/admin/programs/" + fakeProgram.id + "/files/my-file-key");
    resultAsserter.assertValueAtPath(2, ".number_of_items_applicant_can_juggle.number", 123456);
    resultAsserter.assertValueAtPath(2, ".kitchen_tools.selections[0]", "toaster");
    resultAsserter.assertValueAtPath(2, ".kitchen_tools.selections[1]", "pepper grinder");
    resultAsserter.assertValueAtPath(2, ".applicant_ice_cream.selection", "strawberry");
    resultAsserter.assertValueAtPath(2, ".radio.selection", "winter");

    testApplicationTopLevelAnswers(fakeProgram, resultAsserter, applicationTwo, 1);
    resultAsserter.assertValueAtPath(1, ".applicant_name.first_name", "Alice");
    resultAsserter.assertNullValueAtPath(1, ".applicant_name.middle_name");
    resultAsserter.assertValueAtPath(1, ".applicant_name.last_name", "Appleton");
    resultAsserter.assertValueAtPath(1, ".applicant_birth_date.date", "01/01/1980");
    resultAsserter.assertValueAtPath(1, ".applicant_email_address.email", "one@example.com");
    resultAsserter.assertValueAtPath(1, ".applicant_address.zip", "54321");
    resultAsserter.assertValueAtPath(1, ".applicant_address.city", "city");
    resultAsserter.assertValueAtPath(1, ".applicant_address.street", "street st");
    resultAsserter.assertValueAtPath(1, ".applicant_address.state", "AB");
    resultAsserter.assertValueAtPath(1, ".applicant_address.line2", "apt 100");
    resultAsserter.assertValueAtPath(
        1, ".applicant_favorite_color.text", "Some Value \" containing ,,, special characters");
    resultAsserter.assertValueAtPath(1, ".applicant_monthly_income.currency_cents", 123456);
    resultAsserter.assertValueAtPath(
        1,
        ".applicant_file.file_key",
        "http://localhost:9000/admin/programs/" + fakeProgram.id + "/files/my-file-key");
    resultAsserter.assertValueAtPath(1, ".number_of_items_applicant_can_juggle.number", 123456);
    resultAsserter.assertValueAtPath(1, ".kitchen_tools.selections[0]", "toaster");
    resultAsserter.assertValueAtPath(1, ".kitchen_tools.selections[1]", "pepper grinder");
    resultAsserter.assertValueAtPath(1, ".applicant_ice_cream.selection", "strawberry");
    resultAsserter.assertValueAtPath(1, ".radio.selection", "winter");

    testApplicationTopLevelAnswers(fakeProgram, resultAsserter, applicationFour, 0);
    resultAsserter.assertValueAtPath(0, ".applicant_name.first_name", "Bob");
    resultAsserter.assertNullValueAtPath(0, ".applicant_name.middle_name");
    resultAsserter.assertValueAtPath(0, ".applicant_name.last_name", "Baker");
  }

  @Test
  public void testQuestionTypesWithEnumerators() throws Exception {
    createFakeProgramWithEnumerator();
    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString = exporter.export(fakeProgramWithEnumerator.getProgramDefinition());

    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);
    resultAsserter.assertLengthOf(3);

    testApplicationTopLevelAnswers(fakeProgramWithEnumerator, resultAsserter, applicationOne, 2);
    testApplicationTopLevelAnswers(fakeProgramWithEnumerator, resultAsserter, applicationTwo, 1);
    testApplicationTopLevelAnswers(fakeProgramWithEnumerator, resultAsserter, applicationThree, 0);
    resultAsserter.assertValueAtPath(0, ".applicant_name.first_name", "John");
    resultAsserter.assertNullValueAtPath(0, ".applicant_name.middle_name");
    resultAsserter.assertValueAtPath(0, ".applicant_name.last_name", "Doe");
    resultAsserter.assertValueAtPath(0, ".applicant_favorite_color.text", "brown");
    resultAsserter.assertNullValueAtPath(0, ".applicant_monthly_income.currency_cents");
    resultAsserter.assertValueAtPath(
        0, ".applicant_household_members[0].household_members_name.last_name", "Jameson");
    resultAsserter.assertNullValueAtPath(
        0, ".applicant_household_members[0].household_members_name.middle_name");
    resultAsserter.assertValueAtPath(
        0, ".applicant_household_members[0].household_members_name.first_name", "James");
    resultAsserter.assertValueAtPath(
        0,
        ".applicant_household_members[0].household_members_jobs[0].household_members_days_worked.number",
        111);
    resultAsserter.assertValueAtPath(
        0,
        ".applicant_household_members[0].household_members_jobs[1].household_members_days_worked.number",
        222);
    resultAsserter.assertValueAtPath(
        0,
        ".applicant_household_members[0].household_members_jobs[2].household_members_days_worked.number",
        333);
  }

  private void testApplicationTopLevelAnswers(
      Program program, ResultAsserter resultAsserter, Application application, int resultIndex) {
    resultAsserter.assertValueAtPath(
        "$[" + resultIndex + "].program_name", program.getProgramDefinition().adminName());
    resultAsserter.assertValueAtPath("$[" + resultIndex + "].program_version_id", program.id);
    resultAsserter.assertValueAtPath(
        "$[" + resultIndex + "].applicant_id", application.getApplicant().id);
    resultAsserter.assertValueAtPath("$[" + resultIndex + "].application_id", application.id);
    resultAsserter.assertValueAtPath("$[" + resultIndex + "].language", "en-US");
    resultAsserter.assertValueAtPath("$[" + resultIndex + "].submitter_email", "Applicant");
  }

  private static class ResultAsserter {
    public final CfJsonDocumentContext resultJson;

    ResultAsserter(String resultJsonString) {
      this.resultJson = new CfJsonDocumentContext(resultJsonString);
    }

    void assertLengthOf(int num) {
      assertThat((int) resultJson.getDocumentContext().read("$.length()")).isEqualTo(num);
    }

    void assertValueAtPath(String path, String value) {
      assertThat(resultJson.readString(Path.create(path)).get()).isEqualTo(value);
    }

    void assertValueAtPath(int resultNumber, String innerPath, String value) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.readString(path).get()).isEqualTo(value);
    }

    void assertValueAtPath(int resultNumber, String innerPath, int value) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.readLong(path).get()).isEqualTo(value);
    }

    void assertNullValueAtPath(int resultNumber, String innerPath) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.readString(path)).isEqualTo(Optional.empty());
    }

    void assertValueAtPath(String path, Long value) {
      assertThat(resultJson.readLong(Path.create(path)).get()).isEqualTo(value);
    }
  }
}
