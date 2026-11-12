package views.trustedintermediary;

import com.google.inject.Inject;
import controllers.LanguageUtils;
import play.i18n.Messages;
import services.MessageKey;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the trusted intermediary client list page, rendering
 * TrustedIntermediaryClientListPage.html. A 1:1 port of {@link TrustedIntermediaryClientListView}.
 */
public final class TrustedIntermediaryClientListPageView
    extends TrustedIntermediaryLayoutBaseView<TrustedIntermediaryClientListPageViewModel> {

  @Inject
  public TrustedIntermediaryClientListPageView(LayoutDeps layoutDeps, LanguageUtils languageUtils) {
    super(layoutDeps, languageUtils);
  }

  @Override
  protected String pageTitle(TrustedIntermediaryClientListPageViewModel model, Messages messages) {
    return messages.at(MessageKey.TITLE_TI_DASHBOARD.getKeyName());
  }

  @Override
  protected String pageTemplate() {
    return "trustedintermediary/TrustedIntermediaryClientListPage.html";
  }
}
