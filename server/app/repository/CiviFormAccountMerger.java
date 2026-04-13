package repository;

import auth.NewGuestMergeLaunchStage;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CiviFormAccountMerger {
  private static final Logger logger = LoggerFactory.getLogger(CiviFormAccountMerger.class);

  /**
   * Merge data from the two applicants.
   *
   * <p>This is part of an in-progress and incomplete method.
   *
   * <p>The {@code civiformUser} account and applicant will be retained in the database with {@code
   * guestUser} merged into it. When merging question answers, {@code guestApp}'s data will take
   * precedence because it is newer.
   *
   * <p>When Draft applications must be merged the code will keep the most relevant one, however
   * note that there is no real data in a Draft application other than the creation time.
   *
   * <p>When a Guest Active application is moved it will have originalApplicantId set to the Guest
   * applicant to allow for matching across api data pulls. Draft applications will not have it set
   * because Draft applications are not pulled by the api and it serves no purpose then.
   *
   * @param newMergeStage what launch stage the new merge feature is at. Must be DRY_RUN OR ENABLED.
   */
  public void mergeApplicants(
      ApplicantModel civiformUser,
      ApplicantModel guestUser,
      NewGuestMergeLaunchStage newMergeStage) {
    boolean applyChanges =
        switch (newMergeStage) {
          case DRY_RUN -> false;
          case ENABLED -> true;
          default ->
              throw new IllegalArgumentException(
                  "New merge launch stage is not supported: " + newMergeStage);
        };
    // 1. Merge Applications.
    String log = mergeGuestApplicationsIntoCfUser(civiformUser, guestUser, applyChanges);
    // TODO(#11389): Steps 2 and 3 are not yet implemented.
    // 2. Update File references for guest to allow CF App
    //    * Set CFApp in ApplicantReadAcls
    // 3. Merge CFApp question answers and PAI into guest data and store in CF App
    logger.info(log);
  }

  /**
   * A container for the CiviForm user's applications. We don't need obsolete records for the CF
   * user as compared to the Guest.
   */
  private record CfUserApps(Optional<ApplicationModel> active, Optional<ApplicationModel> draft) {}

  /** A container for the Guest user's applications. */
  private record GuestUserApps(
      List<ApplicationModel> obsolete,
      Optional<ApplicationModel> active,
      Optional<ApplicationModel> draft) {}

  // Collect the applications into the nicer structure.
  private GuestUserApps categorizeGuestApps(List<ApplicationModel> apps) {
    List<ApplicationModel> obsolete =
        apps.stream()
            .filter(a -> a.getLifecycleStage() == LifecycleStage.OBSOLETE)
            .collect(Collectors.toList());
    Optional<ApplicationModel> active =
        apps.stream().filter(a -> a.getLifecycleStage() == LifecycleStage.ACTIVE).findFirst();
    Optional<ApplicationModel> draft =
        apps.stream().filter(a -> a.getLifecycleStage() == LifecycleStage.DRAFT).findFirst();
    return new GuestUserApps(obsolete, active, draft);
  }

  // Collect the applications into the nicer structure.
  private CfUserApps categorizeCfApps(List<ApplicationModel> apps) {
    Optional<ApplicationModel> active =
        apps.stream().filter(a -> a.getLifecycleStage() == LifecycleStage.ACTIVE).findFirst();
    Optional<ApplicationModel> draft =
        apps.stream().filter(a -> a.getLifecycleStage() == LifecycleStage.DRAFT).findFirst();
    return new CfUserApps(active, draft);
  }

  /// Merge logic:
  /// 1. For programs only in guest, move their applications to the cfUser
  /// 2. For programs in both:
  ///    1. Obsolete applications are moved over
  ///    2. Active and Draft applications are reconciled based on the versions present:
  ///
  /// An important note on reasoning about what is kept pertains to the
  /// impact of Draft applications.
  /// * A Draft application when there is also an Active application is largely meaningless.
  ///   * The UI shows the Active application state regardless of the Draft application.
  ///   * There is no data in a Draft application other than created time, which is not
  ///  particularly useful. Any question answers are stored on the applicant,
  ///  not in a Draft application.
  /// * A Draft-only application, when there is no Active application, does change the UI.
  ///
  ///  So Draft applications when there is an Active application can largely be ignored when we
  /// consider the user impact and complexity of merging data.
  ///
  /// | CiviForm      | Guest         | Result |
  /// |-|-|-|
  /// |Draft-only     | Draft-only    | Keep Guest |
  /// |Active         | Draft-only    | Keep CF |
  /// |Draft-only     | Active        | Keep Guest |
  /// |Active         | Active        | Keep newer, obsolete older|
  ///
  /// A Draft application not kept is deleted. An Active application not kept will be
  ///  obsoleted.
  ///
  /// If there is an Active and Draft application on either user, the Active application will be
  /// reconciled and the associated Draft application will be persisted. As Noted above, the Draft
  /// application is not particularly useful, but we prefer to keep the Draft associated with
  /// whichever Active was maintained as the Active for consistency.
  ///
  ///  @return a log message indicating what changes occurred.
  private String mergeGuestApplicationsIntoCfUser(
      ApplicantModel cfUser, ApplicantModel guestUser, boolean applyChanges) {
    // It is a bug of the formatter that this can't be in the markdown above
    // https://github.com/google/google-java-format/issues/1369
    // Design Doc:
    // https://docs.google.com/document/d/1qq5lLXMgAxMvrsZzcyKEmZsQAtnCt6lcpkqQ836IWhE/edit?tab=t.0#heading=h.w8d3omccpuw1

    // Collect the user apps into lookup tables keyed on Program IDs.
    // We don't collect into a GuestUserApps immediately here as the List is
    // valuable.
    Map<Long, List<ApplicationModel>> guestAppsByProgram =
        guestUser.getApplications().stream()
            .collect(Collectors.groupingBy(app -> app.getProgram().id));

    Map<Long, CfUserApps> cfAppsByProgram =
        cfUser.getApplications().stream()
            .collect(
                Collectors.groupingBy(
                    app -> app.getProgram().id,
                    Collectors.collectingAndThen(Collectors.toList(), this::categorizeCfApps)));

    // Log messages for programs where we only move vs merge data. Broken out
    // to render the final display nicer.
    StringBuilder logMessageMove = new StringBuilder();
    StringBuilder logMessageMerge = new StringBuilder();

    // Evaluate each of the guest's programs.
    for (Map.Entry<Long, List<ApplicationModel>> entry : guestAppsByProgram.entrySet()) {
      long programId = entry.getKey();
      List<ApplicationModel> guestApps = entry.getValue();

      boolean bothHaveProgram = cfAppsByProgram.containsKey(programId);
      if (bothHaveProgram) {
        GuestUserApps guestUserApps = categorizeGuestApps(guestApps);
        // Merge the applications.
        CfUserApps cfUserApps = cfAppsByProgram.get(programId);
        var log =
            reconcileApplicationsForProgram(
                cfUser, guestUser, cfUserApps, guestUserApps, programId, applyChanges);
        logMessageMerge.append(log);
      } else {
        // The CiviForm user doesn't have the program so just move over the
        // applications.
        StringJoiner programIds = new StringJoiner(", ");
        for (ApplicationModel app : guestApps) {
          programIds.add(app.id.toString());
          if (applyChanges) {
            // Don't set on Draft applications as there's no utility.
            if (!app.getLifecycleStage().equals(LifecycleStage.DRAFT)) {
              app.setOriginalApplicantId(guestUser.id);
            }
            app.setApplicant(cfUser);
            app.save();
          }
        }
        // We'll add a header to these later.
        logMessageMove.append(
            """
              * Program ID %d: Application IDs %s
            """
                .formatted(programId, programIds.toString()));
      }
    }

    // Build the complete log message.
    StringJoiner finalLogMessage = new StringJoiner("\n");
    finalLogMessage.add(
        """
        Merging CiviForm user and Guest user:
          CiviForm:
            Account  : id %d
            Applicant: id %d creation: %s
          Guest:
            Account  : id %d
            Applicant: id %d creation: %s

        """
            .formatted(
                cfUser.getAccount().id,
                cfUser.id,
                cfUser.getWhenCreated(),
                guestUser.getAccount().id,
                guestUser.id,
                guestUser.getWhenCreated()));
    if (!logMessageMove.isEmpty()) {
      finalLogMessage.add(
          """
          CiviForm user does not have the following programs, moving all applications from Guest to CiviForm user.
          %s
          """
              .formatted(logMessageMove));
    }
    if (!logMessageMerge.isEmpty()) {
      finalLogMessage.add(logMessageMerge);
    }

    return finalLogMessage.toString();
  }

  /**
   * Reconcile applications between the CiviForm user and the Guest user.
   *
   * @return a log message indicating what changes occurred.
   */
  private String reconcileApplicationsForProgram(
      ApplicantModel cfUser,
      ApplicantModel guestUser,
      CfUserApps cfUserApps,
      GuestUserApps guestUserApps,
      long programId,
      boolean applyChanges) {

    // To reconcile applications, we prefer:
    // * Active applications over Draft applications.
    // * The logged in CiviForm user's applications over the guest's.
    // * Newer over older.
    //
    // See the comment on mergeGuestApplicationsIntoCfUser for more
    // details.
    //
    // For ease of readability, the code will handle application states
    // from earlier to later in the lifecycle: Obsolete, Active, Draft

    boolean cfHasActive = cfUserApps.active().isPresent();
    boolean cfHasDraft = cfUserApps.draft().isPresent();
    boolean guestHasActive = guestUserApps.active().isPresent();
    boolean guestHasDraft = guestUserApps.draft().isPresent();
    // Both users must have at least an Active or Draft application for this method to be useful.
    // (The system doesn't allow for Obsolete without an Active application.)
    Preconditions.checkState(
        cfHasActive || cfHasDraft,
        "CiviForm user must have at least one of an Active (%s) or Draft (%s) Application but does"
            + " not.",
        cfHasActive,
        cfHasDraft);
    Preconditions.checkState(
        guestHasActive || guestHasDraft,
        "Guest user must have at least one of an Active (%s) or Draft (%s) Application but does"
            + " not.",
        guestHasActive,
        guestHasDraft);

    StringBuilder logMessage =
        new StringBuilder(
            """
            Reconciling Program id %d

            """
                .formatted(programId));

    // Always move the guest's Obsolete apps to cfUser.
    StringJoiner obsoleteIds = new StringJoiner(", ");
    for (ApplicationModel obsoleteApp : guestUserApps.obsolete()) {
      obsoleteIds.add(obsoleteApp.id.toString());
      if (applyChanges) {
        obsoleteApp.setOriginalApplicantId(guestUser.id);
        obsoleteApp.setApplicant(cfUser);
        obsoleteApp.save();
      }
    }
    if (obsoleteIds.length() > 0) {
      logMessage.append(
          """
            Moving obsolete application IDs %s to CiviForm applicant %s

          """
              .formatted(obsoleteIds, cfUser.id));
    }

    // Reconcile the Active and Draft applications.
    final String log;
    if (cfHasActive) {
      if (guestHasActive) {
        // Handle all cases where both have Active applications. Either may have a Draft application
        // too.
        log =
            reconcileApplicationsBothWithActives(
                cfUser, guestUser, cfUserApps, guestUserApps, applyChanges);
      } else {
        // Handle CF user having an Active application, Guest does not, but it implicitly
        // has a Draft application.
        log = reconcileApplicationsCfActiveGuestNoActive(cfUserApps, guestUserApps, applyChanges);
      }
    } else if (guestHasActive) {
      // Guest user has an Active application, CF user does not, but it  implicitly has a draft
      // application.
      log =
          reconcileApplicationsCfNoActiveGuestActive(
              cfUser, guestUser, cfUserApps, guestUserApps, applyChanges);
    }
    // Both only have a Draft application.
    else if (cfHasDraft && guestHasDraft) {
      // IDE static analysis checker confirms these are always true at this
      // point but explicitly specifying for readability.
      log = reconcileApplicationsBothWithDraftOnly(cfUser, cfUserApps, guestUserApps, applyChanges);
    } else {
      // IDE static analysis checker confirms this is not reachable but putting
      // here for readability, and future change detection.
      throw new IllegalArgumentException(
          "Should not reach this state, previous conditions should be exhaustive.");
    }
    logMessage.append(log);
    return logMessage.toString();
  }

  /**
   * Reconciles when both users have an Active application.
   *
   * <p>Keeps whichever Active application is newest along with its Draft application, if present.
   * The other Active application is obsoleted and its Draft application deleted.
   *
   * <p>The system has an invariant that an Active application shouldn't be older than an obsolete
   * one, so we must keep the newer Active application.
   *
   * @param applyChanges if database changes should be applied. If false the return will log what
   *     would have occurred.
   * @return a log message indicating what changes occurred.
   */
  private static String reconcileApplicationsBothWithActives(
      ApplicantModel cfUser,
      ApplicantModel guestUser,
      CfUserApps cfUserApps,
      GuestUserApps guestUserApps,
      boolean applyChanges) {
    ApplicationModel cfActive = cfUserApps.active().orElseThrow();
    ApplicationModel guestActive = guestUserApps.active().orElseThrow();
    boolean cfUserIsNewer = cfActive.getSubmitTime().isAfter(guestActive.getSubmitTime());

    StringBuilder logMessage =
        new StringBuilder(
            """
            CiviForm user and Guest both have an Active application.
              * CF user:
                * Application id %d
                * Submitted: %s
              * Guest user:
                * Application id %d
                * Submitted: %s

            """
                .formatted(
                    cfActive.id,
                    cfActive.getSubmitTime(),
                    guestActive.id,
                    guestActive.getSubmitTime()));
    if (cfUserIsNewer) {
      // Obsolete the guest Active app and delete its Draft if it exists.
      logMessage.append(
          """
            * Keeping CF user application
            * Obsoleting and moving Guest Active application id %d to CiviForm applicant id %d
          """
              .formatted(guestActive.id, cfUser.id));
      if (applyChanges) {
        // Guest's is older to obsolete it.
        guestActive
            .setLifecycleStage(LifecycleStage.OBSOLETE)
            .setOriginalApplicantId(guestUser.id)
            .setApplicant(cfUser);
        guestActive.save();
      }

      if (guestUserApps.draft().isPresent()) {
        var guestDraft = guestUserApps.draft().get();
        logMessage.append("  * Deleting Guest Draft application id %d".formatted(guestDraft.id));
        if (applyChanges) {
          // Guest's Active is obsoleted so delete its draft since there is
          // not direct association anymore.
          guestDraft.delete();
        }
      }
      return logMessage.toString();
    }

    // Guest's Active is Newer so keep it and obsolete the CiviForm user's.
    logMessage.append(
        """
          * Keeping Guest user application
          * Obsoleting CF user Active application id %d
        """
            .formatted(cfActive.id));
    if (applyChanges) {
      cfActive.setLifecycleStage(LifecycleStage.OBSOLETE);
      cfActive.save();
    }

    logMessage.append(
        """
          * Moving Guest Active application id %d to CiviForm applicant id %d
        """
            .formatted(guestActive.id, cfUser.id));
    // Move the guest's Active to the CF user since it is newer, associating
    // it with the guest applicant.
    if (applyChanges) {
      guestActive.setOriginalApplicantId(guestUser.id);
      guestActive.setApplicant(cfUser);
      guestActive.save();
    }

    // If there are Drafts: Move the Guest's and delete the CiviForm user's
    // since we are using the Guest's Active and ont the CiviForm user's.
    if (cfUserApps.draft().isPresent()) {
      var cfDraft = cfUserApps.draft().get();
      logMessage.append("  * Deleting CF Draft application id %d".formatted(cfDraft.id));
      if (applyChanges) {
        cfDraft.delete();
      }
    }

    // Move the guest Draft if it exists.
    if (guestUserApps.draft.isPresent()) {
      var guestDraft = guestUserApps.draft().get();
      logMessage.append(
          """
            * Moving Guest Draft application id %d to CiviForm applicant id %d
          """
              .formatted(guestDraft.id, cfUser.id));
      if (applyChanges) {
        guestDraft.setApplicant(cfUser);
        guestDraft.save();
      }
    }

    return logMessage.toString();
  }

  /**
   * Reconciles when the CF user has an Active application and the Guest does not, but implicitly
   * has a Draft application.
   *
   * <p>The guest's Draft is removed in favor of the CiviForm user's, if present, and they will see
   * the submitted application they have in the CiviForm user's account. They can then decide if
   * they actually want to resubmit.
   *
   * @param applyChanges if database changes should be applied. If false the return will log what
   *     would have occurred.
   * @return a log message indicating what changes occurred.
   */
  private String reconcileApplicationsCfActiveGuestNoActive(
      CfUserApps cfUserApps, GuestUserApps guestUserApps, boolean applyChanges) {

    // Keep the CF user data as is, delete the Guest Draft application.
    if (applyChanges) {
      guestUserApps.draft().orElseThrow().delete();
    }

    return """
    CiviForm user has an Active application, Guest does not.
      * Keeping CiviForm user application id %d
      * Deleting Guest Draft application id %d
    """
        .formatted(cfUserApps.active().orElseThrow().id, guestUserApps.draft.orElseThrow().id);
  }

  /**
   * Reconciles when the CF user does not have an Active application and implicitly has a draft
   * application, and the Guest user has an Active application.
   *
   * <p>The guest's applications are moved to the CiviForm User, and the CiviForm user's Draft is
   * removed if present. This will provide the same view of the program to the guest post-login as
   * they had before.
   *
   * @param applyChanges if database changes should be applied. If false the return will log what
   *     would have occurred.
   * @return a log message indicating what changes occurred.
   */
  private static String reconcileApplicationsCfNoActiveGuestActive(
      ApplicantModel cfUser,
      ApplicantModel guestUser,
      CfUserApps cfUserApps,
      GuestUserApps guestUserApps,
      boolean applyChanges) {

    StringBuilder logMessage =
        new StringBuilder("CiviForm user does not have an Active application, Guest does.");

    logMessage.append(
        """
          * Deleting CF Draft application id %d
        """
            .formatted(cfUserApps.draft.orElseThrow().id));
    if (applyChanges) {
      // There must be a Draft since there is no Active
      cfUserApps.draft().orElseThrow().delete();
    }

    // Move the guests Active and Draft over.
    var guestActive = guestUserApps.active().orElseThrow();
    logMessage.append(
        """
          * Moving Guest Active application id %d to CiviForm applicant id %d
        """
            .formatted(guestActive.id, cfUser.id));
    if (applyChanges) {
      // Move the guest's Active over and associate it with the guest applicant.
      guestActive.setOriginalApplicantId(guestUser.id);
      guestActive.setApplicant(cfUser);
      guestActive.save();
    }

    if (guestUserApps.draft().isPresent()) {
      var guestDraft = guestUserApps.draft().get();

      logMessage.append(
          """
            * Moving Guest Draft application id %d to CiviForm applicant id %d
          """
              .formatted(guestDraft.id, cfUser.id));
      if (applyChanges) {
        guestDraft.setApplicant(cfUser);
        guestDraft.save();
      }
    }

    return logMessage.toString();
  }

  /**
   * Reconciles when both only have a Draft application.
   *
   * <p>Keeps the guest's Draft. There is no material difference to the user, but if we ever look at
   * creation dates the guest's will be more relevant as it's the last one the user saw.
   *
   * @param applyChanges if database changes should be applied. If false the return will log what
   *     would have occurred.
   * @return a log message indicating what changes occurred.
   */
  private static String reconcileApplicationsBothWithDraftOnly(
      ApplicantModel cfUser,
      CfUserApps cfUserApps,
      GuestUserApps guestUserApps,
      boolean applyChanges) {
    var cfDraft = cfUserApps.draft.orElseThrow();
    var guestDraft = guestUserApps.draft.orElseThrow();

    if (applyChanges) {
      // Delete the CF Draft, keep the guest.
      cfDraft.delete();
      guestDraft.setApplicant(cfUser).save();
    }
    return """
    CiviForm user and Guest both only have a Draft application.
      * Deleting CiviForm Draft application id %d
      * Moving Guest Draft application id %d to CiviForm applicant id %d
    """
        .formatted(cfDraft.id, guestDraft.id, cfUser.id);
  }
}
