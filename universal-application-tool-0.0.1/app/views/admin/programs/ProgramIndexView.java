package views.admin.programs;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.Tag;
import java.util.Comparator;
import models.LifecycleStage;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.StyleUtils;
import views.style.Styles;

public final class ProgramIndexView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramIndexView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(ImmutableList<ProgramDefinition> programs, Http.Request request) {
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1("All Programs").withClasses(Styles.MY_4),
                each(
                    programs.stream()
                        .sorted(Comparator.comparing(program -> program.lifecycleStage()))
                        .collect(ImmutableList.toImmutableList()),
                    e -> this.renderProgramListItem(e, request)),
                renderNewProgramButton());

    return layout.render(head(layout.tailwindStyles()), body(contentDiv));
  }

  public Tag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    return new LinkElement()
        .setId("new-program-button")
        .setHref(link)
        .setText("Create new program")
        .asButton();
  }

  public Tag renderProgramListItem(ProgramDefinition program, Http.Request request) {
    // TODO: Move Strings out of here for i18n.
    String programStatusText = program.lifecycleStage().name();
    String lastEditText = "Last updated 2 hours ago."; // TODO: Need to generate this.
    String publishLinkText = "Publish";
    String viewApplicationsLinkText = "Applications →";

    String programTitleText = program.adminName();
    String programDescriptionText = program.getDescriptionForDefaultLocale();

    long programId = program.id();
    String blockCountText = "Blocks: " + program.getBlockCount();
    String questionCountText = "Questions: " + program.getQuestionCount();

    Tag topContent =
        div(
                div(
                    p(programStatusText).withClasses(Styles.TEXT_SM, Styles.TEXT_GRAY_700),
                    div(programTitleText)
                        .withClasses(
                            Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_XL, Styles.MB_2)),
                p().withClasses(Styles.FLEX_GROW),
                div(p(blockCountText), p(questionCountText))
                    .withClasses(
                        Styles.TEXT_RIGHT,
                        Styles.TEXT_XS,
                        Styles.TEXT_GRAY_700,
                        Styles.MR_2,
                        StyleUtils.applyUtilityClass(StyleUtils.RESPONSIVE_MD, Styles.MR_4)))
            .withClasses(Styles.FLEX);

    Tag midContent =
        div(programDescriptionText)
            .withClasses(
                Styles.TEXT_GRAY_700,
                Styles.TEXT_BASE,
                Styles.MB_8,
                "line-clamp-3" /* TODO: Add tailwind plugin for line clamping. */);

    Tag bottomContent =
        div(
                p(lastEditText).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC),
                p().withClasses(Styles.FLEX_GROW),
                renderViewApplicationsLink(viewApplicationsLinkText, programId),
                renderEditLink(program, request),
                maybeRenderPublishLink(publishLinkText, program, request))
            .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL);

    Tag innerDiv =
        div(topContent, midContent, bottomContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv).withClasses(Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4);
  }

  Tag maybeRenderPublishLink(String text, ProgramDefinition program, Http.Request request) {
    if (program.lifecycleStage().equals(LifecycleStage.DRAFT)) {
      String publishLink =
          controllers.admin.routes.AdminProgramController.publish(program.id()).url();

      return new LinkElement()
          .setId("program-publish-link-" + program.id())
          .setHref(publishLink)
          .setText(text)
          .setStyles(Styles.MR_2)
          .asHiddenForm(request);
    }
    // an empty div().
    return div();
  }

  Tag renderEditLink(ProgramDefinition program, Http.Request request) {
    String editLinkText = "Edit →";
    String newVersionText = "New Version";

    if (program.lifecycleStage().equals(LifecycleStage.DRAFT)) {
      String editLink = controllers.admin.routes.AdminProgramController.edit(program.id()).url();

      return new LinkElement()
          .setId("program-edit-link-" + program.id())
          .setHref(editLink)
          .setText(editLinkText)
          .setStyles(Styles.MR_2)
          .asAnchorText();
    } else if (program.lifecycleStage().equals(LifecycleStage.ACTIVE)) {
      String newVersionLink =
          controllers.admin.routes.AdminProgramController.newVersionFrom(program.id()).url();

      return new LinkElement()
          .setId("program-new-version-link-" + program.id())
          .setHref(newVersionLink)
          .setText(newVersionText)
          .setStyles(Styles.MR_2)
          .asHiddenForm(request);
    } else {
      // obsolete or deleted, no edit link, empty div.
      return div();
    }
  }

  Tag renderViewApplicationsLink(String text, long programId) {
    String editLink = routes.AdminApplicationController.answerList(programId).url();

    return new LinkElement()
        .setId("program-view-apps-link-" + programId)
        .setHref(editLink)
        .setText(text)
        .setStyles(Styles.MR_2)
        .asAnchorText();
  }
}
