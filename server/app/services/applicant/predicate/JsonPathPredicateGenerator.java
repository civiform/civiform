package services.applicant.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import services.DateConverter;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.RepeatedEntity;
import services.applicant.exception.InvalidPredicateException;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.geo.ServiceAreaState;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.question.types.QuestionDefinition;

/** Generates {@link JsonPathPredicate}s based on the current applicant filling out the program. */
public final class JsonPathPredicateGenerator {

  private final DateConverter dateConverter;
  private final ImmutableMap<Long, QuestionDefinition> questionsById;
  private final Optional<RepeatedEntity> currentRepeatedContext;

  /**
   * This cannot be built from a set of {@link ApplicantQuestion}s because the question IDs for
   * repeated questions are used multiple times. For example, if there is a repeated name question,
   * that name question is repeated for each entity. Instead, we use {@link QuestionDefinition}s and
   * pass in the current block's repeated entity context, so we can determine which repeated entity
   * we are currently on, as well as find the target question used in the predicate.
   */
  public JsonPathPredicateGenerator(
      DateConverter dateConverter,
      ImmutableList<QuestionDefinition> programQuestions,
      Optional<RepeatedEntity> currentRepeatedContext) {
    this.dateConverter = checkNotNull(dateConverter);
    this.questionsById =
        checkNotNull(programQuestions).stream()
            .collect(toImmutableMap(QuestionDefinition::getId, q -> q));
    this.currentRepeatedContext = checkNotNull(currentRepeatedContext);
  }

  public static final ImmutableList<Operator> AGE_OPERATORS =
      ImmutableList.of(Operator.AGE_BETWEEN, Operator.AGE_OLDER_THAN, Operator.AGE_YOUNGER_THAN);

  /**
   * Formats a {@link LeafOperationExpressionNode} in JsonPath format: {@code path[?(expression)]}
   *
   * <p>Example: \$.applicant.name[?(@.last in ["Smith", "Lee"])]
   */
  public JsonPathPredicate fromLeafNode(LeafOperationExpressionNode node)
      throws InvalidPredicateException {
    if (AGE_OPERATORS.contains(node.operator())) {
      return formatAgePredicate(node);
    }
    if (node.operator() == Operator.BETWEEN) {
      return formatBetweenPredicate(node);
    }

    return JsonPathPredicate.create(
        String.format(
            "%s[?(@.%s %s %s)]",
            getPath(node).predicateFormat(),
            node.scalar().name().toLowerCase(Locale.ROOT),
            node.operator().toJsonPathOperator(),
            node.comparedValue().value()));
  }

  /**
   * Formats a {@link services.program.predicate.LeafAddressServiceAreaExpressionNode} in JsonPath
   * format: {@code path[?(expression)]}
   *
   * <p>Example: $.applicant.addresstest.service_areas[?((@.state == 'NOT_IN_AREA' || @.state ==
   * 'FAILED') && @.serviceAreaId == 'Seattle')]
   */
  public JsonPathPredicate fromLeafAddressServiceAreaNode(LeafAddressServiceAreaExpressionNode node)
      throws InvalidPredicateException {
    ServiceAreaState serviceAreaState =
        node.operator() == Operator.IN_SERVICE_AREA
            ? ServiceAreaState.IN_AREA
            : ServiceAreaState.NOT_IN_AREA;

    return JsonPathPredicate.create(
        String.format(
            "%1$s.%2$s[?((@.%3$s == '%4$s' || @.%3$s == '%5$s') && @.%6$s == '%7$s')]",
            getPath(node).predicateFormat(),
            Scalar.SERVICE_AREAS.toDisplayString(),
            Scalar.SERVICE_AREA_STATE.toDisplayString(),
            serviceAreaState.name(),
            ServiceAreaState.FAILED.name(),
            Scalar.SERVICE_AREA_ID.toDisplayString(),
            node.serviceAreaId()));
  }

  /**
   * Formats a {@link LeafOperationExpressionNode} with an age-related operator in JsonPath format:
   * {@code path[?(expression)]}
   *
   * <p>Age predicates are handled differently because we have to convert the long age inputs to
   * {@link Long} timestamps to compare them with the date value the applicant enters.
   *
   * <p>Greater than example: \$.applicant.date_of_birth[?(732153600000 > @.date)]
   *
   * <p>Between example: \$.applicant.date_of_birth[?(795225600000 >= @.date && @.date <=
   * 732153600000)]
   */
  private JsonPathPredicate formatAgePredicate(LeafOperationExpressionNode node)
      throws InvalidPredicateException {
    switch (node.operator()) {
      case AGE_BETWEEN:
        // Value is stored as "[18, 30]"
        String arrayString = node.comparedValue().value();
        ImmutableList<Long> ageRange =
            Splitter.on(", ")
                .splitToStream(arrayString.substring(1, arrayString.length() - 1))
                .map(Long::parseLong)
                .sorted()
                .collect(ImmutableList.toImmutableList());

        // Check that the date value is between the two age timestamps.
        return JsonPathPredicate.create(
            String.format(
                "%s[?(%2$s >= @.%4$s && %3$s <= @.%4$s)]",
                getPath(node).predicateFormat(),
                dateConverter.getDateTimestampFromAge(ageRange.get(0)),
                dateConverter.getDateTimestampFromAge(ageRange.get(1)),
                node.scalar().name().toLowerCase(Locale.ROOT)));
      case AGE_OLDER_THAN:
      case AGE_YOUNGER_THAN:
        return JsonPathPredicate.create(
            String.format(
                "%s[?(%s %s @.%s)]",
                getPath(node).predicateFormat(),
                dateConverter.getDateTimestampFromAge(
                    Double.parseDouble(node.comparedValue().value())),
                node.operator().toJsonPathOperator(),
                node.scalar().name().toLowerCase(Locale.ROOT)));
      default:
        throw new InvalidPredicateException(
            String.format("Expecting an age predicate but instead received %s", node.operator()));
    }
  }

  private JsonPathPredicate formatBetweenPredicate(LeafOperationExpressionNode node)
      throws InvalidPredicateException {
    // Value is stored as "[123, 456]"
    String arrayString = node.comparedValue().value();
    ImmutableList<Long> values =
        Splitter.on(", ")
            .splitToStream(arrayString.substring(1, arrayString.length() - 1))
            .map(Long::parseLong)
            .sorted()
            .collect(ImmutableList.toImmutableList());
    checkArgument(values.size() == 2);

    return JsonPathPredicate.create(
        String.format(
            "%s[?(%2$s <= @.%4$s && @.%4$s <= %3$s)]",
            getPath(node).predicateFormat(),
            values.get(0),
            values.get(1),
            node.scalar().name().toLowerCase(Locale.ROOT)));
  }

  private Path getPath(LeafExpressionNode node) throws InvalidPredicateException {
    if (!questionsById.containsKey(node.questionId())) {
      // This means a predicate was incorrectly configured - we are depending upon a question that
      // does not appear anywhere in this program.
      throw new InvalidPredicateException(
          String.format(
              "Tried to apply a predicate based on question %d, which is not found in this"
                  + " program.",
              node.questionId()));
    }

    QuestionDefinition targetQuestion = questionsById.get(node.questionId());
    Optional<RepeatedEntity> predicateContext;

    if (targetQuestion.getEnumeratorId().isEmpty()) {
      // This is a top-level question (i.e. is not repeated) - use an empty repeated context.
      predicateContext = Optional.empty();
    } else {
      predicateContext = getTargetContext(targetQuestion);
    }

    Path path =
        targetQuestion.getContextualizedPath(predicateContext, ApplicantData.APPLICANT_PATH);

    if (path.isArrayElement() && targetQuestion.isEnumerator()) {
      // In this case, we don't want the [] at the end of the path.
      path = path.withoutArrayReference();
    }

    return path;
  }

  private Optional<RepeatedEntity> getTargetContext(QuestionDefinition targetQuestion)
      throws InvalidPredicateException {
    // Walk up the RepeatedEntity ancestors to find the right context. We need the context of the
    // question in the predicate definition - that is, the one where the predicate question's
    // enumerator ID matches the context's enumerator ID.
    long enumeratorId = targetQuestion.getEnumeratorId().get();
    Optional<RepeatedEntity> predicateContext = this.currentRepeatedContext;
    while (predicateContext.isPresent()
        && predicateContext.get().enumeratorQuestionDefinition().getId() != enumeratorId) {
      predicateContext = predicateContext.get().parent();
    }

    // If the context is empty here, it means we never found the context of the predicate
    // question. We are trying to depend on an enumerator or repeated question that is not
    // an ancestor of the current block, which is not allowed and should never happen.
    if (predicateContext.isEmpty()) {
      throw new InvalidPredicateException(
          String.format(
              "Enumerator %d is not an ancestor of the current repeated context", enumeratorId));
    }

    return predicateContext;
  }
}
