package views.admin.apikeys;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the API key page fragments under {@code admin/apikeys/fragments/} with thymeleaf-testing
 * and compares the result against the expected markup.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/apikeys/apiKeyFragments/}: it declares the fragment call, the
 * context it needs, and the markup the fragment is expected to produce.
 */
public class ApiKeyFragmentsTest {

  private static final String DIR = "admin/apikeys/apiKeyFragments/";

  @Test
  public void apiKeyCard_active() {
    ThymeleafFragmentTester.run(DIR + "apiKeyCardActive.thtest");
  }

  @Test
  public void apiKeyCard_retired() {
    ThymeleafFragmentTester.run(DIR + "apiKeyCardRetired.thtest");
  }
}
