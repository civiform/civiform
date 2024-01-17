package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import forms.admin.ProgramImageDescriptionForm;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LiTag;
import java.time.ZoneId;
import java.util.NoSuchElementException;
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
import views.ViewUtils;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.applicant.ProgramCardViewRenderer;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.ToastMessage;
import views.components.breadcrumb.BreadcrumbFactory;
import views.components.breadcrumb.BreadcrumbItem;
import views.fileupload.FileUploadViewStrategy;

/** A view for admins to update the image associated with a particular program. */
public final class ProgramImageView extends BaseHtmlView {
  // TODO(#5676): Should we prohibit gifs?
  private static final String MIME_TYPES_IMAGES = "image/*";
  private static final String IMAGE_DESCRIPTION_FORM_ID = "image-description-form";
  private static final String IMAGE_FILE_UPLOAD_FORM_ID = "image-file-upload-form";
  private static final String PAGE_TITLE = "Image upload";
  private static final String DELETE_IMAGE_BUTTON_TEXT = "Delete image";

  private final AdminLayout layout;
  private final BreadcrumbFactory breadcrumbFactory;
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
      BreadcrumbFactory breadcrumbFactory,
      Config config,
      FormFactory formFactory,
      FileUploadViewStrategy fileUploadViewStrategy,
      MessagesApi messagesApi,
      ProfileUtils profileUtils,
      ProgramCardViewRenderer programCardViewRenderer,
      PublicStorageClient publicStorageClient,
      ZoneId zoneId) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
    this.breadcrumbFactory = checkNotNull(breadcrumbFactory);
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
    DivTag breadcrumbs = createBreadcrumbs(programDefinition);

    DivTag mainContent = div().withClass("mx-20");

    DivTag titleAndImageDescriptionContainer =
        div()
            .withClasses("flex", "mt-2", "mb-10")
            .with(
                div()
                    .withClass("w-2/5")
                    .with(renderHeader(PAGE_TITLE))
                    .with(span("Browse or drag and drop an image to upload")))
            .with(
                div()
                    .withClasses("w-3/5", "mt-4")
                    .with(createImageDescriptionForm(request, programDefinition)));

    Modal deleteImageModal = createDeleteImageModal(request, programDefinition);
    DivTag imageUploadAndCurrentCardContainer =
        div()
            .withClasses("grid", "grid-cols-2", "gap-2", "w-full")
            .with(
                div()
                    .with(createImageUploadForm(programDefinition))
                    .with(deleteImageModal.getButton()))
            .with(renderCurrentProgramCard(request, programDefinition));
    mainContent.with(titleAndImageDescriptionContainer, imageUploadAndCurrentCardContainer);

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(PAGE_TITLE)
            .addMainContent(div().with(breadcrumbs, mainContent))
            .addModals(deleteImageModal);

    // TODO(#5676): This toast code is re-implemented across multiple controllers. Can we write a
    // helper method for it?
    Http.Flash flash = request.flash();
    if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()));
    }

    return layout.renderCentered(htmlBundle);
  }

  private DivTag createBreadcrumbs(ProgramDefinition program) {
    ImmutableList<BreadcrumbItem> breadcrumbItems =
        ImmutableList.of(
            BreadcrumbItem.create(
                "All Programs",
                /* link= */ baseUrl + routes.AdminProgramController.index().url(),
                /* icon= */ null),
            BreadcrumbItem.create(
                program.localizedName().getDefault(),
                /* link= */ baseUrl + routes.AdminProgramBlocksController.index(program.id()).url(),
                /* icon= */ null),
            BreadcrumbItem.create(PAGE_TITLE, /* link= */ null, Icons.IMAGE));
    return div()
        .withClasses("mt-4", "mx-10")
        .with(breadcrumbFactory.buildBreadcrumbTrail(breadcrumbItems));
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
                .setLabelText("Enter image description (Alt Text)")
                .setPlaceholderText("Colorful fruits and vegetables in bins")
                .setValue(form.value().get().getSummaryImageDescription())
                .getInputTag())
        .with(
            submitButton("Save image description")
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

    // TODO(#5676): Replace with final UX once we have it.
    return div()
        .with(fullForm)
        .with(
            submitButton("Save image")
                .withForm(IMAGE_FILE_UPLOAD_FORM_ID)
                .withClasses(ButtonStyles.SOLID_BLUE, "mb-2"))
        .with(fileUploadViewStrategy.footerTags());

    // TODO(#5676): Warn admins of recommended image size and dimensions.
  }

  private StorageUploadRequest createStorageUploadRequest(ProgramDefinition program) {
    String key = PublicFileNameFormatter.formatPublicProgramImageFileKey(program.id());
    String onSuccessRedirectUrl =
        baseUrl + routes.AdminProgramImageController.updateFileKey(program.id()).url();
    return publicStorageClient.getSignedUploadRequest(key, onSuccessRedirectUrl);
  }

  private DivTag renderCurrentProgramCard(Http.Request request, ProgramDefinition program) {
    DivTag currentProgramCardSection =
        div().with(h2("What the applicant will see").withClasses("mb-4"));

    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);
    Long applicantId;
    try {
      applicantId = profile.get().getApplicant().get().id;
    } catch (NoSuchElementException | ExecutionException | InterruptedException e) {
      return currentProgramCardSection.with(
          p("Applicant preview can't be rendered: Applicant ID for admin couldn't be fetched."));
    }

    Messages messages = messagesApi.preferred(request);
    // We don't need to fill in any applicant data besides the program information since this is
    // just for a card preview.
    ApplicantService.ApplicantProgramData card =
        ApplicantService.ApplicantProgramData.builder().setProgram(program).build();

    LiTag programCard =
        programCardViewRenderer.createProgramCard(
            request,
            messages,
            // An admin *does* have an associated applicant account, so consider them logged in so
            // that the "Apply" button on the preview card takes them to the full program preview.
            ApplicantPersonalInfo.ApplicantType.LOGGED_IN,
            card,
            applicantId,
            messages.lang().toLocale(),
            MessageKey.BUTTON_APPLY,
            MessageKey.BUTTON_APPLY_SR,
            /* nestedUnderSubheading= */ false,
            layout.getBundle(request),
            profile.get(),
            zoneId);
    return currentProgramCardSection.with(programCard);
    // Note: The "Program details" link inside the card preview will not work if the admin hasn't
    // provided a custom external link. This is because the default "Program details" link redirects
    // to ApplicantProgramsController#showWithApplicantId, which only allows access to the published
    // versions of programs. When editing a program image, the program is in *draft* form and has a
    // different ID, so ApplicantProgramsController prevents access.
  }

  private Modal createDeleteImageModal(Http.Request request, ProgramDefinition program) {
    ButtonTag deleteImageButton =
        ViewUtils.makeSvgTextButton(DELETE_IMAGE_BUTTON_TEXT, Icons.DELETE)
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "mt-8")
            // Disable the delete button if there's no image in the first place.
            .withCondDisabled(program.summaryImageFileKey().isEmpty());
    FormTag deleteBlockForm =
        form(makeCsrfTokenInputTag(request))
            .withMethod(Http.HttpVerbs.POST)
            .withAction(routes.AdminProgramImageController.deleteFileKey(program.id()).url())
            .with(p("Once you delete this image, you'll need to re-upload a new image."))
            .with(
                submitButton(DELETE_IMAGE_BUTTON_TEXT)
                    .withClasses(ButtonStyles.SOLID_BLUE, "mt-8"));

    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(deleteBlockForm)
        .setModalTitle("Delete this program image?")
        .setTriggerButtonContent(deleteImageButton)
        .build();
  }
}
