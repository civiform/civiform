package views.admin.programs;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the legacy-parity program form fragments under {@code admin/programs/fragments/} with
 * thymeleaf-testing and compares the result against the expected markup. The text and textarea
 * fields the form uses are the shared fragments covered by {@link
 * views.admin.shared.LegacyFieldFragmentsTest}.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/programs/programFormFragments/}: it declares the fragment call,
 * the context it needs, and the markup the fragment is expected to produce, derived from the legacy
 * j2html ProgramFormBuilder output.
 */
public class ProgramFormFragmentsTest {

  private static final String DIR = "admin/programs/programFormFragments/";

  @Test
  public void uswdsRadioOption_checkedWithDescription() {
    ThymeleafFragmentTester.run(DIR + "radioOptionCheckedWithDescription.thtest");
  }

  @Test
  public void uswdsRadioOption_uncheckedDisabledWithoutDescription() {
    ThymeleafFragmentTester.run(DIR + "radioOptionDisabledNoDescription.thtest");
  }

  @Test
  public void uswdsCheckboxOption_checkedWithValue() {
    ThymeleafFragmentTester.run(DIR + "checkboxOptionChecked.thtest");
  }

  @Test
  public void uswdsCheckboxOption_nullValueOmitsValueAttribute() {
    ThymeleafFragmentTester.run(DIR + "checkboxOptionNoValue.thtest");
  }

  @Test
  public void uswdsCheckboxOption_customWrapperClassesReplaceUsaCheckbox() {
    ThymeleafFragmentTester.run(DIR + "checkboxOptionCustomWrapper.thtest");
  }
}
