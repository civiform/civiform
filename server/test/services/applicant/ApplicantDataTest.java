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
