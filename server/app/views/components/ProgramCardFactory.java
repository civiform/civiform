package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import services.DateConverter;
import services.program.ProgramDefinition;
import views.ViewUtils;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Responsible for generating a program card for view by CiviForm admins / program admins. */
public final class ProgramCardFactory {

  private final DateConverter dateConverter;

  @Inject
  public ProgramCardFactory(DateConverter dateConverter) {
    this.dateConverter = checkNotNull(dateConverter);
  }

  public DivTag renderCard(ProgramCardData cardData) {
    ProgramDefinition displayProgram = getDisplayProgram(cardData);

    String programTitleText = displayProgram.localizedName().getDefault();
    String programDescriptionText = displayProgram.localizedDescription().getDefault();

    DivTag statusDiv = div();
    if (cardData.draftProgram().isPresent()) {
      statusDiv =
          statusDiv.with(renderProgramRow(/* isActive = */ false, cardData.draftProgram().get()));
    }

    if (cardData.activeProgram().isPresent()) {
      statusDiv =
          statusDiv.with(
              renderProgramRow(
                  /* isActive = */ true,
                  cardData.activeProgram().get(),
                  cardData.draftProgram().isPresent() ? Styles.BORDER_T : ""));
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
        .withData("last-updated-millis", Long.toString(extractLastUpdated(cardData).toEpochMilli()))
        .withData("name", programTitleText);
  }

  private DivTag renderProgramRow(
      boolean isActive, ProgramCardData.ProgramRow programRow, String... extraStyles) {
    ProgramDefinition program = programRow.program();
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
        ViewUtils.makeSvgTextButton("", Icons.MORE_VERT)
            .withId(extraActionsButtonId)
            .withClasses(
                AdminStyles.TERTIARY_BUTTON_STYLES,
                ReferenceClasses.WITH_DROPDOWN,
                Styles.H_12,
                programRow.extraRowActions().size() == 0 ? Styles.INVISIBLE : "");

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
                .with(programRow.rowActions())
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
                                .with(programRow.extraRowActions()))));
  }

  private ProgramDefinition getDisplayProgram(ProgramCardData cardData) {
    if (cardData.draftProgram().isPresent()) {
      return cardData.draftProgram().get().program();
    }
    return cardData.activeProgram().get().program();
  }

  private static Instant extractLastUpdated(ProgramCardData cardData) {
    // Prefer when the draft was last updated, since active versions should be immutable after
    // being published.
    if (cardData.draftProgram().isEmpty() && cardData.activeProgram().isEmpty()) {
      throw new IllegalArgumentException("Program neither active nor draft.");
    }

    ProgramDefinition program =
        cardData.draftProgram().isPresent()
            ? cardData.draftProgram().get().program()
            : cardData.activeProgram().get().program();
    return program.lastModifiedTime().orElse(Instant.EPOCH);
  }

  @AutoValue
  public abstract static class ProgramCardData {
    abstract Optional<ProgramRow> activeProgram();

    abstract Optional<ProgramRow> draftProgram();

    public static Builder builder() {
      return new AutoValue_ProgramCardFactory_ProgramCardData.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setActiveProgram(Optional<ProgramRow> v);

      public abstract Builder setDraftProgram(Optional<ProgramRow> v);

      public abstract ProgramCardData build();
    }

    @AutoValue
    public abstract static class ProgramRow {
      abstract ProgramDefinition program();

      abstract ImmutableList<ButtonTag> rowActions();

      abstract ImmutableList<ButtonTag> extraRowActions();

      public static Builder builder() {
        return new AutoValue_ProgramCardFactory_ProgramCardData_ProgramRow.Builder();
      }

      @AutoValue.Builder
      public abstract static class Builder {
        public abstract Builder setProgram(ProgramDefinition v);

        public abstract Builder setRowActions(ImmutableList<ButtonTag> v);

        public abstract Builder setExtraRowActions(ImmutableList<ButtonTag> v);

        public abstract ProgramRow build();
      }
    }
  }
}
