package views.admin.programs;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.Tag;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.StyleUtils;
import views.Styles;
import views.admin.AdminLayout;

public final class ProgramIndexView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramIndexView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(ImmutableList<ProgramDefinition> programs) {
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1("All Programs").withClasses(Styles.MY_4),
                each(programs, this::renderProgramListItem),
                renderNewProgramButton());

    return layout.render(head(layout.tailwindStyles()), body(contentDiv));
  }

  public Tag renderNewProgramButton() {
    return a("Create new program")
        .withId("new-program")
        .withHref(controllers.admin.routes.AdminProgramController.newOne().url())
        .withClasses(
            Styles.INLINE_BLOCK,
            Styles.CURSOR_POINTER,
            Styles.PY_4,
            Styles.PX_3,
            Styles.MY_2,
            Styles.ROUNDED_MD,
            Styles.RING_BLUE_200,
            Styles.RING_OFFSET_2,
            Styles.BG_BLUE_400,
            Styles.TEXT_WHITE,
            StyleUtils.hover(Styles.BG_BLUE_500),
            StyleUtils.focus(ImmutableList.of(Styles.OUTLINE_NONE, Styles.RING_2)));
  }

  public Tag renderProgramListItem(ProgramDefinition program) {
    // TODO: Move Strings out of here for i18n.
    String programStatusText = "Draft";
    String lastEditText = "Last updated 2 hours ago."; // TODO: Need to generate this.
    String editLinkText = "Edit →";

    String programTitleText = program.name();
    String programDescriptionText = program.description();
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
                renderEditLink(editLinkText, programId))
            .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL);

    Tag innerDiv =
        div(topContent, midContent, bottomContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv).withClasses(Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4);
  }

  Tag renderEditLink(String text, long programId) {
    return a(text)
        .withHref(controllers.admin.routes.AdminProgramController.edit(programId).url())
        .withClasses(
            Styles.MR_2,
            Styles.TEXT_BLUE_400,
            StyleUtils.hover(Styles.TEXT_BLUE_500),
            StyleUtils.applyUtilityClass(StyleUtils.RESPONSIVE_MD, Styles.MR_4));
  }
}
