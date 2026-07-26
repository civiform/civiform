package views.admin.questions;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the question-specific field fragments under {@code admin/questions/fragments/} (one file
 * per fragment, e.g. {@code MultiOptionRow.html}) with thymeleaf-testing and compares the result
 * against the expected markup. The generic field fragments these build on live under {@code
 * admin/shared/LegacyFieldWithLabel/} and are covered by {@code
 * views.admin.shared.LegacyFieldFragmentsTest}.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/questions/legacyQuestionFragments/}: it declares the fragment
 * call, the context/messages it needs, and the markup the fragment is expected to produce.
 */
public class LegacyQuestionFragmentsTest {

  private static final String DIR = "admin/questions/legacyQuestionFragments/";

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
   * DateTypeSelect and MemorableDate internals are covered above; this stubs them out and asserts
   * only DateFragment's wiring: the ANY fallback for an unset date type and the hidden flag pairing
   * each memorable date picker with its type select.
   */
  @Test
  public void dateFragment_customMinShowsPickerUnsetMaxHidesIt() {
    ThymeleafFragmentTester.run(DIR + "dateFragmentCustomMin.thtest");
  }

  /**
   * Mirror of the CUSTOM-min case: the min/max expressions are copy-paste symmetric, and only a
   * CUSTOM max evaluates the max-present branch.
   */
  @Test
  public void dateFragment_customMaxShowsPickerUnsetMinHidesIt() {
    ThymeleafFragmentTester.run(DIR + "dateFragmentCustomMax.thtest");
  }

  /**
   * Guards the required-option posting trick: a required option's checkbox is disabled (so it does
   * not post) and must be duplicated by a hidden displayedOptionIds[] input.
   */
  @Test
  public void yesNo_newQuestionShowsLabelAndRequiredOptionPostsViaHiddenInput() {
    ThymeleafFragmentTester.run(DIR + "yesNoRequiredHiddenInput.thtest");
  }

  @Test
  public void yesNo_existingQuestionHidesLabelAndUndisplayedOptionIsUnchecked() {
    ThymeleafFragmentTester.run(DIR + "yesNoUndisplayedUnchecked.thtest");
  }

  @Test
  public void translationField_textareaWithMarkdown() {
    ThymeleafFragmentTester.run(DIR + "translationFieldTextarea.thtest");
  }

  @Test
  public void translationField_textInput() {
    ThymeleafFragmentTester.run(DIR + "translationFieldTextInput.thtest");
  }

  @Test
  public void multiOptionRow_newOption() {
    ThymeleafFragmentTester.run(DIR + "multiOptionRowNew.thtest");
  }

  @Test
  public void multiOptionRow_existingOption() {
    ThymeleafFragmentTester.run(DIR + "multiOptionRowExisting.thtest");
  }

  /**
   * With showScores on (the answer-option-scoring flag and a supported question type), the row
   * grows to six grid rows and renders a never-readonly score input posting to optionScores[],
   * with the pre-formatted display value.
   */
  @Test
  public void multiOptionRow_scoredOption() {
    ThymeleafFragmentTester.run(DIR + "multiOptionRowScored.thtest");
  }
}
