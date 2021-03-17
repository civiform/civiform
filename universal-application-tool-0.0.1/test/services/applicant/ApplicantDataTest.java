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
    assertThat(data.preferredLocale()).isEqualTo(Locale.US);
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
  public void putLong_convertsToString() {
    ApplicantData data = new ApplicantData();

    data.putLong(Path.create("applicant.snap_id"), 14628L);

    String expected = "{\"applicant\":{\"snap_id\":\"14628\"},\"metadata\":{}}";
    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void putUpdateMetadata_writesCorrectFormat() {
    ApplicantData data = new ApplicantData();

    data.putUpdateMetadata(Path.create("applicant.name"), 123L, 12345L);

    // The order of fields in JSON object is sometimes swapped; assert on containment instead of
    // equality.
    String expectedStart = "{\"applicant\":{},\"metadata\":{\"updates\":{\"applicant#name\":{";
    assertThat(data.asJsonString()).startsWith(expectedStart);
    assertThat(data.asJsonString()).contains("\"program_id\":\"123\"");
    assertThat(data.asJsonString()).contains("\"last_updated\":\"12345\"");
  }

  @Test
  public void readUpdateTimestamp() {
    Path questionPath = Path.create("applicant.name");
    String applicantJson =
        "{\"applicant\":{},\"metadata\":{\"updates\":{\"applicant#name\":{\"program_id\":\"123\",\"last_updated\":\"12345\"}}}}";
    ApplicantData data = new ApplicantData(applicantJson);

    Optional<Long> read = data.readUpdateTimestampForQuestionPath(questionPath);

    assertThat(read).hasValue(12345L);
  }

  @Test
  public void readUpdateProgramId() {
    Path questionPath = Path.create("applicant.name");
    String applicantJson =
        "{\"applicant\":{},\"metadata\":{\"updates\":{\"applicant#name\":{\"program_id\":\"123\",\"last_updated\":\"12345\"}}}}";
    ApplicantData data = new ApplicantData(applicantJson);

    Optional<Long> read = data.readProgramIdMetadataForQuestionPath(questionPath);

    assertThat(read).hasValue(123L);
  }

  @Test
  public void readString_findsCorrectValue() throws Exception {
    String testData = "{ \"applicant\": { \"favorites\": { \"color\": \"orange\"} } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.readString(Path.create("applicant.favorites.color"));

    assertThat(found).hasValue("orange");
  }

  @Test
  public void readInteger_findsCorrectValue() throws Exception {
    String testData = "{ \"applicant\": { \"age\": 30 } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<Integer> found = data.readInteger(Path.create("applicant.age"));

    assertThat(found).hasValue(30);
  }

  @Test
  public void readLong_findsCorrectValue() {
    String testData = "{ \"applicant\": { \"snap_id\": \"12345\" } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<Long> found = data.readLong(Path.create("applicant.snap_id"));

    assertThat(found).hasValue(12345L);
  }

  @Test
  public void readString_pathNotPresent_returnsEmptyOptional() throws Exception {
    ApplicantData data = new ApplicantData();

    Optional<String> found = data.readString(Path.create("my.fake.path"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readInteger_pathNotPresent_returnsEmptyOptional() throws Exception {
    ApplicantData data = new ApplicantData();

    Optional<Integer> found = data.readInteger(Path.create("my.fake.path"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readLong_pathNotPresent_returnsEmptyOptional() throws Exception {
    ApplicantData data = new ApplicantData();

    Optional<Long> found = data.readLong(Path.create("my.fake.path"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readString_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"number\": 27 } } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.readString(Path.create("applicant.object"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readInteger_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"name\": \"John\" } } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<Integer> found = data.readInteger(Path.create("applicant.object.name"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readLong_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"name\": \"John\" } } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<Long> found = data.readLong(Path.create("applicant.object.name"));

    assertThat(found).isEmpty();
  }
}
