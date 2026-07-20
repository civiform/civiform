package views.admin.questions;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the fragments in {@code admin/questions/fragments/LegacyFieldFragments.html} with
 * thymeleaf-testing and compares the result against the expected markup.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/questions/legacyFieldFragments/}: it declares the fragment call,
 * the context/messages it needs, and the markup the fragment is expected to produce.
 */
public class LegacyFieldFragmentsTest {

  private static final String DIR = "admin/questions/legacyFieldFragments/";

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

  @Test
  public void dateTypeSelect_noValue() {
    ThymeleafFragmentTester.run(DIR + "dateTypeSelectNoValue.thtest");
  }

  @Test
  public void dateTypeSelect_custom() {
    ThymeleafFragmentTester.run(DIR + "dateTypeSelectCustom.thtest");
  }

  @Test
  public void memorableDate() {
    ThymeleafFragmentTester.run(DIR + "memorableDate.thtest");
  }

  /**
   * Guards an OGNL pitfall: single-quoted single-character literals ('3') are chars, which never
   * equal a String month value. The fragment must compare against double-quoted literals so months
   * 1-9 render as selected, like the legacy j2html did.
   */
  @Test
  public void memorableDate_singleDigitMonthIsSelected() {
    ThymeleafFragmentTester.run(DIR + "memorableDateSingleDigitMonth.thtest");
  }

  /**
   * The pages render the admin-ID tooltip inline (not via a field fragment); this pins the icon it
   * pulls from LegacySvgFragments to the legacy FieldWithLabel tooltip icon, Icons.INFO.
   */
  @Test
  public void iconInfo_matchesLegacyTooltipIcon() {
    ThymeleafFragmentTester.run(DIR + "iconInfo.thtest");
  }

  @Test
  public void multiOptionRow_newOption() {
    ThymeleafFragmentTester.run(DIR + "multiOptionRowNew.thtest");
  }

  @Test
  public void multiOptionRow_existingOption() {
    ThymeleafFragmentTester.run(DIR + "multiOptionRowExisting.thtest");
  }
}
