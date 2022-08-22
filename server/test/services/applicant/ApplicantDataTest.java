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
    assertThatThrownBy(() -> data.asJsonString()).isInstanceOf(IllegalStateException.class);
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
}
