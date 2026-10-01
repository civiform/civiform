package views.admin.programs;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the program admin page fragments under {@code admin/programs/fragments/} with
 * thymeleaf-testing and compares the result against the expected markup.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/programs/programAdminFragments/}: it declares the fragment call,
 * the context it needs, and the markup the fragment is expected to produce. The expected markup is
 * the DOM the legacy {@code ProgramCardFactory} rendered for {@code
 * ProgramAdministratorProgramListView}.
 */
public class ProgramAdminFragmentsTest {

  private static final String DIR = "admin/programs/programAdminFragments/";

  /** Default program seen by a program admin: share link, applications, empty image column. */
  @Test
  public void programCard_defaultProgram() {
    ThymeleafFragmentTester.run(DIR + "programCardDefault.thtest");
  }

  /** Pre-screener form: type indicator and a "Forms" button instead of "Applications". */
  @Test
  public void programCard_preScreenerForm() {
    ThymeleafFragmentTester.run(DIR + "programCardPreScreener.thtest");
  }

  /** External program: type indicator and no row actions. */
  @Test
  public void programCard_externalProgram() {
    ThymeleafFragmentTester.run(DIR + "programCardExternal.thtest");
  }

  /** Admin note paragraph, joined categories and singular screen/question counts. */
  @Test
  public void programCard_adminNoteCategoriesAndSingularCounts() {
    ThymeleafFragmentTester.run(DIR + "programCardAdminNote.thtest");
  }

  /** CiviForm admin with a program image sees the image column. */
  @Test
  public void programCard_civiFormAdminWithImage() {
    ThymeleafFragmentTester.run(DIR + "programCardImage.thtest");
  }

  /** CiviForm admin without a program image sees the gray placeholder icon. */
  @Test
  public void programCard_civiFormAdminImagePlaceholder() {
    ThymeleafFragmentTester.run(DIR + "programCardImagePlaceholder.thtest");
  }
}
