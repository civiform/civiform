package controllers;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import play.api.mvc.Call;

/**
 * Class that computes routes for applicant actions. The route for an applicant may be different
 * from that for a TI taking action on behalf of an applicant.
 */
public final class ApplicantRoutes {

  // There are two cases where we want to use the URL that contains the applicant id:
  // - TIs performing actions on behalf of applicants.
  // - The applicant has a profile that does not include the applicant id.
  //   This case will eventually go away once existing profiles have expired and been replaced.
  private static boolean includeApplicantIdInRoute(CiviFormProfile profile) {
    boolean isTi = profile.isTrustedIntermediary();
    boolean applicantIdInProfile =
        profile.getProfileData().containsAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);

    return isTi || !applicantIdInProfile;
  }

  /**
   * Returns the URL corresponding to the applicant index action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @return Route for the index action.
   */
  public static Call index(CiviFormProfile profile, long applicantId) {
    if (includeApplicantIdInRoute(profile)) {
      return controllers.applicant.routes.ApplicantProgramsController.indexWithApplicantId(
          applicantId);
    } else {
      return controllers.applicant.routes.ApplicantProgramsController.index();
    }
  }
}
