package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.label;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import forms.admin.ProgramImageDescriptionForm;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.H1Tag;
import org.apache.commons.lang3.RandomStringUtils;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.MessageKey;
import services.cloud.FileNameFormatter;
import services.cloud.StorageClient;
import services.cloud.StorageUploadRequest;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.FileUploadViewStrategy;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.ToastMessage;

import java.util.Optional;

/** A view for admins to update the image associated with a particular program. */
public final class ProgramImageView extends BaseHtmlView {
  private final AdminLayout layout;
  private final String baseUrl;
  private final FormFactory formFactory;
  private final FileUploadViewStrategy fileUploadViewStrategy;
  private final StorageClient storageClient;

  // The ID used to associate the file input field with its screen reader label.
  private final String fileInputId;

  @Inject
  public ProgramImageView(AdminLayoutFactory layoutFactory,
                          Config config,
                          FormFactory formFactory,
                          FileUploadViewStrategy fileUploadViewStrategy,
                          StorageClient storageClient) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.formFactory = checkNotNull(formFactory);
    this.fileUploadViewStrategy = checkNotNull(fileUploadViewStrategy);
    this.storageClient = storageClient;
    this.fileInputId = RandomStringUtils.randomAlphabetic(8);
  }

  /**
   * Renders the image currently associated with the program and a form to add / edit / delete the
   * image (and its alt text).
   *
   * <p>TODO(#5676): Implement the forms to add an image and alt text.
   */
  public Content render(Http.Request request, ProgramDefinition programDefinition) {
    String title =
        String.format(
            "Manage program image for %s", programDefinition.localizedName().getDefault());
    H1Tag headerDiv = renderHeader(title, "my-10", "mx-10");
    FormTag imageDescriptionForm = createImageDescriptionForm(request, programDefinition);
    DivTag imageFileUploadForm = createImageUploadForm(programDefinition);
    HtmlBundle htmlBundle =
        layout.getBundle(request).setTitle(title).addMainContent(headerDiv, imageDescriptionForm, imageFileUploadForm);

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
      .withId("imageDescriptionForm")
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
        .with(submitButton("Save description").withForm("imageDescriptionForm").withClass(ButtonStyles.SOLID_BLUE));
  }

  private DivTag createImageUploadForm(ProgramDefinition program) {
    // TODO: Restrict mime-type to just png/jpeg

    // From FileUploadViewStrategy
    String key = FileNameFormatter.formatProgramImageFilename(
      program.id()
    );
    // TODO: Does the filename get auto populated here?
    String onSuccessRedirectUrl = baseUrl + routes.AdminProgramImageController.updateImageFile(program.id()).url();
    StorageUploadRequest storageUploadRequest =
      storageClient.getSignedUploadRequest(key, onSuccessRedirectUrl);

    // Copied from FileUploadViewStrategy#renderFileUploadFormElement
    String formId = "program-image-file-form";

    FormTag form = fileUploadViewStrategy.renderFileUploadFormElement(Optional.empty(), storageUploadRequest,
      formId)
           // TODO: #*signed*FileUploadFields adds additional stuff like the already upload file name,
    // error about it being required, and mobile file upload helptext4.
      .with(
      fileUploadViewStrategy.fileUploadFields(
        Optional.of(storageUploadRequest),
        fileInputId,
        /* ariaDescribedByIds= */ ImmutableList.of(),
        /* hasErrors= */ false
      )
    )
      .with(
        label()
          .withFor(fileInputId)
          .with(
            span()
              .attr("role", "button")
              .attr("tabindex", 0)
              .withText("Choose program image")
              .withClasses(
                ButtonStyles.OUTLINED_TRANSPARENT, "w-44", "mt-2", "cursor-pointer")))
      .with(
        label()
          .withFor(fileInputId)
          .withClass("sr-only")
          .withText("Upload a program image for the program card displayed on the homepage.")
      );


    // Copied from FileUploadQuestionRenderer
    return div()
      .with(form)

      .with(submitButton("Save image")
        .withForm(formId).withClass(ButtonStyles.SOLID_BLUE));
  }
}
