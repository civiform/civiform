package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
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
  public void hasPath_returnsTrueForExistingPath() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.school");
    data.putString(path, "Elementary School");

    assertThat(data.hasPath(path)).isTrue();
  }

  @Test
  public void hasPath_returnsFalseForMissingPath() {
    ApplicantData data = new ApplicantData();

    assertThat(data.hasPath(Path.create("I_don't_exist!"))).isFalse();
  }

  @Test
  public void hasValueAtPath_returnsTrueIfValuePresent() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.horses");
    data.putLong(path, 278);

    assertThat(data.hasValueAtPath(path)).isTrue();
  }

  @Test
  public void hasValueAtPath_returnsFalseForNull() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.horses");
    data.putLong(path, "");

    assertThat(data.hasValueAtPath(path)).isFalse();
  }

  @Test
  public void hasValueAtPath_returnsFalseForMissingPath() {
    ApplicantData data = new ApplicantData();

    assertThat(data.hasValueAtPath(Path.create("not_here!"))).isFalse();
  }

  @Test
  public void putScalarAtRoot() {
    ApplicantData data = new ApplicantData();

    data.putString(Path.create("root"), "value");

    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{},\"metadata\":{},\"root\":\"value\"}");
  }

  @Test
  public void putNestedPathAtRoot() {
    ApplicantData data = new ApplicantData();

    data.putString(Path.create("new.path.at.root"), "hooray");

    assertThat(data.asJsonString())
        .isEqualTo(
            "{\"applicant\":{},\"metadata\":{},\"new\":{\"path\":{\"at\":{\"root\":\"hooray\"}}}}");
  }

  @Test
  public void putLong_addsAScalar() {
    ApplicantData data = new ApplicantData();

    data.putLong(Path.create("applicant.age"), 99);

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"age\":99},\"metadata\":{}}");
  }

  @Test
  public void putString_addsANestedScalar() {
    ApplicantData data = new ApplicantData();
    String expected =
        "{\"applicant\":{\"favorites\":{\"food\":{\"apple\":\"Granny Smith\"}}},\"metadata\":{}}";

    data.putString(Path.create("applicant.favorites.food.apple"), "Granny Smith");

    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void putString_writesNullIfStringIsEmpty() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.name");
    String expected = "{\"applicant\":{\"name\":null},\"metadata\":{}}";

    data.putString(path, "");

    assertThat(data.asJsonString()).isEqualTo(expected);
    assertThat(data.readString(path)).isEmpty();
  }

  @Test
  public void putLong_writesNullIfStringIsEmpty() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.age");
    String expected = "{\"applicant\":{\"age\":null},\"metadata\":{}}";

    data.putLong(path, "");

    assertThat(data.asJsonString()).isEqualTo(expected);
    assertThat(data.readLong(path)).isEmpty();
  }

  @Test
  public void putList_writesListAsString() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.favorite_fruits");

    data.putList(path, ImmutableList.of("apple", "orange"));

    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"favorite_fruits\":\"apple`orange\"},\"metadata\":{}}");
  }

  @Test
  public void putList_writesNullIfListIsEmpty() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.favorite_fruits");

    data.putList(path, ImmutableList.of());

    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"favorite_fruits\":null},\"metadata\":{}}");
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

  @Test
  public void readLong_findsCorrectValue() throws Exception {
    String testData = "{ \"applicant\": { \"age\": 30 } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<Long> found = data.readLong(Path.create("applicant.age"));

    assertThat(found).hasValue(30L);
  }

  @Test
  public void readLong_pathNotPresent_returnsEmptyOptional() throws Exception {
    ApplicantData data = new ApplicantData();

    Optional<Long> found = data.readLong(Path.create("my.fake.path"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readLong_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"name\": \"John\" } } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<Long> found = data.readLong(Path.create("applicant.object.name"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readList_findsCorrectValue() {
    String testData = "{\"applicant\":{\"favorite_fruits\":\"apple`orange\"}}";
    ApplicantData data = new ApplicantData(testData);

    Optional<ImmutableList<String>> found = data.readList(Path.create("applicant.favorite_fruits"));

    assertThat(found).hasValue(ImmutableList.of("apple", "orange"));
  }

  @Test
  public void readListWithOneValue_findsCorrectValue() {
    String testData = "{\"applicant\":{\"favorite_fruits\":\"apple\"}}";
    ApplicantData data = new ApplicantData(testData);

    Optional<ImmutableList<String>> found = data.readList(Path.create("applicant.favorite_fruits"));

    assertThat(found).hasValue(ImmutableList.of("apple"));
  }

  @Test
  public void readList_pathNotPresent_returnsEmptyOptional() {
    ApplicantData data = new ApplicantData();

    Optional<ImmutableList<String>> found = data.readList(Path.create("not.here"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readList_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"age\": 12 } } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<ImmutableList<String>> found = data.readList(Path.create("applicant.object.name"));

    assertThat(found).isEmpty();
  }
}
