package views.admin.settings;

import org.junit.Test;
import support.thymeleaf.ThymeleafFragmentTester;

/**
 * Renders the settings page fragments under {@code admin/settings/fragments/} ({@code
 * SettingInputs.html}, {@code Setting.html} and {@code SettingsSection.html}) with
 * thymeleaf-testing and compares the result against the markup the legacy j2html settings page
 * produced.
 *
 * <p>Each case is a {@code .thtest} file under {@code
 * test/resources/thymeleaf/admin/settings/settingsFragments/}: it declares the fragment call, the
 * context it needs, and the markup the fragment is expected to produce.
 */
public class SettingsFragmentsTest {

  private static final String DIR = "admin/settings/settingsFragments/";

  @Test
  public void stringInput_rendersValueAndPlaceholder() {
    ThymeleafFragmentTester.run(DIR + "stringInput.thtest");
  }

  @Test
  public void stringInput_readOnlyIsDisabledAndAnnotated() {
    ThymeleafFragmentTester.run(DIR + "stringInputReadOnly.thtest");
  }

  @Test
  public void stringInput_rendersUpdateError() {
    ThymeleafFragmentTester.run(DIR + "stringInputError.thtest");
  }

  @Test
  public void numberInput_rendersValue() {
    ThymeleafFragmentTester.run(DIR + "numberInputValue.thtest");
  }

  @Test
  public void numberInput_unsetValueDropsTheValueAttribute() {
    ThymeleafFragmentTester.run(DIR + "numberInputEmpty.thtest");
  }

  @Test
  public void enumInput_selectsTheCurrentValue() {
    ThymeleafFragmentTester.run(DIR + "enumInputSelected.thtest");
  }

  @Test
  public void enumInput_unsetSelectsThePlaceholderOption() {
    ThymeleafFragmentTester.run(DIR + "enumInputUnset.thtest");
  }

  @Test
  public void boolInput_checksTrueWhenEnabled() {
    ThymeleafFragmentTester.run(DIR + "boolInputChecked.thtest");
  }

  @Test
  public void boolInput_readOnlyDisablesBothRadiosAndShowsTheError() {
    ThymeleafFragmentTester.run(DIR + "boolInputReadOnlyError.thtest");
  }

  @Test
  public void setting_stringSettingRendersTheStringInput() {
    ThymeleafFragmentTester.run(DIR + "settingString.thtest");
  }

  @Test
  public void setting_booleanSettingRendersTheRadios() {
    ThymeleafFragmentTester.run(DIR + "settingBoolean.thtest");
  }

  @Test
  public void setting_enumSettingRendersTheSelect() {
    ThymeleafFragmentTester.run(DIR + "settingEnum.thtest");
  }

  @Test
  public void setting_intSettingRendersTheNumberInput() {
    ThymeleafFragmentTester.run(DIR + "settingInt.thtest");
  }

  @Test
  public void topSection_rendersAnchoredHeadingSettingsAndSubsections() {
    ThymeleafFragmentTester.run(DIR + "topSectionWithSubsection.thtest");
  }
}
