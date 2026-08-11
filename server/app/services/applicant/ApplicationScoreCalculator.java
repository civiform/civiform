package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import models.ApplicantModel;
import services.Path;
import services.applicant.predicate.JsonPathPredicateGeneratorFactory;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Computes answer-option scores for a submitted application and writes them, along with the total
 * score, to the application's private snapshot at the {@link ApplicationScoreMetadata} paths:
 * per-question scores as siblings of the {@code selection}/{@code selections} answer, the total at
 * the document root.
 *
 * <p>The calculator only adds {@code score}/{@code scores} keys to question objects that already
 * exist and the one root {@code total_score} key; it never creates question objects and never
 * modifies any existing answer value, including checkbox {@code selections} values and ordering.
 */
public final class ApplicationScoreCalculator {

  private final JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory;

  @Inject
  public ApplicationScoreCalculator(
      JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory) {
    this.jsonPathPredicateGeneratorFactory = checkNotNull(jsonPathPredicateGeneratorFactory);
  }

  /**
   * Enriches the given snapshot with per-question score metadata and the application total score.
   *
   * <p>Always writes {@code total_score} (an enriched application with no scored answers persists
   * {@code 0}); its presence is the authoritative "scoring was applied" marker. Repeated and
   * nested-repeated questions expand to concrete instances via a snapshot-backed {@link
   * ReadOnlyApplicantProgramService}.
   *
   * @param applicant the applicant model, used only to construct the read-only service
   * @param snapshot the application's private {@link ApplicantData} copy; never the applicant's
   *     own shared instance
   * @param submittedVersion the program version the application is associated with; scores resolve
   *     from this version's question definitions
   */
  public void enrich(
      ApplicantModel applicant, ApplicantData snapshot, ProgramDefinition submittedVersion) {
    ReadOnlyApplicantProgramService roService =
        new ReadOnlyApplicantProgramService(
            jsonPathPredicateGeneratorFactory, applicant, snapshot, submittedVersion);

    // Totals accumulate in BigDecimal over each score's shortest decimal representation, so sums
    // of admin-entered decimals carry no binary floating-point artifacts (0.1 + 0.2 persists 0.3).
    BigDecimal total = BigDecimal.ZERO;
    // Deduplicate by contextualized path so a malformed program containing the same question more
    // than once cannot double-count a score; mirrors the exporters' buildKeepingLast behavior for
    // known issue #9212.
    Set<Path> seenPaths = new HashSet<>();
    ImmutableList<ApplicantQuestion> questions =
        roService.getAllQuestionsIncludingHidden().collect(ImmutableList.toImmutableList());
    for (ApplicantQuestion question : questions) {
      if (!QuestionType.supportsOptionScores(question.getType())) {
        continue;
      }
      Path contextualizedPath = question.getContextualizedPath();
      if (!seenPaths.add(contextualizedPath)) {
        continue;
      }
      ImmutableMap<Long, Double> scoresByOptionId =
          optionScoresById((MultiOptionQuestionDefinition) question.getQuestionDefinition());
      if (question.getType() == QuestionType.CHECKBOX) {
        total = total.add(enrichCheckboxQuestion(snapshot, contextualizedPath, scoresByOptionId));
      } else {
        total =
            total.add(enrichSingleSelectQuestion(snapshot, contextualizedPath, scoresByOptionId));
      }
    }

    snapshot.putDouble(ApplicationScoreMetadata.totalScorePath(), total.doubleValue());
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
   * Scores a radio/dropdown answer. The stored {@code selection} value is the selected option id.
   * No key is written for unanswered questions or unscored options; export layers emit null from
   * absence.
   */
  private static BigDecimal enrichSingleSelectQuestion(
      ApplicantData snapshot, Path contextualizedPath, ImmutableMap<Long, Double> scoresById) {
    Optional<Long> selectedOptionId =
        snapshot.readLong(contextualizedPath.join(Scalar.SELECTION));
    if (selectedOptionId.isEmpty() || !scoresById.containsKey(selectedOptionId.get())) {
      return BigDecimal.ZERO;
    }
    double score = scoresById.get(selectedOptionId.get());
    snapshot.putDouble(ApplicationScoreMetadata.scorePath(contextualizedPath), score);
    return BigDecimal.valueOf(score);
  }

  /**
   * Scores a checkbox answer without modifying the stored {@code selections}. Writes a same-length
   * nullable score array preserving the stored selection order. Unknown ids, unscored options, and
   * occurrences after the first duplicate id get null; only the first scored occurrence of an id
   * contributes to the total, so an option contributes at most once.
   */
  private static BigDecimal enrichCheckboxQuestion(
      ApplicantData snapshot, Path contextualizedPath, ImmutableMap<Long, Double> scoresById) {
    Optional<ImmutableList<Long>> maybeSelections =
        snapshot.readLongList(contextualizedPath.join(Scalar.SELECTIONS));
    if (maybeSelections.isEmpty()) {
      // Unanswered: no per-question metadata at all.
      return BigDecimal.ZERO;
    }
    BigDecimal subtotal = BigDecimal.ZERO;
    // ArrayList because the array may contain null holes; see CfJsonDocumentContext#putArray.
    List<Double> scores = new ArrayList<>();
    Set<Long> countedOptionIds = new HashSet<>();
    for (Long selectedOptionId : maybeSelections.get()) {
      Double score = scoresById.get(selectedOptionId);
      if (score != null && countedOptionIds.add(selectedOptionId)) {
        scores.add(score);
        subtotal = subtotal.add(BigDecimal.valueOf(score));
      } else {
        scores.add(null);
      }
    }
    snapshot.putArray(ApplicationScoreMetadata.scoresPath(contextualizedPath), scores);
    return subtotal;
  }
}
