package views.admin.shared;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the shared legacy-parity field fragments under {@code admin/shared/LegacyFieldWithLabel/}
 * (one file per fragment, e.g. {@code RequiredIndicator.html}, {@code TextareaField.html}) with
 * thymeleaf-testing and compares the result against the expected markup.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/shared/legacyFieldFragments/}: it declares the fragment call, the
 * context/messages it needs, and the markup the fragment is expected to produce.
 */
public class LegacyFieldFragmentsTest {

  private static final String DIR = "admin/shared/legacyFieldFragments/";

  @Test
  public void requiredIndicator_visible() {
    ThymeleafFragmentTester.run(DIR + "requiredIndicatorVisible.thtest");
  }

  @Test
  public void requiredIndicator_hidden() {
    ThymeleafFragmentTester.run(DIR + "requiredIndicatorHidden.thtest");
  }

  @Test
  public void markdownIndicator() {
    ThymeleafFragmentTester.run(DIR + "markdownIndicator.thtest");
  }

  @Test
  public void slimInfoAlert_visible() {
    ThymeleafFragmentTester.run(DIR + "slimInfoAlertVisible.thtest");
  }

  @Test
  public void slimInfoAlert_hidden() {
    ThymeleafFragmentTester.run(DIR + "slimInfoAlertHidden.thtest");
  }

  @Test
  public void toggle_checked() {
    ThymeleafFragmentTester.run(DIR + "toggleChecked.thtest");
  }

  @Test
  public void toggle_uncheckedAndHidden() {
    ThymeleafFragmentTester.run(DIR + "toggleUncheckedAndHidden.thtest");
  }

  @Test
  public void textareaField_requiredWithMarkdown() {
    ThymeleafFragmentTester.run(DIR + "textareaFieldRequiredWithMarkdown.thtest");
  }

  @Test
  public void textareaField_optionalEmpty() {
    ThymeleafFragmentTester.run(DIR + "textareaFieldOptionalEmpty.thtest");
  }

  @Test
  public void numberField() {
    ThymeleafFragmentTester.run(DIR + "numberField.thtest");
  }

  @Test
  public void numberField_empty() {
    ThymeleafFragmentTester.run(DIR + "numberFieldEmpty.thtest");
  }

  @Test
  public void radioOption_checked() {
    ThymeleafFragmentTester.run(DIR + "radioOptionChecked.thtest");
  }

  @Test
  public void radioOption_unchecked() {
    ThymeleafFragmentTester.run(DIR + "radioOptionUnchecked.thtest");
  }
}
