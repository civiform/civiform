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
  public void inputField_textWithValue() {
    ThymeleafFragmentTester.run(DIR + "inputFieldText.thtest");
  }

  @Test
  public void inputField_dateWithError() {
    ThymeleafFragmentTester.run(DIR + "inputFieldDateWithError.thtest");
  }

  @Test
  public void programCheckbox_unchecked() {
    ThymeleafFragmentTester.run(DIR + "programCheckboxUnchecked.thtest");
  }

  @Test
  public void programCheckbox_checked() {
    ThymeleafFragmentTester.run(DIR + "programCheckboxChecked.thtest");
  }
}
