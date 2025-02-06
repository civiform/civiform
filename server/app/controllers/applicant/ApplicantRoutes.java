package controllers.applicant;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import com.google.common.collect.ImmutableList;
import io.prometheus.client.Counter;
import java.util.Optional;
import play.api.mvc.Call;

/**
 * Class that computes routes for applicant actions. The route for an applicant may be different
 * from that for a TI taking action on behalf of an applicant.
 */
public final class ApplicantRoutes {
  private static final Counter APPLICANT_ID_IN_PROFILE_COUNT =
      Counter.build()
          .name("applicant_id_in_profile")
          .help("Count of profiles that contain applicant id")
          .labelNames("existence")
          .register();

  // There are two cases where we want to use the URL that contains the applicant id:
  // - TIs performing actions on behalf of applicants.
  // - The applicant has a profile that does /not/ (yet) include the applicant id.
  //   This case will eventually go away once existing profiles have expired and been replaced.
  private boolean includeApplicantIdInRoute(CiviFormProfile profile) {
    boolean isTi = profile.isTrustedIntermediary();
    boolean applicantIdInProfile =
        profile.getProfileData().containsAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);

    // Count the occurrences so we know when it is safe to remove the special-case code for
    // migration.
    String existence = applicantIdInProfile ? "present" : "absent";
    APPLICANT_ID_IN_PROFILE_COUNT.labels(existence).inc();

    return isTi || !applicantIdInProfile;
  }

  /**
   * Returns the route corresponding to the applicant index action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @return Route for the index action.
   */
  public Call index(CiviFormProfile profile, long applicantId) {
    if (includeApplicantIdInRoute(profile)) {
      return controllers.applicant.routes.ApplicantProgramsController.indexWithApplicantId(
          applicantId, /* categories= */ ImmutableList.of());
    } else {
      return controllers.applicant.routes.ApplicantProgramsController.index(ImmutableList.of());
    }
  }

  /**
   * Returns the route corresponding to the applicant show action. Used when there is no
   * account/applicant created yet when browsing the home page.
   *
   * @param programId - ID of the program to view
   * @return Route for the program view action
   */
  public Call show(long programId) {
    return controllers.applicant.routes.ApplicantProgramsController.show(String.valueOf(programId));
  }

  /**
   * Returns the route corresponding to the applicant show action. In the North Star UI, this
   * returns the program overview page
   *
   * @param programSlug - slug of the program to view
   * @return Route for the program view action
   */
  public Call show(String programSlug) {
    return controllers.applicant.routes.ApplicantProgramsController.show(programSlug);
  }

  /**
   * Returns the route corresponding to the applicant show action. In the North Star UI, this
   * returns the program overview page
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programSlug - slug of the program to view
   * @return Route for the program view action
   */
  public Call show(CiviFormProfile profile, long applicantId, String programSlug) {
    if (includeApplicantIdInRoute(profile)) {
      return controllers.applicant.routes.ApplicantProgramsController.showWithApplicantId(
          applicantId, programSlug);
    } else {
      return routes.ApplicantProgramsController.show(programSlug);
    }
  }

  public Call edit(long programId) {
    return routes.ApplicantProgramsController.edit(programId);
  }

  /**
   * Returns the route corresponding to the applicant edit action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to edit
   * @return Route for the applicant edit action
   */
  public Call edit(CiviFormProfile profile, long applicantId, long programId) {
    if (includeApplicantIdInRoute(profile)) {
      return controllers.applicant.routes.ApplicantProgramsController.editWithApplicantId(
          applicantId, programId);
    } else {
      return routes.ApplicantProgramsController.edit(programId);
    }
  }

  /**
   * Returns the route corresponding to the applicant review action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to review
   * @return Route for the applicant review action
   */
  public Call review(CiviFormProfile profile, long applicantId, long programId) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramReviewController.reviewWithApplicantId(applicantId, programId);
    } else {
      return routes.ApplicantProgramReviewController.review(programId);
    }
  }

  /**
   * Returns the route corresponding to the applicant review action. Used when there is no
   * account/applicant created yet when browsing the home page.
   *
   * @param programId - ID of the program to review
   * @return Route for the applicant review action
   */
  public Call review(long programId) {
    return routes.ApplicantProgramReviewController.review(programId);
  }

  /**
   * Returns the route corresponding to the applicant submit action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to review
   * @return Route for the applicant submit action
   */
  public Call submit(CiviFormProfile profile, long applicantId, long programId) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramReviewController.submitWithApplicantId(applicantId, programId);
    } else {
      return routes.ApplicantProgramReviewController.submit(programId);
    }
  }

  /**
   * Returns the route corresponding to the applicant block edit action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to review
   * @param blockId - ID of the block to edit
   * @param questionName - Name of question being edited, if applicable
   * @return Route for the applicant block edit action
   */
  public Call blockEdit(
      CiviFormProfile profile,
      long applicantId,
      long programId,
      String blockId,
      Optional<String> questionName) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.editWithApplicantId(
          applicantId, programId, blockId, questionName);
    } else {
      return routes.ApplicantProgramBlocksController.edit(programId, blockId, questionName);
    }
  }

  /**
   * Returns the route corresponding to the applicant block edit action without an applicant ID.
   *
   * @param programId - ID of program to edit
   * @return Route for the applicant block edit action
   */
  public Call blockEdit(long programId) {
    return routes.ApplicantProgramBlocksController.edit(programId, "1", Optional.empty());
  }

  /**
   * Returns the route corresponding to the applicant block review action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to review
   * @param blockId - ID of block to review
   * @param questionName - Name of the question being reviewed, if applicable.
   * @return Route for the applicant block review action
   */
  public Call blockReview(
      CiviFormProfile profile,
      long applicantId,
      long programId,
      String blockId,
      Optional<String> questionName) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.reviewWithApplicantId(
          applicantId, programId, blockId, questionName);
    } else {
      return routes.ApplicantProgramBlocksController.review(programId, blockId, questionName);
    }
  }

  /**
   * Returns the route to the block specified by {@code blockId}.
   *
   * @param inReview true if the applicant is reviewing their application answers and false if
   *     they're filling out the application step-by-step. See {@link #edit} and {@link #review} for
   *     more details.
   */
  public Call blockEditOrBlockReview(
      CiviFormProfile profile, long applicantId, long programId, String blockId, boolean inReview) {
    if (inReview) {
      return blockReview(
          profile, applicantId, programId, blockId, /* questionName= */ Optional.empty());
    } else {
      return blockEdit(
          profile, applicantId, programId, blockId, /* questionName= */ Optional.empty());
    }
  }

  /**
   * Returns the route corresponding to the applicant confirm address action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to review
   * @param blockId - ID of the block containing the address
   * @param inReview - true if executing the review action (as opposed to edit)
   * @param applicantRequestedAction - the page the applicant would like to see after the updates
   *     are made
   * @return Route for the applicant confirm address action
   */
  public Call confirmAddress(
      CiviFormProfile profile,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ApplicantRequestedAction applicantRequestedAction) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
          applicantId,
          programId,
          blockId,
          inReview,
          new ApplicantRequestedActionWrapper(applicantRequestedAction));
    } else {
      return routes.ApplicantProgramBlocksController.confirmAddress(
          programId,
          blockId,
          inReview,
          new ApplicantRequestedActionWrapper(applicantRequestedAction));
    }
  }

  /**
   * Returns the route corresponding to the applicant previous block action, or the route
   * corresponding to the review page if there's no valid previous block.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to review
   * @param currentBlockIndex - index of the current block
   * @param inReview - true if executing the review action (as opposed to edit)
   * @return Route for the applicant previous block action
   */
  public Call blockPreviousOrReview(
      CiviFormProfile profile,
      long applicantId,
      long programId,
      int currentBlockIndex,
      boolean inReview) {
    int previousBlockIndex = currentBlockIndex - 1;
    if (previousBlockIndex >= 0) {
      return blockPrevious(profile, applicantId, programId, previousBlockIndex, inReview);
    } else {
      return review(profile, applicantId, programId);
    }
  }

  private Call blockPrevious(
      CiviFormProfile profile,
      long applicantId,
      long programId,
      int previousBlockIndex,
      boolean inReview) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.previousWithApplicantId(
          applicantId, programId, previousBlockIndex, inReview);
    } else {
      return routes.ApplicantProgramBlocksController.previous(
          programId, previousBlockIndex, inReview);
    }
  }

  /**
   * Returns the route corresponding to the applicant add file action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to review
   * @param blockId - ID of the block containing file upload question
   * @param inReview - true if executing the review action (as opposed to edit)
   * @return Route for the applicant update file action
   */
  public Call addFile(
      CiviFormProfile profile, long applicantId, long programId, String blockId, boolean inReview) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.addFileWithApplicantId(
          applicantId, programId, blockId, inReview);
    } else {
      return routes.ApplicantProgramBlocksController.addFile(programId, blockId, inReview);
    }
  }

  /**
   * Returns the route corresponding to the applicant remove file action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to review
   * @param blockId - ID of the block containing file upload question
   * @param fileKey - The key for the stored file.
   * @return Route for the applicant update file action
   */
  public Call removeFile(
      CiviFormProfile profile,
      long applicantId,
      long programId,
      String blockId,
      String fileKey,
      boolean inReview) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.removeFileWithApplicantId(
          applicantId, programId, blockId, fileKey, inReview);
    } else {
      return routes.ApplicantProgramBlocksController.removeFile(
          programId, blockId, fileKey, inReview);
    }
  }

  /**
   * Returns the route corresponding to the applicant update file action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to review
   * @param blockId - ID of the block containing file upload question
   * @param inReview - true if executing the review action (as opposed to edit)
   * @param applicantRequestedAction - the page the applicant would like to see after the updates
   *     are made
   * @return Route for the applicant update file action
   */
  public Call updateFile(
      CiviFormProfile profile,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ApplicantRequestedAction applicantRequestedAction) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
          applicantId,
          programId,
          blockId,
          inReview,
          new ApplicantRequestedActionWrapper(applicantRequestedAction));
    } else {
      return routes.ApplicantProgramBlocksController.updateFile(
          programId,
          blockId,
          inReview,
          new ApplicantRequestedActionWrapper(applicantRequestedAction));
    }
  }

  /**
   * Returns the route corresponding to the applicant update block action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to review
   * @param blockId - ID of the block to be updated
   * @param inReview - true if executing the review action (as opposed to edit)
   * @param applicantRequestedAction - the page the applicant would like to see after the updates
   *     are made
   * @return Route for the applicant update block action
   */
  public Call updateBlock(
      CiviFormProfile profile,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ApplicantRequestedAction applicantRequestedAction) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.updateWithApplicantId(
          applicantId,
          programId,
          blockId,
          inReview,
          new ApplicantRequestedActionWrapper(applicantRequestedAction));
    } else {
      return routes.ApplicantProgramBlocksController.update(
          programId,
          blockId,
          inReview,
          new ApplicantRequestedActionWrapper(applicantRequestedAction));
    }
  }
}
