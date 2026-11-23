package views.admin.programs;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders {@code admin/programs/fragments/ProgramIndexProgramCard.html} with thymeleaf-testing and
 * compares the result against the expected markup.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/programs/programIndexFragments/}: it declares the fragment call,
 * the context it needs, and the markup the fragment is expected to produce.
 */
public class ProgramIndexFragmentsTest {

  private static final String DIR = "admin/programs/programIndexFragments/";

  /** Draft row above a bordered active row, with admin note and universal question text. */
  @Test
  public void programCard_draftAndActiveRows() {
    ThymeleafFragmentTester.run(DIR + "programCardDraftAndActive.thtest");
  }

  /** External program: image, incomplete translation, and an invisible extra-actions button. */
  @Test
  public void programCard_externalActiveRowWithImageAndNoExtraActions() {
    ThymeleafFragmentTester.run(DIR + "programCardExternalActive.thtest");
  }

  /** Pre-screener indicator, and an extra action that POSTs through a hidden form. */
  @Test
  public void programCard_preScreenerActiveRowWithPostAction() {
    ThymeleafFragmentTester.run(DIR + "programCardPreScreenerPostAction.thtest");
  }
}
