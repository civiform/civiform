package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.applicant.ApplicantRoutes;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;
import views.ProgramImageUtils;
import views.components.Modal;

/**
 * Factory for creating parameter info for applicant program card sections.
 *
 * <p>The template which uses this is defined in ProgramCardsSectionFragment.html
 */
public final class ProgramCardsSectionParamsFactory {
  private final ApplicantRoutes applicantRoutes;
  private final ProfileUtils profileUtils;
  private final PublicStorageClient publicStorageClient;

  @Inject
  public ProgramCardsSectionParamsFactory(
      ApplicantRoutes applicantRoutes,
      ProfileUtils profileUtils,
      PublicStorageClient publicStorageClient) {
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.profileUtils = checkNotNull(profileUtils);
    this.publicStorageClient = checkNotNull(publicStorageClient);
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
      ApplicantPersonalInfo personalInfo) {

    ProgramSectionParams.Builder sectionBuilder =
        ProgramSectionParams.builder()
            .setCards(
                getCards(
                    request,
                    messages,
                    buttonText,
                    programData,
                    preferredLocale,
                    profile,
                    applicantId,
                    personalInfo));

    if (title.isPresent()) {
      sectionBuilder.setTitle(messages.at(title.get().getKeyName()));
      sectionBuilder.setId(Modal.randomModalId());
    }

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

    //  TODO ssandbekkhaug create guest profile
    //  Notes
    //  Try to recreate this:
    //  GET       /callback?client_name=GuestClient       20ms    303 -> /programs/102/review
    //   
    //  GET     /callback                    controllers.CallbackController.callback(request: Request, client_name: String)
    //  CallbackController.java
    //  [java.base/java.lang.Thread.getStackTrace(Thread.java:1619), auth.ProfileFactory.createNewApplicant(ProfileFactory.java:60), auth.GuestClient.lambda$internalInit$1(GuestClient.java:42), org.pac4j.core.client.BaseClient.lambda$retrieveCredentials$0(BaseClient.java:75), java.base/java.util.Optional.ifPresent(Optional.java:178), org.pac4j.core.client.BaseClient.retrieveCredentials(BaseClient.java:72), org.pac4j.core.client.IndirectClient.getCredentials(IndirectClient.java:145), org.pac4j.core.engine.DefaultCallbackLogic.perform(DefaultCallbackLogic.java:75), org.pac4j.play.CallbackController.lambda$callback$0(CallbackController.java:53), java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1768), play.core.j.ClassLoaderExecutionContext.$anonfun$execute$1(ClassLoaderExecutionContext.scala:64), akka.dispatch.TaskInvocation.run(AbstractDispatcher.scala:49), akka.dispatch.ForkJoinExecutorConfigurator$AkkaForkJoinTask.exec(ForkJoinExecutorConfigurator.scala:48), java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:373), java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1182), java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1655), java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1622), java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:165)]

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

    boolean isGuest = personalInfo.getType() == GUEST;
    // String actionUrl = profile.isPresent() && applicantId.isPresent()
    //         ? applicantRoutes.review(profile.get(), applicantId.get(), program.id()).url()
    //         : applicantRoutes.review(program.id()).url();
    // String actionUrl =
    // profile.isPresent() && applicantId.isPresent() ?
    //     applicantRoutes
    //         .blockEdit(profile.get(), applicantId.get(), program.id(), "1", Optional.empty())
    //         .url()
    //         :
    //         applicantRoutes.blockEdit(null, 0, 0, null, null)
    // System.out.println("ssandbekkhaug profile: " + profile);
    String actionUrl =
          applicantRoutes.edit(profile.get(),
          applicantId.get(),
          program.id()
          ).url();

    // Note this doesn't yet manage markdown, links and appropriate aria labels for links, and
    // whatever else our current cards do.
    String detailsUrl = program.externalLink();
    if (detailsUrl.isEmpty() || detailsUrl.isBlank()) {
      detailsUrl =
          profile.isPresent() && applicantId.isPresent()
              ? applicantRoutes.show(profile.get(), applicantId.get(), program.id()).url()
              : applicantRoutes.show(program.id()).url();
    }
    cardBuilder
        .setTitle(program.localizedName().getOrDefault(preferredLocale))
        .setBody(program.localizedDescription().getOrDefault(preferredLocale))
        .setDetailsUrl(detailsUrl)
        .setActionUrl(actionUrl)
        .setIsGuest(isGuest)
        .setActionText(messages.at(buttonText.getKeyName()));

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
   * If eligibility is gating, the eligibility tag should always show when present. If eligibility
   * is non-gating, the eligibility tag should only show if the user may be eligible.
   */
  private static boolean shouldShowEligibilityTag(ApplicantProgramData programData) {
    if (!programData.isProgramMaybeEligible().isPresent()) {
      return false;
    }

    return programData.program().eligibilityIsGating()
        || programData.isProgramMaybeEligible().get();
  }

  @AutoValue
  public abstract static class ProgramSectionParams {
    public abstract ImmutableList<ProgramCardParams> cards();

    public abstract Optional<String> title();

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

      public abstract Builder setId(String id);

      public abstract ProgramSectionParams build();
    }
  }

  @AutoValue
  public abstract static class ProgramCardParams {
    public abstract String title();

    public abstract String actionText();

    public abstract String body();

    public abstract String detailsUrl();

    public abstract String actionUrl();

    public abstract boolean isGuest();

    public abstract Optional<String> loginModalId();

    public abstract Optional<Boolean> eligible();

    public abstract Optional<String> eligibilityMessage();

    public abstract Optional<String> applicationStatus();

    public abstract Optional<String> imageSourceUrl();

    public abstract Optional<String> altText();

    public static Builder builder() {
      return new AutoValue_ProgramCardsSectionParamsFactory_ProgramCardParams.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setTitle(String title);

      public abstract Builder setActionText(String actionText);

      public abstract Builder setBody(String body);

      public abstract Builder setDetailsUrl(String detailsUrl);

      public abstract Builder setActionUrl(String actionUrl);

      public abstract Builder setIsGuest(Boolean isGuest);

      public abstract Builder setLoginModalId(String loginModalId);

      public abstract Builder setEligible(Boolean eligible);

      public abstract Builder setEligibilityMessage(String eligibilityMessage);

      public abstract Builder setApplicationStatus(String applicationStatus);

      public abstract Builder setImageSourceUrl(String imageSourceUrl);

      public abstract Builder setAltText(String altText);

      public abstract ProgramCardParams build();
    }
  }
}
