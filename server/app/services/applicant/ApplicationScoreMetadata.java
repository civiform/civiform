package services.applicant;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import services.Path;

/**
 * Owns the JSON keys and paths of the answer-option score data written to a submitted
 * application's private snapshot.
 *
 * <p>Per-question scores live inside the scored question's own JSON object, as siblings of the
 * {@code selection}/{@code selections} answer scalar — the same additive-sibling shape the {@code
 * updated_at}/{@code program_updated_in} metadata scalars already use. The application total lives
 * at the document root, as a sibling of the {@code applicant} object. These keys are deliberately
 * not {@link services.applicant.question.Scalar}s, which keeps them out of question scalar sets,
 * predicates, API bridges, and scalar-driven schema generation by construction.
 *
 * <p>The sibling keys cannot collide with any question admin name: a question's JSON object
 * contains only scalar keys, and no question type has a scalar named {@code score} or {@code
 * scores}. A question admin-named {@code score} produces a question <em>object</em> one level
 * higher, never a key inside another question's object, and {@code total_score} sits outside the
 * {@code applicant} tree entirely. Applicant update endpoints reject the three key names in
 * scalar position as reserved (see {@link #isReservedScoreKey}).
 */
public final class ApplicationScoreMetadata {

  /** Key of a single-select question's score, a sibling of its {@code selection} scalar. */
  public static final String SCORE_KEY = "score";

  /** Key of a checkbox question's score array, a sibling of its {@code selections} scalar. */
  public static final String SCORES_KEY = "scores";

  /** Root-level key of the application's total score, a sibling of {@code applicant}. */
  public static final String TOTAL_SCORE_KEY = "total_score";

  private static final ImmutableSet<String> RESERVED_SCORE_KEYS =
      ImmutableSet.of(SCORE_KEY, SCORES_KEY, TOTAL_SCORE_KEY);

  private static final Path TOTAL_SCORE = Path.create(TOTAL_SCORE_KEY);

  private ApplicationScoreMetadata() {}

  /**
   * Path of the application's total score. Presence of a value at this path is the authoritative
   * marker that scoring was applied to the application at submission time.
   */
  public static Path totalScorePath() {
    return TOTAL_SCORE;
  }

  /**
   * Path of a single-select question's score: a {@code score} key inside the question's object,
   * next to its {@code selection} scalar.
   */
  public static Path scorePath(Path contextualizedQuestionPath) {
    return checkApplicantRooted(contextualizedQuestionPath).join(SCORE_KEY);
  }

  /**
   * Path of a checkbox question's score array (parallel to its stored {@code selections} array): a
   * {@code scores} key inside the question's object, next to its {@code selections} scalar.
   */
  public static Path scoresPath(Path contextualizedQuestionPath) {
    return checkApplicantRooted(contextualizedQuestionPath).join(SCORES_KEY);
  }

  /**
   * Returns true if the key name is reserved for score storage. Applicant update endpoints reject
   * updates whose path ends in a reserved key as defense in depth: score keys occupy scalar
   * position, so reserving them as scalar key names is sufficient, and question admin names are
   * unaffected because they occupy a different path depth.
   */
  public static boolean isReservedScoreKey(String keyName) {
    return RESERVED_SCORE_KEYS.contains(keyName);
  }

  private static Path checkApplicantRooted(Path contextualizedQuestionPath) {
    ImmutableList<String> segments = contextualizedQuestionPath.segments();
    checkArgument(
        !segments.isEmpty() && segments.get(0).equals(ApplicantData.APPLICANT_PATH.toString()),
        "Contextualized question path must be rooted at 'applicant', got: %s",
        contextualizedQuestionPath);
    return contextualizedQuestionPath;
  }
}
