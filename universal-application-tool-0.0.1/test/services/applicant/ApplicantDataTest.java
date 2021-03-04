package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.google.common.testing.EqualsTester;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
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
  public void put_addsAScalar() {
    ApplicantData data = new ApplicantData();

    data.put(Path.create("applicant.age"), 99);

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"age\":99},\"metadata\":{}}");
  }

  @Test
  public void put_addsANestedScalar() {
    ApplicantData data = new ApplicantData();
    String expected =
        "{\"applicant\":{\"favorites\":{\"food\":{\"apple\":\"Granny Smith\"}}},\"metadata\":{}}";

    data.put(Path.create("applicant.favorites.food.apple"), "Granny Smith");

    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void put_addsAMap() {
    ApplicantData data = new ApplicantData();
    // Use LinkedHashMap for predictable ordering so that string comparison is consistent.
    Map<String, String> favorites = new LinkedHashMap<>();
    favorites.put("sandwich", "PB&J");
    favorites.put("color", "blue");
    String expected =
        "{\"applicant\":{\"favorites\":{\"sandwich\":\"PB&J\",\"color\":\"blue\"}},\"metadata\":{}}";

    data.put(Path.create("applicant.favorites"), favorites);

    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void put_withStringPath_addsAScalar() {
    ApplicantData data = new ApplicantData();

    data.put("applicant.age", 99);

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"age\":99},\"metadata\":{}}");
  }

  @Test
  public void put_withStringPath_addsANestedScalar() {
    ApplicantData data = new ApplicantData();
    String expected =
        "{\"applicant\":{\"favorites\":{\"food\":{\"apple\":\"Granny Smith\"}}},\"metadata\":{}}";

    data.put(Path.create("applicant.favorites.food.apple"), "Granny Smith");

    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void read_findsCorrectValue() throws Exception {
    DocumentContext testData =
        JsonPath.parse("{ \"applicant\": { \"favorites\": { \"color\": \"orange\"} } }");
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.read(Path.create("applicant.favorites.color"), String.class);

    assertThat(found).hasValue("orange");
  }

  @Test
  public void read_pathNotPresent_returnsEmptyOptional() throws Exception {
    ApplicantData data = new ApplicantData();

    Optional<String> found = data.read(Path.create("my.fake.path"), String.class);

    assertThat(found).isEmpty();
  }

  @Test
  public void read_valueIsWrongType_throwsException() {
    DocumentContext testData =
        JsonPath.parse("{ \"applicant\": { \"favorites\": { \"color\": \"orange\"} } }");
    ApplicantData data = new ApplicantData(testData);

    assertThatThrownBy(() -> data.read(Path.create("applicant.favorites.color"), Integer.class))
        .isInstanceOf(JsonPathTypeMismatchException.class)
        .hasMessage(
            "applicant.favorites.color does not have expected type class java.lang.Integer");
  }

  @Test
  public void read_withStringPath_findsCorrectValue() throws Exception {
    DocumentContext testData =
        JsonPath.parse("{ \"applicant\": { \"favorites\": { \"color\": \"orange\"} } }");
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.read("applicant.favorites.color", String.class);

    assertThat(found).hasValue("orange");
  }

  @Test
  public void readString_findsStringValue() {
    DocumentContext testData = JsonPath.parse("{ \"applicant\": { \"color\": \"orange\"} }");
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.readString("applicant.color");

    assertThat(found).hasValue("orange");
  }

  @Test
  public void readString_returnsEmptyWhenTypeMismatch() {
    DocumentContext testData =
        JsonPath.parse("{ \"applicant\": { \"object\": { \"number\": 27 } } }");
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.readString("applicant.object");

    assertThat(found).isEmpty();
  }
}
