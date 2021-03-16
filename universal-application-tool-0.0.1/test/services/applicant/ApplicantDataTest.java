package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.Path;

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
    String testData = "{ \"applicant\": { \"testKey\": \"testValue\"}, \"metadata\": {}}";

    new EqualsTester()
        .addEqualityGroup(new ApplicantData(), new ApplicantData())
        .addEqualityGroup(new ApplicantData(testData), new ApplicantData(testData))
        .testEquals();
  }

  @Test
  public void preferredLocale_defaultsToEnglish() {
    ApplicantData data = new ApplicantData();
    assertThat(data.preferredLocale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void hasValueAtPath_pathNotFound_returnsFalse() {
    ApplicantData data = new ApplicantData();
    assertThat(data.hasValueAtPath(Path.create("nonexistent"))).isFalse();
  }

  @Test
  public void hasValueAtPath_pathExists_returnsTrue() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.text");
    data.putString(path, "hello");
    assertThat(data.hasValueAtPath(path)).isTrue();
  }

  @Test
  public void put_addsAScalar() {
    ApplicantData data = new ApplicantData();

    data.putInteger(Path.create("applicant.age"), 99);

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"age\":99},\"metadata\":{}}");
  }

  @Test
  public void put_addsANestedScalar() {
    ApplicantData data = new ApplicantData();
    String expected =
        "{\"applicant\":{\"favorites\":{\"food\":{\"apple\":\"Granny Smith\"}}},\"metadata\":{}}";

    data.putString(Path.create("applicant.favorites.food.apple"), "Granny Smith");

    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void put_addsAMap() {
    ApplicantData data = new ApplicantData();
    ImmutableMap<String, String> map = ImmutableMap.of("sandwich", "PB&J", "color", "blue");
    String expected =
        "{\"applicant\":{\"favorites\":{\"sandwich\":\"PB&J\",\"color\":\"blue\"}},\"metadata\":{}}";

    data.putObject(Path.create("applicant.favorites"), map);

    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void readString_findsCorrectValue() throws Exception {
    String testData = "{ \"applicant\": { \"favorites\": { \"color\": \"orange\"} } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.readString(Path.create("applicant.favorites.color"));

    assertThat(found).hasValue("orange");
  }

  @Test
  public void readString_pathNotPresent_returnsEmptyOptional() throws Exception {
    ApplicantData data = new ApplicantData();

    Optional<String> found = data.readString(Path.create("my.fake.path"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readString_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"number\": 27 } } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.readString(Path.create("applicant.object"));

    assertThat(found).isEmpty();
  }
}
