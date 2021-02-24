package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.Test;

public class ApplicantDataTest {
  @Test
  public void testCreatedTime() {
    ApplicantData applicantData = new ApplicantData();
    // Just an arbitrary time.
    Instant i = Instant.ofEpochMilli(10000000L);
    applicantData.setCreatedTime(i);
    assertThat(applicantData.getCreatedTime()).isEqualTo(i);
  }
}
