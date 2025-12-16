package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import repository.VersionRepository;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantPersonalInfo.Representation;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import views.admin.programs.ProgramCardPreview;

/** Controller for rendering a program card preview. */
public final class ProgramCardPreviewController extends CiviFormController {
  private final ProgramCardPreview programCardPreview;
  private final Messages messages;
  private final ProgramService programService;

  @Inject
  public ProgramCardPreviewController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ProgramCardPreview programCardPreview,
      MessagesApi messagesApi,
      ProgramService programService) {
    super(profileUtils, versionRepository);
    this.programCardPreview = checkNotNull(programCardPreview);
    this.messages = messagesApi.preferred(ImmutableList.of(Lang.defaultLang()));
    this.programService = checkNotNull(programService);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public String cardPreview(Http.Request request, long programId)
      throws InterruptedException, ExecutionException {

    Representation representation = Representation.builder().build();
    ApplicantPersonalInfo api = ApplicantPersonalInfo.ofGuestUser(representation);
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    ProgramDefinition programDefinition =
        programService.getFullProgramDefinitionAsync(programId).toCompletableFuture().get();
    ApplicantProgramData apd = ApplicantProgramData.builder(programDefinition).build();

    ProgramCardPreview.Params params =
        ProgramCardPreview.Params.builder()
            .setRequest(request)
            .setApplicantPersonalInfo(api)
            .setApplicantProgramData(apd)
            .setProfile(profile)
            .setMessages(messages)
            .build();

    return programCardPreview.render(params);
  }
}
