package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import controllers.BadRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import play.data.DynamicForm;
import services.applicant.question.Scalar;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;

/** Creates {@link PredicateDefinition}s from form inputs. */
public final class PredicateGenerator {

  private static final Pattern SINGLE_PREDICATE_VALUE_FORM_KEY_PATTERN =
      Pattern.compile("^group-(\\d+)-question-(\\d+)-predicateValue$");
  private static final Pattern MULTI_PREDICATE_VALUE_FORM_KEY_PATTERN =
      Pattern.compile("^group-(\\d+)-question-(\\d+)-predicateValues\\[\\d+\\]$");

  /**
   * Generates a {@link PredicateDefinition} from the given form.
   *
   * <p>Determines {@link PredicateDefinition.PredicateFormat} based on form contents. If the form
   * contains a single leaf node then it generates a SINGLE_QUESTION, otherwise a
   * OR_OF_SINGLE_LAYER_ANDS. For each question a single scalar and operator are selected with one
   * or more values. The values for all questions are grouped into rows containing one value for
   * each question. Note that the group IDs are not persisted explicitly since they are used for
   * grouping values which is accomplished structurally in the resulting predicate's AST.
   *
   * <p>Requires the form to have the following keys:
   *
   * <ul>
   *   <li>{@code predicateAction} - a {@link PredicateAction}
   *   <li>{@code question-QID-scalar} - a {@link Scalar} for the question identified by QID
   *   <li>{@code question-QID-operator} - an {@link Operator} for the question identified by QID
   *   <li>{@code group-GID-question-QID-predicateValue} - a {@link PredicateValue} identifying a
   *       leaf node on a given AND node. The GID specifies the AND node and the QID specifies the
   *       leaf node.
   *   <li>{@code group-GID-question-QID-predicateValues[VID]} - a single value in a multi-value
   *       {@link PredicateValue}. The VID distinguishes the key from the others in the same leaf
   *       node and is otherwise unused.
   * </ul>
   *
   * @param predicateForm contains key-value pairs specifying the predicate.
   * @param roQuestionService a {@link ReadOnlyQuestionService} for validating that questions
   *     referenced in the form are active or draft.
   * @throws QuestionNotFoundException if the form references a question ID that is not in the
   *     {@link ReadOnlyQuestionService}.
   * @throws BadRequestException if the form is invalid.
   */
  public PredicateDefinition generatePredicateDefinition(
      DynamicForm predicateForm, ReadOnlyQuestionService roQuestionService)
      throws QuestionNotFoundException {
    final PredicateAction predicateAction;

    try {
      predicateAction = PredicateAction.valueOf(predicateForm.get("predicateAction"));
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          String.format(
              "Missing or unknown predicateAction: %s", predicateForm.get("predicateAction")));
    }

    Multimap<Integer, LeafOperationExpressionNode> leafNodes =
        getLeafNodes(predicateForm, roQuestionService);

    switch (detectFormat(leafNodes)) {
      case OR_OF_SINGLE_LAYER_ANDS:
        {
          return PredicateDefinition.create(
              PredicateExpressionNode.create(
                  OrNode.create(
                      leafNodes.keySet().stream()
                          // Sorting here ensures the AND nodes are created in the same order as
                          // value groups/rows in the UI.
                          // This ensures the edit UI will show the value rows in the original
                          // order.
                          .sorted()
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
          throw new BadRequestException(
              String.format("Unrecognized predicate format: %s", detectFormat(leafNodes)));
        }
    }
  }

  private static Multimap<Integer, LeafOperationExpressionNode> getLeafNodes(
      DynamicForm predicateForm, ReadOnlyQuestionService roQuestionService)
      throws QuestionNotFoundException {
    Multimap<Integer, LeafOperationExpressionNode> leafNodes = LinkedHashMultimap.create();
    HashSet<String> consumedMultiValueKeys = new HashSet<>();

    for (String key : predicateForm.rawData().keySet()) {
      Matcher singleValueMatcher = SINGLE_PREDICATE_VALUE_FORM_KEY_PATTERN.matcher(key);
      Matcher multiValueMatcher = MULTI_PREDICATE_VALUE_FORM_KEY_PATTERN.matcher(key);

      final int groupId;
      final long questionId;
      final Scalar scalar;
      final Operator operator;
      final PredicateValue predicateValue;

      if (singleValueMatcher.find()) {
        Pair<Integer, Long> groupIdQuestionIdPair =
            getGroupIdAndQuestionId(roQuestionService, singleValueMatcher);
        groupId = groupIdQuestionIdPair.getLeft();
        questionId = groupIdQuestionIdPair.getRight();
        Pair<Scalar, Operator> scalarOperatorPair = getScalarAndOperator(predicateForm, questionId);
        scalar = scalarOperatorPair.getLeft();
        operator = scalarOperatorPair.getRight();

        predicateValue =
            parsePredicateValue(scalar, operator, predicateForm.get(key), ImmutableList.of());
      } else if (multiValueMatcher.find() && !consumedMultiValueKeys.contains(key)) {
        // For the first encountered key of a multivalued question, we process all the keys now for
        // the question, then skip them later.
        Pair<Integer, Long> groupIdQuestionIdPair =
            getGroupIdAndQuestionId(roQuestionService, multiValueMatcher);
        groupId = groupIdQuestionIdPair.getLeft();
        questionId = groupIdQuestionIdPair.getRight();
        Pair<Scalar, Operator> scalarOperatorPair = getScalarAndOperator(predicateForm, questionId);
        scalar = scalarOperatorPair.getLeft();
        operator = scalarOperatorPair.getRight();

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
    return leafNodes;
  }

  /**
   * Gets the groupId and questionId for the provided predicate value matcher.
   *
   * @throws QuestionNotFoundException if the resulting questionId is not in the {@link
   *     ReadOnlyQuestionService}
   */
  private static Pair<Integer, Long> getGroupIdAndQuestionId(
      ReadOnlyQuestionService roQuestionService, Matcher matcher) throws QuestionNotFoundException {
    int groupId = Integer.parseInt(matcher.group(1));
    long questionId = Long.parseLong(matcher.group(2));

    roQuestionService.getQuestionDefinition(questionId);

    return Pair.of(groupId, questionId);
  }

  private static Pair<Scalar, Operator> getScalarAndOperator(
      DynamicForm predicateForm, Long questionId) {
    String rawScalarValue = predicateForm.get(String.format("question-%d-scalar", questionId));
    String rawOperatorValue = predicateForm.get(String.format("question-%d-operator", questionId));

    if (rawOperatorValue == null || rawScalarValue == null) {
      throw new BadRequestException(
          String.format(
              "Missing scalar or operator for predicate update form: %s", predicateForm.rawData()));
    }

    try {
      return Pair.of(Scalar.valueOf(rawScalarValue), Operator.valueOf(rawOperatorValue));
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          String.format(
              "Bad scalar or operator for predicate update form: %s", predicateForm.rawData()));
    }
  }

  private static PredicateDefinition.PredicateFormat detectFormat(
      Multimap<Integer, LeafOperationExpressionNode> leafNodes) {
    return leafNodes.size() > 1
        ? PredicateDefinition.PredicateFormat.OR_OF_SINGLE_LAYER_ANDS
        : PredicateDefinition.PredicateFormat.SINGLE_QUESTION;
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
