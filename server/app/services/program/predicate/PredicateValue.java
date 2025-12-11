package services.program.predicate;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.join;
import static j2html.TagCreator.strong;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import controllers.admin.PredicateUtils;
import j2html.tags.UnescapedText;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.stream.Stream;
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

  public static PredicateValue of(double value) {
    return create(String.valueOf(value), OperatorRightHandType.DOUBLE);
  }

  public static PredicateValue of(String value) {
    // Escape the string value
    return create(surroundWithQuotes(value), OperatorRightHandType.STRING);
  }

  public static PredicateValue of(LocalDate value) {
    return create(toEpochMilliString(value), OperatorRightHandType.DATE);
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

  public static PredicateValue pairOfDates(LocalDate value1, LocalDate value2) {
    return create(
        ImmutableList.of(toEpochMilliString(value1), toEpochMilliString(value2)).toString(),
        OperatorRightHandType.PAIR_OF_DATES);
  }

  public static PredicateValue pairOfLongs(long value1, long value2) {
    return create(
        ImmutableList.of(String.valueOf(value1), String.valueOf(value2)).toString(),
        OperatorRightHandType.PAIR_OF_LONGS);
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

    return toDisplayStringInternal(
        question,
        /* formatter= */ str -> str, // Don't apply extra formating
        /* aggregator= */ collection -> String.join(" and ", collection));
  }

  /**
   * Returns the value in a formatted, human-readable HTML format.
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
  public UnescapedText toDisplayFormattedHtml(QuestionDefinition question) {
    return toDisplayStringInternal(
        question,
        PredicateValue::formatDisplayHtml,
        collection -> PredicateUtils.joinUnescapedText(collection, "and"));
  }

  /**
   * Generalized string creator to represent the predicate that allows for customization.
   *
   * @param formatter formats the String value into the templated type.
   * @param aggregator collects multiple templated items into a single representation of the
   *     templates type.
   */
  private <T> T toDisplayStringInternal(
      QuestionDefinition question,
      Function<String, T> formatter,
      Function<ImmutableList<T>, T> aggregator) {
    /* Special handling of "simple" question types, EG non-multivalued questions. */

    // Currency is stored as cents and displayed as dollars/cents with 2 cent digits.
    if (question.getQuestionType().equals(QuestionType.CURRENCY)) {
      if (type() == OperatorRightHandType.PAIR_OF_LONGS) {
        return aggregator.apply(
            splitListString(value())
                .map(PredicateValue::displayCurrencyString)
                .map(formatter)
                .collect(toImmutableList()));
      }
      return formatter.apply(displayCurrencyString(value()));
    }

    if (type() == OperatorRightHandType.DATE) {
      return formatter.apply(displayDateString(value()));
    }

    if (type() == OperatorRightHandType.PAIR_OF_DATES) {
      return aggregator.apply(
          splitListString(value())
              .map(PredicateValue::displayDateString)
              .map(formatter)
              .collect(toImmutableList()));
    }

    if (type() == OperatorRightHandType.PAIR_OF_LONGS) {
      return aggregator.apply(splitListString(value()).map(formatter).collect(toImmutableList()));
    }

    // For all other "simple" questions use the stored value directly.
    if (!question.getQuestionType().isMultiOptionType()) {
      return formatter.apply(value());
    }

    // For multi option questions the value ids are stored in the database, so we need to convert
    // them to the human-readable strings.
    // We return the readable values in the default locale.
    // If an ID is not valid for the question, show "<obsolete>"; An obsolete ID does not affect
    // evaluation.
    MultiOptionQuestionDefinition multiOptionQuestion = (MultiOptionQuestionDefinition) question;
    if (type() == OperatorRightHandType.LIST_OF_STRINGS) {
      return formatter.apply(
          splitListString(value())
              .map(id -> parseMultiOptionIdToText(multiOptionQuestion, id))
              .collect(toImmutableList())
              .toString());
    }
    return formatter.apply(parseMultiOptionIdToText(multiOptionQuestion, value()));
  }

  /**
   * Splits a List in its toString format (e.g. "[123, 456]" or "[abc, def]" to a stream of strings
   * (e.g. "123", "456" or "abc", "def"). The strings can be further parsed to an expected type
   * (e.g. long).
   */
  private static Stream<String> splitListString(String listString) {
    return Splitter.on(", ").splitToStream(listString.substring(1, listString.length() - 1));
  }

  private static String parseMultiOptionIdToText(
      MultiOptionQuestionDefinition question, String id) {
    return question
        .getOptionAdminNameForId(Long.parseLong(id.substring(1, id.length() - 1)))
        .orElse("<obsolete>");
  }

  private static String toEpochMilliString(LocalDate value) {
    return String.valueOf(value.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
  }

  private static String displayCurrencyString(String value) {
    long storedCents = Long.parseLong(value);
    long dollars = storedCents / 100;
    long cents = storedCents % 100;
    return String.format("$%d.%02d", dollars, cents);
  }

  private String toValueString(String value, QuestionType questionType) {
    value = value.trim();
    if (questionType.equals(QuestionType.CURRENCY)) {
      return displayCurrencyString(value).replace("$", "");
    } else if (type().equals(OperatorRightHandType.DATE)
        || type().equals(OperatorRightHandType.PAIR_OF_DATES)) {
      return displayDateString(value);
    }
    return value.replace("\"", "");
  }

  private static String displayDateString(String value) {
    return Instant.ofEpochMilli(Long.parseLong(value))
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  private static UnescapedText formatDisplayHtml(String value) {
    return join(strong(value));
  }

  /**
   * Remove any double quotes currently in the string, as this would interfere with how we store the
   * predicate, then surround the string with a pair of double quotes.
   *
   * @param s the string to surround in double quotes
   * @return the same string with double quotes removed, then surrounded by double quotes
   */
  private static String surroundWithQuotes(String s) {
    return '"' + s.replace("\"", "") + '"';
  }

  /**
   * Get the stringified value of the predicate without surrounding quotes. Used for displaying the
   * predicate string in the UI. Only removes surrounding quotes from plain string predicate types.
   * Lists of strings will still contain double quotes around each item in the list.
   *
   * @return A plain string with surrounding double quotes removed, or the result of value() if it
   *     is not a plain string type.
   */
  public String valueWithoutSurroundingQuotes() {
    return type() == OperatorRightHandType.STRING ? value().replace("\"", "") : value();
  }

  /**
   * Converts this PredicateValue to a {@link SelectedValue} for use in the admin expanded predicate
   * view. Type conversions are as follows:
   *
   * <ul>
   *   <li>STRING, LONG, DOUBLE, DATE, SERVICE_AREA -> SINGLE
   *   <li>PAIR_OF_LONGS, PAIR_OF_DATES -> PAIR
   *   <li>LIST_OF_LONGS, LIST_OF_STRINGS -> MULTIPLE
   * </ul>
   */
  public SelectedValue toSelectedValue(QuestionType questionType) {
    SelectedValue selectedValue = SelectedValue.single("");
    switch (type()) {
      case DATE, DOUBLE, LONG, SERVICE_AREA, STRING -> {
        return SelectedValue.single(toValueString(value(), questionType));
      }
      case PAIR_OF_LONGS, PAIR_OF_DATES -> {
        ImmutableList<String> values =
            splitListString(value())
                .map(value -> toValueString(value, questionType))
                .collect(toImmutableList());
        checkState(values.size() == 2, "Expected exactly two values for %s PredicateValue", type());
        selectedValue =
            SelectedValue.pair(new SelectedValue.ValuePair(values.get(0), values.get(1)));
      }
      case LIST_OF_STRINGS, LIST_OF_LONGS -> {
        ImmutableSet<String> values =
            splitListString(value())
                .map(value -> toValueString(value, questionType))
                .collect(ImmutableSet.toImmutableSet());
        selectedValue = SelectedValue.multiple(values);
      }
      default -> {
        throw new IllegalArgumentException(
            String.format("Unsupported PredicateValue type: %s", type()));
      }
    }
    return selectedValue;
  }
}
