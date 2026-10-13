package services.applicant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Computes answer-option scores for an application being submitted.
 *
 * <p>The calculator is a pure computation over a {@link ReadOnlyApplicantProgramService}: it reads
 * the applicant's answers and the program version's option scores and returns an {@link
 * ApplicationScores} value. Writing the scores to the submitted application's applicant data
 * happens later, inside the submit transaction, via {@link ApplicationScores#applyTo}.
 */
public final class ApplicationScoreCalculator {

  @Inject
  public ApplicationScoreCalculator() {}

  /**
   * Computes per-question scores and the application total from the answers and program version
   * held by {@code roService}.
   *
   * <p>The total is always present, {@code 0} when nothing scored; once written, its presence marks
   * that scoring was applied. Repeated questions expand to concrete instances via the read-only
   * service, so each instance is scored on its own.
   */
  public ApplicationScores calculate(ReadOnlyApplicantProgramService roService) {
    ApplicantData applicantData = roService.getApplicantData();
    ApplicationScores.ApplicationScoresBuilder scores = ApplicationScores.builder();

    // Accumulate in BigDecimal so sums of admin-entered decimals carry no floating-point artifacts
    // (0.1 + 0.2 persists 0.3).
    BigDecimal total = BigDecimal.ZERO;

    // Score the same questions eligibility evaluates (visible questions in active blocks). An
    // applicant can answer a block and later change an answer that hides it. The stale answers stay
    // in applicant data but are not part of the submitted application, so they must not count.
    ImmutableList<ApplicantQuestion> questions =
        roService.getAllActiveBlocks().stream()
            .flatMap(block -> block.getVisibleQuestions().stream())
            .collect(ImmutableList.toImmutableList());

    for (ApplicantQuestion question : questions) {
      if (!QuestionType.supportsOptionScores(question.getType())) {
        continue;
      }
      Path contextualizedPath = question.getContextualizedPath();

      ImmutableMap<Long, Double> scoresByOptionId =
          optionScoresById((MultiOptionQuestionDefinition) question.getQuestionDefinition());
      if (question.getType().isMultiSelectType()) {
        total =
            total.add(
                scoreCheckboxQuestion(applicantData, contextualizedPath, scoresByOptionId, scores));
      } else {
        total =
            total.add(
                scoreSingleSelectQuestion(
                    applicantData, contextualizedPath, scoresByOptionId, scores));
      }
    }

    return scores.total(total.doubleValue()).build();
  }

  private static ImmutableMap<Long, Double> optionScoresById(
      MultiOptionQuestionDefinition definition) {
    return definition.getOptions().stream()
        .filter(option -> option.score().isPresent())
        .collect(
            ImmutableMap.toImmutableMap(
                QuestionOption::id, option -> option.score().get(), (first, second) -> first));
  }

  /**
   * Scores a radio/dropdown answer; the stored {@code selection} is the selected option id. Records
   * nothing for unanswered questions or unscored options.
   */
  private static BigDecimal scoreSingleSelectQuestion(
      ApplicantData applicantData,
      Path contextualizedPath,
      ImmutableMap<Long, Double> scoresById,
      ApplicationScores.ApplicationScoresBuilder scores) {
    Optional<Long> selectedOptionId =
        applicantData.readLong(contextualizedPath.join(Scalar.SELECTION));
    if (selectedOptionId.isEmpty() || !scoresById.containsKey(selectedOptionId.get())) {
      return BigDecimal.ZERO;
    }

    double score = scoresById.get(selectedOptionId.get());
    scores.singleSelectScore(contextualizedPath, score);
    return BigDecimal.valueOf(score);
  }

  /**
   * Records a score array parallel to the stored {@code selections}, preserving their order.
   * Unknown ids, unscored options, and repeat occurrences of an id get an empty entry, so each
   * option counts at most once.
   */
  private static BigDecimal scoreCheckboxQuestion(
      ApplicantData applicantData,
      Path contextualizedPath,
      ImmutableMap<Long, Double> scoresById,
      ApplicationScores.ApplicationScoresBuilder scores) {
    Optional<ImmutableList<Long>> maybeSelections =
        applicantData.readLongList(contextualizedPath.join(Scalar.SELECTIONS));
    if (maybeSelections.isEmpty()) {
      // Unanswered: record nothing for this question.
      return BigDecimal.ZERO;
    }

    BigDecimal subtotal = BigDecimal.ZERO;
    ImmutableList.Builder<Optional<Double>> questionScores = ImmutableList.builder();
    Set<Long> countedOptionIds = new HashSet<>();
    for (Long selectedOptionId : maybeSelections.get()) {
      // ImmutableMap returns null for a missing key, so a null here means the selected id is
      // unknown to this program version or the admin gave the option no score. Neither should
      // happen in practice; this is defensive. Either way the position still gets an empty entry
      // below, keeping the array parallel to selections.
      Double score = scoresById.get(selectedOptionId);
      if (score != null && countedOptionIds.add(selectedOptionId)) {
        questionScores.add(Optional.of(score));
        subtotal = subtotal.add(BigDecimal.valueOf(score));
      } else {
        questionScores.add(Optional.empty());
      }
    }

    scores.checkboxScore(contextualizedPath, questionScores.build());
    return subtotal;
  }
}
