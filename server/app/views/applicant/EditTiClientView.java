package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.hr;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.ti.routes;
import forms.EditTiClientInfoForm;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.AccountRepository;
import services.DateConverter;
import services.applicant.ApplicantData;
import services.applicant.ApplicantPersonalInfo;
import services.ti.TrustedIntermediaryService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.ToastMessage;

/** Renders a page for a trusted intermediary to manage their clients. */
public class EditTiClientView extends BaseHtmlView {
  private final ApplicantLayout layout;
  private final DateConverter dateConverter;
  public static final String OPTIONAL_INDICATOR = " (optional)";
  private final String baseUrl;
  private AccountRepository accountRepository;

  // private final Messages enUsMessages;

  @Inject
  public EditTiClientView(
      ApplicantLayout layout,
      DateConverter dateConverter,
      Config configuration,
      AccountRepository accountRepository) { // , @EnUsLang Messages enUsMessages) {
    this.layout = checkNotNull(layout);
    this.dateConverter = checkNotNull(dateConverter);
    this.accountRepository = checkNotNull(accountRepository);
    checkNotNull(configuration);
    this.baseUrl = configuration.getString("base_url");
    // this.enUsMessages = checkNotNull(enUsMessages);
  }

  public Content render(
      TrustedIntermediaryGroupModel tiGroup,
      ApplicantPersonalInfo personalInfo,
      Http.Request request,
      Messages messages,
      Long accountId,
      Optional<Form<EditTiClientInfoForm>> editTiClientInfoForm) {

    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle("CiviForm")
            .addMainContent(
                renderHeader(tiGroup.getName(), "py-12", "mb-0", "bg-gray-50"),
                hr(),
                renderSubHeader("Edit Client").withId("edit-client").withClass("my-4"),
                renderBackLink(),
                requiredFieldsExplanationContent(),
                // hr().withClasses("mt-6"),
                // renderSubHeader("Clients").withClass("my-4"),
                renderEditClientForm(
                    accountRepository.lookupAccount(accountId).get(),
                    request,
                    editTiClientInfoForm,
                    messages))
            .addMainStyles("px-20", "max-w-screen-xl");

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      LoggerFactory.getLogger(EditTiClientView.class).info(request.flash().get("error").get());
      bundle.addToastMessages(
          ToastMessage.errorNonLocalized(flash.get("error").get()).setDuration(-1));
    } else if (flash.get("success").isPresent()) {
      bundle.addToastMessages(ToastMessage.success(flash.get("success").get()).setDuration(-1));
    }
    return layout.renderWithNav(request, personalInfo, messages, bundle, accountId);
  }

  private ATag renderBackLink() {
    String tiDashLink =
        baseUrl
            + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                    /* nameQuery= */ Optional.empty(),
                    /* dateQuery= */ Optional.empty(),
                    /* page= */ Optional.of(1))
                .url();
    LinkElement link = new LinkElement().setHref(tiDashLink).setText("Back to client list");
    return link.asAnchorText();
  }

  private DivTag renderEditClientForm(
      AccountModel account,
      Http.Request request,
      Optional<Form<EditTiClientInfoForm>> form,
      Messages messages) {
    ApplicantData applicantData = account.newestApplicant().get().getApplicantData();
    FormTag formTag =
        form()
            .withId("edit-ti")
            .withMethod("POST")
            .withAction(routes.TrustedIntermediaryController.updateClientInfo(account.id).url());
    FieldWithLabel firstNameField =
        FieldWithLabel.input()
            .setId("first-name-input")
            .setFieldName("firstName")
            .setLabelText("First Name")
            .setRequired(true)
            .setValue(applicantData.getApplicantFirstName());
    if (form.isPresent()
        && form.get().hasErrors()
        && !form.get().errors(TrustedIntermediaryService.FORM_FIELD_NAME_FIRST_NAME).isEmpty()) {
      firstNameField.setFieldErrors(
          messages, form.get().errors(TrustedIntermediaryService.FORM_FIELD_NAME_FIRST_NAME));
    }
    FieldWithLabel middleNameField =
        FieldWithLabel.input()
            .setId("middle-name-input")
            .setFieldName("middleName")
            .setLabelText("Middle Name")
            .setValue(applicantData.getApplicantMiddleName());
    FieldWithLabel lastNameField =
        FieldWithLabel.input()
            .setId("last-name-input")
            .setFieldName("lastName")
            .setLabelText("Last Name")
            .setRequired(true)
            .setValue(applicantData.getApplicantLastName());
    if (form.isPresent()
        && form.get().hasErrors()
        && !form.get().errors(TrustedIntermediaryService.FORM_FIELD_NAME_LAST_NAME).isEmpty()) {
      lastNameField.setFieldErrors(
          messages, form.get().errors(TrustedIntermediaryService.FORM_FIELD_NAME_LAST_NAME));
    }
    FieldWithLabel phoneNumberField =
        FieldWithLabel.input()
            .setId("edit-phone-number-input")
            .setPlaceholderText("(xxx) xxx-xxxx")
            .setFieldName("phoneNumber")
            .setLabelText("Phone Number")
            .setValue(applicantData.getPhoneNumber().orElse(""));
    if (form.isPresent()
        && form.get().hasErrors()
        && !form.get().errors(TrustedIntermediaryService.FORM_FIELD_NAME_PHONE).isEmpty()) {
      phoneNumberField.setFieldErrors(
          messages, form.get().errors(TrustedIntermediaryService.FORM_FIELD_NAME_PHONE));
    }
    FieldWithLabel emailField =
        FieldWithLabel.email()
            .setId("email-input")
            .setFieldName("emailAddress")
            .setLabelText("Email Address")
            .setToolTipIcon(Icons.INFO)
            .setToolTipText(
                "Add an email address for your client to receive status updates about their"
                    + " application automatically. Without an email, you or your community-based"
                    + " organization will be responsible for communicating updates to your"
                    + " client.")
            .setValue(account.getEmailAddress());
    if (form.isPresent()
        && form.get().hasErrors()
        && !form.get().errors(TrustedIntermediaryService.FORM_FIELD_NAME_EMAIL_ADDRESS).isEmpty()) {
      emailField.setFieldErrors(
          messages, form.get().errors(TrustedIntermediaryService.FORM_FIELD_NAME_EMAIL_ADDRESS));
    }
    FieldWithLabel dateOfBirthField =
        FieldWithLabel.date()
            .setId("date-of-birth-input")
            .setFieldName("dob")
            .setLabelText("Date Of Birth")
            .setRequired(true)
            .setValue(
                applicantData
                    .getDateOfBirth()
                    .map(this.dateConverter::formatIso8601Date)
                    .orElse(""));
    if (form.isPresent()
        && form.get().hasErrors()
        && !form.get().errors(TrustedIntermediaryService.FORM_FIELD_NAME_DOB).isEmpty()) {
      dateOfBirthField.setFieldErrors(
          messages, form.get().errors(TrustedIntermediaryService.FORM_FIELD_NAME_DOB));
    }
    FieldWithLabel tiNoteField =
        FieldWithLabel.textArea()
            .setId("ti-note-input")
            .setFieldName("tiNote")
            .setLabelText("Notes")
            .setValue(account.getTiNote());

    return div()
        .with(
            formTag.with(
                firstNameField.getInputTag(),
                middleNameField.getInputTag(),
                lastNameField.getInputTag(),
                phoneNumberField.getInputTag(),
                emailField.getEmailTag(),
                dateOfBirthField.getDateTag(),
                tiNoteField.getTextareaTag(),
                makeCsrfTokenInputTag(request),
                submitButton("Save").withClasses("ml-2", "mb-6")))
        .withClasses("border", "border-gray-300", "shadow-md", "w-1/2", "mt-6");
  }
}
