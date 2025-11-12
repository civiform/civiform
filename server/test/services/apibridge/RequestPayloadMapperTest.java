package services.apibridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.ApiBridgeConfigurationModel.ApiBridgeDefinitionItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;
import services.question.YesNoQuestionOption;

@RunWith(JUnitParamsRunner.class)
public class RequestPayloadMapperTest extends ResetPostgres {
  private static final String schema =
      """
      {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "https://example.com/simple-schema.json",
        "title": "Simple Data Types Schema",
        "description": "A JSON Schema with one example of each core data type",
        "type": "object",
        "properties": {
          "stringExample": {
            "type": "string",
            "description": "Basic string type"
          },
          "numberExample": {
            "type": "number",
            "description": "Number type (integer or decimal)"
          },
          "integerExample": {
            "type": "integer",
            "description": "Integer type only"
          },
          "booleanExample": {
            "type": "boolean",
            "description": "Boolean true/false value"
          },
          "nullExample": {
            "type": "null",
            "description": "Null value"
          },
          "arrayExample": {
            "type": "array",
            "items": {
              "type": "string"
            },
            "description": "Array of strings"
          },
          "objectExample": {
            "type": "object",
            "properties": {
              "name": { "type": "string" },
              "age": { "type": "integer" }
            },
            "description": "Object with defined properties"
          }
        },
        "additionalProperties": false
      }
      """;

  private RequestPayloadMapper requestPayloadMapper;

  @Before
  public void setup() {
    requestPayloadMapper = instanceOf(RequestPayloadMapper.class);
  }

  private Object[] validStringValues() {
    return new Object[] {
      new Object[] {"stringExample", Scalar.ZIP, "99999"},
      new Object[] {"stringExample", Scalar.CITY, "city"},
      new Object[] {"stringExample", Scalar.COUNTRY_CODE, "001"},
      new Object[] {"stringExample", Scalar.EMAIL, "test@localhost.localdomain"},
      new Object[] {"stringExample", Scalar.FIRST_NAME, "fname"},
      new Object[] {"stringExample", Scalar.ID, "W12345"},
      new Object[] {"stringExample", Scalar.LAST_NAME, "lname"},
      new Object[] {"stringExample", Scalar.LINE2, "ste 123"},
      new Object[] {"stringExample", Scalar.MIDDLE_NAME, "mname"},
      new Object[] {"stringExample", Scalar.NAME_SUFFIX, "Jr."},
      new Object[] {"stringExample", Scalar.PHONE_NUMBER, "555-555-5555"},
      new Object[] {"stringExample", Scalar.SELECTION, "yes"},
      new Object[] {"stringExample", Scalar.STATE, "WA"},
      new Object[] {"stringExample", Scalar.STREET, "123 Main St"},
      new Object[] {"stringExample", Scalar.TEXT, "lorem ipsum"}
    };
  }

  @Test
  @Parameters(method = "validStringValues")
  public void map_strings_successfully(String propertyName, Scalar scalar, String value) {
    var applicantData = new ApplicantData();
    applicantData.putString(Path.create("applicant.questionname").join(scalar), value);

    var inputFields =
        ImmutableList.of(new ApiBridgeDefinitionItem("questionname", scalar, propertyName));
    var requestPayload = requestPayloadMapper.map(applicantData, schema, inputFields);

    assertThat(requestPayload)
        .isNotNull()
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(propertyName, value));
  }

  private Object[] validNumberValues() {
    return new Object[] {
      new Object[] {"numberExample", Scalar.NUMBER, 123L},
      new Object[] {"numberExample", Scalar.DAY, 12L},
      new Object[] {"numberExample", Scalar.MONTH, 20L},
      new Object[] {"numberExample", Scalar.YEAR, 2001L},
      new Object[] {"numberExample", Scalar.TIMESTAMP, 1755178892000L}
    };
  }

  @Test
  @Parameters(method = "validNumberValues")
  public void map_numbers_successfully(String propertyName, Scalar scalar, Long value) {
    var applicantData = new ApplicantData();
    applicantData.putLong(Path.create("applicant.questionname").join(scalar), value);

    var inputFields =
        ImmutableList.of(new ApiBridgeDefinitionItem("questionname", scalar, propertyName));
    var requestPayload = requestPayloadMapper.map(applicantData, schema, inputFields);

    assertThat(requestPayload)
        .isNotNull()
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(propertyName, value));
  }

  private Object[] validDateValues() {
    return new Object[] {new Object[] {"stringExample", Scalar.DATE, "2025-08-14"}};
  }

  @Test
  @Parameters(method = "validDateValues")
  public void map_date_successfully(String propertyName, Scalar scalar, String value) {
    var applicantData = new ApplicantData();
    applicantData.putDate(Path.create("applicant.questionname").join(scalar), value);

    var inputFields =
        ImmutableList.of(new ApiBridgeDefinitionItem("questionname", scalar, propertyName));
    var requestPayload = requestPayloadMapper.map(applicantData, schema, inputFields);

    assertThat(requestPayload)
        .isNotNull()
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(propertyName, value));
  }

  private Object[] validDoubleValues() {
    return new Object[] {
      new Object[] {"numberExample", Scalar.LATITUDE, 10.123},
      new Object[] {"numberExample", Scalar.LONGITUDE, 20.456}
    };
  }

  @Test
  @Parameters(method = "validDoubleValues")
  public void map_double_successfully(String propertyName, Scalar scalar, Double value) {
    var applicantData = new ApplicantData();
    applicantData.putDouble(Path.create("applicant.questionname").join(scalar), value);

    var inputFields =
        ImmutableList.of(new ApiBridgeDefinitionItem("questionname", scalar, propertyName));
    var requestPayload = requestPayloadMapper.map(applicantData, schema, inputFields);

    assertThat(requestPayload)
        .isNotNull()
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(propertyName, value));
  }

  private Object[] validCurrencyValues() {
    return new Object[] {new Object[] {"numberExample", Scalar.CURRENCY_CENTS, "10.58"}};
  }

  @Test
  @Parameters(method = "validCurrencyValues")
  public void map_currency_successfully(String propertyName, Scalar scalar, String value) {
    var applicantData = new ApplicantData();
    applicantData.putCurrencyDollars(Path.create("applicant.questionname").join(scalar), value);

    var inputFields =
        ImmutableList.of(new ApiBridgeDefinitionItem("questionname", scalar, propertyName));
    var requestPayload = requestPayloadMapper.map(applicantData, schema, inputFields);

    assertThat(requestPayload)
        .isNotNull()
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(propertyName, Double.valueOf(value)));
  }

  private Object[] validBooleanValues() {
    return new Object[] {
      new Object[] {"booleanExample", Scalar.SELECTION, YesNoQuestionOption.YES, true},
      new Object[] {"booleanExample", Scalar.SELECTION, YesNoQuestionOption.NO, false},
    };
  }

  @Test
  @Parameters(method = "validBooleanValues")
  public void map_boolean_successfully(
      String propertyName, Scalar scalar, YesNoQuestionOption value, Boolean expected) {
    var applicantData = new ApplicantData();
    applicantData.putLong(
        Path.create("applicant.questionname").join(scalar), Long.toString(value.getId()));

    var inputFields =
        ImmutableList.of(new ApiBridgeDefinitionItem("questionname", scalar, propertyName));
    var requestPayload = requestPayloadMapper.map(applicantData, schema, inputFields);

    assertThat(requestPayload)
        .isNotNull()
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(propertyName, expected));
  }

  private Object[] notSupportedValues() {
    return new Object[] {
      new Object[] {"nullExample"}, new Object[] {"arrayExample"}, new Object[] {"objectExample"}
    };
  }

  @Test
  @Parameters(method = "notSupportedValues")
  public void not_supported_external_types_are_skipped(String propertyName) {
    var inputFields =
        ImmutableList.of(
            new ApiBridgeDefinitionItem("questionname", Scalar.SELECTION, propertyName));
    var requestPayload = requestPayloadMapper.map(new ApplicantData(), schema, inputFields);

    assertThat(requestPayload).isNotNull().isEmpty();
  }

  private Object[] notSupportedScalars() {
    return new Object[] {new Object[] {Scalar.SELECTIONS}, new Object[] {Scalar.SERVICE_AREA}};
  }

  @Test
  @Parameters(method = "notSupportedScalars")
  public void not_supported_scalar_types_throw(Scalar scalar) {
    var inputFields =
        ImmutableList.of(new ApiBridgeDefinitionItem("questionname", scalar, "stringExample"));

    assertThatThrownBy(() -> requestPayloadMapper.map(new ApplicantData(), schema, inputFields));
  }
}
