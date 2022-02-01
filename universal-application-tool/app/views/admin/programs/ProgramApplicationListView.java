package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.Tag;
import java.util.Comparator;
import java.util.Optional;
import models.Application;
import models.Program;
import models.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Renders a page for viewing applications to a program. */
public final class ProgramApplicationListView extends BaseHtmlView {
  private final AdminLayout layout;
  private final Logger log = LoggerFactory.getLogger(ProgramApplicationListView.class);

  @Inject
  public ProgramApplicationListView(AdminLayout layout) {
    this.layout = checkNotNull(layout).setOnlyProgramAdminType();
  }

  public Content render(
      Http.Request request,
      long programId,
      ImmutableList<Application> applications,
      int page,
      int pageCount,
      Optional<String> search,
      ImmutableList<Program> previousVersions) {
    String title = "All Applications";
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1(title).withClasses(Styles.MY_4),
                renderPaginationDiv(
                        page,
                        pageCount,
                        pageNumber ->
                            routes.AdminApplicationController.index(
                                programId, search, Optional.of(pageNumber)))
                    .withClasses(Styles.MB_2),
                br(),
                renderSearchForm(
                        request,
                        search,
                        routes.AdminApplicationController.index(
                            programId, Optional.empty(), Optional.empty()))
                    .withClasses(Styles.MT_6),
                each(
                    applications,
                    application -> this.renderApplicationListItem(programId, application)),
                br(),
                renderDownloadButton(programId))
            .withClasses(Styles.MB_16);

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);
    if (!previousVersions.isEmpty()) {
      htmlBundle.addMainContent(
          br(),
          h2("Applications for other versions"),
          div(each(previousVersions, this::renderPreviousVersionDiv)));
    }
    return layout.renderCentered(htmlBundle);
  }

  private Tag renderPreviousVersionDiv(Program program) {
    Optional<Version> lastContainingVersion =
        program.getVersions().stream().max(Comparator.comparing(Version::getSubmitTime));
    if (lastContainingVersion.isEmpty()) {
      return div();
    }
    return div(
            div(String.format("Version %d", lastContainingVersion.get().id))
                .withClasses(Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_LG, Styles.MB_2),
            div(
                    p("Last edited " + lastContainingVersion.get().getSubmitTime().toString())
                        .withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC),
                    p().withClasses(Styles.FLEX_GROW),
                    renderApplicationsLink(
                        String.format(
                            "Applications (%d) →", program.getSubmittedApplications().size()),
                        program.id))
                .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL))
        .withClasses(
            Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);
  }

  private Tag renderDownloadButton(long programId) {
    String link = controllers.admin.routes.AdminApplicationController.downloadAll(programId).url();
    return new LinkElement()
        .setId("download-all-button")
        .setHref(link)
        .setText("Download all versions (CSV)")
        .setStyles(ReferenceClasses.DOWNLOAD_ALL_BUTTON)
        .asButton();
  }

  private Tag renderApplicationListItem(long programId, Application application) {
    String downloadLinkText = "Download (PDF)";
    long applicationId = application.id;
    String applicantNameWithApplicationId =
        String.format("%s (%d)", application.getApplicantData().getApplicantName(), application.id);
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
                    div(applicantNameWithApplicationId)
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
    // This link doesn't work since we don't have PDF filling yet.  Disable.
    String downloadLink =
        controllers.admin.routes.AdminApplicationController.download(programId, applicationId)
            .url();

    return new LinkElement()
        .setId("application-download-link-" + applicationId)
        .setHref(downloadLink)
        .setText(text)
        .setStyles(Styles.MR_2, ReferenceClasses.DOWNLOAD_BUTTON)
        .asAnchorText()
        // TODO: when the download link works, un-hide.
        .isHidden();
  }

  private Tag renderViewLink(String text, long programId, long applicationId) {
    String viewLink =
        controllers.admin.routes.AdminApplicationController.show(programId, applicationId).url();

    return new LinkElement()
        .setId("application-view-link-" + applicationId)
        .setHref(viewLink)
        .setText(text)
        .setStyles(Styles.MR_2, ReferenceClasses.VIEW_BUTTON)
        .asAnchorText();
  }

  private Tag renderApplicationsLink(String text, long programId) {
    String viewLink =
        controllers.admin.routes.AdminApplicationController.index(
                programId, Optional.empty(), Optional.empty())
            .url();

    return new LinkElement()
        .setId("applications-view-program-link-" + programId)
        .setHref(viewLink)
        .setText(text)
        .setStyles(Styles.MR_2, ReferenceClasses.VIEW_BUTTON)
        .asAnchorText();
  }
}
