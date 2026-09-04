package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import controllers.BadRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.data.DynamicForm;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramQuestionDefinitionNotFoundException;

/** Creates {@link PredicateDefinition}s from form inputs. */
public final class PredicateGenerator {
  // Example form keys:
  // condition-1-subcondition-1-value
  // condition-1-subcondition-2-values[0]
  private static final Pattern SINGLE_PREDICATE_VALUE_FORM_KEY_PATTERN =
      Pattern.compile("^condition-(\\d+)-subcondition-(\\d+)-value$");
  private static final Pattern MULTI_PREDICATE_VALUE_FORM_KEY_PATTERN =
      Pattern.compile("^condition-(\\d+)-subcondition-(\\d+)-values\\[\\d+\\]$");

  /**
   * Generates a {@link PredicateDefinition} from the given form.
   *
   * <p>Determines {@link PredicateDefinition.PredicateFormat} based on form contents. Each
   * subcondition consists of a {@code LeafExpressionNode} consisting of a question, scalar,
   * operator, and one or more values. Conditions and subconditions are grouped structurally into
   * AND and OR nodes to form the final predicate AST expression tree.
   *
   * <p>Requires the form to have the following keys:
   *
   * <ul>
   *   <li>{@code predicateAction} - a {@link PredicateAction}
   *   <li>{@code root-nodeType} - an AND/OR {@link PredicateExpressionNodeType} for the root node
   *   <li>{@code condition-CID-nodeType} - an AND/OR {@link PredicateExpressionNodeType} for the
   *       condition identified by CID
   *   <li>{@code condition-CID-subcondition-SCID-question} a question ID for the expression
   *       identified by condition CID and subcondition SCID
   *   <li>{@code condition-CID-subcondition-SCID-scalar} - a {@link Scalar} for the expression
   *       identified by condition CID and subcondition SCID
   *   <li>{@code condition-CID-subcondition-SCID-operator} - an {@link Operator} for the expression
   *       identified by condition CID and subcondition SCID
   *   <li>{@code condition-CID-subcondition-SCID-value} - a {@link PredicateValue} for the
   *       expression identified by condition CID and subcondition SCID
   * </ul>
   *
   * @param programDefinition the program this predicate is being generated for.
   * @param predicateForm contains key-value pairs specifying the predicate.
   * @throws BadRequestException if the form is invalid.
   */
  public PredicateDefinition generatePredicateDefinition(
      ProgramDefinition programDefinition, DynamicForm predicateForm)
      throws ProgramQuestionDefinitionNotFoundException {
    final PredicateAction predicateAction;

    try {
      predicateAction = PredicateAction.valueOf(predicateForm.get("predicateAction"));
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          String.format(
              "Missing or unknown predicateAction: %s", predicateForm.get("predicateAction")));
    }

    Map<Integer, Map<Integer, PredicateExpressionNode>> leafNodes =
        getLeafNodes(programDefinition, predicateForm);

    // Single leaf node predicate
    if (leafNodes.size() == 1 && leafNodes.values().iterator().next().size() == 1) {
      return PredicateDefinition.create(
          leafNodes.values().iterator().next().values().iterator().next(), predicateAction);
    }
    // Predicate with conditions and subcondition layers
    PredicateExpressionNodeType rootNodeType = getNodeType(predicateForm, "root-node-type");
    ImmutableList<PredicateExpressionNode> conditionNodes =
        leafNodes.keySet().stream()
            .sorted()
            .map(
                conditionId -> {
                  Map<Integer, PredicateExpressionNode> subconditionMap =
                      leafNodes.get(conditionId);
                  PredicateExpressionNodeType conditionNodeType =
                      getNodeType(
                          predicateForm, String.format("condition-%d-node-type", conditionId));
                  return createAndOrNode(
                      conditionNodeType,
                      subconditionMap.values().stream().collect(toImmutableList()),
                      /* errorMessage= */ String.format(
                          "Invalid node type %s for condition %d", conditionNodeType, conditionId));
                })
            .collect(toImmutableList());
    return PredicateDefinition.create(
        createAndOrNode(
            rootNodeType,
            conditionNodes,
            /* errorMessage= */ String.format("Invalid root node type, %s", rootNodeType)),
        predicateAction);
  }

  /**
   * Generates LeafExpressionNodes from the form input. Creates a map of conditionIds to
   * subconditionIds to leaf {@link PredicateExpressionNode}s.
   *
   * @throws ProgramQuestionDefinitionNotFoundException if a parsed questionId is not in the {@link
   *     ProgramDefinition}
   */
  private static Map<Integer, Map<Integer, PredicateExpressionNode>> getLeafNodes(
      ProgramDefinition programDefinition, DynamicForm predicateForm)
      throws ProgramQuestionDefinitionNotFoundException {
    Map<Integer, Map<Integer, PredicateExpressionNode>> leafNodes = new HashMap<>();
    HashSet<String> processedFormKeys = new HashSet<>();

    for (String key : predicateForm.rawData().keySet()) {
      Matcher singleValueMatcher = SINGLE_PREDICATE_VALUE_FORM_KEY_PATTERN.matcher(key);
      Matcher multiValueMatcher = MULTI_PREDICATE_VALUE_FORM_KEY_PATTERN.matcher(key);

      final Matcher matcher;
      if (singleValueMatcher.find()) {
        matcher = singleValueMatcher;
      } else if (multiValueMatcher.find()) {
        matcher = multiValueMatcher;
      } else {
        // Skip form keys that aren't related to specific subconditions (e.g. and/or node type
        // inputs).
        continue;
      }
      int conditionId = Integer.parseInt(matcher.group(1));
      int subconditionId = Integer.parseInt(matcher.group(2));
      long questionId = getQuestionId(predicateForm, conditionId, subconditionId);

      // Validate the questionId - throws an exception
      ProgramQuestionDefinition questionDefinition =
          programDefinition.getProgramQuestionDefinition(questionId);

      String conditionSubconditionPrefix =
          String.format("condition-%d-subcondition-%d-", conditionId, subconditionId);

      final Scalar scalar = getScalar(predicateForm, conditionSubconditionPrefix + "scalar");
      if (scalar.equals(Scalar.SERVICE_AREAS)) {
        validateServiceAreas(questionId, questionDefinition, predicateForm);
      }

      final Operator operator =
          getOperator(predicateForm, conditionSubconditionPrefix + "operator");

      final PredicateValue predicateValue;
      if (matcher == singleValueMatcher) {
        String secondKey = conditionSubconditionPrefix + "secondValue";
        processedFormKeys.add(secondKey);
        predicateValue =
            parsePredicateValue(
                scalar,
                operator,
                predicateForm.get(key),
                Optional.ofNullable(predicateForm.get(secondKey)),
                /* values= */ ImmutableList.of());
      } else if (matcher == multiValueMatcher && !processedFormKeys.contains(key)) {
        // For the first encountered key of a subcondition with a multivalued question, we process
        // all the keys now for the subcondition, then skip them later. This is necessary because we
        // need all the multivalue inputs together to build the full predicate value.
        ImmutableList<String> multiSelectKeys =
            predicateForm.rawData().keySet().stream()
                .filter(
                    filteredKey -> filteredKey.startsWith(conditionSubconditionPrefix + "values"))
                .sorted()
                .collect(ImmutableList.toImmutableList());

        processedFormKeys.addAll(multiSelectKeys);

        ImmutableList<String> rawPredicateValues =
            multiSelectKeys.stream()
                .map(predicateForm.rawData()::get)
                .collect(ImmutableList.toImmutableList());

        predicateValue =
            parsePredicateValue(
                scalar,
                operator,
                /* value= */ "",
                /* secondValue= */ Optional.empty(),
                rawPredicateValues);
      } else {
        // Skip already-processed multivalue keys
        continue;
      }

      LeafExpressionNode leafNode =
          scalar.equals(Scalar.SERVICE_AREAS)
              ? LeafAddressServiceAreaExpressionNode.create(
                  questionId, predicateValue.value(), operator)
              : LeafOperationExpressionNode.builder()
                  .setQuestionId(questionId)
                  .setScalar(scalar)
                  .setOperator(operator)
                  .setComparedValue(predicateValue)
                  .build();

      if (!leafNodes.containsKey(conditionId)) {
        leafNodes.put(conditionId, new HashMap<>());
      }
      leafNodes.get(conditionId).put(subconditionId, PredicateExpressionNode.create(leafNode));
    }

    return leafNodes;
  }

  private static PredicateExpressionNode createAndOrNode(
      PredicateExpressionNodeType nodeType,
      ImmutableList<PredicateExpressionNode> children,
      String errorMessage) {
    return switch (nodeType) {
      case AND -> PredicateExpressionNode.create(AndNode.create(children));
      case OR -> PredicateExpressionNode.create(OrNode.create(children));
      case LEAF_OPERATION, LEAF_ADDRESS_SERVICE_AREA -> throw new BadRequestException(errorMessage);
    };
  }

  private static void validateServiceAreas(
      long questionId, ProgramQuestionDefinition questionDefinition, DynamicForm predicateForm) {
    if (!questionDefinition.getQuestionDefinition().isAddress()) {
      throw new BadRequestException(
          String.format(
              "Question ID %d is not an address question in predicate update form: %s",
              questionId, predicateForm.rawData()));
    }

    if (!questionDefinition.addressCorrectionEnabled()) {
      throw new BadRequestException(
          String.format(
              "Address correction not enabled for question ID %d in predicate update form: %s",
              questionId, predicateForm.rawData()));
    }
  }

  private static PredicateExpressionNodeType getNodeType(
      DynamicForm predicateForm, String nodeTypeKey) {
    String rawNodeType = predicateForm.get(nodeTypeKey);
    if (rawNodeType == null) {
      throw new BadRequestException(
          String.format(
              "Missing node type for predicate update form: %s", predicateForm.rawData()));
    }
    try {
      return PredicateExpressionNodeType.valueOf(rawNodeType);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          String.format("Bad node type for predicate update form: %s", predicateForm.rawData()));
    }
  }

  private static long getQuestionId(
      DynamicForm predicateForm, Integer conditionId, Integer subconditionId) {
    String questionKey =
        String.format("condition-%d-subcondition-%d-question", conditionId, subconditionId);
    Optional<String> rawQuestionId = Optional.ofNullable(predicateForm.get(questionKey));
    if (rawQuestionId.isEmpty()) {
      throw new BadRequestException(
          String.format("Missing question for predicate update form: %s", predicateForm.rawData()));
    }
    try {
      return Long.parseLong(rawQuestionId.get());
    } catch (NumberFormatException e) {
      throw new BadRequestException(
          String.format(
              "Bad question ID %s for predicate update form: %s",
              rawQuestionId.get(), predicateForm.rawData()));
    }
  }

  private static Scalar getScalar(DynamicForm predicateForm, String scalarKey) {
    Optional<String> rawScalarValue = Optional.ofNullable(predicateForm.get(scalarKey));
    if (rawScalarValue.isEmpty()) {
      throw new BadRequestException(
          String.format("Missing scalar for predicate update form: %s", predicateForm.rawData()));
    }
    try {
      return Scalar.valueOf(rawScalarValue.get());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          String.format(
              "Bad scalar %s for predicate update form: %s",
              rawScalarValue.get(), predicateForm.rawData()));
    }
  }

  private static Operator getOperator(DynamicForm predicateForm, String operatorKey) {
    Optional<String> rawOperatorValue = Optional.ofNullable(predicateForm.get(operatorKey));

    if (rawOperatorValue.isEmpty()) {
      throw new BadRequestException(
          String.format("Missing operator for predicate update form: %s", predicateForm.rawData()));
    }

    try {
      return Operator.valueOf(rawOperatorValue.get());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          String.format(
              "Bad operator %s for predicate update form: %s",
              rawOperatorValue.get(), predicateForm.rawData()));
    }
  }

  /**
   * Parses the given value based on the given scalar type and operator. For example, if the scalar
   * is of type LONG and the operator is of type ANY_OF, the value will be parsed as a list of
   * comma-separated longs.
   *
   * <p>If value is the empty string, then parses the list of values instead.
   */
  private static PredicateValue parsePredicateValue(
      Scalar scalar,
      Operator operator,
      String value,
      Optional<String> secondValue,
      List<String> values) {
    // TODO: if scalar is not SELECTION or SELECTIONS and there values then throw an exception.
    // If the scalar is SELECTION or SELECTIONS then this is a multi-option question predicate, and
    // the right hand side values are in the `values` list rather than the `value` string.
    if (scalar == Scalar.SELECTION || scalar == Scalar.SELECTIONS) {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      return PredicateValue.listOfStrings(builder.addAll(values).build());
    }

    switch (scalar.toScalarType()) {
      case CURRENCY_CENTS -> {
        // Currency is inputted as dollars and cents but stored as cents.
        if (operator == Operator.BETWEEN) {
          return PredicateValue.pairOfLongs(parseCents(value), parseCents(secondValue.get()));
        }
        return PredicateValue.of(parseCents(value));
      }
      case DATE -> {
        // Age values are inputted as numbers.
        if (operator.equals(Operator.AGE_OLDER_THAN)
            || operator.equals(Operator.AGE_YOUNGER_THAN)) {
          Double ageVal = Double.parseDouble(value);
          // If the age is a whole number, store it as a long
          if (DoubleMath.isMathematicalInteger(ageVal)) {
            return PredicateValue.of(ageVal.longValue());
          }
          return PredicateValue.of(ageVal);
        } else if (operator.equals(Operator.AGE_BETWEEN)) {
          return PredicateValue.pairOfLongs(
              Long.parseLong(value), Long.parseLong(secondValue.get()));
        } else if (operator.equals(Operator.BETWEEN)) {
          return PredicateValue.pairOfDates(parseDate(value), parseDate(secondValue.get()));
        } else {
          return PredicateValue.of(parseDate(value));
        }
        // Age values are inputted as numbers.
      }
      case SERVICE_AREA -> {
        return PredicateValue.serviceArea(value);
      }
      case LONG -> {
        switch (operator) {
          case IN:
          case NOT_IN:
            ImmutableList<Long> listOfLongs =
                Splitter.on(",")
                    .splitToStream(value)
                    .map(s -> Long.parseLong(s.trim()))
                    .collect(ImmutableList.toImmutableList());
            return PredicateValue.listOfLongs(listOfLongs);

          case BETWEEN:
            return PredicateValue.pairOfLongs(
                Long.parseLong(value), Long.parseLong(secondValue.get()));

          default: // EQUAL_TO, NOT_EQUAL_TO, GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN,
            // LESS_THAN_OR_EQUAL_TO
            return PredicateValue.of(Long.parseLong(value));
        }
      }
      default -> {
        // expected. The others are handled using the "values" field in the predicate form
        return switch (operator) {
          case ANY_OF, IN, NONE_OF, NOT_IN, SUBSET_OF -> {
            ImmutableList<String> listOfStrings =
                Splitter.on(",")
                    .splitToStream(value)
                    .map(String::trim)
                    .collect(ImmutableList.toImmutableList());
            yield PredicateValue.listOfStrings(listOfStrings);
          }
          default -> // EQUAL_TO, NOT_EQUAL_TO
              PredicateValue.of(value);
        };
      }
    }
  }

  private static long parseCents(String value) {
    return ((Float) (Float.parseFloat(value) * 100)).longValue();
  }

  private static LocalDate parseDate(String value) {
    return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }
}
