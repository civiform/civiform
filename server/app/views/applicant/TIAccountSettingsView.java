package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.TdTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
import java.util.Comparator;
import java.util.stream.Collectors;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.ti.TrustedIntermediaryGroupListView;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

public class TIAccountSettingsView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final String baseUrl;

  @Inject
  public TIAccountSettingsView(ApplicantLayout layout, Config configuration) {
    this.layout = checkNotNull(layout);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
  }

  public Content render(
      TrustedIntermediaryGroupModel tiGroup,
      ApplicantPersonalInfo personalInfo,
      Http.Request request,
      Messages messages,
      Long currentTisApplicantId) {
    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle("CiviForm")
            .addMainContent(
                h1(tiGroup.getName()).withClasses(BaseStyles.TI_HEADER_BAND_H1),
                TrustedIntermediaryDashboardView.renderTabButtons(
                    messages, baseUrl, TrustedIntermediaryDashboardView.TabType.ACCOUNT_SETTINGS),
                div(
                    renderSubHeader(messages.at(MessageKey.HEADER_ACCT_SETTING.getKeyName()))
                        .withClasses(BaseStyles.TI_HEADER_BAND_H2),
                    div(
                            h3(messages.at(MessageKey.TITLE_ORG_MEMBERS.getKeyName()))
                                .withClass("mt-8"),
                            renderTIMembersTable(tiGroup, messages))
                        .withClass("px-20")))
            .addMainStyles("bg-white");

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      LoggerFactory.getLogger(TrustedIntermediaryGroupListView.class)
          .info(request.flash().get("error").get());
      bundle.addToastMessages(
          ToastMessage.errorNonLocalized(flash.get("error").get()).setDuration(-1));
    }
    return layout.renderWithNav(request, personalInfo, messages, bundle, currentTisApplicantId);
  }

  private DivTag renderTIMembersTable(TrustedIntermediaryGroupModel tiGroup, Messages messages) {
    return div(
        table()
            .withData("testid", "org-members-table")
            .withClasses("usa-table", "usa-table--striped", "w-5/6")
            .with(renderGroupTableHeader(messages))
            .with(
                tbody(
                    each(
                        tiGroup.getTrustedIntermediaries().stream()
                            .sorted(Comparator.comparing(AccountModel::getApplicantName))
                            .collect(Collectors.toList()),
                        this::renderTIRow))));
  }

  private TheadTag renderGroupTableHeader(Messages messages) {
    return thead(
        tr().with(
                th(messages.at(MessageKey.NAME_LABEL.getKeyName()))
                    .withScope("col")
                    .withData("testid", "org-members-name")
                    .withClass("w-1/3"))
            .with(
                th(messages.at(MessageKey.EMAIL_LABEL.getKeyName()))
                    .withScope("col")
                    .withData("testid", "org-members-email")
                    .withClass("w-2/5"))
            .with(
                th(messages.at(MessageKey.ACCT_STATUS_LABEL.getKeyName()))
                    .withScope("col")
                    .withData("testid", "org-members-status")
                    .withClass("w-1/5")));
  }

  private TrTag renderTIRow(AccountModel ti) {
    return tr().withClass(ReferenceClasses.ADMIN_QUESTION_TABLE_ROW)
        .with(renderNameCell(ti))
        .with(renderEmailCell(ti))
        .with(renderStatusCell(ti));
  }

  private TdTag renderNameCell(AccountModel ti) {
    return td(ti.getApplicantName());
  }

  private TdTag renderEmailCell(AccountModel ti) {
    String emailField = ti.getEmailAddress();
    if (Strings.isNullOrEmpty(emailField)) {
      emailField = "(no email address)";
    }
    return td(emailField).withClasses(ReferenceClasses.BT_EMAIL);
  }

  private TdTag renderStatusCell(AccountModel ti) {
    String accountStatus = "OK";
    if (ti.ownedApplicantIds().isEmpty()) {
      accountStatus = "Not yet signed in.";
    }
    return td(accountStatus);
  }
}
