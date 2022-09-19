package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.PTag;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import javax.inject.Inject;
import services.program.ProgramDefinition;
import views.ViewUtils;
import views.ViewUtils.BadgeStatus;
import views.style.AdminStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Responsible for generating a program card for view by CiviForm admins / program admins. */
public final class ProgramCardFactory {

  private final ViewUtils viewUtils;

  @Inject
  public ProgramCardFactory(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  public DivTag renderCard(ProgramCardData cardData) {
    ProgramDefinition displayProgram = getDisplayProgram(cardData);

    String programTitleText = displayProgram.localizedName().getDefault();
    String programDescriptionText = displayProgram.localizedDescription().getDefault();
    String adminNoteText = displayProgram.adminDescription();

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
                div()
                    .withClasses(Styles.W_1_3, Styles.PY_7)
                    .with(
                        p(programTitleText)
                            .withClasses(
                                ReferenceClasses.ADMIN_PROGRAM_CARD_TITLE,
                                Styles.TEXT_BLACK,
                                Styles.FONT_BOLD,
                                Styles.TEXT_XL),
                        p(programDescriptionText)
                            .withClasses(
                                Styles.LINE_CLAMP_2, Styles.TEXT_GRAY_700, Styles.TEXT_BASE)),
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
        .with(titleAndStatus)
        .condWith(
            !adminNoteText.isBlank(),
            p().withClasses(
                    Styles.W_3_4,
                    Styles.MB_8,
                    Styles.PT_4,
                    Styles.LINE_CLAMP_3,
                    Styles.TEXT_GRAY_700,
                    Styles.TEXT_BASE)
                .with(span("Admin note: ").withClasses(Styles.FONT_SEMIBOLD), span(adminNoteText)));
  }

  private DivTag renderProgramRow(
      boolean isActive, ProgramCardData.ProgramRow programRow, String... extraStyles) {
    ProgramDefinition program = programRow.program();
    String updatedPrefix = "Edited on ";
    Optional<Instant> updatedTime = program.lastModifiedTime();
    if (isActive) {
      updatedPrefix = "Published on ";
    }

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

    PTag badge =
        ViewUtils.makeBadge(
            isActive ? BadgeStatus.ACTIVE : BadgeStatus.DRAFT,
            Styles.ML_2,
            StyleUtils.responsiveXLarge(Styles.ML_8));
    return div()
        .withClasses(
            Styles.PY_7,
            Styles.FLEX,
            Styles.FLEX_ROW,
            StyleUtils.hover(Styles.BG_GRAY_100),
            StyleUtils.joinStyles(extraStyles))
        .with(
            badge,
            div()
                .withClasses(Styles.ML_4, StyleUtils.responsiveXLarge(Styles.ML_10))
                .with(
                    viewUtils.renderEditOnText(updatedPrefix, updatedTime),
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

  private static ProgramDefinition getDisplayProgram(ProgramCardData cardData) {
    if (cardData.draftProgram().isPresent()) {
      return cardData.draftProgram().get().program();
    }
    return cardData.activeProgram().get().program();
  }

  public static Comparator<ProgramCardData> lastModifiedTimeThenNameComparator() {
    Comparator<ProgramCardData> c =
        Comparator.<ProgramCardData, Instant>comparing(
                cardData -> getDisplayProgram(cardData).lastModifiedTime().orElse(Instant.EPOCH))
            .reversed();
    return c.thenComparing(
        cardData -> getDisplayProgram(cardData).localizedName().getDefault().toLowerCase());
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
