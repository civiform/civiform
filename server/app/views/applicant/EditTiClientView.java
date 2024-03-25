package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.hr;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.ti.routes;
import forms.TiClientInfoForm;
import j2html.tags.Tag;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.time.LocalDate;
import java.util.Optional;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.AccountRepository;
import services.MessageKey;
import services.applicant.ApplicantData;
import services.applicant.ApplicantPersonalInfo;
import services.ti.TrustedIntermediaryService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.MemorableDateFieldWithLabel;
import views.style.BaseStyles;

/** Renders a page for a trusted intermediary to edit a client */
public class EditTiClientView extends BaseHtmlView {
  private final ApplicantLayout layout;

  private final String baseUrl;
  private AccountRepository accountRepository;

  @Inject
  public EditTiClientView(
      ApplicantLayout layout, Config configuration, AccountRepository accountRepository) {

    this.layout = checkNotNull(layout);

    this.accountRepository = checkNotNull(accountRepository);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
  }

  public Content render(
      TrustedIntermediaryGroupModel tiGroup,
      ApplicantPersonalInfo personalInfo,
      Http.Request request,
      Messages messages,
      Optional<Long> accountIdToEdit,
      Long applicantIdOfTi,
      Optional<Form<TiClientInfoForm>> tiClientInfoForm) {
    Optional<AccountModel> optionalAccountModel = Optional.empty();
    String title = messages.at(MessageKey.TITLE_CREATE_CLIENT.getKeyName());
    String pageHeader = "Add client";
    String pageId = "add-client";
    Optional<String> optionalSuccessMessage =
        Optional.ofNullable(messages.at(MessageKey.BANNER_NEW_CLIENT_CREATED.getKeyName()));
    String successToast =
        messages.at(
            MessageKey.CONTENT_CLIENT_CREATED.getKeyName(),
            personalInfo.getDisplayString(messages));
    if (accountIdToEdit.isPresent()) {
      optionalAccountModel =
          Optional.of(accountRepository.lookupAccount(accountIdToEdit.get()).get());
      title = "Edit client information";
      pageHeader = "Edit client";
      pageId = "edit-client";
      successToast = messages.at(MessageKey.BANNER_CLIENT_INFO_UPDATED.getKeyName());
      optionalSuccessMessage = Optional.empty();
    }
    boolean isSuccessfulSave = tiClientInfoForm.isPresent() && !tiClientInfoForm.get().hasErrors();
    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainContent(
                renderHeader(tiGroup.getName(), "py-12", "mb-0", "bg-gray-50"),
                hr(),
                renderSubHeader(pageHeader).withId(pageId).withClass("my-4"),
                renderBackLink(),
                renderSuccessAlert(isSuccessfulSave, successToast, optionalSuccessMessage),
                requiredFieldsExplanationContent(),
                renderAddOrEditClientForm(
                    tiGroup, optionalAccountModel, request, tiClientInfoForm, messages))
            .addMainStyles("px-20", "max-w-screen-xl");

    return layout.renderWithNav(request, personalInfo, messages, bundle, applicantIdOfTi);
  }

  private Tag renderSuccessAlert(
      boolean isSuccessfulSave, String successToast, Optional<String> optionalSuccessMessage) {
    if (!isSuccessfulSave) {
      return div();
    }
    return ViewUtils.makeAlert(
        successToast, false, optionalSuccessMessage, BaseStyles.ALERT_SUCCESS, "mb-4", "w-3/5");
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
    LinkElement link =
        new LinkElement()
            .setHref(tiDashLink)
            .setText("Back to client list")
            .setId("ti-dashboard-link");
    return link.asAnchorText();
  }

  private DivTag renderAddOrEditClientForm(
      TrustedIntermediaryGroupModel tiGroup,
      Optional<AccountModel> optionalAccount,
      Http.Request request,
      Optional<Form<TiClientInfoForm>> form,
      Messages messages) {
    Optional<ApplicantData> optionalApplicantData = Optional.empty();
    FormTag formTag;
    if (optionalAccount.isPresent()) {
      optionalApplicantData =
          Optional.of(optionalAccount.get().newestApplicant().get().getApplicantData());

      formTag =
          form()
              .withId("edit-ti")
              .withMethod("POST")
              .withAction(
                  routes.TrustedIntermediaryController.editClient(optionalAccount.get().id).url());
    } else {
      formTag =
          form()
              .withId("add-ti")
              .withMethod("POST")
              .withAction(routes.TrustedIntermediaryController.addClient(tiGroup.id).url());
    }
    FieldWithLabel firstNameField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("first-name-input")
                .setFieldName("firstName")
                .setLabelText(messages.at(MessageKey.NAME_LABEL_FIRST.getKeyName()))
                .setRequired(true)
                .setValue(setDefaultFirstName(optionalApplicantData)),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_FIRST_NAME,
            messages);

    FieldWithLabel middleNameField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("middle-name-input")
                .setFieldName("middleName")
                .setLabelText(
                    messages.at(MessageKey.NAME_LABEL_MIDDLE.getKeyName())
                        + " "
                        + messages.at(MessageKey.CONTENT_OPTIONAL.getKeyName()))
                .setValue(setDefaultMiddleName(optionalApplicantData)),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_MIDDLE_NAME,
            messages);
    FieldWithLabel lastNameField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("last-name-input")
                .setFieldName("lastName")
                .setLabelText(messages.at(MessageKey.NAME_LABEL_LAST.getKeyName()))
                .setRequired(true)
                .setValue(setDefaultLastName(optionalApplicantData)),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_LAST_NAME,
            messages);
    FieldWithLabel phoneNumberField =
        setStateIfPresent(
            FieldWithLabel.input()
                .setId("phone-number-input")
                .setPlaceholderText("(xxx) xxx-xxxx")
                .setAttribute("inputmode", "tel")
                .setFieldName("phoneNumber")
                .setLabelText(
                    messages.at(MessageKey.PHONE_LABEL_PHONE_NUMBER.getKeyName())
                        + " "
                        + messages.at(MessageKey.CONTENT_OPTIONAL.getKeyName()))
                .setValue(setDefaultPhone(optionalApplicantData)),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_PHONE,
            messages);

    FieldWithLabel emailField =
        setStateIfPresent(
            FieldWithLabel.email()
                .setId("email-input")
                .setFieldName("emailAddress")
                .setLabelText(
                    messages.at(MessageKey.EMAIL_LABEL.getKeyName())
                        + " "
                        + messages.at(MessageKey.CONTENT_OPTIONAL.getKeyName()))
                .setToolTipIcon(Icons.INFO)
                .setToolTipText(
                    "Add an email address for your client to receive status updates about their"
                        + " application automatically. Without an email, you or your"
                        + " community-based organization will be responsible for communicating"
                        + " updates to your client.")
                .setValue(setDefaultEmail(optionalAccount)),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_EMAIL_ADDRESS,
            messages);
    MemorableDateFieldWithLabel dateOfBirthField =
        setStateIfPresent(
            new MemorableDateFieldWithLabel()
                .setId("date-of-birth-input")
                .setLegend(messages.at(MessageKey.DOB_LABEL.getKeyName()))
                .setRequired(true)
                .setLocalDateValue(getDefaultDob(optionalApplicantData)),
            form,
            TrustedIntermediaryService.FORM_FIELD_NAME_DOB,
            messages);
    FieldWithLabel tiNoteField =
        setStateIfPresent(
            FieldWithLabel.textArea()
                .setId("ti-note-input")
                .setFieldName("tiNote")
                .setLabelText(
                    messages.at(MessageKey.NOTES_LABEL.getKeyName())
                        + " "
                        + messages.at(MessageKey.CONTENT_OPTIONAL.getKeyName()))
                .setValue(setDefaultTiNotes(optionalAccount)),
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
                    dateOfBirthField.makeMemorableDate(),
                    tiNoteField.getTextareaTag(),
                    makeCsrfTokenInputTag(request),
                    submitButton("Save").withClasses("ml-2", "mb-6"),
                    asRedirectElement(button("Cancel").withClasses("m-2"), cancelUrl))
                .withClasses("border", "border-gray-300", "shadow-md", "w-1/2", "mt-6"));
  }

  private Optional<LocalDate> getDefaultDob(Optional<ApplicantData> optionalApplicantData) {
    return optionalApplicantData.isPresent()
        ? optionalApplicantData.get().getDateOfBirth()
        : Optional.empty();
  }

  private Optional<String> setDefaultPhone(Optional<ApplicantData> optionalApplicantData) {
    return optionalApplicantData.isPresent()
        ? optionalApplicantData.get().getPhoneNumber()
        : Optional.empty();
  }

  private String setDefaultEmail(Optional<AccountModel> optionalAccount) {
    return optionalAccount.isPresent() ? optionalAccount.get().getEmailAddress() : "";
  }

  private String setDefaultTiNotes(Optional<AccountModel> optionalAccount) {
    return optionalAccount.isPresent() ? optionalAccount.get().getTiNote() : "";
  }

  private Optional<String> setDefaultMiddleName(Optional<ApplicantData> optionalApplicantData) {
    return optionalApplicantData.isPresent()
        ? optionalApplicantData.get().getApplicantMiddleName()
        : Optional.empty();
  }

  private Optional<String> setDefaultLastName(Optional<ApplicantData> optionalApplicantData) {
    return optionalApplicantData.isPresent()
        ? optionalApplicantData.get().getApplicantLastName()
        : Optional.empty();
  }

  private Optional<String> setDefaultFirstName(Optional<ApplicantData> optionalApplicantData) {
    return optionalApplicantData.isPresent()
        ? optionalApplicantData.get().getApplicantFirstName()
        : Optional.empty();
  }

  private MemorableDateFieldWithLabel setStateIfPresent(
      MemorableDateFieldWithLabel field,
      Optional<Form<TiClientInfoForm>> optionalForm,
      String key,
      Messages messages) {
    if (optionalForm.isEmpty()) {
      return field;
    }

    TiClientInfoForm form = optionalForm.get().value().get();
    switch (key) {
      case TrustedIntermediaryService.FORM_FIELD_NAME_DOB:
        field.setDayQuery(form.getDayQuery());
        field.setMonthQuery(form.getMonthQuery());
        field.setYearQuery(form.getYearQuery());
        break;
    }

    if (optionalForm.get().error(key).isPresent()) {
      field.setFieldErrors(messages, optionalForm.get().errors(key));
    }

    return field;
  }

  private FieldWithLabel setStateIfPresent(
      FieldWithLabel field,
      Optional<Form<TiClientInfoForm>> optionalForm,
      String key,
      Messages messages) {
    if (optionalForm.isEmpty()) {
      return field;
    }

    TiClientInfoForm form = optionalForm.get().value().get();
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
        //      case TrustedIntermediaryService.FORM_FIELD_NAME_DOB:
        //        field.setValue(form.getDob());
        //        break;
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

    if (optionalForm.get().error(key).isPresent()) {
      field.setFieldErrors(messages, optionalForm.get().errors(key));
    }

    return field;
  }
}
