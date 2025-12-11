package controllers.applicant;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import play.api.mvc.Call;

/**
 * Class that computes routes for applicant actions.
 *
 * <p>Routes for TIs and CiviForm Admins previewing programs will differ from Applicants, they will
 * contain the applicants ID in the route.
 *
 * <p>Applicants store their ID in their profile (which is not managed here).
 */
public final class ApplicantRoutes {
  // There are two cases where we want to use the URL that contains the applicant id:
  // - TIs performing actions on behalf of applicants.
  // - CiviForm Admins previewing a program.
  private boolean includeApplicantIdInRoute(CiviFormProfile profile) {
    return profile.isTrustedIntermediary() || profile.isCiviFormAdmin();
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
    return routes.ApplicantProgramsController.edit(
        Long.toString(programId), /* isFromUrlCall= */ false);
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
          applicantId, Long.toString(programId), /* isFromUrlCall= */ false);
    } else {
      return edit(programId);
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
    String programIdStr = Long.toString(programId);
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramReviewController.reviewWithApplicantId(
          applicantId, programIdStr, /* isFromUrlCall= */ false);
    }
    return routes.ApplicantProgramReviewController.review(programIdStr, /* isFromUrlCall= */ false);
  }

  /**
   * Returns the route corresponding to the applicant review action. Used when there is no
   * account/applicant created yet when browsing the home page.
   *
   * @param programId - ID of the program to review
   * @return Route for the applicant review action
   */
  public Call review(long programId) {
    return routes.ApplicantProgramReviewController.review(
        Long.toString(programId), /* isFromUrlCall= */ false);
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
    String programIdStr = Long.toString(programId);
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.editWithApplicantId(
          applicantId, programIdStr, blockId, questionName, /* isFromUrlCall= */ false);
    }
    return routes.ApplicantProgramBlocksController.edit(
        programIdStr, blockId, questionName, /* isFromUrlCall= */ false);
  }

  /**
   * Returns the route corresponding to the applicant block edit action without an applicant ID.
   *
   * @param programId - ID of program to edit
   * @return Route for the applicant block edit action
   */
  public Call blockEdit(long programId) {
    return routes.ApplicantProgramBlocksController.edit(
        Long.toString(programId),
        /* blockId= */ "1",
        /* questionName= */ Optional.empty(),
        /* isFromUrlCall= */ false);
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
    String programIdStr = Long.toString(programId);
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.reviewWithApplicantId(
          applicantId, programIdStr, blockId, questionName, /* isFromUrlCall= */ false);
    }
    return routes.ApplicantProgramBlocksController.review(
        programIdStr, blockId, questionName, /* isFromUrlCall= */ false);
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
    String programIdStr = Long.toString(programId);
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.previousWithApplicantId(
          applicantId, programIdStr, previousBlockIndex, inReview, /* isFromUrlCall= */ false);
    }
    return routes.ApplicantProgramBlocksController.previous(
        programIdStr, previousBlockIndex, inReview, /* isFromUrlCall= */ false);
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
    String programIdStr = Long.toString(programId);
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.addFileWithApplicantId(
          applicantId, programIdStr, blockId, inReview, /* isFromUrlCall= */ false);
    }
    return routes.ApplicantProgramBlocksController.addFile(
        programIdStr, blockId, inReview, /* isFromUrlCall= */ false);
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
    String programIdStr = Long.toString(programId);
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.removeFileWithApplicantId(
          applicantId, programIdStr, blockId, fileKey, inReview, /* isFromUrlCall= */ false);
    }
    return routes.ApplicantProgramBlocksController.removeFile(
        programIdStr, blockId, fileKey, inReview, /* isFromUrlCall= */ false);
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
    String programIdStr = Long.toString(programId);
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
          applicantId,
          programIdStr,
          blockId,
          inReview,
          new ApplicantRequestedActionWrapper(applicantRequestedAction),
          /* isFromUrlCall= */ false);
    }
    return routes.ApplicantProgramBlocksController.updateFile(
        programIdStr,
        blockId,
        inReview,
        new ApplicantRequestedActionWrapper(applicantRequestedAction),
        /* isFromUrlCall= */ false);
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
