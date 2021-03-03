package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.testing.EqualsTester;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
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

    data.put(Path.create("age"), 99);

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"age\":99},\"metadata\":{}}");
  }

  @Test
  public void put_addsANestedScalar() {
    ApplicantData data = new ApplicantData();

    data.put(Path.create("favorites.food.apple"), "Granny Smith");

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"favorites\":{\"food\":{\"apple\":\"Granny Smith\"}}},\"metadata\":{}}");
  }

  @Test
  public void put_addsAMap() {
    ApplicantData data = new ApplicantData();
    Map<String, String> favorites = Map.of("sandwich", "PB&J", "color", "blue");

    data.put(Path.create("favorites"), favorites);

    String expected =
    "{\"applicant\":{\"favorites\":{\"sandwich\":\"PB&J\",\"color\":\"blue\"}},\"metadata\":{}}";
    assertThat(data.asJsonString()).isEqualTo(expected);
  }

//  @Test
//  public void read_findsCorrectValue() throws Exception {
//    DocumentContext testData =
//        JsonPath.parse(
//            "{ \"applicant\": { \"favorites\": { \"color\": \"orange\"} }, \"metadata\": {}}");
//    ApplicantData data = new ApplicantData(testData);
//
//    Optional<String> found = data.read(Path.create("favorites.color"), String.class);
//
//    assertThat(found).hasValue("orange");
//  }
//
//  @Test
//  public void read_pathNotPresent_returnsEmptyOptional() throws Exception {
//    ApplicantData data = new ApplicantData();
//
//    Optional<String> found = data.read(Path.create("my.fake.path"), String.class);
//
//    assertThat(found).isEmpty();
//  }
//
//  @Test
//  public void read_valueIsWrongType_throwsException() {
//    ApplicantData data = new ApplicantData();
//    data.put(Path.create("favorite"), "orange");
//
//    assertThatThrownBy(() -> data.read(Path.create("favorite"), Integer.class))
//        .isInstanceOf(JsonPathTypeMismatchException.class)
//        .hasMessage("favorite does not have expected type class java.lang.Integer");
//  }
}
