package views.admin.programs;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the legacy-parity program form fragments under {@code admin/programs/fragments/} with
 * thymeleaf-testing and compares the result against the expected markup.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/programs/programFormFragments/}: it declares the fragment call,
 * the context it needs, and the markup the fragment is expected to produce, derived from the legacy
 * j2html ProgramFormBuilder output.
 */
public class ProgramFormFragmentsTest {

  private static final String DIR = "admin/programs/programFormFragments/";

  @Test
  public void textField_requiredWithCustomMaxLengthAndWrapper() {
    ThymeleafFragmentTester.run(DIR + "textFieldRequired.thtest");
  }

  @Test
  public void textField_disabledRendersReadonlyAttributesAndClasses() {
    ThymeleafFragmentTester.run(DIR + "textFieldDisabled.thtest");
  }

  @Test
  public void textareaField_markdownIndicatorWithoutDocsLink() {
    ThymeleafFragmentTester.run(DIR + "textareaFieldMarkdown.thtest");
  }

  @Test
  public void textareaField_disabledWithConfirmationMessageWrapper() {
    ThymeleafFragmentTester.run(DIR + "textareaFieldDisabled.thtest");
  }

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

  @Test
  public void applicationStep_firstStepWiresRequiredOneBasedFields() {
    ThymeleafFragmentTester.run(DIR + "applicationStepFirstRequired.thtest");
  }

  @Test
  public void applicationStep_laterStepIsOptionalAndPassesDisabledThrough() {
    ThymeleafFragmentTester.run(DIR + "applicationStepLaterDisabled.thtest");
  }
}
