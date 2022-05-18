package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.iframe;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.Tag;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import models.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import services.PaginationResult;
import services.PaginationSpec;
import services.program.ProgramDefinition;
import views.ApplicantUtils;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Renders a page for viewing applications to a program. */
public final class ProgramApplicationListView extends BaseHtmlView {
  private final AdminLayout layout;
  private final ApplicantUtils applicantUtils;
  private final Logger log = LoggerFactory.getLogger(ProgramApplicationListView.class);

  @Inject
  public ProgramApplicationListView(AdminLayout layout, ApplicantUtils applicantUtils) {
    this.layout = checkNotNull(layout).setOnlyProgramAdminType();
    this.applicantUtils = checkNotNull(applicantUtils);
  }

  public Content render(
      Http.Request request,
      ProgramDefinition program,
      PaginationSpec paginationSpec,
      PaginationResult<Application> paginatedApplications,
      Optional<String> search) {
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1(program.adminName()).withClasses(Styles.MY_4),
                renderPaginationDiv(
                        paginationSpec.getCurrentPage(),
                        paginatedApplications.getNumPages(),
                        pageNumber ->
                            routes.AdminApplicationController.index(
                                program.id(), search, Optional.of(pageNumber)))
                    .withClasses(Styles.MB_2),
                br(),
                renderSearchForm(
                        request,
                        search,
                        routes.AdminApplicationController.index(
                            program.id(), Optional.empty(), Optional.empty()),
                        Optional.of(Styles.W_FULL),
                        Optional.of("Search first name, last name, or application ID"))
                    .withClasses(Styles.MT_6),
                each(paginatedApplications.getPageContents(), this::renderApplicationListItem),
                br(),
                renderCsvDownloadButton(program.id()),
                renderJsonDownloadButton(program.id()))
            .withClasses(Styles.MB_16, Styles.MR_2);

    Tag applicationShowDiv =
        div()
            .withClasses(Styles.W_FULL, Styles.H_FULL)
            .with(
                iframe()
                    .withId("application-display-frame")
                    .withClasses(Styles.W_FULL, Styles.H_FULL));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(program.adminName() + " - Applications")
            .addFooterScripts(layout.viewUtils.makeLocalJsTag("admin_applications"))
            .addMainStyles(Styles.FLEX)
            .addMainContent(contentDiv, applicationShowDiv);

    return layout.renderCentered(htmlBundle);
  }

  private Tag renderCsvDownloadButton(long programId) {
    String link = controllers.admin.routes.AdminApplicationController.downloadAll(programId).url();
    return new LinkElement()
        .setId("download-all-button")
        .setHref(link)
        .setText("Download all versions (CSV)")
        .setStyles(ReferenceClasses.DOWNLOAD_ALL_BUTTON)
        .asButton();
  }

  private Tag renderJsonDownloadButton(long programId) {
    String link =
        controllers.admin.routes.AdminApplicationController.downloadAllJson(programId).url();
    return new LinkElement()
        .setId("download-all-json-button")
        .setHref(link)
        .setText("Download all versions (JSON)")
        .setStyles(ReferenceClasses.DOWNLOAD_ALL_BUTTON)
        .asButton();
  }

  private Tag renderApplicationListItem(Application application) {
    String applicantNameWithApplicationId =
        String.format(
            "%s (%d)",
            applicantUtils.getApplicantNameEnUs(application.getApplicantData().getApplicantName()),
            application.id);
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
                p(getSubmitTime(application)).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC),
                p().withClasses(Styles.FLEX_GROW),
                renderViewLink(viewLinkText, application))
            .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL);

    Tag innerDiv =
        div(topContent, bottomContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv)
        .withClasses(
            ReferenceClasses.ADMIN_APPLICATION_CARD, Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4);
  }

  private Tag getSubmitTime(Application application) {
    try {
      return span()
          .withText(
              DateTimeFormatter.RFC_1123_DATE_TIME
                  .withZone(ZoneId.systemDefault())
                  .format(application.getSubmitTime()));
    } catch (NullPointerException e) {
      log.error("Application {} submitted without submission time marked.", application.id);
      return span();
    }
  }

  private Tag renderViewLink(String text, Application application) {
    String viewLink =
        controllers.admin.routes.AdminApplicationController.show(
                application.getProgram().id, application.id)
            .url();

    return new LinkElement()
        .setId("application-view-link-" + application.id)
        .setHref(viewLink)
        .setText(text)
        .setStyles(Styles.MR_2, ReferenceClasses.VIEW_BUTTON)
        .asAnchorText();
  }
}
