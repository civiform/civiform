package views.trustedintermediary;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the fragments of {@code trustedintermediary/fragments/ClientInfoForm.html}, shared by the
 * add client and edit client pages, with thymeleaf-testing and compares the result against the
 * markup the legacy j2html EditTiClientView produced.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/trustedintermediary/clientInfoForm/}.
 */
public class TrustedIntermediaryClientInfoFormFragmentsTest {

  private static final String DIR = "trustedintermediary/clientInfoForm/";

  /** Legacy source: FieldWithLabel.input().setRequired(true).getUSWDSInputTag(). */
  @Test
  public void inputField_required_showsIndicatorAndSetsAriaRequired() {
    ThymeleafFragmentTester.run(DIR + "inputFieldRequired.thtest");
  }

  /** Legacy source: FieldWithLabel.setFieldErrors, one "Error: ..." div per error. */
  @Test
  public void inputField_withErrors_outlinesInputAndListsErrors() {
    ThymeleafFragmentTester.run(DIR + "inputFieldErrors.thtest");
  }

  /** Legacy source: the phone field's inputmode attribute and placeholder text. */
  @Test
  public void inputField_withInputmodeAndPlaceholder_rendersBothAttributes() {
    ThymeleafFragmentTester.run(DIR + "inputFieldPhone.thtest");
  }

  /** Legacy source: FieldWithLabel.genLabelTag with a tool tip, ViewUtils.makeSvgToolTip. */
  @Test
  public void inputField_withTooltip_addsSpaceAndInfoTooltipToLabel() {
    ThymeleafFragmentTester.run(DIR + "inputFieldTooltip.thtest");
  }

  /** Legacy source: FieldWithLabel.textArea().getUSWDSTextareaTag(). */
  @Test
  public void textareaField_rendersValueAsEscapedText() {
    ThymeleafFragmentTester.run(DIR + "textareaField.thtest");
  }

  /** Legacy source: SelectWithLabel.getUSWDSSelectTag with a matching value. */
  @Test
  public void nameSuffixSelect_selectsStoredSuffix() {
    ThymeleafFragmentTester.run(DIR + "nameSuffixSelected.thtest");
  }

  /** Legacy source: SelectWithLabel.getUSWDSSelectTag with an empty value. */
  @Test
  public void nameSuffixSelect_withoutSuffix_selectsHiddenPlaceholder() {
    ThymeleafFragmentTester.run(DIR + "nameSuffixEmpty.thtest");
  }

  /** Legacy source: EditTiClientView.renderAddOrEditClientForm with the suffix flag off. */
  @Test
  public void clientInfoForm_rendersNoteFieldsCsrfTokenAndButtonsInLegacyOrder() {
    ThymeleafFragmentTester.run(DIR + "clientInfoFormFields.thtest");
  }

  /** Legacy source: AlertComponent.renderFullAlert with a title, as after adding a client. */
  @Test
  public void successAlert_withHeading_rendersHeadingAndText() {
    ThymeleafFragmentTester.run(DIR + "successAlertHeading.thtest");
  }

  /** Legacy source: AlertComponent.renderFullAlert without a title, as after editing a client. */
  @Test
  public void successAlert_withoutHeading_omitsHeading() {
    ThymeleafFragmentTester.run(DIR + "successAlertNoHeading.thtest");
  }

  /** Legacy source: EditTiClientView.renderBackLink. */
  @Test
  public void backLink_rendersArrowIconBeforeText() {
    ThymeleafFragmentTester.run(DIR + "backLink.thtest");
  }
}
