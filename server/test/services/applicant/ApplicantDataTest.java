package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.Path;
import services.applicant.question.Scalar;

public class ApplicantDataTest {

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
  public void getApplicantName_exists() {
    ApplicantData data = new ApplicantData();
    data.setUserName("First Last");
    assertThat(data.getApplicantName()).isEqualTo(Optional.of("Last, First"));
  }

  @Test
  public void getApplicantName_noName() {
    ApplicantData data = new ApplicantData();
    assertThat(data.getApplicantName()).isEmpty();
  }

  @Test
  public void asJsonString() {
    ApplicantData data = new ApplicantData();
    data.setUserName("First Last");
    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"name\":{\"last_name\":\"Last\",\"first_name\":\"First\"}}}");
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
  public void setDateOfBirth_isSuccessful() {
    ApplicantData data = new ApplicantData();
    String sampleDob = "2022-01-05";
    data.setDateOfBirth(sampleDob);
    assertThat(data.getDateOfBirth().get()).isEqualTo(sampleDob);
    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"applicant_date_of_birth\":1641340800000}}");
  }

  @Test
  public void getDateOfBirth_isEmptyWhenNotSet() {
    ApplicantData applicantData = new ApplicantData();
    assertThat(applicantData.getDateOfBirth()).isEqualTo(Optional.empty());
  }

  @Test
  public void isDuplicate_returnsTrue() {
    String userName = "First Last";
    String dob = "2022-01-05";

    ApplicantData data1 = new ApplicantData();
    data1.setUserName(userName);
    data1.setDateOfBirth(dob);

    ApplicantData data2 = new ApplicantData();
    data2.setUserName(userName);
    data2.setDateOfBirth(dob);

    assertThat(data1.isDuplicateOf(data2)).isTrue();
    assertThat(data2.isDuplicateOf(data1)).isTrue();
  }

  @Test
  public void isDuplicate_returnsFalse() {
    String userName1 = "First Last";
    String userName2 = "User Name";
    String dob = "2022-01-05";

    ApplicantData data1 = new ApplicantData();
    data1.setUserName(userName1);
    data1.setDateOfBirth(dob);

    ApplicantData data2 = new ApplicantData();
    data2.setUserName(userName2);
    data2.setDateOfBirth(dob);

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
}
