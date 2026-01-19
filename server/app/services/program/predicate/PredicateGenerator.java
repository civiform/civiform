package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
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
import play.mvc.Http.Request;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.settings.SettingsManifest;

/** Creates {@link PredicateDefinition}s from form inputs. */
public final class PredicateGenerator {

  // Example form keys:
  // group-0-question-123-predicateValue
  // group-1-question-456-predicateValues[0]
  private static final Pattern LEGACY_SINGLE_PREDICATE_VALUE_FORM_KEY_PATTERN =
      Pattern.compile("^group-(\\d+)-question-(\\d+)-predicateValue$");
  private static final Pattern LEGACY_MULTI_PREDICATE_VALUE_FORM_KEY_PATTERN =
      Pattern.compile("^group-(\\d+)-question-(\\d+)-predicateValues\\[\\d+\\]$");
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
   * <p>TODO(#11764): This method generated predicates in the legacy format and can be removed once
   * expanded form logic is fully rolled out.
   *
   * @param programDefinition the program this predicate is being generated for.
   * @param predicateForm contains key-value pairs specifying the predicate.
   * @param roQuestionService a {@link ReadOnlyQuestionService} for validating that questions
   *     referenced in the form are active or draft.
   * @throws QuestionNotFoundException if the form references a question ID that is not in the
   *     {@link ReadOnlyQuestionService}.
   * @throws BadRequestException if the form is invalid.
   */
  public PredicateDefinition legacyGeneratePredicateDefinition(
      ProgramDefinition programDefinition,
      DynamicForm predicateForm,
      ReadOnlyQuestionService roQuestionService)
      throws QuestionNotFoundException, ProgramQuestionDefinitionNotFoundException {
    final PredicateAction predicateAction;

    try {
      predicateAction = PredicateAction.valueOf(predicateForm.get("predicateAction"));
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          String.format(
              "Missing or unknown predicateAction: %s", predicateForm.get("predicateAction")));
    }

    Multimap<Integer, LeafExpressionNode> leafNodes =
        legacyGetLeafNodes(programDefinition, predicateForm, roQuestionService);

    return switch (detectFormat(leafNodes)) {
      case MULTIPLE_CONDITIONS ->
          PredicateDefinition.create(
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
              predicateAction);
      case SINGLE_CONDITION -> {
        LeafExpressionNode singleQuestionNode =
            leafNodes.entries().stream().map(Map.Entry::getValue).findFirst().get();

        yield PredicateDefinition.create(
            PredicateExpressionNode.create(singleQuestionNode), predicateAction);
      }
    };
  }

  /**
   * Generates LeafExpressionNodes from the form input
   *
   * @throws ProgramQuestionDefinitionNotFoundException if a parsed questionId is not in the {@link
   *     ProgramDefinition}
   * @throws QuestionNotFoundException if a parsed questionId is not in the current active or draft
   *     version.
   */
  private static Multimap<Integer, LeafExpressionNode> legacyGetLeafNodes(
      ProgramDefinition programDefinition,
      DynamicForm predicateForm,
      ReadOnlyQuestionService roQuestionService)
      throws QuestionNotFoundException, ProgramQuestionDefinitionNotFoundException {
    Multimap<Integer, LeafExpressionNode> leafNodes = LinkedHashMultimap.create();
    HashSet<String> consumedKeys = new HashSet<>();

    for (String key : predicateForm.rawData().keySet()) {
      Matcher singleValueMatcher = LEGACY_SINGLE_PREDICATE_VALUE_FORM_KEY_PATTERN.matcher(key);
      Matcher multiValueMatcher = LEGACY_MULTI_PREDICATE_VALUE_FORM_KEY_PATTERN.matcher(key);

      Matcher matcher =
          singleValueMatcher.find()
              ? singleValueMatcher
              : multiValueMatcher.find() ? multiValueMatcher : null;
      if (matcher == null) {
        continue;
      }
      final int groupId = Integer.parseInt(matcher.group(1));
      final long questionId = Long.parseLong(matcher.group(2));

      // Validate the questionId - these throw exceptions
      roQuestionService.getQuestionDefinition(questionId);
      programDefinition.getProgramQuestionDefinition(questionId);

      final Scalar scalar =
          getScalar(predicateForm, String.format("question-%d-scalar", questionId));
      final Operator operator =
          getOperator(predicateForm, String.format("question-%d-operator", questionId));
      final PredicateValue predicateValue;

      if (scalar.equals(Scalar.SERVICE_AREAS)) {
        ProgramQuestionDefinition questionDefinition =
            programDefinition.getProgramQuestionDefinition(questionId);
        if (!questionDefinition.getQuestionDefinition().isAddress()) {
          throw new BadRequestException(String.format("%d is not an address question", questionId));
        }

        if (!questionDefinition.addressCorrectionEnabled()) {
          throw new BadRequestException(
              String.format(
                  "Address correction not enabled for Question ID %d in program ID %d",
                  questionId, programDefinition.id()));
        }
      }

      if (matcher == singleValueMatcher) {
        String secondKey =
            String.format("group-%d-question-%d-predicateSecondValue", groupId, questionId);
        consumedKeys.add(secondKey);
        predicateValue =
            parsePredicateValue(
                scalar,
                operator,
                predicateForm.get(key),
                Optional.ofNullable(predicateForm.get(secondKey)),
                ImmutableList.of());
      } else if (matcher == multiValueMatcher && !consumedKeys.contains(key)) {
        // For the first encountered key of a multivalued question, we process all the keys now for
        // the question, then skip them later.
        ImmutableList<String> multiSelectKeys =
            predicateForm.rawData().keySet().stream()
                .filter(
                    filteredKey ->
                        filteredKey.startsWith(
                            String.format(
                                "group-%d-question-%d-predicateValues", groupId, questionId)))
                .sorted()
                .collect(ImmutableList.toImmutableList());

        consumedKeys.addAll(multiSelectKeys);

        ImmutableList<String> rawPredicateValues =
            multiSelectKeys.stream()
                .map(predicateForm.rawData()::get)
                .collect(ImmutableList.toImmutableList());

        predicateValue =
            parsePredicateValue(scalar, operator, "", Optional.empty(), rawPredicateValues);
      } else {
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

      leafNodes.put(groupId, leafNode);
    }

    return leafNodes;
  }

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
   * @param roQuestionService a {@link ReadOnlyQuestionService} for validating that questions
   *     referenced in the form are active or draft.
   * @throws QuestionNotFoundException if the form references a question ID that is not in the
   *     {@link ReadOnlyQuestionService}.
   * @throws BadRequestException if the form is invalid.
   */
  public PredicateDefinition generatePredicateDefinition(
      ProgramDefinition programDefinition,
      DynamicForm predicateForm,
      ReadOnlyQuestionService roQuestionService,
      SettingsManifest settingsManifest,
      Request request)
      throws QuestionNotFoundException, ProgramQuestionDefinitionNotFoundException {
    if (!settingsManifest.getExpandedFormLogicEnabled()) {
      throw new BadRequestException("Expanded form logic is not enabled for this request.");
    }
    final PredicateAction predicateAction;

    try {
      predicateAction = PredicateAction.valueOf(predicateForm.get("predicateAction"));
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          String.format(
              "Missing or unknown predicateAction: %s", predicateForm.get("predicateAction")));
    }

    Map<Integer, Map<Integer, PredicateExpressionNode>> leafNodes =
        getLeafNodes(programDefinition, predicateForm, roQuestionService);

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
   * @throws QuestionNotFoundException if a parsed questionId is not in the current active or draft
   *     version.
   */
  private static Map<Integer, Map<Integer, PredicateExpressionNode>> getLeafNodes(
      ProgramDefinition programDefinition,
      DynamicForm predicateForm,
      ReadOnlyQuestionService roQuestionService)
      throws QuestionNotFoundException, ProgramQuestionDefinitionNotFoundException {
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

      // Validate the questionId - these throw exceptions
      roQuestionService.getQuestionDefinition(questionId);
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

  private static PredicateDefinition.PredicateFormat detectFormat(
      Multimap<Integer, LeafExpressionNode> leafNodes) {
    return leafNodes.size() > 1
        ? PredicateDefinition.PredicateFormat.MULTIPLE_CONDITIONS
        : PredicateDefinition.PredicateFormat.SINGLE_CONDITION;
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
