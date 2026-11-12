package views.admin.programs;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the fragments in {@code admin/programs/fragments/ProgramStatusesFragments.html} with
 * thymeleaf-testing and compares the result against the expected markup.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/programs/programStatusesFragments/}: it declares the fragment
 * call, the context it needs, and the markup the fragment is expected to produce, derived from the
 * legacy j2html ProgramStatusesView output.
 */
public class ProgramStatusesFragmentsTest {

  private static final String DIR = "admin/programs/programStatusesFragments/";

  /** Status row without an email or the default flag: both indicator columns stay empty. */
  @Test
  public void statusItem_plainStatus() {
    ThymeleafFragmentTester.run(DIR + "statusItemPlain.thtest");
  }

  /** Status row showing both the notification email and default status indicators. */
  @Test
  public void statusItem_withEmailAndDefault() {
    ThymeleafFragmentTester.run(DIR + "statusItemEmailDefault.thtest");
  }

  /** Empty create modal: closed on load, no errors, bare "to " message part. */
  @Test
  public void statusFormModal_emptyCreateModal() {
    ThymeleafFragmentTester.run(DIR + "statusFormModalCreate.thtest");
  }

  /**
   * Rejected edit: opens on load, renders global and field errors, the email deletion warning, the
   * checked default checkbox, and the current default spliced into the message part.
   */
  @Test
  public void statusFormModal_rejectedEditWithErrors() {
    ThymeleafFragmentTester.run(DIR + "statusFormModalEditErrors.thtest");
  }

  /** Delete confirmation modal posting the status text. */
  @Test
  public void deleteStatusModal_postsStatusText() {
    ThymeleafFragmentTester.run(DIR + "deleteStatusModal.thtest");
  }

  /** Status name input carrying a validation error. */
  @Test
  public void statusTextField_withError() {
    ThymeleafFragmentTester.run(DIR + "statusTextFieldError.thtest");
  }

  /** Email body textarea carrying a validation error. */
  @Test
  public void emailBodyField_withError() {
    ThymeleafFragmentTester.run(DIR + "emailBodyFieldError.thtest");
  }
}
