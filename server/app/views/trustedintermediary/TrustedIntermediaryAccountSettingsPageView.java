package views.trustedintermediary;

import com.google.inject.Inject;
import controllers.LanguageUtils;
import play.i18n.Messages;
import services.MessageKey;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the trusted intermediary account settings page, rendering
 * TrustedIntermediaryAccountSettingsPage.html. A 1:1 port of {@link
 * TrustedIntermediaryAccountSettingsView}.
 */
public final class TrustedIntermediaryAccountSettingsPageView
    extends TrustedIntermediaryLayoutBaseView<TrustedIntermediaryAccountSettingsPageViewModel> {

  @Inject
  public TrustedIntermediaryAccountSettingsPageView(
      LayoutDeps layoutDeps, LanguageUtils languageUtils) {
    super(layoutDeps, languageUtils);
  }

  @Override
  protected String pageTitle(
      TrustedIntermediaryAccountSettingsPageViewModel model, Messages messages) {
    return messages.at(MessageKey.TITLE_TI_ACCOUNT_SETTINGS.getKeyName());
  }

  @Override
  protected String pageTemplate() {
    return "trustedintermediary/TrustedIntermediaryAccountSettingsPage.html";
  }
}
