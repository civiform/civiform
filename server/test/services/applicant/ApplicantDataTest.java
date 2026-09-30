package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import repository.ResetPostgres;
import services.Path;
import services.applicant.question.Scalar;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;

public class ApplicantDataTest extends ResetPostgres {

  @Test
  public void preferredLocale_defaultsToEnglish() {
    ApplicantData data = new ApplicantData();
    assertThat(data.preferredLocale()).isEqualTo(Locale.US);
  }

  @Test
  public void hasPreferredLocale_onlyReturnsTrueIfPreferredLocaleIsSet() {
    ApplicantData data = new ApplicantData();
    assertThat(data.hasPreferredLocale()).isFalse();

    data = new ApplicantData("{\"applicant\":{}}");
    assertThat(data.hasPreferredLocale()).isFalse();

    data = new ApplicantData(Optional.empty(), "{\"applicant\":{}}");
    assertThat(data.hasPreferredLocale()).isFalse();

    data = new ApplicantData(Optional.of(Locale.FRENCH), "{\"applicant\":{}}");
    assertThat(data.hasPreferredLocale()).isTrue();
  }

  @Test
  public void asJsonString() {
    String blob =
        "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690288712068}}}";
    ApplicantData data = new ApplicantData(blob);
    assertThat(data.asJsonString()).isEqualTo(blob);
  }

  @Test
  public void withFailedUpdates() {
    ApplicantData data = new ApplicantData();
    Path samplePath = Path.create("samplepath").join(Scalar.FIRST_NAME);
    data.setFailedUpdates(ImmutableMap.of(samplePath, "invalid_value"));

    assertThat(data.getFailedUpdates()).isEqualTo(ImmutableMap.of(samplePath, "invalid_value"));
    assertThatThrownBy(data::asJsonString).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void isDuplicate_returnsTrue() {
    ApplicantData data1 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690288712068}}}");
    ApplicantData data2 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690288712068}}}");

    assertThat(data1.isDuplicateOf(data2)).isTrue();
    assertThat(data2.isDuplicateOf(data1)).isTrue();
  }

  @Test
  public void isDuplicate_returnsFalse() {
    ApplicantData data1 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690288712068}}}");
    ApplicantData data2 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"User\",\"last_name\":\"Name\",\"program_updated_in\":2,\"updated_at\":1690293297676}}}");

    assertThat(data1.isDuplicateOf(data2)).isFalse();
    assertThat(data2.isDuplicateOf(data1)).isFalse();
  }

  @Test
  public void isDuplicateWithMetadata_returnsTrue() {
    // The only difference is the timestamp in the `updated_at` field.
    //
    // Since this field is not settable by any API, we use the JSON representation to specify the
    // applicant data.
    ApplicantData data1 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690288712068}}}");
    ApplicantData data2 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690293297676}}}");

    assertThat(data1.isDuplicateOf(data2)).isTrue();
    assertThat(data2.isDuplicateOf(data1)).isTrue();
  }

  @Test
  public void isDuplicate_ignoresSiblingScoreKeys() {
    // A submitted application carries score keys next to the answers plus the root total_score; a
    // live applicant answering identically does not. The unscored radio checks partial matches and
    // the zero-scored radio checks that the scrub keys on existence, not truthiness.
    ApplicantData submittedApplication =
        new ApplicantData(
            "{\"applicant\":{\"color\":{\"selection\":3,\"score\":7},"
                + "\"toppings\":{\"selections\":[1,9,2],\"scores\":[5,null,2]},"
                + "\"unscored_radio\":{\"selection\":8},"
                + "\"zero_radio\":{\"selection\":4,\"score\":0}},"
                + "\"total_score\":14}");
    ApplicantData liveApplicant =
        new ApplicantData(
            "{\"applicant\":{\"color\":{\"selection\":3},"
                + "\"toppings\":{\"selections\":[1,9,2]},"
                + "\"unscored_radio\":{\"selection\":8},"
                + "\"zero_radio\":{\"selection\":4}}}");

    assertThat(liveApplicant.isDuplicateOf(submittedApplication)).isTrue();
    assertThat(submittedApplication.isDuplicateOf(liveApplicant)).isTrue();
  }

  @Test
  public void isDuplicate_keepsQuestionKeysNamedLikeScoreKeys() {
    // Question admin names may legitimately produce keys called score/scores/total_score below
    // `applicant`. The scrub only removes score keys that sit next to a selection/selections
    // scalar, and only the root-level total_score, so these question subtrees stay compared.
    ApplicantData data1 =
        new ApplicantData(
            "{\"applicant\":{\"score\":{\"text\":\"a\"},\"scores\":{\"text\":\"b\"},"
                + "\"total_score\":{\"text\":\"c\"}}}");
    ApplicantData data2 =
        new ApplicantData(
            "{\"applicant\":{\"score\":{\"text\":\"a\"},\"scores\":{\"text\":\"b\"},"
                + "\"total_score\":{\"text\":\"DIFFERENT\"}}}");

    assertThat(data1.isDuplicateOf(data2)).isFalse();
    assertThat(data1.isDuplicateOf(new ApplicantData(data1.asJsonString()))).isTrue();
  }

  @Test
  public void isDuplicate_keepsSiblingQuestionsNamedSelectionAndScore() {
    // Questions admin-named "selection" and "score" sit side by side under `applicant`, so a scrub
    // keyed on sibling existence alone would mistake the `applicant` object for a scored answer.
    // Shape decides instead: a question is an object, a score key is a number or an array.
    String before =
        "{\"applicant\":{\"selection\":{\"text\":\"same\"},\"score\":{\"text\":\"before\"},"
            + "\"selections\":{\"text\":\"same\"},\"scores\":{\"text\":\"before\"}}}";
    ApplicantData original = new ApplicantData(before);
    ApplicantData scoreChanged =
        new ApplicantData(
            before.replace("\"score\":{\"text\":\"before\"}", "\"score\":{\"text\":\"after\"}"));
    ApplicantData scoresChanged =
        new ApplicantData(
            before.replace("\"scores\":{\"text\":\"before\"}", "\"scores\":{\"text\":\"after\"}"));

    assertThat(original.isDuplicateOf(scoreChanged)).isFalse();
    assertThat(original.isDuplicateOf(scoresChanged)).isFalse();
    assertThat(original.isDuplicateOf(new ApplicantData(before))).isTrue();
  }

  @Test
  public void isDuplicate_keepsSiblingEnumeratorsNamedSelectionsAndScores() {
    // Enumerators admin-named "selections" and "scores" are stored as sibling arrays, the same key
    // pair a scored checkbox produces. A scrub keyed on "both are arrays" would drop the whole
    // "scores" enumerator. The elements decide instead: entity objects are never a score array.
    String before =
        "{\"applicant\":{\"selections\":[{\"entity_name\":\"same\"}],"
            + "\"scores\":[{\"entity_name\":\"Alice\",\"age\":{\"number\":30}}]}}";
    ApplicantData original = new ApplicantData(before);
    ApplicantData entityRenamed =
        new ApplicantData(
            before.replace("\"entity_name\":\"Alice\"", "\"entity_name\":\"Alicia\""));
    ApplicantData entityAdded =
        new ApplicantData(
            before.replace(
                "\"age\":{\"number\":30}}]",
                "\"age\":{\"number\":30}},{\"entity_name\":\"Bob\"}]"));
    ApplicantData nestedAnswerChanged =
        new ApplicantData(before.replace("\"number\":30", "\"number\":31"));

    assertThat(original.isDuplicateOf(entityRenamed)).isFalse();
    assertThat(original.isDuplicateOf(entityAdded)).isFalse();
    assertThat(original.isDuplicateOf(nestedAnswerChanged)).isFalse();
    assertThat(original.isDuplicateOf(new ApplicantData(before))).isTrue();
  }

  @Test
  public void isDuplicate_keepsEmptySiblingEnumeratorsNamedSelectionsAndScores() {
    // An enumerator with zero entities is stored as an empty array, which has no element shape to
    // inspect. Empty "scores" arrays stay compared, so empty-to-nonempty is still a change.
    ApplicantData bothEmpty =
        new ApplicantData("{\"applicant\":{\"selections\":[],\"scores\":[]}}");
    ApplicantData scoresFilled =
        new ApplicantData(
            "{\"applicant\":{\"selections\":[],\"scores\":[{\"entity_name\":\"Alice\"}]}}");
    ApplicantData selectionsFilled =
        new ApplicantData(
            "{\"applicant\":{\"selections\":[{\"entity_name\":\"Alice\"}],\"scores\":[]}}");
    ApplicantData scoresUnanswered = new ApplicantData("{\"applicant\":{\"selections\":[]}}");

    assertThat(bothEmpty.isDuplicateOf(scoresFilled)).isFalse();
    assertThat(scoresFilled.isDuplicateOf(bothEmpty)).isFalse();
    assertThat(bothEmpty.isDuplicateOf(selectionsFilled)).isFalse();
    assertThat(bothEmpty.isDuplicateOf(scoresUnanswered)).isFalse();
    assertThat(bothEmpty.isDuplicateOf(new ApplicantData(bothEmpty.asJsonString()))).isTrue();
  }

  @Test
  public void isDuplicate_ignoresScoreKeysInsideRepeatedEntities() {
    ApplicantData submittedApplication =
        new ApplicantData(
            "{\"applicant\":{\"household\":[{\"entity_name\":\"a\","
                + "\"color\":{\"selection\":3,\"score\":7},"
                + "\"toppings\":{\"selections\":[1,2],\"scores\":[null,4]}}]},"
                + "\"total_score\":11}");
    ApplicantData liveApplicant =
        new ApplicantData(
            "{\"applicant\":{\"household\":[{\"entity_name\":\"a\","
                + "\"color\":{\"selection\":3},"
                + "\"toppings\":{\"selections\":[1,2]}}]}}");

    assertThat(liveApplicant.isDuplicateOf(submittedApplication)).isTrue();
    assertThat(submittedApplication.isDuplicateOf(liveApplicant)).isTrue();
  }

  @Test
  public void putServiceAreaInclusionEntities_setsCorrectValues() {
    Path path = Path.create("applicant.address").join(Scalar.SERVICE_AREAS.name()).asArrayElement();
    ImmutableList<ServiceAreaInclusion> entityNames =
        ImmutableList.of(
            ServiceAreaInclusion.builder()
                .setServiceAreaId("cityvilleTownship")
                .setState(ServiceAreaState.IN_AREA)
                .setTimeStamp(1709069741L)
                .build());
    ApplicantData data = new ApplicantData("{\"applicant\":{}}");
    data.putServiceAreaInclusionEntities(path, entityNames);
    String expectedJson =
        "{\"applicant\":{\"address\":{\"service_areas\":[{\"service_area_id\":\"cityvilleTownship\",\"service_area_state\":\"IN_AREA\",\"timestamp\":1709069741}]}}}";

    assertThat(data.asJsonString()).isEqualTo(expectedJson);
  }

  @Test
  public void putServiceAreaInclusionEntities_doesNotSetValuesWhenEmpty() {
    Path path = Path.create("applicant.address").join(Scalar.SERVICE_AREAS.name()).asArrayElement();
    ImmutableList<ServiceAreaInclusion> entityNames = ImmutableList.of();
    ApplicantData data = new ApplicantData("{\"applicant\":{}}");
    data.putServiceAreaInclusionEntities(path, entityNames);
    String expectedJson = "{\"applicant\":{\"address\":{}}}";

    assertThat(data.asJsonString()).isEqualTo(expectedJson);
  }

  @Test
  public void putServiceAreaInclusionEntities_clearsValuesWhenEmpty() {
    Path path = Path.create("applicant.address").join(Scalar.SERVICE_AREAS.name()).asArrayElement();
    ImmutableList<ServiceAreaInclusion> entityNames =
        ImmutableList.of(
            ServiceAreaInclusion.builder()
                .setServiceAreaId("cityvilleTownship")
                .setState(ServiceAreaState.IN_AREA)
                .setTimeStamp(1709069741L)
                .build());
    ApplicantData data = new ApplicantData("{\"applicant\":{}}");
    data.putServiceAreaInclusionEntities(path, entityNames);
    String expectedJsonWithEntities =
        "{\"applicant\":{\"address\":{\"service_areas\":[{\"service_area_id\":\"cityvilleTownship\",\"service_area_state\":\"IN_AREA\",\"timestamp\":1709069741}]}}}";

    assertThat(data.asJsonString()).isEqualTo(expectedJsonWithEntities);

    ImmutableList<ServiceAreaInclusion> emptyEntities = ImmutableList.of();
    data.putServiceAreaInclusionEntities(path, emptyEntities);
    String expectedJsonWithoutEntitites = "{\"applicant\":{\"address\":{}}}";

    assertThat(data.asJsonString()).isEqualTo(expectedJsonWithoutEntitites);
  }

  @Test
  public void scorePaths_areSiblingsOfTheAnswerScalar() {
    Path questionPath = Path.create("applicant.favorite_color");

    assertThat(ApplicantData.scorePath(questionPath).toString())
        .isEqualTo("applicant.favorite_color.score");
    assertThat(ApplicantData.scoresPath(questionPath).toString())
        .isEqualTo("applicant.favorite_color.scores");
  }

  @Test
  public void scorePaths_rejectPathsNotRootedAtApplicant() {
    assertThatThrownBy(() -> ApplicantData.scorePath(Path.create("other.favorite_color")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ApplicantData.scoresPath(Path.empty()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void putScore_writesNextToSelectionAndReadsBack() {
    ApplicantData data = new ApplicantData("{\"applicant\":{\"color\":{\"selection\":3}}}");
    Path questionPath = Path.create("applicant.color");

    data.putScore(questionPath, 7.5);

    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"color\":{\"selection\":3,\"score\":7.5}}}");
    assertThat(data.readScore(questionPath)).hasValue(7.5);
    assertThat(data.readScore(Path.create("applicant.other"))).isEmpty();
  }

  @Test
  public void putScores_writesNullsForEmptyEntriesAndReadsBack() {
    ApplicantData data =
        new ApplicantData("{\"applicant\":{\"toppings\":{\"selections\":[1,9,2]}}}");
    Path questionPath = Path.create("applicant.toppings");
    ImmutableList<Optional<Double>> scores =
        ImmutableList.of(Optional.of(5.5), Optional.empty(), Optional.of(-2.25));

    data.putScores(questionPath, scores);

    assertThat(data.asJsonString())
        .isEqualTo(
            "{\"applicant\":{\"toppings\":{\"selections\":[1,9,2],\"scores\":[5.5,null,-2.25]}}}");
    assertThat(data.readScores(questionPath)).hasValue(scores);
    assertThat(data.readScores(Path.create("applicant.other"))).isEmpty();
  }
}
