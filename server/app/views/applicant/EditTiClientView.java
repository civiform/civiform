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

/** Renders a page for a trusted intermediary to edit a client */
public class EditTiClientView extends BaseHtmlView {
  private final ApplicantLayout layout;
  private final DateConverter dateConverter;
  private final String baseUrl;
  private AccountRepository accountRepository;

  @Inject
  public EditTiClientView(
      ApplicantLayout layout,
      DateConverter dateConverter,
      Config configuration,
      AccountRepository accountRepository) {
    this.layout = checkNotNull(layout);
    this.dateConverter = checkNotNull(dateConverter);
    this.accountRepository = checkNotNull(accountRepository);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
  }

  public Content render(
      TrustedIntermediaryGroupModel tiGroup,
      ApplicantPersonalInfo personalInfo,
      Http.Request request,
      Messages messages,
      Optional<Long> accountId,
      Optional<Long> currentTisAccountId,
      Optional<Form<EditTiClientInfoForm>> editTiClientInfoForm) {

    if (currentTisAccountId.isEmpty()) {
      HtmlBundle bundle =
          layout
              .getBundle(request)
              .setTitle("Edit client info")
              .addMainContent(
                  renderHeader(tiGroup.getName(), "py-12", "mb-0", "bg-gray-50"),
                  hr(),
                  renderSubHeader("Edit client").withId("edit-client").withClass("my-4"),
                  renderBackLink(),
                  requiredFieldsExplanationContent(),
                  renderEditClientForm(
                      accountRepository.lookupAccount(accountId.get()).get(),
                      request,
                      editTiClientInfoForm,
                      messages))
              .addMainStyles("px-20", "max-w-screen-xl");
      return layout.renderWithNav(request, personalInfo, messages, bundle, accountId.get());
    } else {
      HtmlBundle bundle =
          layout
              .getBundle(request)
              .setTitle("Add new client")
              .addMainContent(
                  renderHeader(tiGroup.getName(), "py-12", "mb-0", "bg-gray-50"),
                  hr(),
                  renderSubHeader("Add client").withId("add-client").withClass("my-4"),
                  renderBackLink(),
                  requiredFieldsExplanationContent(),
                  renderAddClientForm(tiGroup, request, editTiClientInfoForm, messages))
              .addMainStyles("px-20", "max-w-screen-xl");
      return layout.renderWithNav(
          request, personalInfo, messages, bundle, currentTisAccountId.get());
    }
  }

  private ATag renderBackLink() {
    String tiDashLink =
        baseUrl
            + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                    /* nameQuery= */ Optional.empty(),
                    /* dayQuery= */ Optional.empty(),
                    /* monthQuery= */ Optional.empty(),
                    /* yearQuery= */ Optional.empty(),
                    /* page= */ Optional.of(1))
                .url();
    LinkElement link = new LinkElement().setHref(tiDashLink).setText("Back to client list");
    return link.asAnchorText();
  }

  private DivTag renderAddClientForm(
      TrustedIntermediaryGroupModel tiGroup,
      Http.Request request,
      Optional<Form<EditTiClientInfoForm>> form,
      Messages messages) {
    FormTag formTag =
        form()
            .withId("add-ti")
            .withMethod("POST")
            .withAction(routes.TrustedIntermediaryController.addApplicant(tiGroup.id).url());
    FieldWithLabel firstNameField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("add-first-name-input")
                .setFieldName("firstName")
                .setLabelText("First name")
                .setRequired(true)
                .setValue(""),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_FIRST_NAME,
            messages);

    FieldWithLabel middleNameField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("add-middle-name-input")
                .setFieldName("middleName")
                .setLabelText("Middle name")
                .setValue(""),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_MIDDLE_NAME,
            messages);
    FieldWithLabel lastNameField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("add-last-name-input")
                .setFieldName("lastName")
                .setLabelText("Last name")
                .setRequired(true)
                .setValue(""),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_LAST_NAME,
            messages);
    FieldWithLabel phoneNumberField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("add-phone-number-input")
                .setPlaceholderText("(xxx) xxx-xxxx")
                .setAttribute("inputmode", "tel")
                .setFieldName("phoneNumber")
                .setLabelText("Phone number")
                .setValue(""),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_PHONE,
            messages);

    FieldWithLabel emailField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("add-email-input")
                .setFieldName("emailAddress")
                .setLabelText("Email address")
                .setToolTipIcon(Icons.INFO)
                .setToolTipText(
                    "Add an email address for your client to receive status updates about their"
                        + " application automatically. Without an email, you or your"
                        + " community-based organization will be responsible for communicating"
                        + " updates to your client.")
                .setValue(""),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_EMAIL_ADDRESS,
            messages);
    FieldWithLabel dateOfBirthField =
        setStateIfPresent(
            FieldWithLabel.date()
                .setId("add-date-of-birth-input")
                .setFieldName("dob")
                .setLabelText("Date of birth")
                .setRequired(true)
                .setValue(""),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_DOB,
            messages);
    FieldWithLabel tiNoteField =
        setStateIfPresent(
            FieldWithLabel.textArea()
                .setId("add-ti-note-input")
                .setFieldName("tiNote")
                .setLabelText("Notes")
                .setValue(""),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_TI_NOTES,
            messages);
    String cancelUrl =
        baseUrl
            + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                    /* nameQuery= */ Optional.empty(),
                    /* dayQuery= */ Optional.empty(),
                    /* monthQuery= */ Optional.empty(),
                    /* yearQuery= */ Optional.empty(),
                    /* page= */ Optional.of(1))
                .url();
    return div()
        .with(
            formTag
                .with(
                    firstNameField.getInputTag(),
                    middleNameField.getInputTag(),
                    lastNameField.getInputTag(),
                    phoneNumberField.getInputTag(),
                    emailField.getEmailTag(),
                    dateOfBirthField.getDateTag(),
                    tiNoteField.getTextareaTag(),
                    makeCsrfTokenInputTag(request),
                    submitButton("Save").withClasses("ml-2", "mb-6"),
                    asRedirectElement(button("Cancel").withClasses("m-2"), cancelUrl))
                .withClasses("border", "border-gray-300", "shadow-md", "w-1/2", "mt-6"));
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
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("edit-first-name-input")
                .setFieldName("firstName")
                .setLabelText("First name")
                .setRequired(true)
                .setValue(applicantData.getApplicantFirstName()),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_FIRST_NAME,
            messages);

    FieldWithLabel middleNameField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("edit-middle-name-input")
                .setFieldName("middleName")
                .setLabelText("Middle name")
                .setValue(applicantData.getApplicantMiddleName()),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_MIDDLE_NAME,
            messages);
    FieldWithLabel lastNameField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("edit-last-name-input")
                .setFieldName("lastName")
                .setLabelText("Last name")
                .setRequired(true)
                .setValue(applicantData.getApplicantLastName()),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_LAST_NAME,
            messages);
    FieldWithLabel phoneNumberField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("edit-phone-number-input")
                .setPlaceholderText("(xxx) xxx-xxxx")
                .setAttribute("inputmode", "tel")
                .setFieldName("phoneNumber")
                .setLabelText("Phone number")
                .setValue(applicantData.getPhoneNumber().orElse("")),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_PHONE,
            messages);

    FieldWithLabel emailField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("edit-email-input")
                .setFieldName("emailAddress")
                .setLabelText("Email address")
                .setToolTipIcon(Icons.INFO)
                .setToolTipText(
                    "Add an email address for your client to receive status updates about their"
                        + " application automatically. Without an email, you or your"
                        + " community-based organization will be responsible for communicating"
                        + " updates to your client.")
                .setValue(account.getEmailAddress()),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_EMAIL_ADDRESS,
            messages);
    FieldWithLabel dateOfBirthField =
        setStateIfPresent(
            FieldWithLabel.date()
                .setId("edit-date-of-birth-input")
                .setFieldName("dob")
                .setLabelText("Date of birth")
                .setRequired(true)
                .setValue(
                    applicantData
                        .getDateOfBirth()
                        .map(this.dateConverter::formatIso8601Date)
                        .orElse("")),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_DOB,
            messages);
    FieldWithLabel tiNoteField =
        setStateIfPresent(
            FieldWithLabel.textArea()
                .setId("edit-ti-note-input")
                .setFieldName("tiNote")
                .setLabelText("Notes")
                .setValue(account.getTiNote()),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_TI_NOTES,
            messages);
    String cancelUrl =
        baseUrl
            + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                    /* nameQuery= */ Optional.empty(),
                    /* dayQuery= */ Optional.empty(),
                    /* monthQuery= */ Optional.empty(),
                    /* yearQuery= */ Optional.empty(),
                    /* page= */ Optional.of(1))
                .url();
    return div()
        .with(
            formTag
                .with(
                    firstNameField.getInputTag(),
                    middleNameField.getInputTag(),
                    lastNameField.getInputTag(),
                    phoneNumberField.getInputTag(),
                    emailField.getEmailTag(),
                    dateOfBirthField.getDateTag(),
                    tiNoteField.getTextareaTag(),
                    makeCsrfTokenInputTag(request),
                    submitButton("Save").withClasses("ml-2", "mb-6"),
                    asRedirectElement(button("Cancel").withClasses("m-2"), cancelUrl))
                .withClasses("border", "border-gray-300", "shadow-md", "w-1/2", "mt-6"));
  }

  private FieldWithLabel setStateIfPresent(
      FieldWithLabel field,
      Optional<Form<EditTiClientInfoForm>> maybeForm,
      String key,
      Messages messages) {
    if (maybeForm.isEmpty()) {
      return field;
    }

    EditTiClientInfoForm form = maybeForm.get().value().get();
    switch (key) {
      case TrustedIntermediaryService.FORM_FIELD_NAME_FIRST_NAME:
        field.setValue(form.getFirstName());
        break;
      case TrustedIntermediaryService.FORM_FIELD_NAME_LAST_NAME:
        field.setValue(form.getLastName());
        break;
      case TrustedIntermediaryService.FORM_FIELD_NAME_MIDDLE_NAME:
        field.setValue(form.getMiddleName());
        break;
      case TrustedIntermediaryService.FORM_FIELD_NAME_DOB:
        field.setValue(form.getDob());
        break;
      case TrustedIntermediaryService.FORM_FIELD_NAME_PHONE:
        field.setValue(form.getPhoneNumber());
        break;
      case TrustedIntermediaryService.FORM_FIELD_NAME_EMAIL_ADDRESS:
        field.setValue(form.getEmailAddress());
        break;
      case TrustedIntermediaryService.FORM_FIELD_NAME_TI_NOTES:
        field.setValue(form.getTiNote());
        break;
    }

    if (maybeForm.get().error(key).isPresent()) {
      field.setFieldErrors(messages, maybeForm.get().errors(key));
    }

    return field;
  }
}
