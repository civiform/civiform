package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.iffElse;
import static j2html.TagCreator.nav;

import com.typesafe.config.Config;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.NavTag;
import java.util.Optional;
import play.i18n.Messages;
import services.MessageKey;
import views.BaseHtmlView;
import views.style.BaseStyles;

/** Parent class for all trusted intermediary dashboard-centered views */
public abstract class TrustedIntermediaryDashboardView extends BaseHtmlView {
  protected final String baseUrl;
  protected final ApplicantLayout layout;

  public TrustedIntermediaryDashboardView(Config configuration, ApplicantLayout layout) {
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.layout = checkNotNull(layout);
  }

  private ATag renderAccountSettingsLink(Messages messages) {
    return new ATag()
        .withId("account-settings-link")
        .withHref(
            baseUrl + controllers.ti.routes.TrustedIntermediaryController.accountSettings().url())
        .withText(messages.at(MessageKey.HEADER_ACCT_SETTING.getKeyName()));
  }

  private ATag renderClientListLink(Messages messages) {
    return new ATag()
        .withId("client-list-link")
        .withHref(
            baseUrl
                + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                        /* nameQuery= */ Optional.empty(),
                        /* dayQuery= */ Optional.empty(),
                        /* monthQuery= */ Optional.empty(),
                        /* yearQuery= */ Optional.empty(),
                        /* page= */ Optional.of(1))
                    .url())
        .withText(messages.at(MessageKey.HEADER_CLIENT_LIST.getKeyName()));
  }

  protected enum TabType {
    CLIENT_LIST,
    ACCOUNT_SETTINGS
  }

  protected NavTag renderTabButtons(Messages messages, TabType currentTab) {
    boolean isClientList = currentTab == TabType.CLIENT_LIST;
    boolean isAccountSettings = currentTab == TabType.ACCOUNT_SETTINGS;
    return nav(
            renderClientListLink(messages)
                .withClasses(
                    "mr-10",
                    "py-4",
                    iffElse(isClientList, BaseStyles.TI_NAV_CURRENT, BaseStyles.TI_NAV_NOT_CURRENT))
                .condAttr(isClientList, "aria-current", "page"),
            renderAccountSettingsLink(messages)
                .withClasses(
                    "py-4",
                    iffElse(
                        isAccountSettings,
                        BaseStyles.TI_NAV_CURRENT,
                        BaseStyles.TI_NAV_NOT_CURRENT))
                .condAttr(isAccountSettings, "aria-current", "page"))
        .withClasses("px-20", "text-sm", "tracking-tight", "flex")
        .attr("aria-label", "Trusted Intermediary")
        .attr("data-testid", "ti-nav");
  }
}
