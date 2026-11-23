package views.trustedintermediary;

import com.google.inject.Inject;
import controllers.LanguageUtils;
import play.i18n.Messages;
import services.MessageKey;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the trusted intermediary edit client page, rendering
 * TrustedIntermediaryEditClientPage.html. A 1:1 port of {@link EditTiClientView}.
 */
public final class TrustedIntermediaryEditClientPageView
    extends TrustedIntermediaryLayoutBaseView<TrustedIntermediaryEditClientPageViewModel> {

  @Inject
  public TrustedIntermediaryEditClientPageView(LayoutDeps layoutDeps, LanguageUtils languageUtils) {
    super(layoutDeps, languageUtils);
  }

  @Override
  protected String pageTitle(TrustedIntermediaryEditClientPageViewModel model, Messages messages) {
    return messages.at(MessageKey.TITLE_EDIT_CLIENT.getKeyName());
  }

  @Override
  protected String pageTemplate() {
    return "trustedintermediary/TrustedIntermediaryEditClientPage.html";
  }
}
