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
                  cardData.draftProgram().isPresent() ? "border-t" : ""));
    }

    DivTag titleAndStatus =
        div()
            .withClass("flex")
            .with(
                div()
                    .withClasses("w-1/3", "py-7")
                    .with(
                        p(programTitleText)
                            .withClasses(
                                ReferenceClasses.ADMIN_PROGRAM_CARD_TITLE,
                                "text-black",
                                "font-bold",
                                "text-xl"),
                        p(programDescriptionText)
                            .withClasses("line-clamp-2", "text-gray-700", "text-base")),
                statusDiv.withClasses(
                    "flex-grow", "text-sm", StyleUtils.responsiveLarge("text-base")));

    return div()
        .withClasses(
            ReferenceClasses.ADMIN_PROGRAM_CARD,
            "w-full",
            "my-4",
            "pl-6",
            "border",
            "border-gray-300",
            "rounded-lg")
        .with(titleAndStatus)
        .condWith(
            !adminNoteText.isBlank(),
            p().withClasses("w-3/4", "mb-8", "pt-4", "line-clamp-3", "text-gray-700", "text-base")
                .with(span("Admin note: ").withClasses("font-semibold"), span(adminNoteText)));
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
                "h-12",
                programRow.extraRowActions().size() == 0 ? "invisible" : "");

    PTag badge =
        ViewUtils.makeBadge(
            isActive ? BadgeStatus.ACTIVE : BadgeStatus.DRAFT,
            "ml-2",
            StyleUtils.responsiveXLarge("ml-8"));
    return div()
        .withClasses(
            "py-7",
            "flex",
            "flex-row",
            StyleUtils.hover("bg-gray-100"),
            StyleUtils.joinStyles(extraStyles))
        .with(
            badge,
            div()
                .withClasses("ml-4", StyleUtils.responsiveXLarge("ml-10"))
                .with(
                    viewUtils.renderEditOnText(updatedPrefix, updatedTime),
                    p().with(
                            span(String.format("%d", blockCount)).withClass("font-semibold"),
                            span(blockCount == 1 ? " screen, " : " screens, "),
                            span(String.format("%d", questionCount)).withClass("font-semibold"),
                            span(questionCount == 1 ? " question" : " questions"))),
            div().withClass("flex-grow"),
            div()
                .withClasses("flex", "space-x-2", "pr-6", "font-medium")
                .with(programRow.rowActions())
                .with(
                    div()
                        .withClass("relative")
                        .with(
                            extraActionsButton,
                            div()
                                .withId(extraActionsButtonId + "-dropdown")
                                .withClasses(
                                    "hidden",
                                    "flex",
                                    "flex-col",
                                    "border",
                                    "bg-white",
                                    "absolute",
                                    "right-0",
                                    "w-56",
                                    "z-50")
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
