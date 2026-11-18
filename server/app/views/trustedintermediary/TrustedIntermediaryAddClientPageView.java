package views.trustedintermediary;

import com.google.inject.Inject;
import controllers.LanguageUtils;
import play.i18n.Messages;
import services.MessageKey;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the trusted intermediary add client page, rendering
 * TrustedIntermediaryAddClientPage.html. A 1:1 port of {@link EditTiClientView}.
 */
public final class TrustedIntermediaryAddClientPageView
    extends TrustedIntermediaryLayoutBaseView<TrustedIntermediaryAddClientPageViewModel> {

  @Inject
  public TrustedIntermediaryAddClientPageView(LayoutDeps layoutDeps, LanguageUtils languageUtils) {
    super(layoutDeps, languageUtils);
  }

  @Override
  protected String pageTitle(TrustedIntermediaryAddClientPageViewModel model, Messages messages) {
    return messages.at(MessageKey.TITLE_CREATE_CLIENT.getKeyName());
  }

  @Override
  protected String pageTemplate() {
    return "trustedintermediary/TrustedIntermediaryAddClientPage.html";
  }
}
