package views.admin.ti;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the trusted intermediary page fragments under {@code admin/ti/fragments/} with
 * thymeleaf-testing and compares the result against the expected markup.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/ti/trustedIntermediaryFragments/}: it declares the fragment call,
 * the context it needs, and the markup the fragment is expected to produce.
 */
public class TrustedIntermediaryFragmentsTest {

  private static final String DIR = "admin/ti/trustedIntermediaryFragments/";

  @Test
  public void tiRow_signedInMemberShowsOkStatusAndDeleteForm() {
    ThymeleafFragmentTester.run(DIR + "tiRowSignedIn.thtest");
  }

  @Test
  public void tiRow_memberWithoutApplicantShowsNotYetSignedIn() {
    ThymeleafFragmentTester.run(DIR + "tiRowNotSignedIn.thtest");
  }
}
