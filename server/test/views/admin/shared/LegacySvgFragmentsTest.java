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

  /** Legacy source: QuestionBank.renderFilterAndSort filter input. */
  @Test
  public void iconSearch_questionFilter() {
    ThymeleafFragmentTester.run(DIR + "iconSearch.thtest");
  }

  /** Legacy source: ViewUtils.makeUniversalBadge. */
  @Test
  public void iconStar_universalBadge() {
    ThymeleafFragmentTester.run(DIR + "iconStar.thtest");
  }

  /** Legacy source: ViewUtils.makeLifecycleBadge status dot. */
  @Test
  public void iconNoiseControlOff_lifecycleBadge() {
    ThymeleafFragmentTester.run(DIR + "iconNoiseControlOff.thtest");
  }

  /** Legacy source: QuestionsListView translation complete badge. */
  @Test
  public void iconCheck_translationBadge() {
    ThymeleafFragmentTester.run(DIR + "iconCheck.thtest");
  }

  /** Legacy source: QuestionsListView translation incomplete badge. */
  @Test
  public void iconClose_translationBadge() {
    ThymeleafFragmentTester.run(DIR + "iconCloseTranslationBadge.thtest");
  }

  /** Legacy source: Modal close button (Modal.getModalHeader). */
  @Test
  public void iconClose_modalCloseButton() {
    ThymeleafFragmentTester.run(DIR + "iconCloseModal.thtest");
  }

  /** Legacy source: ApiKeyCredentialsView API documentation link (LinkElement.setIcon). */
  @Test
  public void iconOpenInNew_link() {
    ThymeleafFragmentTester.run(DIR + "iconOpenInNewLink.thtest");
  }

  /** Legacy source: ViewUtils.makeSvgTextButton ("Edit"). */
  @Test
  public void iconEdit_svgTextButton() {
    ThymeleafFragmentTester.run(DIR + "iconEditSvgTextButton.thtest");
  }

  /** Legacy source: ViewUtils.makeSvgTextButton (extra actions dropdown button). */
  @Test
  public void iconMoreVert_svgTextButton() {
    ThymeleafFragmentTester.run(DIR + "iconMoreVertSvgTextButton.thtest");
  }

  /** Legacy source: ViewUtils.makeSvgTextButton ("Manage translations"). */
  @Test
  public void iconTranslate_svgTextButton() {
    ThymeleafFragmentTester.run(DIR + "iconTranslateSvgTextButton.thtest");
  }

  /** Legacy source: ViewUtils.makeSvgTextButton ("Archive"). */
  @Test
  public void iconArchive_svgTextButton() {
    ThymeleafFragmentTester.run(DIR + "iconArchiveSvgTextButton.thtest");
  }

  /** Legacy source: ViewUtils.makeSvgTextButton ("Restore archived"). */
  @Test
  public void iconUnarchive_svgTextButton() {
    ThymeleafFragmentTester.run(DIR + "iconUnarchiveSvgTextButton.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconAddress_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconAddressCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconCheckbox_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconCheckboxCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconCurrency_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconCurrencyCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconDate_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconDateCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconDropdown_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconDropdownCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconEmail_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconEmailCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconFileupload_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconFileuploadCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconId_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconIdCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconMap_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconMapCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconName_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconNameCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconNumber_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconNumberCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconRadioButton_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconRadioButtonCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconEnumerator_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconEnumeratorCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconAnnotation_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconAnnotationCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconText_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconTextCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconPhone_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconPhoneCreateDropdown.thtest");
  }

  /** Legacy source: CreateQuestionButton dropdown (Icons.questionTypeSvgWithId). */
  @Test
  public void iconUnknown_createQuestionDropdown() {
    ThymeleafFragmentTester.run(DIR + "iconUnknownCreateDropdown.thtest");
  }

  /** Legacy source: QuestionsListView info cell (no svg-link id). */
  @Test
  public void iconAddress_questionCard() {
    ThymeleafFragmentTester.run(DIR + "iconAddressQuestionCard.thtest");
  }

  /** Legacy source: AdminImportView back button (Icons.svg(Icons.ARROW_LEFT)). */
  @Test
  public void iconArrowLeft_importPageBackLink() {
    ThymeleafFragmentTester.run(DIR + "iconArrowLeft.thtest");
  }

  /** Legacy source: LinkElement.opensInNewTab icon ("existing question" links on import). */
  @Test
  public void iconOpenInNew_existingQuestionLink() {
    ThymeleafFragmentTester.run(DIR + "iconOpenInNew.thtest");
  }
}
