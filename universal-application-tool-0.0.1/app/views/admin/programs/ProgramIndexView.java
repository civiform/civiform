package views.admin.programs;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.Tag;
import java.util.Optional;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public final class ProgramIndexView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramIndexView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(ActiveAndDraftPrograms programs, Http.Request request) {
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1("All Programs").withClasses(Styles.MY_4),
                div()
                    .withClasses(Styles.INLINE_BLOCK)
                    .with(renderNewProgramButton(), maybeRenderPublishButton(programs, request)),
                each(
                    programs.getProgramNames(),
                    name ->
                        this.renderProgramListItem(
                            programs.getActiveProgramDefinition(name),
                            programs.getDraftProgramDefinition(name),
                            request)));

    return layout.render(head(layout.tailwindStyles()), body(contentDiv));
  }

  private Tag maybeRenderPublishButton(ActiveAndDraftPrograms programs, Http.Request request) {
    // We should only render the publish button if there is at least one draft.
    if (programs.anyDraft()) {
      String link = routes.AdminProgramController.publish().url();
      return new LinkElement()
          .setId("publish-programs-button")
          .setHref(link)
          .setText("Publish all drafts")
          .asHiddenForm(request);
    } else {
      return div();
    }
  }

  private Tag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    return new LinkElement()
        .setId("new-program-button")
        .setHref(link)
        .setText("Create new program")
        .asButton();
  }

  public ProgramDefinition getDisplayProgram(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    if (draftProgram.isPresent()) {
      return draftProgram.get();
    }
    return activeProgram.get();
  }

  public Tag renderProgramListItem(
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      Http.Request request) {
    String programStatusText = extractProgramStatusText(draftProgram, activeProgram);
    String lastEditText = "Last updated 2 hours ago."; // TODO: Need to generate this.
    String viewApplicationsLinkText = "Applications →";

    ProgramDefinition displayProgram = getDisplayProgram(draftProgram, activeProgram);

    String programTitleText = displayProgram.adminName();
    String programDescriptionText = displayProgram.adminDescription();
    String blockCountText = "Blocks: " + displayProgram.getBlockCount();
    String questionCountText = "Questions: " + displayProgram.getQuestionCount();

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
                maybeRenderViewApplicationsLink(viewApplicationsLinkText, activeProgram),
                maybeRenderManageTranslationsLink(draftProgram),
                maybeRenderEditLink(draftProgram, activeProgram, request))
            .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL);

    Tag innerDiv =
        div(topContent, midContent, bottomContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv)
        .withClasses(
            ReferenceClasses.ADMIN_PROGRAM_CARD, Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4);
  }

  private String extractProgramStatusText(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    if (draftProgram.isPresent() && activeProgram.isPresent()) {
      return "Active, with draft";
    } else if (draftProgram.isPresent()) {
      return "Draft";
    } else if (activeProgram.isPresent()) {
      return "Active";
    }
    throw new IllegalArgumentException("Program neither active nor draft.");
  }

  Tag maybeRenderEditLink(
      Optional<ProgramDefinition> draftProgram,
      Optional<ProgramDefinition> activeProgram,
      Http.Request request) {
    String editLinkText = "Edit →";
    String newVersionText = "New Version";

    if (draftProgram.isPresent()) {
      String editLink =
          controllers.admin.routes.AdminProgramController.edit(draftProgram.get().id()).url();

      return new LinkElement()
          .setId("program-edit-link-" + draftProgram.get().id())
          .setHref(editLink)
          .setText(editLinkText)
          .setStyles(Styles.MR_2)
          .asAnchorText();
    } else if (activeProgram.isPresent()) {
      String newVersionLink =
          controllers.admin.routes.AdminProgramController.newVersionFrom(activeProgram.get().id())
              .url();

      return new LinkElement()
          .setId("program-new-version-link-" + activeProgram.get().id())
          .setHref(newVersionLink)
          .setText(newVersionText)
          .setStyles(Styles.MR_2)
          .asHiddenForm(request);
    } else {
      // obsolete or deleted, no edit link, of div.
      return div();
    }
  }

  private Tag maybeRenderManageTranslationsLink(Optional<ProgramDefinition> draftProgram) {
    if (draftProgram.isPresent()) {
      String linkText = "Manage Translations →";
      String linkDestination =
          routes.AdminProgramTranslationsController.edit(
                  draftProgram.get().id(), LocalizedStrings.DEFAULT_LOCALE.toLanguageTag())
              .url();
      return new LinkElement()
          .setId("program-translations-link-" + draftProgram.get().id())
          .setHref(linkDestination)
          .setText(linkText)
          .setStyles(Styles.MR_2)
          .asAnchorText();
    } else {
      return div();
    }
  }

  Tag maybeRenderViewApplicationsLink(String text, Optional<ProgramDefinition> activeProgram) {
    if (activeProgram.isPresent()) {
      String editLink =
          routes.AdminApplicationController.answerList(activeProgram.get().id()).url();

      return new LinkElement()
          .setId("program-view-apps-link-" + activeProgram.get().id())
          .setHref(editLink)
          .setText(text)
          .setStyles(Styles.MR_2)
          .asAnchorText();
    } else {
      return div();
    }
  }
}
