package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;

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
}
