package views.admin.programs;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.Tag;
import models.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.twirl.api.Content;
import services.applicant.ApplicantService;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.ReferenceClasses;
import views.style.Styles;

public final class ProgramApplicationListView extends BaseHtmlView {
  private final AdminLayout layout;
  private final Logger log = LoggerFactory.getLogger(ProgramApplicationListView.class);

  @Inject
  public ProgramApplicationListView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(long programId, ImmutableList<Application> applications) {
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1("All Applications").withClasses(Styles.MY_4),
                each(
                    applications,
                    application -> this.renderApplicationListItem(programId, application)),
                renderDownloadButton(programId));

    return layout.render(head(layout.tailwindStyles()), body(contentDiv));
  }

  private Tag renderDownloadButton(long programId) {
    String link = controllers.admin.routes.AdminApplicationController.downloadAll(programId).url();
    return new LinkElement()
        .setId("download-all-button")
        .setHref(link)
        .setText("Download all (CSV)")
        .setStyles(ReferenceClasses.DOWNLOAD_ALL_BUTTON)
        .asButton();
  }

  private Tag renderApplicationListItem(long programId, Application application) {
    String downloadLinkText = "Download (PDF)";
    long applicationId = application.id;
    String applicantName = application.getApplicantData().getApplicantName();
    String lastEditText;
    try {
      lastEditText = application.getSubmitTime().toString();
    } catch (NullPointerException e) {
      log.error("Application {} submitted without submission time marked.", applicationId);
      lastEditText = "<ERROR>";
    }
    lastEditText = "Last edited " + lastEditText;
    String viewLinkText = "View →";

    Tag topContent =
        div(
                div(
                    div(applicantName)
                        .withClasses(
                            Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_XL, Styles.MB_2)),
                p().withClasses(Styles.FLEX_GROW))
            .withClasses(Styles.FLEX);

    Tag bottomContent =
        div(
                p(lastEditText).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC),
                p().withClasses(Styles.FLEX_GROW),
                renderDownloadLink(downloadLinkText, programId, applicationId),
                renderViewLink(viewLinkText, programId, applicationId))
            .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL);

    Tag innerDiv =
        div(topContent, bottomContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv)
        .withClasses(
            ReferenceClasses.ADMIN_APPLICATION_CARD, Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4);
  }

  private Tag renderDownloadLink(String text, long programId, long applicationId) {
    String downloadLink =
        controllers.admin.routes.AdminApplicationController.download(programId, applicationId)
            .url();

    return new LinkElement()
        .setId("application-download-link-" + applicationId)
        .setHref(downloadLink)
        .setText(text)
        .setStyles(Styles.MR_2, ReferenceClasses.DOWNLOAD_BUTTON)
        .asAnchorText();
  }

  private Tag renderViewLink(String text, long programId, long applicationId) {
    String viewLink =
        controllers.admin.routes.AdminApplicationController.view(programId, applicationId).url();

    return new LinkElement()
        .setId("application-view-link-" + applicationId)
        .setHref(viewLink)
        .setText(text)
        .setStyles(Styles.MR_2, ReferenceClasses.VIEW_BUTTON)
        .asAnchorText();
  }
}
