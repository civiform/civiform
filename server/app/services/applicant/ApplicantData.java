package services.applicant;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import services.CfJsonDocumentContext;
import services.LocalizedStrings;
import services.Path;
import services.applicant.question.Scalar;
import services.geo.ServiceAreaInclusion;

/**
 * Brokers access to the answer data for a specific applicant across versions.
 *
 * <p>Instances are hydrated and persisted through {@code models.Applicant}.
 *
 * <p>While the underlying storage format is JSON, this class presents a read/write interface in
 * terms of CiviForm's domain semantics, such as {@code Path}, rather than raw JSON paths and
 * values.
 *
 * <p>When extending this class, seek to avoid leaking details of the JSON format to the code that
 * consumes it and prefer higher-level objects over primitives in method signatures.
 */
public class ApplicantData extends CfJsonDocumentContext {

  private static final String APPLICANT = "applicant";
  public static final Path APPLICANT_PATH = Path.create(APPLICANT);
  private static final String SELECTION_KEY = Scalar.SELECTION.name().toLowerCase(Locale.ROOT);
  private static final String SELECTIONS_KEY = Scalar.SELECTIONS.name().toLowerCase(Locale.ROOT);
  private static final String SCORE_KEY = Scalar.SCORE.name().toLowerCase(Locale.ROOT);
  private static final String SCORES_KEY = Scalar.SCORES.name().toLowerCase(Locale.ROOT);
  private static final String TOTAL_SCORE_KEY = Scalar.TOTAL_SCORE.name().toLowerCase(Locale.ROOT);
  private static final String EMPTY_APPLICANT_DATA_JSON =
      String.format("{ \"%s\": {} }", APPLICANT);
  private Optional<Locale> preferredLocale;
  private Optional<ImmutableMap<Path, String>> failedUpdates;

  public ApplicantData() {
    this(EMPTY_APPLICANT_DATA_JSON);
  }

  public ApplicantData(String jsonData) {
    this(Optional.empty(), jsonData);
  }

  public ApplicantData(Optional<Locale> preferredLocale, String jsonData) {
    super(JsonPathProvider.getJsonPath().parse(checkNotNull(jsonData)));
    this.preferredLocale = preferredLocale;
    this.failedUpdates = Optional.empty();
  }

  /**
   * Returns a deep copy of this data with the same preferred locale. The copy shares no state with
   * this instance, so writes to it never show up here.
   */
  public ApplicantData copy() {
    return new ApplicantData(this.preferredLocale, this.asJsonString());
  }

  /** Returns true if this applicant has set their preferred locale, and false otherwise. */
  public boolean hasPreferredLocale() {
    return this.preferredLocale.isPresent();
  }

  /** Returns this applicant's preferred locale if it is set, or the default locale if not set. */
  public Locale preferredLocale() {
    return this.preferredLocale.orElse(LocalizedStrings.DEFAULT_LOCALE);
  }

  public void setPreferredLocale(Locale locale) {
    checkLocked();
    this.preferredLocale = Optional.of(locale);
  }

  @Override
  public String asJsonString() {
    if (!getFailedUpdates().isEmpty()) {
      throw new IllegalStateException("data cannot be serialized since there were failed updates");
    }
    return super.asJsonString();
  }

  /**
   * Sets updates that couldn't be applied to the {@link ApplicantData}.
   *
   * @param updates Keys are the paths that couldn't be updated and values are the raw input that
   *     couldn't be applied.
   */
  public void setFailedUpdates(ImmutableMap<Path, String> updates) {
    checkLocked();
    failedUpdates = Optional.of(checkNotNull(updates));
  }

  /**
   * A map of updates that couldn't be applied to the {@link ApplicantData}. Keys are the paths that
   * couldn't be updated and values are the raw input that couldn't be applied.
   */
  public ImmutableMap<Path, String> getFailedUpdates() {
    return failedUpdates.orElse(ImmutableMap.of());
  }

  public boolean updateDidFailAt(Path path) {
    return getFailedUpdates().containsKey(path);
  }

  /**
   * Returns `true` if the answers in `other` match the answers in the current object. Ignores
   * `updated_at` timestamps when comparing answers.
   */
  public boolean isDuplicateOf(ApplicantData other) {
    // Copy data and clear fields not required for comparison.
    ApplicantData thisApplicantData = this.copy();
    clearFieldsNotRequiredForComparison(thisApplicantData);
    ApplicantData otherApplicantData = other.copy();
    clearFieldsNotRequiredForComparison(otherApplicantData);

    return thisApplicantData.asJsonString().equals(otherApplicantData.asJsonString());
  }

  private static void clearFieldsNotRequiredForComparison(ApplicantData applicantData) {
    // The `updated_at` timestamp for an answer should not be considered when
    // comparing answers.
    try {
      // The ".." in the path scans the entire document.
      applicantData.getDocumentContext().set("$..updated_at", 0);
    } catch (PathNotFoundException _) {
      // Metadata may be missing in unit tests. No harm, no foul.
    }
    // Score keys only get written at submit time, so a submitted application has them and the
    // live applicant data doesn't. Strip them or the same answers will never match. We can't
    // remove anything named score/scores/total_score though since an admin can name a question
    // that. A question is always an object (or an array of entity objects for an enumerator). A
    // score is a number next to a scalar selection and scores is an array of numbers/nulls the
    // same length as the selections array. Match on that shape instead of the key name.
    Object root = applicantData.getDocumentContext().json();
    if (root instanceof Map<?, ?> rootObject && rootObject.get(TOTAL_SCORE_KEY) instanceof Number) {
      rootObject.remove(TOTAL_SCORE_KEY);
    }
    scrubScoreKeys(root);
  }

  /** Recursively removes score keys that sit next to the answer they score. */
  private static void scrubScoreKeys(Object node) {
    if (node instanceof List<?> list) {
      list.forEach(ApplicantData::scrubScoreKeys);
      return;
    }
    if (!(node instanceof Map<?, ?> object)) {
      return;
    }

    if (object.get(SCORE_KEY) instanceof Number && isScalarValue(object.get(SELECTION_KEY))) {
      object.remove(SCORE_KEY);
    }
    if (isMultiSelectScoreArray(object.get(SCORES_KEY), object.get(SELECTIONS_KEY))) {
      object.remove(SCORES_KEY);
    }

    object.values().forEach(ApplicantData::scrubScoreKeys);
  }

  private static boolean isScalarValue(Object value) {
    return value != null && !(value instanceof Map) && !(value instanceof List);
  }

  /**
   * Returns true if {@code scores} has the shape {@link ApplicationScores#applyTo} writes next to a
   * multi-select answer: a non-empty array of numbers and nulls, the same length as the scalar
   * {@code selections} array.
   *
   * <p>Enumerators named "selections" and "scores" are sibling arrays too, but they hold entity
   * objects so they don't match. Empty arrays are skipped on purpose. An enumerator with no
   * entities is stored as an empty array and the calculator never writes an empty scores array
   * since an unanswered multi-select question has no selections key at all.
   */
  private static boolean isMultiSelectScoreArray(Object scores, Object selections) {
    if (!(scores instanceof List<?> scoreList) || !(selections instanceof List<?> selectionList)) {
      return false;
    }

    return !scoreList.isEmpty()
        && scoreList.size() == selectionList.size()
        && scoreList.stream().allMatch(score -> score == null || score instanceof Number)
        && selectionList.stream().allMatch(ApplicantData::isScalarValue);
  }

  /**
   * Puts an array at a given path, building parent objects as needed.
   *
   * @param path the {@link Path} where the array should be added.
   * @param entityNames a {@link List} containing service area results.
   */
  public void putServiceAreaInclusionEntities(
      Path path, ImmutableList<ServiceAreaInclusion> entityNames) {
    if (entityNames.isEmpty()) {
      // entityNames will be empty if this is the first time they are answering this question, or if
      // they are re-answering the question but there were no address suggestions found. In the
      // second scenario, we need to clear existing service area info off the json data.
      maybeClearArray(path);
    } else {
      for (int i = 0; i < entityNames.size(); i++) {
        putString(
            path.atIndex(i).join(Scalar.SERVICE_AREA_ID), entityNames.get(i).getServiceAreaId());
        putString(
            path.atIndex(i).join(Scalar.SERVICE_AREA_STATE), entityNames.get(i).getState().name());
        putLong(path.atIndex(i).join(Scalar.TIMESTAMP), entityNames.get(i).getTimeStamp());
      }
    }
  }

  /**
   * Attempt to read a list at the given {@link Path}. Returns {@code Optional#empty} if the path
   * does not exist or a value other than an {@link ImmutableList} of {@link ServiceAreaInclusion}
   * is found.
   *
   * @param path the {@link Path} to the list
   * @return an Optional containing an ImmutableList<ServiceAreaInclusion>
   */
  public Optional<ImmutableList<ServiceAreaInclusion>> readServiceAreaList(Path path) {
    return readList(
        path.safeWithoutArrayReference(), new TypeRef<ImmutableList<ServiceAreaInclusion>>() {});
  }

  /**
   * Path of a single-select question's answer-option score: a {@code score} key inside the
   * question's object, next to its {@code selection} scalar. This is the same shape the {@code
   * updated_at} and {@code program_updated_in} metadata use.
   *
   * <p>The key cannot collide with a question admin name: a question object contains only scalar
   * keys, and no question type has a scalar named {@code score} or {@code scores}. A question
   * admin-named "score" lives at {@code applicant.score}, one level up.
   */
  public static Path scorePath(Path contextualizedQuestionPath) {
    return checkApplicantRooted(contextualizedQuestionPath).join(Scalar.SCORE);
  }

  /**
   * Path of a checkbox question's answer-option score array: a {@code scores} key inside the
   * question's object, next to its {@code selections} scalar. See {@link #scorePath} for why the
   * key cannot collide with a question admin name.
   */
  public static Path scoresPath(Path contextualizedQuestionPath) {
    return checkApplicantRooted(contextualizedQuestionPath).join(Scalar.SCORES);
  }

  /** Writes a single-select question's score next to its {@code selection}. */
  public void putScore(Path contextualizedQuestionPath, double score) {
    putDouble(scorePath(contextualizedQuestionPath), score);
  }

  /**
   * Writes a checkbox question's score array next to its {@code selections}. The array is parallel
   * to the stored selections; an empty entry is stored as {@code null} and marks an unscored,
   * unknown, or repeated option.
   */
  public void putScores(Path contextualizedQuestionPath, List<Optional<Double>> scores) {
    // Use ArrayList instead of ImmutableList because the array can contain nulls, which
    // ImmutableList doesn't allow.
    List<Double> nullableScores = new ArrayList<>(scores.size());
    scores.forEach(score -> nullableScores.add(score.orElse(null)));

    putArray(scoresPath(contextualizedQuestionPath), nullableScores);
  }

  /** Reads the score written by {@link #putScore}, or empty if the question was not scored. */
  public Optional<Double> readScore(Path contextualizedQuestionPath) {
    return readDouble(scorePath(contextualizedQuestionPath));
  }

  /**
   * Reads the score array written by {@link #putScores}, or empty if the question was not scored.
   * Null entries come back as {@link Optional#empty}.
   */
  public Optional<ImmutableList<Optional<Double>>> readScores(Path contextualizedQuestionPath) {
    return readNullableDoubleList(scoresPath(contextualizedQuestionPath))
        .map(
            scores ->
                scores.stream().map(Optional::ofNullable).collect(ImmutableList.toImmutableList()));
  }

  private static Path checkApplicantRooted(Path contextualizedQuestionPath) {
    ImmutableList<String> segments = contextualizedQuestionPath.segments();
    checkArgument(
        !segments.isEmpty() && segments.get(0).equals(APPLICANT),
        "Contextualized question path must be rooted at 'applicant', got: %s",
        contextualizedQuestionPath);

    return contextualizedQuestionPath;
  }
}
