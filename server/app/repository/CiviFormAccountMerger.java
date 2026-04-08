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
   * Merge data from the two Applicants.
   *
   * <p>This is part of an in-progress and incomplete method.
   *
   * <p>The {@code civiformUser} Account and Applicant will be retained in the database with {@code
   * guestUser} merged into it. When merging question answers, {@code guestApp}'s data will take
   * precedence.
   *
   * <p>When drafts must be merged the code will keep the most relevant one, however note that there
   * is no real data in a draft other than the creation time.
   *
   * <p>When a Guest Active application is moved it will have originalApplicantId set to the Guest
   * Applicant. Drafts will not have it set because their data is not consumed by an external user,
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

  /**
   * A container for the CiviForm User's applications. We don't need obsolete records for the CF
   * user as compared to the Guest.
   */
  private record CfUserApps(Optional<ApplicationModel> active, Optional<ApplicationModel> draft) {}

  /** A container for the Guest User's applications. */
  private record GuestUserApps(
      List<ApplicationModel> obsolete,
      Optional<ApplicationModel> active,
      Optional<ApplicationModel> draft) {}

  /// Merge logic:
  /// 1. For programs only in guest, move them to the cfUser
  /// 2. For programs in both:
  ///    1. Obsolete are moved over
  ///    2. Reconciled based on the versions present:
  ///
  /// An important note on reasoning about what is kept pertains to the
  /// impact of Drafts.
  /// * A Draft when there is also a Active is largely meaningless.
  ///   * The UI shows the Active state regardless of the Draft.
  ///   * There is no data in a Draft other than created time, which is not
  ///  particularly useful. Any question answers are stored on the Applicant,
  ///  not in a Draft.
  /// * A Draft-only, when there is no Active, does change the UI.
  ///
  ///  So Drafts when there is a Active can largely be ignored when we
  /// consider the user impact and complexity of merging data.
  ///
  /// | CiviForm  | Guest      | Result |
  /// |-|-|-|
  /// |Draft-only | Draft-only | Keep Guest |
  /// |Active     | Draft-only | Keep CF |
  /// |Draft-only | Active     | Keep Guest |
  /// |Active     | Active     | Keep newer, Obsolete older|
  ///
  /// A Draft version not kept is deleted. An Active version not kept will be
  ///  obsoleted.
  ///
  /// If there is an active and draft for either user, the active
  /// version will be reconciled and only the kept ones draft will be
  /// persisted. As noted above, the Draft is not particularly useful, but we
  /// still treat it as such baring just deleting them.
  ///
  ///  @return a log message indicating what changes occurred.
  private String mergeGuestApplicationsIntoCfUser(
      ApplicantModel cfUser, ApplicantModel guestUser, boolean applyChanges) {
    // It is a bug of the formatter that this can't be in the markdown above
    // https://github.com/google/google-java-format/issues/1369
    // Design Doc:
    // https://docs.google.com/document/d/1qq5lLXMgAxMvrsZzcyKEmZsQAtnCt6lcpkqQ836IWhE/edit?tab=t.0#heading=h.w8d3omccpuw1
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
      GuestUserApps guestUserApps = categorizeGuestApps(guestApps);

      boolean bothHaveProgram = cfAppsByProgram.containsKey(programId);
      if (bothHaveProgram) {
        // Merge the applications.
        CfUserApps cfUserApps = cfAppsByProgram.get(programId);
        var log =
            reconcileAppsForProgram(
                cfUser, guestUser, cfUserApps, guestUserApps, programId, applyChanges);
        logMessageMerge.append(log);
      } else {
        // Just move over the applications.
        StringJoiner programIds = new StringJoiner(", ");
        for (ApplicationModel app : guestApps) {
          programIds.add(app.id.toString());
          if (applyChanges) {
            app.setOriginalApplicantId(guestUser.id);
            app.setApplicant(cfUser);
            app.save();
          }
        }
        // We'll add a header to these later.
        logMessageMove.append(
            """
              * Program ID %d: Application ID %s
            """
                .formatted(programId, programIds.toString()));
      }
    }

    // Build the complete log message.
    StringJoiner finalLogMessage = new StringJoiner("\n");
    finalLogMessage.add(
        """
        Merging CiviForm User and Guest User:
          CiviForm:
            Account  : id %d
            Applicant: id %d creation: %s
          Guest
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
          CF User does not have the following programs, moving all applications from Guest to CF User.
          %s
          """
              .formatted(logMessageMove));
    }
    if (!logMessageMerge.isEmpty()) {
      finalLogMessage.add(logMessageMerge);
    }

    return finalLogMessage.toString();
  }

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

  private CfUserApps categorizeCfApps(List<ApplicationModel> apps) {
    Optional<ApplicationModel> active =
        apps.stream().filter(a -> a.getLifecycleStage() == LifecycleStage.ACTIVE).findFirst();
    Optional<ApplicationModel> draft =
        apps.stream().filter(a -> a.getLifecycleStage() == LifecycleStage.DRAFT).findFirst();
    return new CfUserApps(active, draft);
  }

  /**
   * Reconcile applications between the CiviForm User and the Guest User.
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
    // * Actives over Drafts.
    // * The logged in CiviForm User's data over the Guest's.
    // * Newer over older.
    //
    // See the comment on mergeGuestApplicationIntoCFUser for more
    // details.
    //
    // For ease of readability, the code will handle Application states
    // from earlier to later in the lifecycle: Obsolete, Active/Active, Draft

    boolean cfHasActive = cfUserApps.active().isPresent();
    boolean cfHasDraft = cfUserApps.draft().isPresent();
    boolean guestHasActive = guestUserApps.active().isPresent();
    boolean guestHasDraft = guestUserApps.draft().isPresent();
    // Both users must have at least a Draft/Active for this method to be
    // useful. The application doesn't allow for Obsolete without an Active.
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
            Moving Obsolete Application id %s to CiviForm Applicant %s

          """
              .formatted(obsoleteIds, cfUser.id));
    }

    // Reconcile the Active and Draft applications.
    final String log;
    if (cfHasActive) {
      if (guestHasActive) {
        // Handle all cases where both have Actives. Either may have a draft too.
        log = reconcileBothWithActives(cfUser, guestUser, cfUserApps, guestUserApps, applyChanges);
      } else {
        // Handle CF user having an Active, Guest does not, but it implicitly
        // has a Draft.
        log = reconcileCfActiveGuestNoActive(cfUserApps, guestUserApps, applyChanges);
      }
    } else if (guestHasActive) {
      // Guest has active, CF does not, but it implicitly has a Draft.
      log =
          reconcileCfNoActiveGuestActive(
              cfUser, guestUser, cfUserApps, guestUserApps, applyChanges);
    }
    // Both only have a draft.
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
   * Reconciles when both users have an Active Application.
   *
   * <p>Keeps whichever Active is newest along with its Draft; if present. The other Active is
   * Obsoleted and its draft deleted.
   *
   * <p>The system has an invariant that an Active application shouldn't be older than an Obsolete
   * one, so we must keep the newer Active.
   *
   * @param applyChanges if database changes should be applied. If off the return will log what
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
            CiviForm User and Guest both have a Active application.
              * CF User:
                * Application id %d
                * Submitted: %s
              * Guest User:
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
            * Keeping CF User application
            * Obsoleting Guest Active id %d
          """
              .formatted(guestActive.id));
      if (applyChanges) {
        guestActive
            .setLifecycleStage(LifecycleStage.OBSOLETE)
            .setOriginalApplicantId(guestUser.id)
            .setApplicant(cfUser);
        guestActive.save();
      }

      if (guestUserApps.draft().isPresent()) {
        var guestDraft = guestUserApps.draft().get();
        logMessage.append("  * Deleting Guest Draft id %d".formatted(guestDraft.id));
        if (applyChanges) {
          guestDraft.delete();
        }
      }
      return logMessage.toString();
    }

    // Guest's active is Newer.
    logMessage.append(
        """
          * Keeping Guest User application
          * Obsoleting CF User Active id %d
        """
            .formatted(cfActive.id));
    // Obsolete CF app.
    if (applyChanges) {
      cfActive.setLifecycleStage(LifecycleStage.OBSOLETE);
      cfActive.save();
    }

    logMessage.append(
        """
          * Moving Guest Active id %d to CFUser Account id %d
        """
            .formatted(guestActive.id, cfUser.getAccount().id));
    // Move the guest app to the cfUser
    if (applyChanges) {
      guestActive.setOriginalApplicantId(guestUser.id);
      guestActive.setApplicant(cfUser);
      guestActive.save();
    }

    // Delete the CF draft if it exists.
    if (cfUserApps.draft().isPresent()) {
      var cfDraft = cfUserApps.draft().get();
      logMessage.append("  * Deleting CF Draft id %d".formatted(cfDraft.id));
      if (applyChanges) {
        cfDraft.delete();
      }
    }

    // Move the guest draft if it exists.
    if (guestUserApps.draft.isPresent()) {
      var guestDraft = guestUserApps.draft().get();
      logMessage.append(
          """
            * Moving Guest Draft id %d to CFUser Account id %d
          """
              .formatted(guestDraft.id, cfUser.getAccount().id));
      if (applyChanges) {
        guestDraft.setApplicant(cfUser);
        guestDraft.save();
      }
    }

    return logMessage.toString();
  }

  /**
   * Reconciles when the CF User has an Active application and the Guest does not, but implicitly
   * has a Draft one.
   *
   * @param applyChanges if database changes should be applied. If off the return will log what
   *     would have occurred.
   * @return a log message indicating what changes occurred.
   */
  private String reconcileCfActiveGuestNoActive(
      CfUserApps cfUserApps, GuestUserApps guestUserApps, boolean applyChanges) {

    // Keep the CF User data as is, delete the Guest Draft.
    if (applyChanges) {
      // delete the entry
      guestUserApps.draft().orElseThrow().delete();
    }

    return """
    CF User has an Active application, Guest does not.
      * Keeping CF User application id %d
      * Deleting Guest Draft application id %d
    """
        .formatted(cfUserApps.active().orElseThrow().id, guestUserApps.draft.orElseThrow().id);
  }

  /**
   * Reconciles when the CF user does not have an Active application and implicitly has a Draft, and
   * the Guest User has an Active application.
   *
   * @param applyChanges if database changes should be applied. If off the return will log what
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
        new StringBuilder("CF User does not have a Active Application, Guest does.");

    logMessage.append(
        """
          * Deleting CF Draft application id %d
        """
            .formatted(cfUserApps.draft.orElseThrow().id));
    if (applyChanges) {
      // There must be a draft since there is no active.
      cfUserApps.draft().orElseThrow().delete();
    }

    var guestActive = guestUserApps.active().orElseThrow();
    logMessage.append(
        """
          * Moving Guest Active application id %d to CiviForm applicant id %d
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
            * Moving Guest Draft application id %d to CiviForm Applicant id %d
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
   * Reconciles when both only have a Draft.
   *
   * @param applyChanges if database changes should be applied. If off the return will log what
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
    CiviForm User and Guest both only have a Draft Application.
      * Deleting CiviForm Draft application id %d
      * Moving Guest Draft application id %d
    """
        .formatted(cfDraft.id, guestDraft.id);
  }
}
