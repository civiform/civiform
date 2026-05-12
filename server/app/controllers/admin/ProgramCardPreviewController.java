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
import views.admin.programs.ProgramCardPreview.Params;
import views.applicant.programindex.ProgramCardsSectionParamsFactory.ProgramCardParams;

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
  public ProgramCardParams programCardPreviewParams(Http.Request request, long programId)
      throws InterruptedException, ExecutionException {
    return programCardPreview.buildCard(buildPreviewParams(request, programId));
  }

  /**
   * Same as {@link #programCardPreviewParams(Http.Request, long)} but uses an already-loaded
   * program definition so callers do not trigger a second program fetch.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public ProgramCardParams programCardPreviewParams(
      Http.Request request, ProgramDefinition programDefinition) {
    return programCardPreview.buildCard(buildPreviewParams(request, programDefinition));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public String cardPreview(Http.Request request, long programId)
      throws InterruptedException, ExecutionException {
    return programCardPreview.render(buildPreviewParams(request, programId));
  }

  /**
   * Same as {@link #cardPreview(Http.Request, long)} but uses an already-loaded program definition.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public String cardPreview(Http.Request request, ProgramDefinition programDefinition) {
    return programCardPreview.render(buildPreviewParams(request, programDefinition));
  }

  private Params buildPreviewParams(Http.Request request, long programId)
      throws InterruptedException, ExecutionException {
    ProgramDefinition programDefinition =
        programService.getFullProgramDefinitionAsync(programId).toCompletableFuture().get();
    return buildPreviewParams(request, programDefinition);
  }

  private Params buildPreviewParams(Http.Request request, ProgramDefinition programDefinition) {
    Representation representation = Representation.builder().build();
    ApplicantPersonalInfo api = ApplicantPersonalInfo.ofGuestUser(representation);
    CiviFormProfile profile = profileUtils.currentUserProfile(request);
    ApplicantProgramData apd = ApplicantProgramData.builder(programDefinition).build();

    return ProgramCardPreview.Params.builder()
        .setRequest(request)
        .setApplicantPersonalInfo(api)
        .setApplicantProgramData(apd)
        .setProfile(profile)
        .setMessages(messages)
        .build();
  }
}
