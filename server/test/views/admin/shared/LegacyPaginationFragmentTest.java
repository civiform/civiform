package views.admin.shared;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders {@code admin/shared/LegacyPagination.html} with thymeleaf-testing and compares the result
 * against the markup the legacy {@code BaseHtmlView.renderPagination} produced.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/shared/legacyPagination/}. The arrow icons are faked so these
 * tests stay about the pagination list; the icons have their own thtests under {@code
 * admin/shared/legacySvgFragments/}. Which slots appear for a given page is decided by {@code
 * LegacyPaginationMapper}, covered by its own unit test.
 */
public class LegacyPaginationFragmentTest {

  private static final String DIR = "admin/shared/legacyPagination/";

  @Test
  public void pagination_firstPage_hasNextArrowOnly_andMarksCurrentPage() {
    ThymeleafFragmentTester.run(DIR + "firstPage.thtest");
  }

  @Test
  public void pagination_middlePage_hasBothArrowsAndEllipses() {
    ThymeleafFragmentTester.run(DIR + "middlePage.thtest");
  }

  @Test
  public void pagination_lastPage_hasPreviousArrowOnly() {
    ThymeleafFragmentTester.run(DIR + "lastPage.thtest");
  }
}
