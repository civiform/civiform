package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents the value on the right side of a JsonPath (https://github.com/json-path/JsonPath)
 * predicate expression. This value is usually a defined constant, such as a number, string, or
 * array.
 */
@AutoValue
public abstract class PredicateValue {

  public static PredicateValue of(long value) {
    return create(String.valueOf(value), OperatorRightHandType.LONG);
  }

  public static PredicateValue of(String value) {
    // Escape the string value
    return create(surroundWithQuotes(value), OperatorRightHandType.STRING);
  }

  public static PredicateValue of(LocalDate value) {
    return create(
        String.valueOf(value.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()),
        OperatorRightHandType.DATE);
  }

  public static PredicateValue listOfStrings(ImmutableList<String> value) {
    return create(
        value.stream()
            .map(PredicateValue::surroundWithQuotes)
            .collect(toImmutableList())
            .toString(),
        OperatorRightHandType.LIST_OF_STRINGS);
  }

  public static PredicateValue listOfLongs(ImmutableList<Long> value) {
    return create(value.toString(), OperatorRightHandType.LIST_OF_LONGS);
  }

  public static PredicateValue serviceArea(String value) {
    return create(value, OperatorRightHandType.SERVICE_AREA);
  }

  @JsonCreator
  private static PredicateValue create(
      @JsonProperty("value") String value, @JsonProperty("type") OperatorRightHandType type) {
    // Default to STRING type for values added before 2021-06-21 (this is before predicates were
    // launched publicly, so no prod predicates were affected).
    if (type == null) {
      type = OperatorRightHandType.STRING;
    }
    return new AutoValue_PredicateValue(value, type);
  }

  @JsonProperty("value")
  public abstract String value();

  @JsonProperty("type")
  public abstract OperatorRightHandType type();

  /**
   * Returns the value in a human-readable format.
   *
   * <ul>
   *   <li>Currency: $1000.23, $3.00
   *   <li>Dates: yyyy-MM-dd
   *   <li>User entered strings: Always quoted, including in lists
   *   <li>Question defined strings: as defined in the default locale, unquoted
   *   <li>Lists: bracketed - [1, 2, 3] ["Charles", "Jane"] [Option1, Option2]
   * </ul>
   *
   * @param question the question the predicate is applied to.
   */
  public String toDisplayString(QuestionDefinition question) {

    /* Special handling of "simple" question types, EG non-multivalued questions. */

    // Currency is stored as cents and displayed as dollars/cents with 2 cent digits.
    if (question.getQuestionType().equals(QuestionType.CURRENCY)) {
      long storedCents = Long.parseLong(value());
      long dollars = storedCents / 100;
      long cents = storedCents % 100;
      return String.format("$%d.%02d", dollars, cents);
    }

    // Convert to a human-readable date.
    if (type() == OperatorRightHandType.DATE) {
      return Instant.ofEpochMilli(Long.parseLong(value()))
          .atZone(ZoneId.systemDefault())
          .toLocalDate()
          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    // For all other "simple" questions use the stored value directly.
    if (!question.getQuestionType().isMultiOptionType()) {
      return value();
    }

    // For multi option questions the value ids are stored in the database, so we need to convert
    // them to the human-readable strings.
    // We return the readable values in the default locale.
    // If an ID is not valid for the question, show "<obsolete>"; An obsolete ID does not affect
    // evaluation.
    MultiOptionQuestionDefinition multiOptionQuestion = (MultiOptionQuestionDefinition) question;
    if (type() == OperatorRightHandType.LIST_OF_STRINGS) {
      return Splitter.on(", ")
          // Un quote-escape each value.
          .splitToStream(value().substring(1, value().length() - 1))
          .map(id -> parseMultiOptionIdToText(multiOptionQuestion, id))
          .collect(toImmutableList())
          .toString();
    }
    return parseMultiOptionIdToText(multiOptionQuestion, value());
  }

  private static String parseMultiOptionIdToText(
      MultiOptionQuestionDefinition question, String id) {
    return question
        .getDefaultLocaleOptionForId(Long.parseLong(id.substring(1, id.length() - 1)))
        .orElse("<obsolete>");
  }

  private static String surroundWithQuotes(String s) {
    return "\"" + s + "\"";
  }
}
