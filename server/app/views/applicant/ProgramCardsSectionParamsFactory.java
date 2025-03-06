package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.applicant.ApplicantRoutes;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import org.apache.commons.lang3.StringUtils;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.DateConverter;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;
import views.ProgramImageUtils;
import views.components.Modal;
import views.components.TextFormatter;

/**
 * Factory for creating parameter info for applicant program card sections.
 *
 * <p>The template which uses this is defined in ProgramCardsSectionFragment.html
 */
public final class ProgramCardsSectionParamsFactory {
  private final ApplicantRoutes applicantRoutes;
  private final ProfileUtils profileUtils;
  private final PublicStorageClient publicStorageClient;
  private final DateConverter dateConverter;

  /** Enumerates the homepage section types, which may have different card components or styles. */
  public enum SectionType {
    MY_APPLICATIONS,
    COMMON_INTAKE,
    UNFILTERED_PROGRAMS,
    STANDARD;
  }

  @Inject
  public ProgramCardsSectionParamsFactory(
      ApplicantRoutes applicantRoutes,
      ProfileUtils profileUtils,
      PublicStorageClient publicStorageClient,
      DateConverter dateConverter) {
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.profileUtils = checkNotNull(profileUtils);
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.dateConverter = checkNotNull(dateConverter);
  }

  /**
   * Returns ProgramSectionParams containing a list of cards for the provided list of program data.
   */
  public ProgramSectionParams getSection(
      Request request,
      Messages messages,
      Optional<MessageKey> title,
      MessageKey buttonText,
      ImmutableList<ApplicantProgramData> programData,
      Locale preferredLocale,
      Optional<CiviFormProfile> profile,
      Optional<Long> applicantId,
      ApplicantPersonalInfo personalInfo,
      SectionType sectionType) {

    List<ProgramCardParams> cards =
        getCards(
            request,
            messages,
            buttonText,
            programData,
            preferredLocale,
            profile,
            applicantId,
            personalInfo);

    ProgramSectionParams.Builder sectionBuilder = ProgramSectionParams.builder().setCards(cards);

    if (title.isPresent()) {
      sectionBuilder.setTitle(messages.at(title.get().getKeyName(), cards.size()));
      sectionBuilder.setId(Modal.randomModalId());
    }

    sectionBuilder.setSectionType(sectionType);

    return sectionBuilder.build();
  }

  /** Returns a list of ProgramCardParams corresponding with the provided list of program data. */
  public ImmutableList<ProgramCardParams> getCards(
      Request request,
      Messages messages,
      MessageKey buttonText,
      ImmutableList<ApplicantProgramData> programData,
      Locale preferredLocale,
      Optional<CiviFormProfile> profile,
      Optional<Long> applicantId,
      ApplicantPersonalInfo personalInfo) {
    ImmutableList.Builder<ProgramCardParams> cardsListBuilder = ImmutableList.builder();

    for (ApplicantProgramData programDatum : programData) {
      cardsListBuilder.add(
          getCard(
              request,
              messages,
              buttonText,
              programDatum,
              preferredLocale,
              profile,
              applicantId,
              personalInfo));
    }

    return cardsListBuilder.build();
  }

  public ProgramCardParams getCard(
      Request request,
      Messages messages,
      MessageKey buttonText,
      ApplicantProgramData programDatum,
      Locale preferredLocale,
      Optional<CiviFormProfile> profile,
      Optional<Long> applicantId,
      ApplicantPersonalInfo personalInfo) {
    ProgramCardParams.Builder cardBuilder = ProgramCardParams.builder();
    ProgramDefinition program = programDatum.program();

    String actionUrl =
        getActionUrl(
            applicantRoutes,
            program.id(),
            program.slug(),
            program.isCommonIntakeForm(),
            programDatum.latestApplicationLifecycleStage(),
            applicantId,
            profile);

    boolean isGuest = personalInfo.getType() == GUEST;

    ImmutableList.Builder<String> categoriesBuilder = ImmutableList.builder();
    categoriesBuilder.addAll(
        program.categories().stream()
            .map(c -> c.getLocalizedName().getOrDefault(preferredLocale))
            .collect(ImmutableList.toImmutableList()));

    String description = selectAndFormatDescription(program, preferredLocale);

    cardBuilder
        .setTitle(program.localizedName().getOrDefault(preferredLocale))
        .setBody(description)
        .setActionUrl(actionUrl)
        .setIsGuest(isGuest)
        .setIsCommonIntakeForm(program.isCommonIntakeForm())
        .setCategories(categoriesBuilder.build())
        .setActionText(messages.at(buttonText.getKeyName()))
        .setProgramId(program.id());

    if (isGuest) {
      cardBuilder.setLoginModalId("login-dialog-" + program.id());
    }

    if (programDatum.latestSubmittedApplicationStatus().isPresent()) {
      cardBuilder.setApplicationStatus(
          programDatum
              .latestSubmittedApplicationStatus()
              .get()
              .localizedStatusText()
              .getOrDefault(preferredLocale));
      cardBuilder.setDateStatusApplied(
          formattedDateString(
              programDatum.latestSubmittedApplicationStatusTime(), preferredLocale));
    }

    Optional<LifecycleStage> lifecycleStage = programDatum.latestApplicationLifecycleStage();
    cardBuilder.setLifecycleStage(lifecycleStage);
    if (programDatum.latestSubmittedApplicationTime().isPresent()) {
      // Submitted tag says "Submitted on <DATE>" or "Submitted" if no date is found
      cardBuilder.setDateSubmitted(
          formattedDateString(programDatum.latestSubmittedApplicationTime(), preferredLocale));
    }

    if (shouldShowEligibilityTag(programDatum)) {
      boolean isEligible = programDatum.isProgramMaybeEligible().get();
      CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request);
      boolean isTrustedIntermediary = submittingProfile.isTrustedIntermediary();
      MessageKey mayQualifyMessage =
          isTrustedIntermediary ? MessageKey.TAG_MAY_QUALIFY_TI : MessageKey.TAG_MAY_QUALIFY;
      MessageKey mayNotQualifyMessage =
          isTrustedIntermediary
              ? MessageKey.TAG_MAY_NOT_QUALIFY_TI
              : MessageKey.TAG_MAY_NOT_QUALIFY;

      cardBuilder.setEligible(isEligible);
      cardBuilder.setEligibilityMessage(
          messages.at(
              isEligible ? mayQualifyMessage.getKeyName() : mayNotQualifyMessage.getKeyName()));
    }

    Optional<String> fileKey = program.summaryImageFileKey();
    if (fileKey.isPresent()) {
      String imageSourceUrl = publicStorageClient.getPublicDisplayUrl(fileKey.get());
      cardBuilder.setImageSourceUrl(imageSourceUrl);

      String altText = ProgramImageUtils.getProgramImageAltText(program, preferredLocale);
      cardBuilder.setAltText(altText);
    }

    return cardBuilder.build();
  }

  /**
   * Use the short description if present, otherwise use the long description with all markdown
   * removed and truncated to 100 characters.
   */
  static String selectAndFormatDescription(ProgramDefinition program, Locale preferredLocale) {
    String description = program.localizedShortDescription().getOrDefault(preferredLocale);

    if (description.isEmpty()) {
      description = program.localizedDescription().getOrDefault(preferredLocale);
      // Add a space before any new line characters so when markdown is stripped off the words
      // aren't smooshed together
      description = String.join("&nbsp;\n", description.split("\n"));
      description = StringUtils.abbreviate(TextFormatter.removeMarkdown(description), 100);
    }

    return description;
  }

  /**
   * Get the url that the button on the card should redirect to. If it's the first time filling out
   * the application, navigate to the program overview page. If the program is in draft mode,
   * navigate to where the applicant left off. If the program is submitted, navigate to the review
   * page.
   */
  static String getActionUrl(
      ApplicantRoutes applicantRoutes,
      Long programId,
      String programSlug,
      boolean isCommonIntakeForm,
      Optional<LifecycleStage> optionalLifecycleStage,
      Optional<Long> applicantId,
      Optional<CiviFormProfile> profile) {

    boolean haveApplicant = profile.isPresent() && applicantId.isPresent();

    // If it is an applicant's first time applying, render the program overview page
    String actionUrl =
        haveApplicant // TIs need to specify applicant ID.
            ? applicantRoutes.show(profile.get(), applicantId.get(), programSlug).url()
            : applicantRoutes.show(programSlug).url();

    // If the applicant has already started or submitted an application, render the edit or review
    // page accordingly
    if (optionalLifecycleStage.isPresent()) {
      if (optionalLifecycleStage.get() == LifecycleStage.ACTIVE) {
        // ACTIVE lifecycle stage means the application was submitted. Redirect them to the review
        // page.
        actionUrl =
            haveApplicant
                ? applicantRoutes.review(profile.get(), applicantId.get(), programId).url()
                : applicantRoutes.review(programId).url();
      } else if (optionalLifecycleStage.get() == LifecycleStage.DRAFT) {
        // DRAFT lifecycle stage means they have started but not submitted an application. Redirect
        // them to where they left off in the application.
        actionUrl =
            haveApplicant
                ? applicantRoutes.edit(profile.get(), applicantId.get(), programId).url()
                : applicantRoutes.edit(programId).url();
      }
      // If they are completing the common intake form for the first time, skip the program overview
      // page
    } else if (isCommonIntakeForm) {
      actionUrl =
          haveApplicant
              ? applicantRoutes.edit(profile.get(), applicantId.get(), programId).url()
              : applicantRoutes.edit(programId).url();
    }

    return actionUrl;
  }

  /**
   * For unstarted applications: If eligibility is gating, the eligibility tag should always show
   * when present. If eligibility is non-gating, the eligibility tag should only show if the user
   * may be eligible.
   *
   * <p>Applications that have been started do not show eligibility tags.
   */
  static boolean shouldShowEligibilityTag(ApplicantProgramData programData) {
    // This case happens when the applicant hasn't answered the eligibility question
    // on any application or when the program doesn't have eligibility criteria.
    if (!programData.isProgramMaybeEligible().isPresent()) {
      return false;
    }

    if (programData.latestApplicationLifecycleStage().isPresent()
        && (programData.latestApplicationLifecycleStage().get().equals(LifecycleStage.ACTIVE)
            || programData.latestApplicationLifecycleStage().get().equals(LifecycleStage.DRAFT))) {
      return false;
    }

    return programData.program().eligibilityIsGating()
        || programData.isProgramMaybeEligible().get();
  }

  private Optional<String> formattedDateString(
      Optional<Instant> optionalInstant, Locale preferredLocale) {
    return optionalInstant.map(
        instant -> dateConverter.renderShortDateInLocalTime(instant, preferredLocale));
  }

  @AutoValue
  public abstract static class ProgramSectionParams {
    public abstract ImmutableList<ProgramCardParams> cards();

    public abstract Optional<String> title();

    public abstract SectionType sectionType();

    /** The id of the section. Only needs to be specified if a title is also specified. */
    public abstract Optional<String> id();

    public static Builder builder() {
      return new AutoValue_ProgramCardsSectionParamsFactory_ProgramSectionParams.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setCards(List<ProgramCardParams> cards);

      public abstract Builder setTitle(String title);

      public abstract Builder setSectionType(SectionType sectionType);

      public abstract Builder setId(String id);

      public abstract ProgramSectionParams build();
    }
  }

  @AutoValue
  public abstract static class ProgramCardParams {
    public abstract String title();

    public abstract String actionText();

    public abstract String body();

    public abstract String actionUrl();

    public abstract boolean isGuest();

    public abstract boolean isCommonIntakeForm();

    public abstract Optional<String> loginModalId();

    public abstract Optional<Boolean> eligible();

    public abstract Optional<String> eligibilityMessage();

    public abstract Optional<String> applicationStatus();

    public abstract Optional<LifecycleStage> lifecycleStage();

    // Localized date String for the date on which the application was submitted.
    // If not submitted, this is empty.
    public abstract Optional<String> dateSubmitted();

    // Localized date String for the date on which the most recent ApplicationStatus was applied
    public abstract Optional<String> dateStatusApplied();

    public abstract Optional<String> imageSourceUrl();

    public abstract Optional<String> altText();

    public abstract ImmutableList<String> categories();

    public abstract long programId();

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

      public abstract Builder setIsGuest(Boolean isGuest);

      public abstract Builder setIsCommonIntakeForm(Boolean isCommonIntakeForm);

      public abstract Builder setLoginModalId(String loginModalId);

      public abstract Builder setEligible(Boolean eligible);

      public abstract Builder setEligibilityMessage(String eligibilityMessage);

      public abstract Builder setApplicationStatus(String applicationStatus);

      public abstract Builder setLifecycleStage(Optional<LifecycleStage> lifecycleStage);

      public abstract Builder setDateSubmitted(Optional<String> dateSubmitted);

      public abstract Builder setDateStatusApplied(Optional<String> dateStatusApplied);

      public abstract Builder setImageSourceUrl(String imageSourceUrl);

      public abstract Builder setAltText(String altText);

      public abstract Builder setCategories(ImmutableList<String> categories);

      public abstract Builder setProgramId(long id);

      public abstract ProgramCardParams build();
    }
  }
}
