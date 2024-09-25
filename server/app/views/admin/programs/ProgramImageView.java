package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import forms.admin.ProgramImageDescriptionForm;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.H1Tag;
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
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.AlertType;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;
import services.program.ProgramDefinition;
import views.AlertComponent;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.applicant.ProgramCardViewRenderer;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.ToastMessage;
import views.fileupload.FileUploadViewStrategy;
import views.style.StyleUtils;

/** A view for admins to update the image associated with a particular program. */
public final class ProgramImageView extends BaseHtmlView {
  // Only allow JPEG and PNG images. We want to specifically prohibit SVGs (which don't render well
  // in the <img> element) and GIFs (which would be too distracting on the homepage).
  private static final String MIME_TYPES_IMAGES = "image/jpeg,image/png";
  private static final String IMAGE_DESCRIPTION_FORM_ID = "image-description-form";
  private static final String IMAGE_FILE_UPLOAD_FORM_ID = "image-file-upload-form";
  private static final String PAGE_TITLE = "Image upload";
  private static final String DELETE_IMAGE_BUTTON_TEXT = "Delete image";

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
      ProfileUtils profileUtils,
      ProgramCardViewRenderer programCardViewRenderer,
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
   *
   * @param editStatus specifies whether the program is being created or edited so that the "Back"
   *     button can direct the admin appropriately. This should match a name in the {@link
   *     ProgramEditStatus} enum.
   */
  public Content render(
      Http.Request request, ProgramDefinition programDefinition, String editStatus) {
    ProgramEditStatus editStatusEnum = ProgramEditStatus.getStatusFromString(editStatus);
    ATag backButton = createBackButton(programDefinition, editStatusEnum);

    DivTag mainContent = div().withClass("mx-20");

    H1Tag titleContainer = renderHeader(PAGE_TITLE);

    DivTag formsContainer = div();
    Modal deleteImageModal = createDeleteImageModal(request, programDefinition, editStatus);
    formsContainer.with(createImageDescriptionForm(request, programDefinition, editStatus));
    formsContainer.with(
        createImageUploadForm(
            request,
            messagesApi.preferred(request),
            programDefinition,
            deleteImageModal.getButton(),
            editStatus));
    if (editStatusEnum == ProgramEditStatus.CREATION
        || editStatusEnum == ProgramEditStatus.CREATION_EDIT) {
      // When an admin is going through the creation flow, we want to make sure they have a
      // "Continue" button showing them how to finish program creation.
      formsContainer.with(createContinueButton(programDefinition));
    }

    DivTag formsAndCurrentCardContainer =
        div().withClasses("grid", "grid-cols-2", "gap-10", "w-full");
    formsAndCurrentCardContainer.with(formsContainer);
    formsAndCurrentCardContainer.with(renderCurrentProgramCard(request, programDefinition));

    mainContent.with(titleContainer, formsAndCurrentCardContainer);

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(PAGE_TITLE)
            .addMainContent(div().with(backButton, mainContent))
            .addModals(deleteImageModal);

    // TODO(#6593): Write a helper method for this toast display logic.
    Http.Flash flash = request.flash();
    if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()));
    } else if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.errorNonLocalized(flash.get("error").get()));
    }

    return layout.renderCentered(htmlBundle);
  }

  private ATag createBackButton(
      ProgramDefinition programDefinition, ProgramEditStatus programEditStatus) {
    String backTarget;
    switch (programEditStatus) {
      case EDIT:
        backTarget = routes.AdminProgramBlocksController.index(programDefinition.id()).url();
        break;
      case CREATION:
      case CREATION_EDIT:
        // By the time we're in the image view, the program has been created so the new status is
        // CREATION_EDIT.
        backTarget =
            routes.AdminProgramController.edit(
                    programDefinition.id(), ProgramEditStatus.CREATION_EDIT.name())
                .url();
        break;
      default:
        throw new IllegalStateException("All cases should be handled above");
    }

    return new LinkElement()
        .setHref(backTarget)
        .setIcon(Icons.ARROW_LEFT, LinkElement.IconPosition.START)
        .setText("Back")
        .setStyles("my-6", "ml-10")
        .asAnchorText();
  }

  private ButtonTag createContinueButton(ProgramDefinition programDefinition) {
    return redirectButton(
            "continue-button",
            "Continue",
            routes.AdminProgramBlocksController.index(programDefinition.id()).url())
        .withClasses(ButtonStyles.SOLID_BLUE, "mt-20");
  }

  private DivTag createImageDescriptionForm(
      Http.Request request, ProgramDefinition programDefinition, String editStatus) {
    String existingDescription = getExistingDescription(programDefinition);
    ProgramImageDescriptionForm existingDescriptionForm =
        new ProgramImageDescriptionForm(existingDescription);
    Form<ProgramImageDescriptionForm> form =
        formFactory.form(ProgramImageDescriptionForm.class).fill(existingDescriptionForm);

    DivTag buttonsDiv = div().withClass("flex");
    buttonsDiv.with(
        submitButton("Save image description")
            .withForm(IMAGE_DESCRIPTION_FORM_ID)
            .withClasses(ButtonStyles.SOLID_BLUE, "flex")
            // admin_program_image.ts will enable the submit button when the description changes.
            .isDisabled());
    Optional<ButtonTag> manageTranslationsButton =
        createManageTranslationsButton(programDefinition, existingDescription);
    manageTranslationsButton.ifPresent(buttonsDiv::with);

    return div()
        .with(
            AlertComponent.renderSlimAlert(
                AlertType.INFO,
                "Note: Image description is required before uploading an image.",
                /* hidden= */ false,
                "mb-2"))
        .with(
            form()
                .withId(IMAGE_DESCRIPTION_FORM_ID)
                .withMethod("POST")
                .withAction(
                    routes.AdminProgramImageController.updateDescription(
                            programDefinition.id(), editStatus)
                        .url())
                .with(
                    makeCsrfTokenInputTag(request),
                    FieldWithLabel.input()
                        .setFieldName(ProgramImageDescriptionForm.SUMMARY_IMAGE_DESCRIPTION)
                        .setLabelText("Enter image description (Alt Text)")
                        .setRequired(true)
                        .setPlaceholderText("Colorful fruits and vegetables in bins")
                        .setValue(form.value().get().getSummaryImageDescription())
                        .getInputTag()))
        .with(buttonsDiv);
  }

  private Optional<ButtonTag> createManageTranslationsButton(
      ProgramDefinition programDefinition, String existingDescription) {
    Optional<ButtonTag> button =
        layout.createManageTranslationsButton(
            programDefinition.adminName(),
            /* buttonId= */ Optional.empty(),
            StyleUtils.joinStyles(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "flex", "ml-2"));
    // Disable the translations button if there's no description in the first place.
    return button.map(buttonTag -> buttonTag.withCondDisabled(existingDescription.isBlank()));
  }

  private DivTag createImageUploadForm(
      Request request,
      Messages messages,
      ProgramDefinition program,
      ButtonTag deleteButton,
      String editStatus) {
    boolean hasNoDescription = getExistingDescription(program).isBlank();
    StorageUploadRequest storageUploadRequest = createStorageUploadRequest(program, editStatus);
    FormTag form =
        fileUploadViewStrategy
            .renderFileUploadFormElement(storageUploadRequest)
            .withId(IMAGE_FILE_UPLOAD_FORM_ID);
    ImmutableList<InputTag> additionalFileUploadFormInputs =
        fileUploadViewStrategy.additionalFileUploadFormInputs(Optional.of(storageUploadRequest));
    DivTag fileInputElement =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "program-image-upload-file-input",
            /* acceptedMimeTypes= */ MIME_TYPES_IMAGES,
            /* hints= */ ImmutableList.of(
                "The maximum size for image upload is 1MB.",
                "The image will be automatically cropped to 16x9. The program card preview on the"
                    + " right will show the cropping once the image is saved."),
            /* disabled= */ hasNoDescription,
            /* fileLimitMb= */ publicStorageClient.getFileLimitMb(),
            messages);
    FormTag fullForm =
        form.with(additionalFileUploadFormInputs)
            // It's critical that the "file" field be the last input element for the form since S3
            // will ignore any fields after that.
            // See #2653 / https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-HTTPPOSTForms.html
            // for more context.
            .with(fileInputElement);

    DivTag buttonsDiv = div().withClass("flex");
    buttonsDiv.with(
        submitButton("Save image")
            .withForm(IMAGE_FILE_UPLOAD_FORM_ID)
            .withClasses(ButtonStyles.SOLID_BLUE, "flex")
            // admin_program_image.ts will enable the submit button when an image has been uploaded.
            .isDisabled());
    buttonsDiv.with(deleteButton);

    return div()
        .withClass("mt-10")
        .with(fullForm)
        .with(buttonsDiv)
        .with(fileUploadViewStrategy.footerTags(request));
  }

  private StorageUploadRequest createStorageUploadRequest(
      ProgramDefinition program, String editStatus) {
    String key = PublicFileNameFormatter.formatPublicProgramImageFileKey(program.id());
    String onSuccessRedirectUrl =
        baseUrl + routes.AdminProgramImageController.updateFileKey(program.id(), editStatus).url();
    return publicStorageClient.getSignedUploadRequest(key, onSuccessRedirectUrl);
  }

  private String getExistingDescription(ProgramDefinition programDefinition) {
    return programDefinition
        .localizedSummaryImageDescription()
        .map(LocalizedStrings::getDefault)
        .orElse("");
  }

  private DivTag renderCurrentProgramCard(Http.Request request, ProgramDefinition program) {
    DivTag currentProgramCardSection =
        div().withClass("mx-auto").with(h2("What the applicant will see").withClasses("mb-4"));

    Optional<CiviFormProfile> profile = profileUtils.optionalCurrentUserProfile(request);
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
        ApplicantService.ApplicantProgramData.builder(program).build();

    LiTag programCard =
        programCardViewRenderer.createProgramCard(
            request,
            messages,
            // An admin *does* have an associated applicant account, so consider them logged in so
            // that the "Apply" button on the preview card takes them to the full program preview.
            ApplicantPersonalInfo.ApplicantType.LOGGED_IN,
            card,
            Optional.of(applicantId),
            messages.lang().toLocale(),
            MessageKey.BUTTON_APPLY,
            MessageKey.BUTTON_APPLY_SR,
            /* nestedUnderSubheading= */ false,
            layout.getBundle(request),
            profile,
            zoneId,
            /* isInMyApplicationsSection= */ false);
    return currentProgramCardSection.with(programCard);
    // Note: The "Program details" link inside the card preview will not work if the admin hasn't
    // provided a custom external link. This is because the default "Program details" link redirects
    // to ApplicantProgramsController#showWithApplicantId, which only allows access to the published
    // versions of programs. When editing a program image, the program is in *draft* form and has a
    // different ID, so ApplicantProgramsController prevents access.
  }

  private Modal createDeleteImageModal(
      Http.Request request, ProgramDefinition program, String editStatus) {
    ButtonTag deleteImageButton =
        ViewUtils.makeSvgTextButton(DELETE_IMAGE_BUTTON_TEXT, Icons.DELETE)
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "flex", "ml-2")
            // Disable the delete button if there's no image in the first place.
            .withCondDisabled(program.summaryImageFileKey().isEmpty());
    FormTag deleteBlockForm =
        form(makeCsrfTokenInputTag(request))
            .withMethod(Http.HttpVerbs.POST)
            .withAction(
                routes.AdminProgramImageController.deleteFileKey(program.id(), editStatus).url())
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
