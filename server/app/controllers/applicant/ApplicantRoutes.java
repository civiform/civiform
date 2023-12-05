package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import com.google.inject.Inject;
import io.prometheus.client.Counter;
import play.api.mvc.Call;
import services.settings.SettingsManifest;

/**
 * Class that computes routes for applicant actions. The route for an applicant may be different
 * from that for a TI taking action on behalf of an applicant.
 */
public final class ApplicantRoutes {
  private final SettingsManifest settingsManifest;

  @Inject
  public ApplicantRoutes(SettingsManifest settingsManifest) {
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  private static final Counter APPLICANT_ID_IN_PROFILE_COUNT =
      Counter.build()
          .name("applicant_id_in_profile")
          .help("Count of profiles that contain applicant id")
          .labelNames("existence")
          .register();

  // There are three cases where we want to use the URL that contains the applicant id:
  // - The new schema is /not/ active yet.
  // - TIs performing actions on behalf of applicants.
  // - The applicant has a profile that does /not/ (yet) include the applicant id.
  //   This case will eventually go away once existing profiles have expired and been replaced.
  private boolean includeApplicantIdInRoute(CiviFormProfile profile) {
    boolean newUrlSchemaEnabled = settingsManifest.getNewApplicantUrlSchemaEnabled();
    boolean isTi = profile.isTrustedIntermediary();
    boolean applicantIdInProfile =
        profile.getProfileData().containsAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);

    // Count the occurrences so we know when it is safe to remove the special-case code for
    // migration.
    String existence = applicantIdInProfile ? "present" : "absent";
    APPLICANT_ID_IN_PROFILE_COUNT.labels(existence).inc();

    return !newUrlSchemaEnabled || isTi || !applicantIdInProfile;
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
          applicantId);
    } else {
      return controllers.applicant.routes.ApplicantProgramsController.index();
    }
  }

  /**
   * Returns the route corresponding to the applicant show action.
   *
   * @param profile - Profile corresponding to the logged-in user (applicant or TI).
   * @param applicantId - ID of applicant for whom the action should be performed.
   * @param programId - ID of program to view
   * @return Route for the program view action
   */
  public Call show(CiviFormProfile profile, long applicantId, long programId) {
    if (includeApplicantIdInRoute(profile)) {
      return controllers.applicant.routes.ApplicantProgramsController.showWithApplicantId(
          applicantId, programId);
    } else {
      // Since this controller handles two different actions depending on whether it has an integer
      // id or an alphanum slug, we must pass the parameter as the more general type.
      return controllers.applicant.routes.ApplicantProgramsController.show(
          String.valueOf(programId));
    }
  }
}
