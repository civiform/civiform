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

  /**
   * Merge data from the two applicants.
   *
   * <p>This is part of an in-progress and incomplete method.
   *
   * <p>The {@code civiformUser} account and applicant will be retained in the database with {@code
   * guestUser} merged into it. When merging question answers, {@code guestApp}'s data will take
   * precedence because it is newer.
   *
   * <p>When draft applications must be merged the code will keep the most relevant one, however note that there
   * is no real data in a draft application other than the creation time.
   *
   * <p>When a Guest active application is moved it will have originalApplicantId set to the Guest
   * applicant to allow for matching across api data pulls. Draft applications will not have it set because draft applications are not pulled by the api.
   * and it serves no purpose then.
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
    // TODO(#11389): Steps 2 and 3 are not yet implemented.
    // 2. Update File references for guest to allow CF App
    //    * Set CFApp in ApplicantReadAcls
    // 3. Merge CFApp question answers and PAI into guest data and store in CF App
    String log = mergeGuestApplicationsIntoCfUser(civiformUser, guestUser, applyChanges);
    logger.info(log);
  }

  /// Merge logic:
  /// 1. For programs only in guest, move them to the cfUser
  /// 2. For programs in both:
  ///    1. Obsolete are moved over
  ///    2. Reconciled based on the versions present:
  ///
  /// An important note on reasoning about what is kept pertains to the
  /// impact of draft applications.
  /// * A draft application when there is also an active application is largely meaningless.
  ///   * The UI shows the active application state regardless of the draft application.
  ///   * There is no data in a draft application other than created time, which is not
  ///  particularly useful. Any question answers are stored on the applicant,
  ///  not in a draft application.
  /// * A draft-only application, when there is no active application, does change the UI.
  ///
  ///  So draft applications when there is an active application can largely be ignored when we
  /// consider the user impact and complexity of merging data.
  ///
  /// | CiviForm      | Guest         | Result |
  /// |-|-|-|
  /// |Draft-only     | Draft-only    | Keep Guest |
  /// |Active         | Draft-only    | Keep CF |
  /// |Draft-only     | Active        | Keep Guest |
  /// |Active         | Active        | Keep newer, obsolete older|
  ///
  /// A draft application not kept is deleted. An active application not kept will be
  ///  obsoleted.
  ///
  /// If there is an active and draft application for either user, the active application
  /// will be reconciled and only the kept ones draft application will be
  /// persisted. As noted above, the draft application is not particularly useful, but we
  /// still treat it as such baring just deleting them.
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
            reconcileAppsForProgram(
                cfUser, guestUser, cfUserApps, guestUserApps, programId, applyChanges);
        logMessageMerge.append(log);
      } else {
        // The CiviForm user doesn't have the program so just move over the
        // applications.
        StringJoiner programIds = new StringJoiner(", ");
        for (ApplicationModel app : guestApps) {
          programIds.add(app.id.toString());
          if (applyChanges) {
            // Don't set on draft applications as there's no utility.
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
  private String reconcileAppsForProgram(
      ApplicantModel cfUser,
      ApplicantModel guestUser,
      CfUserApps cfUserApps,
      GuestUserApps guestUserApps,
      long programId,
      boolean applyChanges) {

    // To reconcile applications, we prefer:
    // * Active applications over draft applications.
    // * The logged in CiviForm user's data over the Guest's.
    // * Newer over older.
    //
    // See the comment on mergeGuestApplicationIntoCFUser for more
    // details.
    //
    // For ease of readability, the code will handle application states
    // from earlier to later in the lifecycle: obsolete, active, draft

    boolean cfHasActive = cfUserApps.active().isPresent();
    boolean cfHasDraft = cfUserApps.draft().isPresent();
    boolean guestHasActive = guestUserApps.active().isPresent();
    boolean guestHasDraft = guestUserApps.draft().isPresent();
    // Both users must have at least a draft or active application for this method to be
    // useful. (The system doesn't allow for obsolete without an active application.)
    Preconditions.checkState(cfHasDraft || cfHasActive);
    Preconditions.checkState(guestHasDraft || guestHasActive);

    StringBuilder logMessage =
        new StringBuilder(
            """
            Reconciling Program id %d

            """
                .formatted(programId));

    // Always move the guest's obsolete apps to cfUser.
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

    // Reconcile the active and draft applications.
    final String log;
    if (cfHasActive) {
      if (guestHasActive) {
        // Handle all cases where both have active applications. Either may have a draft application too.
        log = reconcileBothWithActives(cfUser, guestUser, cfUserApps, guestUserApps, applyChanges);
      } else {
        // Handle CF user having an active application, Guest does not, but it implicitly
        // has a draft application.
        log = reconcileCfActiveGuestNoActive(cfUserApps, guestUserApps, applyChanges);
      }
    } else if (guestHasActive) {
      // Guest user has an active application, CF user does not, but it  implicitly has a draft application.
      log =
          reconcileCfNoActiveGuestActive(
              cfUser, guestUser, cfUserApps, guestUserApps, applyChanges);
    }
    // Both only have a draft application.
    else if (cfHasDraft && guestHasDraft) {
      // IDE static analysis checker confirms these are always true at this
      // point but explicitly specifying for readability.
      log = reconcileBothWithDraftOnly(cfUser, cfUserApps, guestUserApps, applyChanges);
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
   * Reconciles when both users have an active application.
   *
   * <p>Keeps whichever active application is newest along with its draft application; if present.
   * The other active application is obsoleted and its draft application deleted.
   *
   * <p>The system has an invariant that an active application shouldn't be older than an obsolete
   * one, so we must keep the newer active application.
   *
   * @param applyChanges if database changes should be applied. If false the return will log what
   *     would have occurred.
   * @return a log message indicating what changes occurred.
   */
  private static String reconcileBothWithActives(
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
            CiviForm user and Guest both have an active application.
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
      // Obsolete the guest active app and delete its draft if it exists.
      logMessage.append(
          """
            * Keeping CF user application
            * Obsoleting and moving Guest active application id %d to CiviForm applicant id %d
          """
              .formatted(guestActive.id, cfUser.id));
      if (applyChanges) {
        guestActive
            .setLifecycleStage(LifecycleStage.OBSOLETE)
            .setOriginalApplicantId(guestUser.id)
            .setApplicant(cfUser);
        guestActive.save();
      }

      if (guestUserApps.draft().isPresent()) {
        var guestDraft = guestUserApps.draft().get();
        logMessage.append("  * Deleting Guest draft application id %d".formatted(guestDraft.id));
        if (applyChanges) {
          guestDraft.delete();
        }
      }
      return logMessage.toString();
    }

    // Guest's active is Newer.
    logMessage.append(
        """
          * Keeping Guest user application
          * Obsoleting CF user active application id %d
        """
            .formatted(cfActive.id));
    // Obsolete CF app.
    if (applyChanges) {
      cfActive.setLifecycleStage(LifecycleStage.OBSOLETE);
      cfActive.save();
    }

    logMessage.append(
        """
          * Moving Guest active application id %d to CiviForm applicant id %d
        """
            .formatted(guestActive.id, cfUser.id));
    // Move the guest app to the cfUser
    if (applyChanges) {
      guestActive.setOriginalApplicantId(guestUser.id);
      guestActive.setApplicant(cfUser);
      guestActive.save();
    }

    // Delete the CF draft if it exists.
    if (cfUserApps.draft().isPresent()) {
      var cfDraft = cfUserApps.draft().get();
      logMessage.append("  * Deleting CF draft application id %d".formatted(cfDraft.id));
      if (applyChanges) {
        cfDraft.delete();
      }
    }

    // Move the guest draft if it exists.
    if (guestUserApps.draft.isPresent()) {
      var guestDraft = guestUserApps.draft().get();
      logMessage.append(
          """
            * Moving Guest draft application id %d to CiviForm applicant id %d
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
   * Reconciles when the CF user has an active application and the Guest does not, but implicitly
   * has a draft application.
   *
   * @param applyChanges if database changes should be applied. If false the return will log what
   *     would have occurred.
   * @return a log message indicating what changes occurred.
   */
  private String reconcileCfActiveGuestNoActive(
      CfUserApps cfUserApps, GuestUserApps guestUserApps, boolean applyChanges) {

    // Keep the CF user data as is, delete the Guest draft application.
    if (applyChanges) {
      // delete the entry
      guestUserApps.draft().orElseThrow().delete();
    }

    return """
    CiviForm user has an active application, Guest does not.
      * Keeping CiviForm user application id %d
      * Deleting Guest draft application id %d
    """
        .formatted(cfUserApps.active().orElseThrow().id, guestUserApps.draft.orElseThrow().id);
  }

  /**
   * Reconciles when the CF user does not have an active application and implicitly has a draft
   * application, and the Guest user has an active application.
   *
   * @param applyChanges if database changes should be applied. If false the return will log what
   *     would have occurred.
   * @return a log message indicating what changes occurred.
   */
  private static String reconcileCfNoActiveGuestActive(
      ApplicantModel cfUser,
      ApplicantModel guestUser,
      CfUserApps cfUserApps,
      GuestUserApps guestUserApps,
      boolean applyChanges) {

    StringBuilder logMessage =
        new StringBuilder("CiviForm user does not have an active application, Guest does.");

    logMessage.append(
        """
          * Deleting CF draft application id %d
        """
            .formatted(cfUserApps.draft.orElseThrow().id));
    if (applyChanges) {
      // There must be a draft since there is no active.
      cfUserApps.draft().orElseThrow().delete();
    }

    var guestActive = guestUserApps.active().orElseThrow();
    logMessage.append(
        """
          * Moving Guest active application id %d to CiviForm applicant id %d
        """
            .formatted(guestActive.id, cfUser.id));
    if (applyChanges) {
      guestActive.setOriginalApplicantId(guestUser.id);
      guestActive.setApplicant(cfUser);
      guestActive.save();
    }

    if (guestUserApps.draft().isPresent()) {
      var guestDraft = guestUserApps.draft().get();

      logMessage.append(
          """
            * Moving Guest draft application id %d to CiviForm applicant id %d
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
   * Reconciles when both only have a draft application.
   *
   * @param applyChanges if database changes should be applied. If false the return will log what
   *     would have occurred.
   * @return a log message indicating what changes occurred.
   */
  private static String reconcileBothWithDraftOnly(
      ApplicantModel cfUser,
      CfUserApps cfUserApps,
      GuestUserApps guestUserApps,
      boolean applyChanges) {
    var cfDraft = cfUserApps.draft.orElseThrow();
    var guestDraft = guestUserApps.draft.orElseThrow();

    if (applyChanges) {
      // Delete the CF draft, keep the guest.
      cfDraft.delete();
      guestDraft.setApplicant(cfUser).save();
    }
    return """
    CiviForm user and Guest both only have a draft application.
      * Deleting CiviForm draft application id %d
      * Moving Guest draft application id %d to CiviForm applicant id %d
    """
        .formatted(cfDraft.id, guestDraft.id, cfUser.id);
  }
}
