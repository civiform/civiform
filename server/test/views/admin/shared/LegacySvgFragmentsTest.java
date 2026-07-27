package views.admin.shared;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the fragments in {@code admin/shared/LegacySvgFragments.html} with thymeleaf-testing and
 * compares the result against the expected markup.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/shared/legacySvgFragments/}: it declares the fragment call and the
 * markup the fragment is expected to produce. Each icon the migrated pages use is exercised with
 * every sizing-class combination the legacy j2html admin views pass to {@code Icons.svg} for it, so
 * migrated pages can reuse these fragments beyond the question pages without visual drift.
 */
public class LegacySvgFragmentsTest {

  private static final String DIR = "admin/shared/legacySvgFragments/";

  /** Legacy source: ViewUtils.makeSvgToolTip, used by every admin tooltip. */
  @Test
  public void iconInfo_toolTip() {
    ThymeleafFragmentTester.run(DIR + "iconInfo.thtest");
  }

  /** Legacy source: ViewUtils.makeSvgTextButton ("Create a new status", "New API key", ...). */
  @Test
  public void iconPlus_svgTextButton() {
    ThymeleafFragmentTester.run(DIR + "iconPlus.thtest");
  }

  /** Legacy source: QuestionConfig multi-option move-up button. */
  @Test
  public void iconKeyboardArrowUp_optionRow() {
    ThymeleafFragmentTester.run(DIR + "iconKeyboardArrowUp.thtest");
  }

  /** Legacy source: QuestionConfig multi-option move-down button. */
  @Test
  public void iconKeyboardArrowDown_optionRow() {
    ThymeleafFragmentTester.run(DIR + "iconKeyboardArrowDown.thtest");
  }

  /** Legacy source: QuestionConfig multi-option delete button. */
  @Test
  public void iconDelete_optionRow() {
    ThymeleafFragmentTester.run(DIR + "iconDeleteOptionRow.thtest");
  }

  /** Legacy source: ViewUtils.makeSvgTextButton ("Delete", "Discard Draft", "Retire key", ...). */
  @Test
  public void iconDelete_svgTextButton() {
    ThymeleafFragmentTester.run(DIR + "iconDeleteSvgTextButton.thtest");
  }

  /** Legacy source: ProgramPredicateConfigureView delete-row button. */
  @Test
  public void iconDelete_predicateEditor() {
    ThymeleafFragmentTester.run(DIR + "iconDeletePredicateEditor.thtest");
  }

  /** Legacy source: FieldWithLabel markdown indicator, which fixes the sizing and text color. */
  @Test
  public void iconMarkdown() {
    ThymeleafFragmentTester.run(DIR + "iconMarkdown.thtest");
  }
}
