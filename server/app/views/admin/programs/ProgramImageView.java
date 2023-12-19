package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import forms.admin.ProgramImageDescriptionForm;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LiTag;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.applicant.ProgramCardViewRenderer;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.ToastMessage;
import views.fileupload.FileUploadViewStrategy;

/** A view for admins to update the image associated with a particular program. */
public final class ProgramImageView extends BaseHtmlView {
  // TODO(#5676): Should we prohibit gifs?
  private static final String MIME_TYPES_IMAGES = "image/*";
  private static final String IMAGE_DESCRIPTION_FORM_ID = "image-description-form";
  private static final String IMAGE_FILE_UPLOAD_FORM_ID = "image-file-upload-form";

  private final AdminLayout layout;
  private final String baseUrl;
  private final FormFactory formFactory;
  private final FileUploadViewStrategy fileUploadViewStrategy;
  private final MessagesApi messagesApi;
  private final ProfileUtils profileUtils;
  private final ProgramCardViewRenderer programCardViewRenderer;
  private final PublicStorageClient publicStorageClient;
  private final ZoneId zoneId;

  @Inject
  public ProgramImageView(
      AdminLayoutFactory layoutFactory,
      Config config,
      FormFactory formFactory,
      FileUploadViewStrategy fileUploadViewStrategy,
      MessagesApi messagesApi,
      ProgramCardViewRenderer programCardViewRenderer,
      ProfileUtils profileUtils,
      PublicStorageClient publicStorageClient,
      ZoneId zoneId) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.formFactory = checkNotNull(formFactory);
    this.fileUploadViewStrategy = checkNotNull(fileUploadViewStrategy);
    this.messagesApi = checkNotNull(messagesApi);
    this.profileUtils = checkNotNull(profileUtils);
    this.programCardViewRenderer = checkNotNull(programCardViewRenderer);
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.zoneId = checkNotNull(zoneId);
  }

  /**
   * Renders the image currently associated with the program and a form to add / edit / delete the
   * image (and its alt text).
   */
  public Content render(Http.Request request, ProgramDefinition programDefinition) {
    String title =
        String.format(
            "Manage program image for %s", programDefinition.localizedName().getDefault());

    DivTag mainContent =
        div()
            .withClasses("my-10", "mx-20")
            .with(renderHeader(title))
            .with(createImageDescriptionForm(request, programDefinition));

    DivTag imageUploadAndCurrentCardContainer =
        div()
            .withClasses("grid", "grid-cols-2", "w-full")
            .with(createImageUploadForm(programDefinition))
            .with(renderCurrentProgramCard(request, programDefinition));
    mainContent.with(imageUploadAndCurrentCardContainer);

    HtmlBundle htmlBundle = layout.getBundle(request).setTitle(title).addMainContent(mainContent);

    // TODO(#5676): This toast code is re-implemented across multiple controllers. Can we write a
    // helper method for it?
    Http.Flash flash = request.flash();
    if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()));
    }

    return layout.renderCentered(htmlBundle);
  }

  private FormTag createImageDescriptionForm(
      Http.Request request, ProgramDefinition programDefinition) {
    String existingDescription =
        programDefinition
            .localizedSummaryImageDescription()
            .map(LocalizedStrings::getDefault)
            .orElse("");
    ProgramImageDescriptionForm existingDescriptionForm =
        new ProgramImageDescriptionForm(existingDescription);
    Form<ProgramImageDescriptionForm> form =
        formFactory.form(ProgramImageDescriptionForm.class).fill(existingDescriptionForm);

    return form()
        .withId(IMAGE_DESCRIPTION_FORM_ID)
        .withMethod("POST")
        .withAction(
            routes.AdminProgramImageController.updateDescription(programDefinition.id()).url())
        .with(
            makeCsrfTokenInputTag(request),
            FieldWithLabel.input()
                .setFieldName(ProgramImageDescriptionForm.SUMMARY_IMAGE_DESCRIPTION)
                .setLabelText("Image description")
                .setValue(form.value().get().getSummaryImageDescription())
                .getInputTag())
        .with(
            submitButton("Save description")
                .withForm(IMAGE_DESCRIPTION_FORM_ID)
                .withClass(ButtonStyles.SOLID_BLUE));
  }

  private DivTag createImageUploadForm(ProgramDefinition program) {
    StorageUploadRequest storageUploadRequest = createStorageUploadRequest(program);
    FormTag form =
        fileUploadViewStrategy
            .renderFileUploadFormElement(storageUploadRequest)
            .withId(IMAGE_FILE_UPLOAD_FORM_ID);
    ImmutableList<InputTag> additionalFileUploadFormInputs =
        fileUploadViewStrategy.additionalFileUploadFormInputs(Optional.of(storageUploadRequest));
    DivTag fileInputElement =
        fileUploadViewStrategy.createUswdsFileInputFormElement(
            /* acceptedMimeTypes= */ MIME_TYPES_IMAGES,
            // TODO(#5676): Get final copy for the size warning message.
            /* hintText= */ "File size must be at most 1 MB.");
    FormTag fullForm =
        form.with(additionalFileUploadFormInputs)
            // It's critical that the "file" field be the last input element for the form since S3
            // will ignore any fields after that.
            // See #2653 / https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-HTTPPOSTForms.html
            // for more context.
            .with(fileInputElement);

    return div()
        .with(fullForm)
        .with(
            submitButton("Save image")
                .withForm(IMAGE_FILE_UPLOAD_FORM_ID)
                .withClasses(ButtonStyles.SOLID_BLUE, "mb-2"))
        .with(fileUploadViewStrategy.footerTags());

    // TODO(#5676): Warn admins of recommended image size and dimensions.
    // TODO(#5676): Allow admins to remove an already-uploaded file.
  }

  private StorageUploadRequest createStorageUploadRequest(ProgramDefinition program) {
    String key = PublicFileNameFormatter.formatPublicProgramImageFileKey(program.id());
    String onSuccessRedirectUrl =
        baseUrl + routes.AdminProgramImageController.updateFileKey(program.id()).url();
    return publicStorageClient.getSignedUploadRequest(key, onSuccessRedirectUrl);
  }

  private DivTag renderCurrentProgramCard(Http.Request request, ProgramDefinition program) {
    CiviFormProfile profile =
        profileUtils
            .currentUserProfile(request)
            .orElseThrow(() -> new RuntimeException("Unable to resolve profile"));
    Long applicantId;
    try {
      applicantId = profile.getApplicant().get().id;
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    Messages messages = messagesApi.preferred(request);
    // We don't need to fill in any applicant data besides the program information since this is
    // just for a card preview
    ApplicantService.ApplicantProgramData card =
        ApplicantService.ApplicantProgramData.builder().setProgram(program).build();

    LiTag programCard =
        programCardViewRenderer.createProgramCard(
            request,
            messages,
            // An admin does have an associated applicant account, so consider them logged in so
            // that the "Apply" button takes them to a preview of the program.
            ApplicantPersonalInfo.ApplicantType.LOGGED_IN,
            card,
            applicantId,
            messages.lang().toLocale(),
            MessageKey.BUTTON_APPLY,
            MessageKey.BUTTON_APPLY_SR,
            /* nestedUnderSubheading= */ false,
            layout.getBundle(request),
            profile,
            zoneId);
    return div().with(h2("What the applicant will see").withClasses("mb-4")).with(programCard);
    // Note: The "Program details" link inside the card preview will not work if the admin hasn't
    // provided a custom external link. This is because that link redirects to
    // ApplicantProgramsController#showWithApplicantId, which only allows access to the current
    // versions of programs. When editing a program image, that program is in draft form, so the
    // controller prevents access.
  }
}
