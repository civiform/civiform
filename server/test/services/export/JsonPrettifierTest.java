package services.export;

import static org.assertj.core.api.Assertions.assertThat;
import static services.export.JsonPrettifier.asPrettyJsonString;

import org.junit.Test;
import services.applicant.JsonPathProvider;

public class JsonPrettifierTest {
  @Test
  public void asPrettyJsonString_prettyPrintsJsonString() {
    String testData = "{ \"deeply\": { \"nested\": { \"age\": 12 } } }";

    String prettyJson = asPrettyJsonString(testData);

    assertThat(prettyJson)
        .isEqualTo(
            """
            {
              "deeply\" : {
                "nested\" : {
                  "age\" : 12
                }
              }
            }""");
  }

  @Test
  public void asPrettyJsonString_prettyPrintsNullString() {
    String testData = "\"null\""; // A JSON document that is the string "null"

    String prettyJson = asPrettyJsonString(testData);

    assertThat(prettyJson).isEqualTo("\"null\"");
  }

  @Test
  public void asPrettyJsonString_prettyPrintsJsonObject() {
    String testData = "{ \"deeply\": { \"nested\": { \"age\": 12 } } }";
    Object jsonObject = JsonPathProvider.getJsonPath().parse(testData).json();

    String prettyJson = asPrettyJsonString(jsonObject);

    assertThat(prettyJson)
        .isEqualTo(
            """
            {
              "deeply" : {
                "nested" : {
                  "age" : 12
                }
              }
            }""");
  }

  @Test
  public void asPrettyJsonString_prettyPrintsNullValue() {
    Object testData = null; // A JSON document that is the value `null`

    String prettyJson = asPrettyJsonString(testData);

    assertThat(prettyJson).isEqualTo("null");
  }
}
