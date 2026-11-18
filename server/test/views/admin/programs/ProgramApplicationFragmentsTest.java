package views.admin.programs;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the application view fragments under {@code admin/programs/fragments/} ({@code
 * ApplicationBlockCard.html}, {@code LegacyModal.html}, {@code ApplicationNoteModal.html}, {@code
 * ApplicationStatusModal.html}) with thymeleaf-testing and compares the result against the expected
 * markup.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/programs/programApplicationFragments/}: it declares the fragment
 * call, the context it needs, and the markup the fragment is expected to produce, derived from the
 * legacy j2html {@link ProgramApplicationView} DOM.
 */
public class ProgramApplicationFragmentsTest {

  private static final String DIR = "admin/programs/programApplicationFragments/";

  /** Legacy source: ProgramApplicationView#renderApplicationBlock with a plain text answer. */
  @Test
  public void blockCard_textAnswerWithoutEligibility() {
    ThymeleafFragmentTester.run(DIR + "blockCardTextAnswer.thtest");
  }

  /** Legacy source: ProgramApplicationView#renderAnswer file branches. */
  @Test
  public void blockCard_multipleAndSingleFileAnswers() {
    ThymeleafFragmentTester.run(DIR + "blockCardFileAnswers.thtest");
  }

  @Test
  public void answer_eligibilityMet() {
    ThymeleafFragmentTester.run(DIR + "answerEligibilityMet.thtest");
  }

  @Test
  public void answer_eligibilityNotMet() {
    ThymeleafFragmentTester.run(DIR + "answerEligibilityNotMet.thtest");
  }

  /** Legacy source: Modal.getContainerTag with Width.THREE_FOURTHS and Top.QUARTER. */
  @Test
  public void modal_shellWithContentFragmentAndCloseButton() {
    ThymeleafFragmentTester.run(DIR + "legacyModalShell.thtest");
  }

  /** Legacy source: ProgramApplicationView#renderUpdateNoteConfirmationModal, no note yet. */
  @Test
  public void noteForm_withoutExistingNote() {
    ThymeleafFragmentTester.run(DIR + "noteFormEmpty.thtest");
  }

  /** Legacy source: ProgramApplicationView#renderUpdateNoteConfirmationModal, existing note. */
  @Test
  public void noteForm_withExistingNote() {
    ThymeleafFragmentTester.run(DIR + "noteFormWithNote.thtest");
  }

  /** Legacy source: renderStatusUpdateConfirmationModalEmailSection, no email body configured. */
  @Test
  public void statusUpdateForm_noEmailContentForStatus() {
    ThymeleafFragmentTester.run(DIR + "statusFormNoEmailContent.thtest");
  }

  /** Legacy source: renderStatusUpdateConfirmationModalEmailSection, applicant has no email. */
  @Test
  public void statusUpdateForm_applicantWithoutEmail() {
    ThymeleafFragmentTester.run(DIR + "statusFormNoApplicantEmail.thtest");
  }

  /** Legacy source: renderStatusUpdateConfirmationModalEmailSection, notify checkbox shown. */
  @Test
  public void statusUpdateForm_notifyApplicant() {
    ThymeleafFragmentTester.run(DIR + "statusFormNotify.thtest");
  }
}
