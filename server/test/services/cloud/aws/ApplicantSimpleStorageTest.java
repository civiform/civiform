package services.cloud.aws;

import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.cloud.azure.ApplicantBlobStorage;

public class ApplicantSimpleStorageTest extends ResetPostgres {

  private ApplicantSimpleStorage applicantSimpleStorage;

  @Before
  public void setUp() {
    this.applicantSimpleStorage = instanceOf(ApplicantSimpleStorage.class);
  }

  @Test
  public void getPresignedUrlString_hasSetupParams() {

  }

  @Test
  public void getPresignedUrlString_hasFileKey() {

  }

  @Test
  public void getPresignedUrlString_hasSuccessRedirect() {

  }

}
