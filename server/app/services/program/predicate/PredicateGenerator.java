package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import controllers.BadRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.data.DynamicForm;
import services.applicant.question.Scalar;

public final class PredicateGenerator {

  private static final Pattern SINGLE_PREDICATE_VALUE_FORM_KEY_PATTERN =
      Pattern.compile("^group-(\\d+)-question-(\\d+)-predicateValue$");
  private static final Pattern MULTI_PREDICATE_VALUE_FORM_KEY_PATTERN =
      Pattern.compile("^group-(\\d+)-question-(\\d+)-predicateValues\\[\\d+\\]$");

  public PredicateDefinition generatePredicateDefinition(DynamicForm predicateForm) {
    Multimap<Integer, LeafOperationExpressionNode> leafNodes = LinkedHashMultimap.create();

    HashSet<String> consumedMultiValueKeys = new HashSet<>();
    PredicateAction predicateAction = PredicateAction.valueOf(predicateForm.get("predicateAction"));

    if (predicateAction == null) {
      throw new BadRequestException(
          String.format(
              "Missing or unknown predicateAction: %s", predicateForm.get("predicateAction")));
    }

    for (String key : predicateForm.rawData().keySet()) {
      Matcher singleValueMatcher = SINGLE_PREDICATE_VALUE_FORM_KEY_PATTERN.matcher(key);
      Matcher multiValueMatcher = MULTI_PREDICATE_VALUE_FORM_KEY_PATTERN.matcher(key);

      int groupId;
      int questionId;
      Scalar scalar;
      Operator operator;
      PredicateValue predicateValue;

      if (singleValueMatcher.find()) {
        groupId = Integer.parseInt(singleValueMatcher.group(1));
        questionId = Integer.parseInt(singleValueMatcher.group(2));
        String rawPredicateValue = predicateForm.get(key);
        String rawScalarValue = predicateForm.get(String.format("question-%d-scalar", questionId));
        String rawOperatorValue =
            predicateForm.get(String.format("question-%d-operator", questionId));

        if (rawOperatorValue == null || rawScalarValue == null || rawPredicateValue == null) {
          throw new BadRequestException(
              String.format(
                  "Bad form submission for updating predicate: %s", predicateForm.rawData()));
        }

        scalar = Scalar.valueOf(rawScalarValue);
        operator = Operator.valueOf(rawOperatorValue);
        predicateValue =
            parsePredicateValue(scalar, operator, rawPredicateValue, ImmutableList.of());
      } else if (multiValueMatcher.find()) {
        if (consumedMultiValueKeys.contains(key)) {
          continue;
        }

        groupId = Integer.parseInt(multiValueMatcher.group(1));
        questionId = Integer.parseInt(multiValueMatcher.group(2));
        String rawScalarValue = predicateForm.get(String.format("question-%d-scalar", questionId));
        String rawOperatorValue =
            predicateForm.get(String.format("question-%d-operator", questionId));

        ImmutableList<String> multiSelectKeys =
            predicateForm.rawData().keySet().stream()
                .filter(
                    filteredKey ->
                        filteredKey.startsWith(
                            String.format(
                                "group-%d-question-%d-predicateValues", groupId, questionId)))
                .collect(ImmutableList.toImmutableList());

        consumedMultiValueKeys.addAll(multiSelectKeys);

        ImmutableList<String> rawPredicateValues =
            multiSelectKeys.stream()
                .map(predicateForm.rawData()::get)
                .collect(ImmutableList.toImmutableList());

        if (rawOperatorValue == null || rawScalarValue == null) {
          throw new BadRequestException(
              String.format(
                  "Bad form submission for updating predicate: %s", predicateForm.rawData()));
        }

        scalar = Scalar.valueOf(rawScalarValue);
        operator = Operator.valueOf(rawOperatorValue);
        predicateValue = parsePredicateValue(scalar, operator, "", rawPredicateValues);
      } else {
        continue;
      }

      leafNodes.put(
          groupId,
          LeafOperationExpressionNode.builder()
              .setQuestionId(questionId)
              .setScalar(scalar)
              .setOperator(operator)
              .setComparedValue(predicateValue)
              .build());
    }

    switch (getFormat(leafNodes)) {
      case OR_OF_SINGLE_LAYER_ANDS:
        {
          return PredicateDefinition.create(
              PredicateExpressionNode.create(
                  OrNode.create(
                      leafNodes.keySet().stream()
                          .map(leafNodes::get)
                          .map(
                              leafNodeGroup ->
                                  leafNodeGroup.stream()
                                      .map(PredicateExpressionNode::create)
                                      .collect(toImmutableList()))
                          .map(AndNode::create)
                          .map(PredicateExpressionNode::create)
                          .collect(toImmutableList()))),
              predicateAction,
              PredicateDefinition.PredicateFormat.OR_OF_SINGLE_LAYER_ANDS);
        }

      case SINGLE_QUESTION:
        {
          LeafOperationExpressionNode singleQuestionNode =
              leafNodes.entries().stream().map(Map.Entry::getValue).findFirst().get();

          return PredicateDefinition.create(
              PredicateExpressionNode.create(singleQuestionNode),
              predicateAction,
              PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
        }

      default:
        {
          throw new RuntimeException(
              String.format("Unrecognized predicate format: %s", getFormat(leafNodes)));
        }
    }
  }

  private static PredicateDefinition.PredicateFormat getFormat(
      Multimap<Integer, LeafOperationExpressionNode> leafNodes) {
    if (leafNodes.size() > 1
        || leafNodes.keySet().stream()
            .map(leafNodes::get)
            .filter(
                (Collection<LeafOperationExpressionNode> leafNodeGroup) -> leafNodeGroup.size() > 1)
            .findAny()
            .isPresent()) {
      return PredicateDefinition.PredicateFormat.OR_OF_SINGLE_LAYER_ANDS;
    }

    return PredicateDefinition.PredicateFormat.SINGLE_QUESTION;
  }

  /**
   * Parses the given value based on the given scalar type and operator. For example, if the scalar
   * is of type LONG and the operator is of type ANY_OF, the value will be parsed as a list of
   * comma-separated longs.
   *
   * <p>If value is the empty string, then parses the list of values instead.
   */
  // TODO: make this private once the old predicate logic is removed from
  // AdminProgramBlockPredicatesController
  @VisibleForTesting
  public static PredicateValue parsePredicateValue(
      Scalar scalar, Operator operator, String value, List<String> values) {

    // If the scalar is SELECTION or SELECTIONS then this is a multi-option question predicate, and
    // the right hand side values are in the `values` list rather than the `value` string.
    if (scalar == Scalar.SELECTION || scalar == Scalar.SELECTIONS) {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      return PredicateValue.listOfStrings(builder.addAll(values).build());
    }

    switch (scalar.toScalarType()) {
      case CURRENCY_CENTS:
        // Currency is inputted as dollars and cents but stored as cents.
        Float cents = Float.parseFloat(value) * 100;
        return PredicateValue.of(cents.longValue());

      case DATE:
        LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return PredicateValue.of(localDate);

      case LONG:
        switch (operator) {
          case IN:
          case NOT_IN:
            ImmutableList<Long> listOfLongs =
                Splitter.on(",")
                    .splitToStream(value)
                    .map(s -> Long.parseLong(s.trim()))
                    .collect(ImmutableList.toImmutableList());
            return PredicateValue.listOfLongs(listOfLongs);
          default: // EQUAL_TO, NOT_EQUAL_TO, GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN,
            // LESS_THAN_OR_EQUAL_TO
            return PredicateValue.of(Long.parseLong(value));
        }

      default: // STRING - we list all operators here, but in reality only IN and NOT_IN are
        // expected. The others are handled using the "values" field in the predicate form
        switch (operator) {
          case ANY_OF:
          case IN:
          case NONE_OF:
          case NOT_IN:
          case SUBSET_OF:
            ImmutableList<String> listOfStrings =
                Splitter.on(",")
                    .splitToStream(value)
                    .map(String::trim)
                    .collect(ImmutableList.toImmutableList());
            return PredicateValue.listOfStrings(listOfStrings);
          default: // EQUAL_TO, NOT_EQUAL_TO
            return PredicateValue.of(value);
        }
    }
  }
}
