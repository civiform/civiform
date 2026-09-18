package services.applicant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import lombok.Builder;
import lombok.Singular;
import services.Path;
import services.applicant.question.Scalar;

/**
 * Holds a mapping of the path to calculated score
 *
 * @param singleSelectScores Radio/dropdown scores, keyed by the scored question's contextualized
 *     path.
 * @param checkboxScores Checkbox score arrays, keyed by the scored question's contextualized path.
 *     Each array is parallel to the stored {@code selections} answer: an empty entry marks an
 *     unscored, unknown, or repeated option.
 * @param total The application's total score.
 */
@Builder
public record ApplicationScores(
    @Singular ImmutableMap<Path, Double> singleSelectScores,
    @Singular ImmutableMap<Path, ImmutableList<Optional<Double>>> checkboxScores,
    double total) {

  /**
   * Path of the application's total score. It sits at the document root next to {@code applicant},
   * outside the applicant tree, so it cannot collide with a question admin-named "total_score" at
   * {@code applicant.total_score}. Its presence marks that scoring was applied.
   */
  public static final Path TOTAL_SCORE_PATH = Path.empty().join(Scalar.TOTAL_SCORE);

  /**
   * Writes the scores to {@code applicantData}: each single-select score as a {@code score} sibling
   * of its {@code selection}, each checkbox array as a {@code scores} sibling of its {@code
   * selections}, and the total at the document root.
   *
   * <p>Only adds keys; never creates question objects or modifies an existing answer.
   *
   * @return {@code applicantData}, for chaining
   */
  public ApplicantData applyTo(ApplicantData applicantData) {
    singleSelectScores.forEach(applicantData::putScore);
    checkboxScores.forEach(applicantData::putScores);
    applicantData.putDouble(TOTAL_SCORE_PATH, total);

    return applicantData;
  }
}
