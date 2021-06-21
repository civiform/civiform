package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import services.question.types.ScalarType;

/**
 * Represents the value on the right side of a JsonPath (https://github.com/json-path/JsonPath)
 * predicate expression. This value is usually a defined constant, such as a number, string, or
 * array.
 */
@AutoValue
public abstract class PredicateValue {

  public static PredicateValue of(long value) {
    return create(String.valueOf(value), ScalarType.LONG);
  }

  public static PredicateValue of(String value) {
    // Escape the string value
    return create(surroundWithQuotes(value), ScalarType.STRING);
  }

  public static PredicateValue of(LocalDate value) {
    return create(
        String.valueOf(value.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()),
        ScalarType.DATE);
  }

  public static PredicateValue of(ImmutableList<String> value) {
    return create(
        value.stream()
            .map(PredicateValue::surroundWithQuotes)
            .collect(toImmutableList())
            .toString(),
        ScalarType.LIST_OF_STRINGS);
  }

  @JsonCreator
  private static PredicateValue create(
      @JsonProperty("value") String value, @JsonProperty ScalarType type) {
    return new AutoValue_PredicateValue(value, type);
  }

  @JsonProperty("value")
  public abstract String value();

  @JsonProperty("type")
  public abstract ScalarType type();

  @Memoized
  public String toDisplayString() {
    if (type() == ScalarType.DATE) {
      return Instant.ofEpochMilli(Long.parseLong(value()))
              .atZone(ZoneId.systemDefault())
              .toLocalDate()
              .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    return value();
  }

  private static String surroundWithQuotes(String s) {
    return "\"" + s + "\"";
  }
}
