package views.trustedintermediary;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the trusted intermediary dashboard fragments under {@code trustedintermediary/fragments/}
 * with thymeleaf-testing and compares the result against the markup the legacy j2html views
 * produced.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/trustedintermediary/dashboardFragments/}: it declares the fragment call,
 * the context it needs, and the markup the fragment is expected to produce.
 */
public class TrustedIntermediaryDashboardFragmentsTest {

  private static final String DIR = "trustedintermediary/dashboardFragments/";

  /** Legacy source: TrustedIntermediaryDashboardView.renderTabButtons with TabType.CLIENT_LIST. */
  @Test
  public void tabButtons_clientListIsCurrent() {
    ThymeleafFragmentTester.run(DIR + "tabButtonsClientList.thtest");
  }

  /**
   * Legacy source: TrustedIntermediaryDashboardView.renderTabButtons with TabType.ACCOUNT_SETTINGS.
   */
  @Test
  public void tabButtons_accountSettingsIsCurrent() {
    ThymeleafFragmentTester.run(DIR + "tabButtonsAccountSettings.thtest");
  }

  /** Legacy source: TrustedIntermediaryClientListView.renderClientCard. */
  @Test
  public void clientCard_complete_showsDateOfBirthContactInfoApplicationsAndNotes() {
    ThymeleafFragmentTester.run(DIR + "clientCardComplete.thtest");
  }

  /** Legacy source: the empty divs renderClientCard's helpers return without an applicant. */
  @Test
  public void clientCard_noApplicant_keepsEmptyPlaceholders() {
    ThymeleafFragmentTester.run(DIR + "clientCardNoApplicant.thtest");
  }

  @Test
  public void clientCard_oneApplication_usesSingularWordingAndOmitsMissingContactInfo() {
    ThymeleafFragmentTester.run(DIR + "clientCardOneApplication.thtest");
  }

  /** Legacy source: ViewUtils.makeMemorableDate with showError and showRequired false. */
  @Test
  public void memorableDate_completeDate_selectsMonthAndShowsNoError() {
    ThymeleafFragmentTester.run(DIR + "memorableDateValues.thtest");
  }

  /** Legacy source: ViewUtils.makeMemorableDate with showError true. */
  @Test
  public void memorableDate_incompleteDate_showsErrorAndOutlinesEmptyParts() {
    ThymeleafFragmentTester.run(DIR + "memorableDateError.thtest");
  }
}
