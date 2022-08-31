package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import services.DateConverter;
import services.program.ProgramDefinition;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public final class ProgramCardFactory {

  private final DateConverter dateConverter;

  @Inject
  public ProgramCardFactory(DateConverter dateConverter) {
    this.dateConverter = checkNotNull(dateConverter);
  }

  public DivTag renderCard(ProgramCardParams params) {
    ProgramDefinition displayProgram =
        getDisplayProgram(params.draftProgram(), params.activeProgram());

    String programTitleText = displayProgram.adminName();
    String programDescriptionText = displayProgram.adminDescription();

    DivTag statusDiv = div();
    if (params.draftProgram().isPresent()) {
      statusDiv =
          statusDiv.with(
              renderProgramRow(
                  /* isActive = */ false,
                  params.draftProgram().get(),
                  params.draftRowActions(),
                  params.draftRowExtraActions()));
    }

    if (params.activeProgram().isPresent()) {
      statusDiv =
          statusDiv.with(
              renderProgramRow(
                  /* isActive = */ true,
                  params.activeProgram().get(),
                  params.activeRowActions(),
                  params.activeRowExtraActions(),
                  params.draftProgram().isPresent() ? Styles.BORDER_T : ""));
    }

    DivTag titleAndStatus =
        div()
            .withClass(Styles.FLEX)
            .with(
                p(programTitleText)
                    .withClasses(
                        ReferenceClasses.ADMIN_PROGRAM_CARD_TITLE,
                        Styles.W_1_4,
                        Styles.PY_7,
                        Styles.TEXT_BLACK,
                        Styles.FONT_BOLD,
                        Styles.TEXT_XL),
                statusDiv.withClasses(
                    Styles.FLEX_GROW,
                    Styles.TEXT_SM,
                    StyleUtils.responsiveLarge(Styles.TEXT_BASE)));

    return div()
        .withClasses(
            ReferenceClasses.ADMIN_PROGRAM_CARD,
            Styles.W_FULL,
            Styles.MY_4,
            Styles.PL_6,
            Styles.BORDER,
            Styles.BORDER_GRAY_300,
            Styles.ROUNDED_LG)
        .with(
            titleAndStatus,
            p(programDescriptionText)
                .withClasses(
                    Styles.W_3_4,
                    Styles.MB_8,
                    Styles.PT_4,
                    Styles.LINE_CLAMP_3,
                    Styles.TEXT_GRAY_700,
                    Styles.TEXT_BASE))
        // Add data attributes used for client-side sorting.
        .withData(
            "last-updated-millis",
            Long.toString(
                extractLastUpdated(params.draftProgram(), params.activeProgram()).toEpochMilli()))
        .withData("name", programTitleText);
  }

  private DivTag renderProgramRow(
      boolean isActive,
      ProgramDefinition program,
      List<ButtonTag> actions,
      List<ButtonTag> extraActions,
      String... extraStyles) {
    String badgeText = "Draft";
    String badgeBGColor = BaseStyles.BG_CIVIFORM_PURPLE_LIGHT;
    String badgeFillColor = BaseStyles.TEXT_CIVIFORM_PURPLE;
    String updatedPrefix = "Edited on ";
    Optional<Instant> updatedTime = program.lastModifiedTime();
    if (isActive) {
      badgeText = "Active";
      badgeBGColor = BaseStyles.BG_CIVIFORM_GREEN_LIGHT;
      badgeFillColor = BaseStyles.TEXT_CIVIFORM_GREEN;
      updatedPrefix = "Published on ";
    }

    String formattedUpdateTime =
        updatedTime.map(t -> dateConverter.renderDateTime(t)).orElse("unknown");
    String formattedUpdateDate =
        updatedTime.map(t -> dateConverter.renderDate(t)).orElse("unknown");

    int blockCount = program.getBlockCount();
    int questionCount = program.getQuestionCount();

    String extraActionsButtonId = "extra-actions-" + program.id();
    ButtonTag extraActionsButton =
        makeSvgTextButton("", Icons.MORE_VERT)
            .withId(extraActionsButtonId)
            .withClasses(
                AdminStyles.TERTIARY_BUTTON_STYLES,
                ReferenceClasses.WITH_DROPDOWN,
                Styles.H_12,
                extraActions.size() == 0 ? Styles.INVISIBLE : "");

    return div()
        .withClasses(
            Styles.PY_7,
            Styles.FLEX,
            Styles.FLEX_ROW,
            StyleUtils.hover(Styles.BG_GRAY_100),
            StyleUtils.joinStyles(extraStyles))
        .with(
            p().withClasses(
                    badgeBGColor,
                    badgeFillColor,
                    Styles.ML_2,
                    StyleUtils.responsiveXLarge(Styles.ML_8),
                    Styles.FONT_MEDIUM,
                    Styles.ROUNDED_FULL,
                    Styles.FLEX,
                    Styles.FLEX_ROW,
                    Styles.GAP_X_2,
                    Styles.PLACE_ITEMS_CENTER,
                    Styles.JUSTIFY_CENTER)
                .withStyle("min-width:90px")
                .with(
                    Icons.svg(Icons.NOISE_CONTROL_OFF)
                        .withClasses(Styles.INLINE_BLOCK, Styles.ML_3_5),
                    span(badgeText).withClass(Styles.MR_4)),
            div()
                .withClasses(Styles.ML_4, StyleUtils.responsiveXLarge(Styles.ML_10))
                .with(
                    p().with(
                            span(updatedPrefix),
                            span(formattedUpdateTime)
                                .withClasses(
                                    Styles.FONT_SEMIBOLD,
                                    Styles.HIDDEN,
                                    StyleUtils.responsiveLarge(Styles.INLINE)),
                            span(formattedUpdateDate)
                                .withClasses(
                                    Styles.FONT_SEMIBOLD,
                                    StyleUtils.responsiveLarge(Styles.HIDDEN))),
                    p().with(
                            span(String.format("%d", blockCount)).withClass(Styles.FONT_SEMIBOLD),
                            span(blockCount == 1 ? " screen, " : " screens, "),
                            span(String.format("%d", questionCount))
                                .withClass(Styles.FONT_SEMIBOLD),
                            span(questionCount == 1 ? " question" : " questions"))),
            div().withClass(Styles.FLEX_GROW),
            div()
                .withClasses(Styles.FLEX, Styles.SPACE_X_2, Styles.PR_6, Styles.FONT_MEDIUM)
                .with(actions)
                .with(
                    div()
                        .withClass(Styles.RELATIVE)
                        .with(
                            extraActionsButton,
                            div()
                                .withId(extraActionsButtonId + "-dropdown")
                                .withClasses(
                                    Styles.HIDDEN,
                                    Styles.FLEX,
                                    Styles.FLEX_COL,
                                    Styles.BORDER,
                                    Styles.BG_WHITE,
                                    Styles.ABSOLUTE,
                                    Styles.RIGHT_0,
                                    Styles.W_56,
                                    Styles.Z_50)
                                .with(extraActions))));
  }

  private ProgramDefinition getDisplayProgram(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    if (draftProgram.isPresent()) {
      return draftProgram.get();
    }
    return activeProgram.get();
  }

  private static Instant extractLastUpdated(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    // Prefer when the draft was last updated, since active versions should be immutable after
    // being published.
    if (draftProgram.isEmpty() && activeProgram.isEmpty()) {
      throw new IllegalArgumentException("Program neither active nor draft.");
    }

    ProgramDefinition program = draftProgram.isPresent() ? draftProgram.get() : activeProgram.get();
    return program.lastModifiedTime().orElse(Instant.EPOCH);
  }

  // TODO(clouser): Helper.
  private static ButtonTag makeSvgTextButton(String buttonText, Icons icon) {
    return button()
        .with(
            Icons.svg(icon)
                .withClasses(Styles.ML_1, Styles.INLINE_BLOCK, Styles.FLEX_SHRINK_0)
                // Can't set 18px using Tailwind CSS classes.
                .withStyle("width: 18px; height: 18px;"),
            span(buttonText).withClass(Styles.TEXT_LEFT));
  }

  @AutoValue
  public abstract static class ProgramCardParams {
    abstract Optional<ProgramDefinition> activeProgram();

    abstract ImmutableList<ButtonTag> activeRowActions();

    abstract ImmutableList<ButtonTag> activeRowExtraActions();

    abstract Optional<ProgramDefinition> draftProgram();

    abstract ImmutableList<ButtonTag> draftRowActions();

    abstract ImmutableList<ButtonTag> draftRowExtraActions();

    public static Builder builder() {
      return new AutoValue_ProgramCardFactory_ProgramCardParams.Builder()
          .setActiveRowActions(ImmutableList.of())
          .setActiveRowExtraActions(ImmutableList.of())
          .setDraftRowActions(ImmutableList.of())
          .setDraftRowExtraActions(ImmutableList.of());
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setActiveProgram(Optional<ProgramDefinition> v);

      public abstract Builder setActiveRowActions(ImmutableList<ButtonTag> v);

      public abstract Builder setActiveRowExtraActions(ImmutableList<ButtonTag> v);

      public abstract Builder setDraftProgram(Optional<ProgramDefinition> v);

      public abstract Builder setDraftRowActions(ImmutableList<ButtonTag> v);

      public abstract Builder setDraftRowExtraActions(ImmutableList<ButtonTag> v);

      public abstract ProgramCardParams build();
    }
  }
}
