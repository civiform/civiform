package views.admin.programs;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.OperatorRightHandType;
import services.question.types.ScalarType;

@AutoValue
abstract class FormattedPredicateValue {
  abstract Optional<String> mainValue();

  abstract Optional<String> secondValue();

  boolean isSingleValue() {
    return secondValue().isEmpty();
  }

  private FormattedPredicateValue map(Function<String, String> mapper) {
    return create(mainValue().map(mapper), secondValue().map(mapper));
  }

  private static FormattedPredicateValue createEmpty() {
    return create(Optional.empty(), Optional.empty());
  }

  private static FormattedPredicateValue createSingleValue(String value) {
    return create(Optional.of(value), Optional.empty());
  }

  private static FormattedPredicateValue createWithSecondValue(
      String mainValue, String secondValue) {
    return create(Optional.of(mainValue), Optional.of(secondValue));
  }

  static FormattedPredicateValue create(Optional<String> mainValue, Optional<String> secondValue) {
    return new AutoValue_FormattedPredicateValue(mainValue, secondValue);
  }

  static FormattedPredicateValue fromLeafNode(Optional<LeafOperationExpressionNode> leafNode) {
    return leafNode.map(FormattedPredicateValue::fromLeafNode).orElse(createEmpty());
  }

  private static FormattedPredicateValue fromLeafNode(LeafOperationExpressionNode leafNode) {
    ScalarType scalarType = leafNode.scalar().toScalarType();
    String value = leafNode.comparedValue().value();
    OperatorRightHandType predicateType = leafNode.comparedValue().type();
    switch (scalarType) {
      case CURRENCY_CENTS:
        if (predicateType == OperatorRightHandType.PAIR_OF_LONGS) {
          return parsePairValue(value).map(FormattedPredicateValue::formatCurrency);
        }
        return createSingleValue(formatCurrency(value));
      case DATE:
        if (predicateType == OperatorRightHandType.LIST_OF_LONGS) {
          // Backwards compatibility for the original implementation of the AGE_BETWEEN operator
          // (https://github.com/civiform/civiform/pull/4428) which stored the values as a
          // LIST_OF_LONGS with exactly two values instead of PAIR_OF_LONGS
          if (leafNode.operator() == Operator.AGE_BETWEEN) {
            return parsePairValue(value);
          }
          return createSingleValue(formatListOfLongs(value));
        }
        if (predicateType == OperatorRightHandType.PAIR_OF_LONGS) {
          return parsePairValue(value);
        }
        if (predicateType == OperatorRightHandType.LONG
            || predicateType == OperatorRightHandType.DOUBLE) {
          return createSingleValue(value);
        }
        if (predicateType == OperatorRightHandType.PAIR_OF_DATES) {
          return parsePairValue(value).map(FormattedPredicateValue::formatDate);
        }

        return createSingleValue(formatDate(value));
      case LONG:
        if (predicateType == OperatorRightHandType.LIST_OF_LONGS) {
          return createSingleValue(formatListOfLongs(value));
        }
        if (predicateType == OperatorRightHandType.PAIR_OF_LONGS) {
          return parsePairValue(value);
        }

        return createSingleValue(value);
      case LIST_OF_STRINGS:
      case STRING:
        if (predicateType == OperatorRightHandType.LIST_OF_STRINGS) {
          // Lists of strings are serialized as JSON arrays e.g. "[\"one\", \"two\"]"
          return createSingleValue(
              Splitter.on(", ")
                  // Remove opening and closing brackets
                  .splitToStream(value.substring(1, value.length() - 1))
                  // Remove quotes
                  .map(item -> item.substring(1, item.length() - 1))
                  // Join to CSV
                  .collect(Collectors.joining(",")));
        }

        return createSingleValue(leafNode.comparedValue().valueWithoutSurroundingQuotes());
      default:
        throw new RuntimeException(String.format("Unknown scalar type: %s", scalarType));
    }
  }

  private static FormattedPredicateValue parsePairValue(String value) {
    // Pairs are stored in List.toString format, e.g. "[123, 456]"
    List<String> values = Splitter.on(", ").splitToList(value.substring(1, value.length() - 1));
    Preconditions.checkState(values.size() == 2);
    return createWithSecondValue(values.get(0), values.get(1));
  }

  private static String formatDate(String value) {
    return Instant.ofEpochMilli(Long.parseLong(value))
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  private static String formatCurrency(String value) {
    long storedCents = Long.parseLong(value);
    long dollars = storedCents / 100;
    long cents = storedCents % 100;
    return String.format("%d.%02d", dollars, cents);
  }

  private static String formatListOfLongs(String value) {
    // Lists of longs are serialized as JSON arrays e.g. "[1, 2]"
    return Splitter.on(", ")
        // Remove opening and closing brackets
        .splitToStream(value.substring(1, value.length() - 1))
        // Join to CSV
        .collect(Collectors.joining(","));
  }
}
