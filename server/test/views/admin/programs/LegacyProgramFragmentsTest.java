package views.admin.programs;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the program-specific fragments under {@code admin/programs/fragments/} with
 * thymeleaf-testing and compares the result against the expected markup. The generic field
 * fragments these build on live under {@code admin/shared/LegacyFieldWithLabel/} and are covered by
 * {@code views.admin.shared.LegacyFieldFragmentsTest}.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/programs/legacyProgramFragments/}: it declares the fragment call,
 * the context it needs, and the markup the fragment is expected to produce.
 */
public class LegacyProgramFragmentsTest {

  private static final String DIR = "admin/programs/legacyProgramFragments/";

  /**
   * The shared field fragments are faked so only ProgramTranslationField's wiring is checked: the
   * field's values and required flag are forwarded with the call-site label, and the English hint
   * HTML lands unescaped.
   */
  @Test
  public void translationTextField_forwardsFieldAndLabelWithUnescapedEnglishHint() {
    ThymeleafFragmentTester.run(DIR + "translationTextField.thtest");
  }

  /**
   * The textarea variant forwards the call-site row count, never enables the markdown indicator,
   * and does not render the text-input branch.
   */
  @Test
  public void translationTextareaField_forwardsRowsWithoutMarkdown() {
    ThymeleafFragmentTester.run(DIR + "translationTextareaField.thtest");
  }
}
