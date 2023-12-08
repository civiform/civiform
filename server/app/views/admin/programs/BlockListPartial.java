package views.admin.programs;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.iff;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static play.mvc.Http.HttpVerbs.POST;
import static services.program.ProgramDefinition.Direction.DOWN;
import static services.program.ProgramDefinition.Direction.UP;
import static views.ViewUtils.ProgramDisplayType.DRAFT;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import controllers.admin.routes;
import j2html.tags.specialized.DivTag;
import javax.inject.Inject;
import play.mvc.Http;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmxVals;
import views.ViewUtils;
import views.components.ButtonStyles;
import views.components.Icons;
import views.style.AdminStyles;
import views.style.StyleUtils;

/**
 * Returns a wrapper panel that contains the list of all blocks and, if the view shows an editable
 * program, a button to add a new screen,
 */
public final class BlockListPartial extends BaseHtmlView {
  private static final int BASE_INDENTATION_SIZE = 4;
  private static final int INDENTATION_FACTOR_INCREASE_ON_LEVEL = 2;
  private final ViewUtils.ProgramDisplayType programDisplayType;

  public interface Factory {
    BlockListPartial create(ViewUtils.ProgramDisplayType programDisplayType);
  }

  /** Returns if this view is editable or not. A view is editable only if it represents a draft. */
  private boolean viewAllowsEditingProgram() {
    return programDisplayType.equals(DRAFT);
  }

  @Inject
  BlockListPartial(@Assisted ViewUtils.ProgramDisplayType programDisplayType) {
    this.programDisplayType = Preconditions.checkNotNull(programDisplayType);
  }

  public DivTag render(Http.Request request, ProgramDefinition program, long focusedBlockId) {
    DivTag ret =
        div()
            .withId("blockList")
            .withClasses("shadow-lg", "pt-6", "w-2/12", "border-r", "border-gray-200");
    ret.with(
        renderBlockList(
            request,
            program,
            program.getNonRepeatedBlockDefinitions(),
            focusedBlockId,
            /* level= */ 0));

    if (programDisplayType.equals(DRAFT)) {
      ret.with(
          form(
                  makeCsrfTokenInputTag(request),
                  ViewUtils.makeSvgTextButton("Add screen", Icons.ADD)
                      .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "m-4")
                      .withType("submit")
                      .withId("add-block-button"))
              .withAction(routes.AdminProgramBlocksController.create(program.id()).url())
              .withMethod(POST));
    }

    return ret;
  }

  /**
   * Returns a panel that shows all Blocks of the given program. In an editable view it also adds a
   * button that allows to add a new screen and controls to change the order.
   */
  private DivTag renderBlockList(
      Http.Request request,
      ProgramDefinition programDefinition,
      ImmutableList<BlockDefinition> blockDefinitions,
      long focusedBlockId,
      int level) {
    DivTag container = div();
    String genericBlockDivId = "block_list_item_";
    for (BlockDefinition blockDefinition : blockDefinitions) {

      // TODO: Not i18n safe.
      int numQuestions = blockDefinition.getQuestionCount();
      String questionCountText = String.format("Question count: %d", numQuestions);
      String blockName = blockDefinition.name();
      // indentation value for enums and repeaters
      int listIndentationFactor =
          BASE_INDENTATION_SIZE + (level * INDENTATION_FACTOR_INCREASE_ON_LEVEL);
      String selectedClasses = blockDefinition.id() == focusedBlockId ? "bg-gray-100" : "";
      DivTag blockTag =
          div()
              .withClasses(
                  "flex",
                  "flex-row",
                  "gap-2",
                  "py-2",
                  "px-" + listIndentationFactor,
                  "border",
                  "border-white",
                  StyleUtils.hover("border-gray-300"),
                  selectedClasses);
      String switchBlockLink;

      if (viewAllowsEditingProgram()) {
        switchBlockLink =
            controllers.admin.routes.AdminProgramBlocksController.edit(
                    programDefinition.id(), blockDefinition.id())
                .url();
      } else {
        switchBlockLink =
            controllers.admin.routes.AdminProgramBlocksController.show(
                    programDefinition.id(), blockDefinition.id())
                .url();
      }

      blockTag
          .withId(genericBlockDivId + blockDefinition.id())
          .with(
              a().withClasses("flex-grow", "overflow-hidden")
                  .withHref(switchBlockLink)
                  .with(
                      p(blockName)
                          .withClass(iff(blockDefinition.hasNullQuestion(), "text-red-500")),
                      p(questionCountText).withClasses("text-sm")));

      if (viewAllowsEditingProgram()) {
        DivTag moveButtons =
            renderBlockMoveButtons(
                request, programDefinition.id(), blockDefinitions, blockDefinition);
        blockTag.with(moveButtons);
      }

      container.with(blockTag);

      // Recursively add repeated blocks indented under their enumerator block
      if (blockDefinition.isEnumerator()) {
        container.with(
            renderBlockList(
                request,
                programDefinition,
                programDefinition.getBlockDefinitionsForEnumerator(blockDefinition.id()),
                focusedBlockId,
                level + 1));
      }
    }

    return container;
  }

  /**
   * Creates a set of buttons, which are shown next to each block in the list of blocks. They are
   * used to move a block up or down in the list.
   */
  private DivTag renderBlockMoveButtons(
      Http.Request request,
      long programId,
      ImmutableList<BlockDefinition> blockDefinitions,
      BlockDefinition blockDefinition) {
    String moveFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move up button is invisible for the first block
    String moveUpInvisible =
        blockDefinition.id() == blockDefinitions.get(0).id() ? "invisible" : "";
    DivTag moveUp =
        div(submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON))
            .attr("hx-post", moveFormAction)
            .attr("hx-target", "#blockList")
            .attr("hx-swap", "outerHTML")
            .attr(
                "hx-vals",
                HtmxVals.serializeVals("direction", UP.name(), "csrfToken", getCsrfToken(request)))
            .withClass(moveUpInvisible);

    // Move down button is invisible for the last block
    String moveDownInvisible =
        blockDefinition.id() == blockDefinitions.get(blockDefinitions.size() - 1).id()
            ? "invisible"
            : "";
    DivTag moveDown =
        div(
                submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON),
                input()
                    .isHidden()
                    .withName("direction")
                    .withValue(ProgramDefinition.Direction.DOWN.name()))
            .attr("hx-post", moveFormAction)
            .attr("hx-target", "#blockList")
            .attr("hx-swap", "outerHTML")
            .attr(
                "hx-vals",
                HtmxVals.serializeVals(
                    "direction", DOWN.name(), "csrfToken", getCsrfToken(request)))
            .withClasses("transform", "rotate-180", moveDownInvisible);

    return div().withClasses("flex", "flex-col", "self-center").with(moveUp, moveDown);
  }
}
