package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.img;
import static j2html.TagCreator.label;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import forms.admin.ProgramImageDescriptionForm;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.ImgTag;
import j2html.tags.specialized.InputTag;
import java.util.Optional;

import j2html.tags.specialized.LabelTag;
import org.apache.commons.lang3.RandomStringUtils;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
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
  private final PublicStorageClient publicStorageClient;
  // The ID used to associate the file input field with its screen reader label.
  private final String fileInputId;

  @Inject
  public ProgramImageView(
      AdminLayoutFactory layoutFactory,
      Config config,
      FormFactory formFactory,
      FileUploadViewStrategy fileUploadViewStrategy,
      PublicStorageClient publicStorageClient) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.formFactory = checkNotNull(formFactory);
    this.fileUploadViewStrategy = checkNotNull(fileUploadViewStrategy);
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.fileInputId = RandomStringUtils.randomAlphabetic(8);
  }

  /**
   * Renders the image currently associated with the program and a form to add / edit / delete the
   * image (and its alt text).
   */
  public Content render(Http.Request request, ProgramDefinition programDefinition) {
    String title =
        String.format(
            "Manage program image for %s", programDefinition.localizedName().getDefault());
    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainContent(
                renderHeader(title, "my-10", "mx-10"),
                createImageDescriptionForm(request, programDefinition),
                createImageUploadForm(programDefinition),
                renderCurrentImage(programDefinition));

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
    ImmutableList<InputTag> fileUploadFormInputs =
        fileUploadViewStrategy.fileUploadFormInputs(
            Optional.of(storageUploadRequest),
            MIME_TYPES_IMAGES,
            fileInputId,
            /* ariaDescribedByIds= */ ImmutableList.of(),
            /* hasErrors= */ false);

    LabelTag chooseFileButton = label()
            .withFor(fileInputId)
            .with(
                    span()
                            .attr("role", "button")
                            .attr("tabindex", 0)
                            .withText("Choose program image")
                            .withClasses(
                                    ButtonStyles.OUTLINED_TRANSPARENT,
                                    "w-64",
                                    "mt-10",
                                    "mb-2",
                                    "cursor-pointer"));

    FormTag fullForm =
        form.with(fileUploadFormInputs)
            .with(chooseFileButton);

    // TODO(#5676): Replace with final UX once we have it.
    return div()
        .with(fullForm)
        .with(
            submitButton("Save image")
                .withForm(IMAGE_FILE_UPLOAD_FORM_ID)
                .withClasses(ButtonStyles.SOLID_BLUE, "mb-2"))
        .with(fileUploadViewStrategy.footerTags());

    // TODO(#5676): If there's already a file uploaded, render its name.
    // TODO(#5676): Warn admins of recommended image size and dimensions.
    // TODO(#5676): Allow admins to remove an already-uploaded file.
  }

  private StorageUploadRequest createStorageUploadRequest(ProgramDefinition program) {
    String key = PublicFileNameFormatter.formatPublicProgramImageFilename(program.id());
    String onSuccessRedirectUrl =
            baseUrl + routes.AdminProgramImageController.updateFileKey(program.id()).url();
    return publicStorageClient.getSignedUploadRequest(key, onSuccessRedirectUrl);
  }

  private DivTag renderCurrentImage(ProgramDefinition program) {
    if (program.summaryImageFileKey().isEmpty()) {
      return div();
    }
    ImgTag image =
        img().withSrc(publicStorageClient.getPublicDisplayUrl(program.summaryImageFileKey().get()));
    return div().with(image);
  }
}
