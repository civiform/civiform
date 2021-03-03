package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.testing.EqualsTester;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.Optional;
import org.junit.Test;

public class ApplicantDataTest {
  @Test
  public void createdTime() {
    ApplicantData applicantData = new ApplicantData();
    // Just an arbitrary time.
    Instant i = Instant.ofEpochMilli(10000000L);

    applicantData.setCreatedTime(i);

    assertThat(applicantData.getCreatedTime()).isEqualTo(i);
  }

  @Test
  public void equality() {
    DocumentContext testData =
        JsonPath.parse("{ \"applicant\": { \"testKey\": \"testValue\"}, \"metadata\": {}}");

    new EqualsTester()
        .addEqualityGroup(new ApplicantData(), new ApplicantData())
        .addEqualityGroup(new ApplicantData(testData), new ApplicantData(testData))
        .testEquals();
  }

  @Test
  public void read_findsCorrectValue() throws Exception {
    ApplicantData data = new ApplicantData();
    data.put(Path.create("color"), "blue");
    data.put(Path.create("sandwich"), "PB&J");

    Optional<String> found = data.read(Path.create("sandwich"), String.class);

    assertThat(found).hasValue("PB&J");
  }

  @Test
  public void read_pathNotPresent_returnsEmptyOptional() throws Exception {
    ApplicantData data = new ApplicantData();

    Optional<String> found = data.read(Path.create("my.fake.path"), String.class);

    assertThat(found).isEmpty();
  }

  @Test
  public void read_valueIsWrongType_throwsException() {
    ApplicantData data = new ApplicantData();
    data.put(Path.create("favorite"), "orange");

    assertThatThrownBy(() -> data.read(Path.create("favorite"), Integer.class))
        .isInstanceOf(JsonPathTypeMismatchException.class)
        .hasMessage("favorite does not have expected type class java.lang.Integer");
  }
}
