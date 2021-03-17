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
import java.time.Clock;
import java.util.NoSuchElementException;
import models.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.twirl.api.Content;
import services.WellKnownPaths;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.StyleUtils;
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
                each(applications, this::renderApplicationListItem),
                renderDownloadButton(programId));

    return layout.render(head(layout.tailwindStyles()), body(contentDiv));
  }

  public Tag renderDownloadButton(long programId) {
    String link = controllers.admin.routes.AdminApplicationController.downloadAll(programId).url();
    return new LinkElement()
        .setId("download-all-button")
        .setHref(link)
        .setText("Download all (CSV)")
        .asButton();
  }

  public Tag renderApplicationListItem(Application application) {
    String downloadLinkText = "Download (PDF)";
    long applicationId = application.id;
    String applicantName = "applicant's name here"; // We have to implement "well known answers".
    String lastEditText;
    try {
      lastEditText = application.getApplicantData().readString(WellKnownPaths.APPLICATION_SUBMITTED_TIME).get();
    } catch (NoSuchElementException e) {
      log.error("Application {} submitted without submission time marked.", applicationId);
      lastEditText = "ERROR";
    }
    lastEditText = "Last edited " + lastEditText;

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
                renderDownloadLink(downloadLinkText, applicationId))
            .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL);

    Tag innerDiv =
        div(topContent, bottomContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv).withClasses(Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4);
  }

  Tag renderDownloadLink(String text, long applicationId) {
    String downloadLink = controllers.admin.routes.AdminApplicationController.download(applicationId).url();

    return new LinkElement()
        .setId("application-download-link-" + applicationId)
        .setHref(downloadLink)
        .setText(text)
        .setStyles(Styles.MR_2)
        .asAnchorText();
  }
}
