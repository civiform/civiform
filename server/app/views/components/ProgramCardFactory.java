package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.iffElse;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.ImgTag;
import j2html.tags.specialized.PTag;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.ProgramImageUtils;
import views.ViewUtils;
import views.ViewUtils.ProgramDisplayType;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Responsible for generating a program card for view by CiviForm admins / program admins. */
public final class ProgramCardFactory {
  private final ViewUtils viewUtils;
  private final ProgramImageUtils programImageUtils;

  @Inject
  public ProgramCardFactory(ViewUtils viewUtils, ProgramImageUtils programImageUtils) {
    this.viewUtils = checkNotNull(viewUtils);
    this.programImageUtils = checkNotNull(programImageUtils);
  }

  public DivTag renderCard(ProgramCardData cardData, boolean showCategories) {
    ProgramDefinition displayProgram = getDisplayProgram(cardData);

    String programTitleText = displayProgram.localizedName().getDefault();
    String programDescriptionText = displayProgram.localizedDescription().getDefault();
    String adminNoteText = displayProgram.adminDescription();
    ImmutableList<String> programCategoryNames =
        displayProgram.categories().stream()
            .map(category -> category.getDefaultName())
            .collect(ImmutableList.toImmutableList());

    DivTag statusDiv = div();
    if (cardData.draftProgram().isPresent()) {
      statusDiv =
          statusDiv.with(
              renderProgramRow(
                  cardData.isCiviFormAdmin(),
                  /* isActive= */ false,
                  cardData.draftProgram().get()));
    }

    if (cardData.activeProgram().isPresent()) {
      statusDiv =
          statusDiv.with(
              renderProgramRow(
                  cardData.isCiviFormAdmin(),
                  /* isActive= */ true,
                  cardData.activeProgram().get(),
                  cardData.draftProgram().isPresent() ? "border-t" : ""));
    }

    DivTag cardContent =
        div()
            .withClass("flex")
            .with(
                div()
                    .withClasses("w-1/4", "py-7", "pr-2")
                    .with(
                        p(programTitleText)
                            .withClasses(
                                ReferenceClasses.ADMIN_PROGRAM_CARD_TITLE,
                                "text-black",
                                "font-bold",
                                "text-xl"))
                    .with(
                        div()
                            .with(
                                TextFormatter.formatText(
                                    programDescriptionText,
                                    /* preserveEmptyLines= */ false,
                                    /* addRequiredIndicator= */ false))
                            .withClasses(
                                "line-clamp-2", "text-sm", StyleUtils.responsiveLarge("text-base")))
                    .condWith(
                        shouldShowCommonIntakeFormIndicator(displayProgram),
                        div()
                            .withClasses("text-black", "items-center", "flex", "pt-4")
                            .with(
                                Icons.svg(Icons.CHECK)
                                    .withClasses("inline-block", "ml-3", "mr-2", "w-5", "h-5"))
                            .with(span("Pre-screener").withClasses("text-base", "font-semibold")))
                    .condWith(
                        !adminNoteText.isBlank(),
                        p().withClasses(
                                "mb-4",
                                "pt-4",
                                "line-clamp-3",
                                "text-sm",
                                StyleUtils.responsiveLarge("text-base"))
                            .with(
                                span("Admin note: ").withClasses("font-semibold"),
                                span(adminNoteText)))
                    .condWith(
                        showCategories,
                        p(
                                span("Categories:  ").withClasses("font-semibold"),
                                iffElse(
                                    programCategoryNames.isEmpty(),
                                    span("None"),
                                    span(String.join(", ", programCategoryNames))))
                            .withClasses("text-sm", StyleUtils.responsiveLarge("text-base"))),
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
        .with(cardContent);
  }

  private DivTag renderProgramRow(
      boolean isCiviFormAdmin,
      boolean isActive,
      ProgramCardData.ProgramRow programRow,
      String... extraStyles) {
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
                ButtonStyles.CLEAR_WITH_ICON,
                ReferenceClasses.WITH_DROPDOWN,
                "h-12",
                programRow.extraRowActions().size() == 0 ? "invisible" : "");

    PTag badge =
        ViewUtils.makeLifecycleBadge(
            isActive ? ProgramDisplayType.ACTIVE : ProgramDisplayType.DRAFT,
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
            createImageIcon(program, isCiviFormAdmin),
            badge,
            div()
                .withClasses("ml-4", StyleUtils.responsiveXLarge("ml-10"))
                .with(
                    viewUtils.renderEditOnText(updatedPrefix, updatedTime),
                    p().with(
                            span(String.format("%d", blockCount)).withClass("font-semibold"),
                            span(blockCount == 1 ? " screen, " : " screens, "),
                            span(String.format("%d", questionCount)).withClass("font-semibold"),
                            span(questionCount == 1 ? " question" : " questions"))
                        .condWith(
                            programRow.universalQuestionsText().isPresent(),
                            p(programRow.universalQuestionsText().orElse("")))),
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

  private boolean shouldShowCommonIntakeFormIndicator(ProgramDefinition displayProgram) {
    return displayProgram.programType().equals(ProgramType.COMMON_INTAKE_FORM);
  }

  private static ProgramDefinition getDisplayProgram(ProgramCardData cardData) {
    if (cardData.draftProgram().isPresent()) {
      return cardData.draftProgram().get().program();
    }
    return cardData.activeProgram().get().program();
  }

  private DivTag createImageIcon(ProgramDefinition program, boolean isCiviFormAdmin) {
    if (!isCiviFormAdmin) {
      // Only CiviForm admins need the program image preview since they're the only ones that can
      // modify it. (Specifically, program admins don't need it.)
      return div();
    }

    Optional<ImgTag> image =
        programImageUtils.createProgramImage(
            program,
            Locale.getDefault(),
            /* isWithinProgramCard= */ false,
            /* isProgramFilteringEnabled= */ false); // Hardcoded to false because
    // if isWithProgramCard is false, we never reach the code that evaluates
    // isProgramFilteringEnabled.
    if (image.isPresent()) {
      return div().withClasses("w-16", "h-9").with(image.get());
    } else {
      // Show a grayed-out placeholder image if there's no program image.
      return div().with(Icons.svg(Icons.IMAGE).withClasses("w-16", "h-9", "text-gray-300"));
    }
  }

  public static Comparator<ProgramCardData> programTypeThenLastModifiedThenNameComparator() {
    Comparator<ProgramCardData> c =
        Comparator.comparingInt(
            (cardData) ->
                getDisplayProgram(cardData).programType().equals(ProgramType.COMMON_INTAKE_FORM)
                    ? 0
                    : 1);
    return c.thenComparing(
            cardData -> getDisplayProgram(cardData).lastModifiedTime().orElse(Instant.EPOCH),
            Comparator.reverseOrder())
        .thenComparing(
            cardData ->
                getDisplayProgram(cardData).localizedName().getDefault().toLowerCase(Locale.ROOT));
  }

  @AutoValue
  public abstract static class ProgramCardData {
    abstract Optional<ProgramRow> activeProgram();

    abstract Optional<ProgramRow> draftProgram();

    abstract boolean isCiviFormAdmin();

    public static Builder builder() {
      return new AutoValue_ProgramCardFactory_ProgramCardData.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setActiveProgram(Optional<ProgramRow> v);

      public abstract Builder setDraftProgram(Optional<ProgramRow> v);

      public abstract Builder setIsCiviFormAdmin(boolean isCiviFormAdmin);

      public abstract ProgramCardData build();
    }

    @AutoValue
    public abstract static class ProgramRow {
      abstract ProgramDefinition program();

      abstract ImmutableList<ButtonTag> rowActions();

      abstract ImmutableList<ButtonTag> extraRowActions();

      abstract Optional<String> universalQuestionsText();

      public static Builder builder() {
        return new AutoValue_ProgramCardFactory_ProgramCardData_ProgramRow.Builder();
      }

      @AutoValue.Builder
      public abstract static class Builder {
        public abstract Builder setProgram(ProgramDefinition v);

        public abstract Builder setRowActions(ImmutableList<ButtonTag> v);

        public abstract Builder setExtraRowActions(ImmutableList<ButtonTag> v);

        public abstract Builder setUniversalQuestionsText(Optional<String> v);

        public abstract ProgramRow build();
      }
    }
  }
}
