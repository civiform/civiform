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
    // A previously submitted snapshot of a scoring program carries score keys as siblings of the
    // answers plus the root total_score; a live applicant answering identically does not. The
    // comparison must be score-blind. The unscored radio (selection without score) exercises the
    // filtered delete against partial matches.
    ApplicantData submittedSnapshot =
        new ApplicantData(
            "{\"applicant\":{\"color\":{\"selection\":3,\"score\":7},"
                + "\"toppings\":{\"selections\":[1,9,2],\"scores\":[5,null,2]},"
                + "\"unscored_radio\":{\"selection\":8}},"
                + "\"total_score\":14}");
    ApplicantData liveApplicant =
        new ApplicantData(
            "{\"applicant\":{\"color\":{\"selection\":3},"
                + "\"toppings\":{\"selections\":[1,9,2]},"
                + "\"unscored_radio\":{\"selection\":8}}}");

    assertThat(liveApplicant.isDuplicateOf(submittedSnapshot)).isTrue();
    assertThat(submittedSnapshot.isDuplicateOf(liveApplicant)).isTrue();
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
}
