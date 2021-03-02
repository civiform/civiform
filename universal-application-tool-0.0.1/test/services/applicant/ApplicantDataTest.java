package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.testing.EqualsTester;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.Test;

public class ApplicantDataTest {
  @Test
  public void createdTime() {
    ApplicantData applicantData = new ApplicantData();
    // Just an arbitrary time.
    Instant i = Instant.ofEpochMilli(10000000L);

    applicantData.setCreatedTime(i);

    assertThat(applicantData.getCreatedTime()).isEqualTo(i);
  }

  @Test
  public void equality() {
    DocumentContext jsonData =
        JsonPath.parse("{ \"applicant\": { \"testKey\": \"testValue\"}, \"metadata\": {}}");
    new EqualsTester()
        .addEqualityGroup(new ApplicantData(), new ApplicantData())
        .addEqualityGroup(new ApplicantData(jsonData), new ApplicantData(jsonData))
        .testEquals();
  }
}
