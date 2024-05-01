package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.applicant.ApplicantRoutes;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.program.ProgramDefinition;
import views.components.Modal;

/**
 * Factory for creating parameter info for applicant program card sections.
 *
 * <p>The template which uses this is defined in ProgramCardsSectionFragment.html
 */
public final class ProgramCardsSectionParamsFactory {
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public ProgramCardsSectionParamsFactory(ApplicantRoutes applicantRoutes) {
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  /**
   * Returns ProgramSectionParams containing a list of cards for the provided list of program data.
   */
  public ProgramSectionParams getSection(
      Messages messages,
      Optional<MessageKey> title,
      MessageKey buttonText,
      ImmutableList<ApplicantProgramData> programData,
      Locale preferredLocale,
      CiviFormProfile profile,
      Long applicantId) {

    ProgramSectionParams.Builder sectionBuilder =
        ProgramSectionParams.builder()
            .setCards(
                getCards(messages, buttonText, programData, preferredLocale, profile, applicantId));

    if (title.isPresent()) {
      sectionBuilder.setTitle(messages.at(title.get().getKeyName()));
      sectionBuilder.setId(Modal.randomModalId());
    }

    return sectionBuilder.build();
  }

  /** Returns a list of ProgramCardParams corresponding with the provided list of program data. */
  public ImmutableList<ProgramCardParams> getCards(
      Messages messages,
      MessageKey buttonText,
      ImmutableList<ApplicantProgramData> programData,
      Locale preferredLocale,
      CiviFormProfile profile,
      Long applicantId) {
    ImmutableList.Builder<ProgramCardParams> cardsListBuilder = ImmutableList.builder();

    for (ApplicantProgramData programDatum : programData) {
      ProgramCardParams.Builder cardBuilder = ProgramCardParams.builder();
      ProgramDefinition program = programDatum.program();

      String actionUrl = "~" + applicantRoutes.review(profile, applicantId, program.id()).url();

      // Note this doesn't yet manage markdown, links and appropriate aria labels for links, and
      // whatever else our current cards do.
      cardBuilder
          .setTitle(program.localizedName().getOrDefault(preferredLocale))
          .setBody(program.localizedDescription().getOrDefault(preferredLocale))
          .setActionUrl(actionUrl)
          .setActionText(messages.at(buttonText.getKeyName()));

      cardsListBuilder.add(cardBuilder.build());
    }

    return cardsListBuilder.build();
  }

  @AutoValue
  public abstract static class ProgramSectionParams {
    public abstract ImmutableList<ProgramCardParams> getCards();

    public abstract Optional<String> getTitle();

    /** The id of the section. Only needs to be specified if a title is also specified. */
    public abstract Optional<String> getId();

    public static Builder builder() {
      return new AutoValue_ProgramCardsSectionParamsFactory_ProgramSectionParams.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setCards(List<ProgramCardParams> cards);

      public abstract Builder setTitle(String title);

      public abstract Builder setId(String id);

      public abstract ProgramSectionParams build();
    }
  }

  @AutoValue
  public abstract static class ProgramCardParams {
    public abstract String getTitle();

    public abstract String getActionText();

    public abstract String getBody();

    public abstract String getActionUrl();

    public static Builder builder() {
      return new AutoValue_ProgramCardsSectionParamsFactory_ProgramCardParams.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setTitle(String title);

      public abstract Builder setActionText(String actionText);

      public abstract Builder setBody(String body);

      public abstract Builder setActionUrl(String actionUrl);

      public abstract ProgramCardParams build();
    }
  }
}
