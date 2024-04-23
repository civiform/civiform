package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.ti.routes;
import forms.TiClientInfoForm;
import j2html.tags.Tag;
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
import services.MessageKey;
import services.applicant.ApplicantData;
import services.applicant.ApplicantPersonalInfo;
import services.ti.TrustedIntermediaryService;
import views.HtmlBundle;
import views.ViewUtils;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.style.BaseStyles;

/** Renders a page for a trusted intermediary to edit a client */
public class EditTiClientView extends TrustedIntermediaryDashboardView {
  private final DateConverter dateConverter;
  private AccountRepository accountRepository;

  @Inject
  public EditTiClientView(
      ApplicantLayout layout,
      DateConverter dateConverter,
      Config configuration,
      AccountRepository accountRepository) {
    super(configuration, layout);
    this.dateConverter = checkNotNull(dateConverter);
    this.accountRepository = checkNotNull(accountRepository);
  }

  public Content render(
      TrustedIntermediaryGroupModel tiGroup,
      ApplicantPersonalInfo personalInfo,
      Http.Request request,
      Messages messages,
      Optional<Long> accountIdToEdit,
      Long applicantIdOfTi,
      Optional<Form<TiClientInfoForm>> tiClientInfoForm,
      Long applicantIdOfNewlyAddedClient) {
    Optional<AccountModel> optionalAccountModel = Optional.empty();
    String title = messages.at(MessageKey.TITLE_CREATE_CLIENT.getKeyName());
    String pageHeader = messages.at(MessageKey.TITLE_CREATE_CLIENT.getKeyName());
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
      title = messages.at(MessageKey.TITLE_EDIT_CLIENT.getKeyName());
      pageHeader = messages.at(MessageKey.TITLE_EDIT_CLIENT.getKeyName());
      pageId = "edit-client";
      successToast = messages.at(MessageKey.BANNER_CLIENT_INFO_UPDATED.getKeyName());
      optionalSuccessMessage = Optional.empty();
    }
    boolean isSuccessfulSave = tiClientInfoForm.isPresent() && !tiClientInfoForm.get().hasErrors();
    HtmlBundle bundle;
    bundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainContent(
                h1(tiGroup.getName()).withClasses(BaseStyles.TI_HEADER_BAND_H1),
                renderTabButtons(messages, TabType.CLIENT_LIST),
                renderSubHeader(pageHeader)
                    .withId(pageId)
                    .withClasses(BaseStyles.TI_HEADER_BAND_H2),
                div(
                        renderBackLink(),
                        renderSuccessAlert(isSuccessfulSave, successToast, optionalSuccessMessage),
                        renderMainContent(
                            isSuccessfulSave,
                            applicantIdOfNewlyAddedClient,
                            messages,
                            tiGroup,
                            optionalAccountModel,
                            request,
                            tiClientInfoForm))
                    .withClasses("px-20"))
            .addMainStyles("bg-white");

    return layout.renderWithNav(request, personalInfo, messages, bundle, applicantIdOfTi);
  }

  private DivTag renderMainContent(
      boolean isSuccessfulSave,
      Long applicantIdOfNewlyAddedClient,
      Messages messages,
      TrustedIntermediaryGroupModel tiGroup,
      Optional<AccountModel> optionalAccountModel,
      Http.Request request,
      Optional<Form<TiClientInfoForm>> tiClientInfoForm) {
    boolean isSaved = isSuccessfulSave && (applicantIdOfNewlyAddedClient != null);
    if (isSaved)
      return div()
          .with(
              renderApplicationsStartButton(applicantIdOfNewlyAddedClient, messages),
              renderBackToClientListButton(messages));
    else {
      return div()
          .with(
              requiredFieldsExplanationContent(),
              renderAddOrEditClientForm(
                  tiGroup, optionalAccountModel, request, tiClientInfoForm, messages));
    }
  }

  private String getTiLink() {
    return baseUrl
        + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                /* nameQuery= */ Optional.empty(),
                /* dayQuery= */ Optional.empty(),
                /* monthQuery= */ Optional.empty(),
                /* yearQuery= */ Optional.empty(),
                /* page= */ Optional.of(1))
            .url();
  }

  private ATag renderBackToClientListButton(Messages messages) {
    return new ATag()
        .withClasses("usa-button usa-button--outline")
        .withId("back-to-client-list")
        .withHref(getTiLink())
        .withText(messages.at(MessageKey.BUTTON_BACK_TO_CLIENT_LIST.getKeyName()));
  }

  private ATag renderApplicationsStartButton(Long applicantId, Messages messages) {
    return new ATag()
        .withClasses("usa-button")
        .withId("applications-start-button")
        .withText(messages.at(MessageKey.BUTTON_START_APP.getKeyName()))
        .withHref(
            controllers.applicant.routes.ApplicantProgramsController.indexWithApplicantId(
                    applicantId)
                .url());
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
    LinkElement link =
        new LinkElement()
            .setStyles("underline")
            .setHref(getTiLink())
            .setIcon(Icons.ARROW_LEFT, LinkElement.IconPosition.START)
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
                    messages.at(MessageKey.PHONE_NUMBER_LABEL.getKeyName())
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
    FieldWithLabel dateOfBirthField =
        setStateIfPresent(
            FieldWithLabel.date()
                .setId("date-of-birth-input")
                .setFieldName("dob")
                .setLabelText(messages.at(MessageKey.DOB_LABEL.getKeyName()))
                .setRequired(true)
                .setValue(getDefaultDob(optionalApplicantData)),
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
                    firstNameField.getUSWDSInputTag(),
                    middleNameField.getUSWDSInputTag(),
                    lastNameField.getUSWDSInputTag(),
                    phoneNumberField.getUSWDSInputTag(),
                    emailField.getUSWDSInputTag(),
                    dateOfBirthField.getUSWDSInputTag(),
                    tiNoteField.getUSWDSTextareaTag(),
                    makeCsrfTokenInputTag(request),
                    submitButton(messages.at(MessageKey.BUTTON_SAVE.getKeyName()))
                        .withClasses("usa-button"),
                    asRedirectElement(
                        button(messages.at(MessageKey.BUTTON_CANCEL.getKeyName()))
                            .withClasses("usa-button usa-button--outline", "m-2"),
                        cancelUrl))
                .withClasses("w-1/2", "mt-6"));
  }

  private String getDefaultDob(Optional<ApplicantData> optionalApplicantData) {
    return optionalApplicantData.isPresent()
        ? optionalApplicantData
            .get()
            .getDateOfBirth()
            .map(this.dateConverter::formatIso8601Date)
            .orElse("")
        : "";
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

    if (optionalForm.get().error(key).isPresent()) {
      field.setFieldErrors(messages, optionalForm.get().errors(key));
    }

    return field;
  }
}
