package services.applicant.question;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.RepeatedEntity;
import services.applicant.ValidationErrorMessage;
import services.question.types.QuestionType;

/**
 * All specific applicant question types extend this class.
 *
 * <p>It provides necessary methods to:
 *
 * <ul>
 *   <li>Access question configuration
 *   <li>Access the applicant's response
 *   <li>Validate the applicant's answer to the question: Conditions and data integrity
 *   <li>Present validation errors if any
 * </ul>
 */
public abstract class AbstractQuestion {

  /**
   * Field errors from the name, address and date questions, mapped to the version shown when the
   * question is a list set's initial question. Each list item version takes the original arguments
   * followed by the item type and the row number.
   */
  private static final ImmutableMap<MessageKey, MessageKey> LIST_ITEM_ERROR_KEYS =
      ImmutableMap.<MessageKey, MessageKey>builder()
          .put(
              MessageKey.NAME_VALIDATION_FIRST_REQUIRED,
              MessageKey.NAME_VALIDATION_FIRST_REQUIRED_FOR_LIST_ITEM)
          .put(
              MessageKey.NAME_VALIDATION_LAST_REQUIRED,
              MessageKey.NAME_VALIDATION_LAST_REQUIRED_FOR_LIST_ITEM)
          .put(
              MessageKey.ADDRESS_VALIDATION_STREET_REQUIRED,
              MessageKey.ADDRESS_VALIDATION_STREET_REQUIRED_FOR_LIST_ITEM)
          .put(
              MessageKey.ADDRESS_VALIDATION_CITY_REQUIRED,
              MessageKey.ADDRESS_VALIDATION_CITY_REQUIRED_FOR_LIST_ITEM)
          .put(
              MessageKey.ADDRESS_VALIDATION_STATE_REQUIRED,
              MessageKey.ADDRESS_VALIDATION_STATE_REQUIRED_FOR_LIST_ITEM)
          .put(
              MessageKey.ADDRESS_VALIDATION_INVALID_ZIPCODE,
              MessageKey.ADDRESS_VALIDATION_INVALID_ZIPCODE_FOR_LIST_ITEM)
          .put(
              MessageKey.ADDRESS_VALIDATION_NO_PO_BOX,
              MessageKey.ADDRESS_VALIDATION_NO_PO_BOX_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_MONTH_REQUIRED,
              MessageKey.DATE_VALIDATION_MONTH_REQUIRED_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_DAY_REQUIRED,
              MessageKey.DATE_VALIDATION_DAY_REQUIRED_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_DAY_INVALID,
              MessageKey.DATE_VALIDATION_DAY_INVALID_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_YEAR_MUST_BE_FOUR_DIGITS,
              MessageKey.DATE_VALIDATION_YEAR_MUST_BE_FOUR_DIGITS_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_INVALID_DATE_FORMAT,
              MessageKey.DATE_VALIDATION_INVALID_DATE_FORMAT_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_CURRENT_DATE_REQUIRED,
              MessageKey.DATE_VALIDATION_CURRENT_DATE_REQUIRED_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_DATE_BEYOND_ALLOWABLE_YEARS_IN_PAST,
              MessageKey.DATE_VALIDATION_DATE_BEYOND_ALLOWABLE_YEARS_IN_PAST_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_DATE_BEYOND_ALLOWABLE_YEARS_IN_FUTURE,
              MessageKey.DATE_VALIDATION_DATE_BEYOND_ALLOWABLE_YEARS_IN_FUTURE_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_FUTURE_DATE_REQUIRED,
              MessageKey.DATE_VALIDATION_FUTURE_DATE_REQUIRED_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_PAST_DATE_REQUIRED,
              MessageKey.DATE_VALIDATION_PAST_DATE_REQUIRED_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_DATE_TOO_FAR_IN_PAST,
              MessageKey.DATE_VALIDATION_DATE_TOO_FAR_IN_PAST_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_DATE_TOO_FAR_IN_FUTURE,
              MessageKey.DATE_VALIDATION_DATE_TOO_FAR_IN_FUTURE_FOR_LIST_ITEM)
          .put(
              MessageKey.DATE_VALIDATION_DATE_NOT_IN_RANGE,
              MessageKey.DATE_VALIDATION_DATE_NOT_IN_RANGE_FOR_LIST_ITEM)
          .build();

  protected final ApplicantQuestion applicantQuestion;

  public AbstractQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = Preconditions.checkNotNull(applicantQuestion);

    QuestionType supportedQuestionType = applicantQuestion.getType();
    if (!getClass().equals(supportedQuestionType.getSupportedQuestion())) {
      throw new RuntimeException(
          String.format(
              "The Question class %s is not equal to the one supported by %s, which is %s.",
              getClass(),
              supportedQuestionType,
              supportedQuestionType.getSupportedQuestion().toString()));
    }
  }

  /**
   * Returns any {@link ValidationErrorMessage}s to be shown to the applicant, keyed by the relevant
   * field path. Top-level question errors use the root question path.
   */
  public final ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrors() {
    ImmutableMap<Path, String> failedUpdates =
        applicantQuestion.getApplicantData().getFailedUpdates();
    if (!isAnswered() && applicantQuestion.isOptional() && failedUpdates.isEmpty()) {
      return ImmutableMap.of();
    }

    HashMap<Path, ImmutableSet<ValidationErrorMessage>> errorMap = new HashMap<>();

    // Why not just use the result of getValidationErrorsInternal()?
    // For ease of implementation, subclasses may build the error list by putting a field key
    // in the map along with a call to a validator method that may return an empty set of errors.
    // We remove keys with an empty set of errors here to help defend against downstream consumers
    // assumes that calling isEmpty on the map means that there are no errors.
    errorMap.putAll(
        Maps.filterEntries(getValidationErrorsInternal(), e -> !e.getValue().isEmpty()));

    // We shouldn't have an empty error map if we failed to convert some of the input. If there
    // aren't already errors, append a top-level error that the input couldn't be converted. In
    // practice, this shouldn't happen as long as each question type is properly accounting for bad
    // input.
    if (errorMap.isEmpty()
        && !failedUpdates.isEmpty()
        && getAllPaths().stream().anyMatch(failedUpdates::containsKey)) {
      errorMap.put(
          applicantQuestion.getContextualizedPath(),
          ImmutableSet.of(ValidationErrorMessage.create(MessageKey.INVALID_INPUT)));
    }

    // Add error for unanswered required question.
    if (applicantQuestion.isRequiredButWasSkippedInCurrentProgram()) {
      Path keyPath = applicantQuestion.getContextualizedPath();
      var valueErrors = new ImmutableSet.Builder<ValidationErrorMessage>();
      if (errorMap.containsKey(keyPath)) {
        valueErrors.addAll(errorMap.get(keyPath));
      }
      valueErrors.add(ValidationErrorMessage.create(MessageKey.VALIDATION_REQUIRED));
      errorMap.put(keyPath, valueErrors.build());
    }

    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> errors = ImmutableMap.copyOf(errorMap);
    return applicantQuestion
        .getInitialQuestionEntity()
        .map(entity -> nameListItemInErrors(errors, entity))
        .orElse(errors);
  }

  /**
   * Swaps each field error that has a list item version for that version, which names the item type
   * and row number (for example "Enter the last name for household member #1") instead of saying
   * "your". A list set screen can show the same field error on several rows.
   */
  private ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> nameListItemInErrors(
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> errors, RepeatedEntity entity) {
    String entityType =
        entity
            .enumeratorQuestionDefinition()
            .getEntityType()
            .getOrDefault(applicantQuestion.getApplicantData().preferredLocale());
    int rowNumber = entity.index() + 1;
    return errors.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Map.Entry::getKey,
                entry ->
                    entry.getValue().stream()
                        .map(error -> forListItem(error, entityType, rowNumber))
                        .collect(ImmutableSet.toImmutableSet())));
  }

  private static ValidationErrorMessage forListItem(
      ValidationErrorMessage error, String entityType, int rowNumber) {
    MessageKey listItemKey = LIST_ITEM_ERROR_KEYS.get(error.key());
    if (listItemKey == null) {
      return error;
    }
    return ValidationErrorMessage.create(
        listItemKey,
        ImmutableList.builder().addAll(error.args()).add(entityType, rowNumber).build().toArray());
  }

  /**
   * Question-type specific implementation of {@link AbstractQuestion.getValidationErrors}. Note
   * that keys with an empty set of errors will be filtered out by {@link
   * AbstractQuestion.getValidationErrors} so that calls to isEmpty on the getvalidationErrors
   * result are sufficient to indicate if there any errors.
   */
  protected abstract ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>>
      getValidationErrorsInternal();

  /**
   * A question is considered answered if the applicant data has been set for any of the paths
   * associated with the question. If the applicant data does not contain the question's path, then
   * it will be considered unanswered.
   */
  public boolean isAnswered() {
    return getAllPaths().stream().anyMatch(applicantQuestion.getApplicantData()::hasPath);
  }

  /**
   * Returns the answer as a text string.
   *
   * <p>This is the canonical representation to users in static contexts such as the review page and
   * data export.
   */
  public abstract String getAnswerString();

  /** Returns the default to use when there is no answer */
  public String getDefaultAnswerString() {
    return "-";
  }

  public Path getContextualizedPath() {
    return applicantQuestion.getContextualizedPath();
  }

  /** Return every path used by this question. */
  public abstract ImmutableList<Path> getAllPaths();

  /** Return the first Path with a validation error, or an empty Optional if no errors */
  public Optional<Path> getFirstPathWithError() {
    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors =
        getValidationErrors();
    return getAllPaths().stream()
        .filter(validationErrors::containsKey)
        .findFirst()
        .map(path -> Optional.of(path))
        .orElse(Optional.empty());
  }

  public final ImmutableMap<Path, String> getFailedUpdates() {
    return applicantQuestion.getApplicantData().getFailedUpdates();
  }

  public ApplicantQuestion getApplicantQuestion() {
    return applicantQuestion;
  }
}
