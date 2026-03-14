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
   * Returns the program overview page
   *
   * @param programSlug - slug of the program to view
   * @return Route for the program view action
   */
  public Call show(String programSlug) {
    return controllers.applicant.routes.ApplicantProgramsController.show(programSlug);
  }

  /**
   * Returns the program overview page
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

  // TODO:#11090 Remove method when routes are no longer hit
  public Call edit(long programId) {
    return routes.ApplicantProgramsController.edit(Long.toString(programId));
  }

  public Call edit(String programSlug) {
    return routes.ApplicantProgramsController.edit(programSlug);
  }

  // TODO:#11090 Remove method when routes are no longer hit
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
          applicantId, Long.toString(programId));
    } else {
      return edit(programId);
    }
  }

  /**
   * Returns the route corresponding to the applicant edit action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programSlug - slug of program to edit
   * @return Route for the applicant edit action
   */
  public Call edit(CiviFormProfile profile, long applicantId, String programSlug) {
    if (includeApplicantIdInRoute(profile)) {
      return controllers.applicant.routes.ApplicantProgramsController.editWithApplicantId(
          applicantId, programSlug);
    } else {
      return edit(programSlug);
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
          applicantId, programIdStr);
    }
    return routes.ApplicantProgramReviewController.review(programIdStr);
  }

  /**
   * Returns the route corresponding to the applicant review action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programSlug - slug of program to review
   * @return Route for the applicant review action
   */
  public Call review(CiviFormProfile profile, long applicantId, String programSlug) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramReviewController.reviewWithApplicantId(
          applicantId, programSlug);
    }
    return routes.ApplicantProgramReviewController.review(programSlug);
  }

  /**
   * Returns the route corresponding to the applicant review action. Used when there is no
   * account/applicant created yet when browsing the home page.
   *
   * @param programId - ID of the program to review
   * @return Route for the applicant review action
   */
  public Call review(long programId) {
    return routes.ApplicantProgramReviewController.review(Long.toString(programId));
  }

  /**
   * Returns the route corresponding to the applicant review action. Used when there is no
   * account/applicant created yet when browsing the home page.
   *
   * @param programSlug - slug of the program to review
   * @return Route for the applicant review action
   */
  public Call review(String programSlug) {
    return routes.ApplicantProgramReviewController.review(programSlug);
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

  //   /**
  //  * Returns the route corresponding to the applicant submit action.
  //  *
  //  * @param profile - Profile corresponding to the logged-in user (applicant or TI).
  //  * @param applicantId - ID of applicant for whom the action should be performed.
  //  * @param programId - ID of program to review
  //  * @return Route for the applicant submit action
  //  */
  //   public Call submit(CiviFormProfile profile, long applicantId, String programSlug) {
  //     if (includeApplicantIdInRoute(profile)) {
  //       return routes.ApplicantProgramReviewController.submitWithApplicantId(applicantId,
  // programSlug);
  //     } else {
  //       return routes.ApplicantProgramReviewController.submit(programSlug);
  //     }
  //   }

  /**
   * Returns the route corresponding to the applicant block edit action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to edit
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
          applicantId, programIdStr, blockId, questionName);
    }
    return routes.ApplicantProgramBlocksController.edit(programIdStr, blockId, questionName);
  }

  /**
   * Returns the route corresponding to the applicant block edit action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programSlug - slug of program to edit
   * @param blockId - ID of the block to edit
   * @param questionName - Name of question being edited, if applicable
   * @return Route for the applicant block edit action
   */
  public Call blockEdit(
      CiviFormProfile profile,
      long applicantId,
      String programSlug,
      String blockId,
      Optional<String> questionName) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.editWithApplicantId(
          applicantId, programSlug, blockId, questionName);
    }
    return routes.ApplicantProgramBlocksController.edit(programSlug, blockId, questionName);
  }

  /**
   * Returns the route corresponding to the applicant block edit action without an applicant ID.
   *
   * @param programId - ID of program to edit
   * @return Route for the applicant block edit action
   */
  public Call blockEdit(long programId) {
    return routes.ApplicantProgramBlocksController.edit(
        Long.toString(programId), /* blockId= */ "1", /* questionName= */ Optional.empty());
  }

  /**
   * Returns the route corresponding to the applicant block edit action without an applicant ID.
   *
   * @param programSlug - slug of program to edit
   * @return Route for the applicant block edit action
   */
  public Call blockEdit(String programSlug) {
    return routes.ApplicantProgramBlocksController.edit(
        programSlug, /* blockId= */ "1", /* questionName= */ Optional.empty());
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
          applicantId, programIdStr, blockId, questionName);
    }
    return routes.ApplicantProgramBlocksController.review(programIdStr, blockId, questionName);
  }

  /**
   * Returns the route corresponding to the applicant block review action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programSlug - slug of program to review
   * @param blockId - ID of block to review
   * @param questionName - Name of the question being reviewed, if applicable.
   * @return Route for the applicant block review action
   */
  public Call blockReview(
      CiviFormProfile profile,
      long applicantId,
      String programSlug,
      String blockId,
      Optional<String> questionName) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.reviewWithApplicantId(
          applicantId, programSlug, blockId, questionName);
    }
    return routes.ApplicantProgramBlocksController.review(programSlug, blockId, questionName);
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
   * Returns the route to the block specified by {@code blockId}.
   *
   * @param inReview true if the applicant is reviewing their application answers and false if
   *     they're filling out the application step-by-step. See {@link #edit} and {@link #review} for
   *     more details.
   */
  public Call blockEditOrBlockReview(
      CiviFormProfile profile,
      long applicantId,
      String programSlug,
      String blockId,
      boolean inReview) {
    if (inReview) {
      return blockReview(
          profile, applicantId, programSlug, blockId, /* questionName= */ Optional.empty());
    } else {
      return blockEdit(
          profile, applicantId, programSlug, blockId, /* questionName= */ Optional.empty());
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

  /**
   * Returns the route corresponding to the applicant previous block action, or the route
   * corresponding to the review page if there's no valid previous block.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programSlug - slug of program to review
   * @param currentBlockIndex - index of the current block
   * @param inReview - true if executing the review action (as opposed to edit)
   * @return Route for the applicant previous block action
   */
  public Call blockPreviousOrReview(
      CiviFormProfile profile,
      long applicantId,
      String programSlug,
      int currentBlockIndex,
      boolean inReview) {
    int previousBlockIndex = currentBlockIndex - 1;
    if (previousBlockIndex >= 0) {
      return blockPrevious(profile, applicantId, programSlug, previousBlockIndex, inReview);
    } else {
      return review(profile, applicantId, programSlug);
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
          applicantId, programIdStr, previousBlockIndex, inReview);
    }
    return routes.ApplicantProgramBlocksController.previous(
        programIdStr, previousBlockIndex, inReview);
  }

  private Call blockPrevious(
      CiviFormProfile profile,
      long applicantId,
      String programSlug,
      int previousBlockIndex,
      boolean inReview) {
    if (includeApplicantIdInRoute(profile)) {
      return routes.ApplicantProgramBlocksController.previousWithApplicantId(
          applicantId, programSlug, previousBlockIndex, inReview);
    }
    return routes.ApplicantProgramBlocksController.previous(
        programSlug, previousBlockIndex, inReview);
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
          applicantId, programIdStr, blockId, inReview);
    }
    return routes.ApplicantProgramBlocksController.addFile(programIdStr, blockId, inReview);
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
          applicantId, programIdStr, blockId, fileKey, inReview);
    }
    return routes.ApplicantProgramBlocksController.removeFile(
        programIdStr, blockId, fileKey, inReview);
  }

  /**
   * Returns the route corresponding to the applicant update block action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to update
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
