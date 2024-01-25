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
import controllers.admin.AdminProgramImageController;
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
   * @param referer specifies how an admin got to the program image page so that the "Back" button
   *     can direct the admin appropriately. This should match a name in the {@link
   *     AdminProgramImageController.Referer} enum.
   */
  public Content render(Http.Request request, ProgramDefinition programDefinition, String referer) {
    ATag backButton = createBackButton(programDefinition, referer);

    DivTag mainContent = div().withClass("mx-20");

    H1Tag titleContainer = renderHeader(PAGE_TITLE);

    DivTag formsContainer = div();
    Modal deleteImageModal = createDeleteImageModal(request, programDefinition, referer);
    formsContainer.with(createImageDescriptionForm(request, programDefinition, referer));
    formsContainer.with(
        createImageUploadForm(programDefinition, deleteImageModal.getButton(), referer));

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

    // TODO(#5676): This toast code is re-implemented across multiple controllers. Can we write a
    // helper method for it?
    Http.Flash flash = request.flash();
    if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()));
    }

    return layout.renderCentered(htmlBundle);
  }

  private ATag createBackButton(ProgramDefinition programDefinition, String referer) {
    AdminProgramImageController.Referer refererEnum;
    try {
      refererEnum = AdminProgramImageController.Referer.valueOf(referer);
    } catch (IllegalArgumentException e) {
      refererEnum = AdminProgramImageController.Referer.BLOCKS;
    }

    String backTarget;
    switch (refererEnum) {
      case BLOCKS:
        backTarget = routes.AdminProgramBlocksController.index(programDefinition.id()).url();
        break;
      case DETAILS:
        backTarget = routes.AdminProgramController.edit(programDefinition.id()).url();
        break;
      default:
        throw new IllegalStateException("All referer cases should be handled above");
    }

    return new LinkElement()
            .setHref(backTarget)
            .setIcon(Icons.ARROW_LEFT, LinkElement.IconPosition.START)
            .setText("Back")
            .setStyles("my-6", "ml-10")
            .asAnchorText();
  }

  private DivTag createImageDescriptionForm(
      Http.Request request, ProgramDefinition programDefinition, String referer) {
    String existingDescription = getExistingDescription(programDefinition);
    ProgramImageDescriptionForm existingDescriptionForm =
        new ProgramImageDescriptionForm(existingDescription);
    Form<ProgramImageDescriptionForm> form =
        formFactory.form(ProgramImageDescriptionForm.class).fill(existingDescriptionForm);

    DivTag buttonsDiv = div().withClass("flex");
    buttonsDiv.with(
        submitButton("Save image description")
            .withForm(IMAGE_DESCRIPTION_FORM_ID)
            .withClasses(ButtonStyles.SOLID_BLUE, "flex"));
    Optional<ButtonTag> manageTranslationsButton =
        createManageTranslationsButton(programDefinition, existingDescription);
    manageTranslationsButton.ifPresent(buttonsDiv::with);

    return div()
        .with(
            form()
                .withId(IMAGE_DESCRIPTION_FORM_ID)
                .withMethod("POST")
                .withAction(
                    routes.AdminProgramImageController.updateDescription(
                            programDefinition.id(), referer)
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
      ProgramDefinition program, ButtonTag deleteButton, String referer) {
    boolean hasNoDescription = getExistingDescription(program).isBlank();
    StorageUploadRequest storageUploadRequest = createStorageUploadRequest(program, referer);
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
            /* disabled= */ hasNoDescription);
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
            .withCondDisabled(hasNoDescription));
    buttonsDiv.with(deleteButton);

    // TODO(#5676): Replace with final UX once we have it.
    return div()
        .withClass("mt-10")
        .with(fullForm)
        .with(buttonsDiv)
        .with(p("Note: Image description is required before uploading an image.").withClass("mt-1"))
        .with(fileUploadViewStrategy.footerTags());

    // TODO(#5676): Warn admins of recommended image size and dimensions.
  }

  private StorageUploadRequest createStorageUploadRequest(
      ProgramDefinition program, String referer) {
    String key = PublicFileNameFormatter.formatPublicProgramImageFileKey(program.id());
    String onSuccessRedirectUrl =
        baseUrl + routes.AdminProgramImageController.updateFileKey(program.id(), referer).url();
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

  private Modal createDeleteImageModal(
      Http.Request request, ProgramDefinition program, String referer) {
    ButtonTag deleteImageButton =
        ViewUtils.makeSvgTextButton(DELETE_IMAGE_BUTTON_TEXT, Icons.DELETE)
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "flex", "ml-2")
            // Disable the delete button if there's no image in the first place.
            .withCondDisabled(program.summaryImageFileKey().isEmpty());
    FormTag deleteBlockForm =
        form(makeCsrfTokenInputTag(request))
            .withMethod(Http.HttpVerbs.POST)
            .withAction(
                routes.AdminProgramImageController.deleteFileKey(program.id(), referer).url())
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
