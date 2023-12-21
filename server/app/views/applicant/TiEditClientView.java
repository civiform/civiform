package views.applicant;

import annotations.BindingAnnotations.EnUsLang;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.ti.routes;
import forms.EditTiClientInfoForm;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.TdTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.SearchParameters;
import services.DateConverter;
import services.applicant.ApplicantData;
import services.applicant.ApplicantPersonalInfo;
import services.ti.TrustedIntermediaryService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.admin.ti.TrustedIntermediaryGroupListView;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.hr;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

/** Renders a page for a trusted intermediary to manage their clients. */
public class TiEditClientView extends BaseHtmlView {
  private final ApplicantLayout layout;
  private final DateConverter dateConverter;
  public static final String OPTIONAL_INDICATOR = " (optional)";
  private final Messages enUsMessages;
  @Inject
  public TiEditClientView(ApplicantLayout layout, DateConverter dateConverter, @EnUsLang Messages enUsMessages) {
    this.layout = checkNotNull(layout);
    this.dateConverter = checkNotNull(dateConverter);
    this.enUsMessages = checkNotNull(enUsMessages);
  }

  public Content render(
      TrustedIntermediaryGroupModel tiGroup,
      ApplicantPersonalInfo personalInfo,
      AccountModel currentAccounts,
      Http.Request request,
      Messages messages,
      Long currentTisApplicantId) {
    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle("CiviForm")
            .addMainContent(
                renderHeader(tiGroup.getName(), "py-12", "mb-0", "bg-gray-50"),
                hr(),
                renderSubHeader("Edit Client").withId("edit-client").withClass("my-4"),
                requiredFieldsExplanationContent(),
                renderEditClientForm(Account,request),
                hr().withClasses("mt-6"))
            .addMainStyles("px-20", "max-w-screen-xl");

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      LoggerFactory.getLogger(TrustedIntermediaryGroupListView.class)
          .info(request.flash().get("error").get());
      bundle.addToastMessages(
          ToastMessage.errorNonLocalized(flash.get("error").get()).setDuration(-1));
    } else if (flash.get("success").isPresent()) {
      bundle.addToastMessages(ToastMessage.success(flash.get("success").get()).setDuration(-1));
    }
    return layout.renderWithNav(request, personalInfo, messages, bundle, currentTisApplicantId);
  }

  private TdTag renderEditClientForm(AccountModel account, Http.Request request, Optional<Form<EditTiClientInfoForm>> editTiClientInfoForm) {
    Boolean showModal = false;
    if(editTiClientInfoForm.isPresent())
    {
      showModal = true;
    }
    DivTag modal =
        ViewUtils.makeUSWDSModal(
            /* body= */ createFormTagForAccount(account, request,editTiClientInfoForm),
            /* elementIdPrefix= */ "edit-" + account.id,
            /* headerText= */ "Edit Client",
            /* linkButtonText= */ "Edit",
            /* hasFooter= */ false,
            /* firstButtonText= */ "Save",
            /* secondButtonText= */ "Cancel", /* showModal= */ showModal);
    return td().with(modal);
  }

  private TdTag renderDateOfBirthCell(AccountModel account) {
    Optional<ApplicantModel> newestApplicant = account.newestApplicant();
    if (newestApplicant.isEmpty()) {
      return td().withClasses(BaseStyles.TABLE_CELL_STYLES);
    }
    String currentDob =
        newestApplicant
            .get()
            .getApplicantData()
            .getDateOfBirth()
            .map(this.dateConverter::formatIso8601Date)
            .orElse("");
    return td().with(div(String.format(currentDob)).withClasses("font-semibold"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }
  private FieldWithLabel setStateIfPresent(
    FieldWithLabel field, Optional<Form<EditTiClientInfoForm>> maybeForm, String key) {
    if (maybeForm.isEmpty()) {
      return field;
    }
    if(maybeForm.get().hasErrors()) {
        Form<EditTiClientInfoForm> form = maybeForm.get();
        switch (key) {
            case TrustedIntermediaryService.FORM_FIELD_NAME_DOB:
                field.setValue(form.get().getDob());
                break;
            case TrustedIntermediaryService.FORM_FIELD_NAME_EMAIL_ADDRESS:
                field.setValue(form.get().getEmailAddress());
                break;
            case TrustedIntermediaryService.FORM_FIELD_NAME_PHONE:
                field.setValue(form.get().getPhoneNumber());
                break;
            case TrustedIntermediaryService.FORM_FIELD_NAME_FIRST_NAME:
                field.setValue(form.get().getFirstName());
                break;
            case TrustedIntermediaryService.FORM_FIELD_NAME_LAST_NAME:
                field.setValue(form.get().getLastName());
                break;

        }
        if (form.error(key).isPresent()) {
            field.setFieldErrors(enUsMessages, form.error(key).get());
        }
    }

    return field;
  }

  private FormTag createFormTagForAccount(AccountModel account, Http.Request request, Optional<Form<EditTiClientInfoForm>> editTiClientInfoForm) {
    ApplicantData applicantData = account.newestApplicant().get().getApplicantData();

    FormTag formTag =
        form()
            .withId("edit-ti")
            .withMethod("POST")
            .withAction(routes.TrustedIntermediaryController.updateClientInfo(account.id).url());
    List<String> names =
        Splitter.onPattern(",").splitToList(applicantData.getApplicantFullName().get());
    FieldWithLabel firstNameField =
      setStateIfPresent(FieldWithLabel.input()
            .setId("modal-first-name-input")
            .setFieldName(TrustedIntermediaryService.FORM_FIELD_NAME_FIRST_NAME)
            .setLabelText("First Name")
            .setRequired(true)
            .setValue(names.get(0)),editTiClientInfoForm,TrustedIntermediaryService.FORM_FIELD_NAME_FIRST_NAME);

    FieldWithLabel middleNameField =
        FieldWithLabel.input()
            .setId("modal-middle-name-input")
            .setFieldName("middleName")
            .setLabelText("Middle Name")
            .setValue(names.get(1));
    FieldWithLabel lastNameField =
      setStateIfPresent(FieldWithLabel.input()
            .setId("modal-last-name-input")
            .setFieldName(TrustedIntermediaryService.FORM_FIELD_NAME_LAST_NAME)
            .setLabelText("Last Name")
            .setRequired(true)
            .setValue(names.get(2)),editTiClientInfoForm,TrustedIntermediaryService.FORM_FIELD_NAME_LAST_NAME);
    FieldWithLabel phoneNumberField =
        setStateIfPresent(FieldWithLabel.input()
            .setId("modal-phone-number-input")
            .setPlaceholderText("(xxx) xxx-xxxx")
            .setFieldName(TrustedIntermediaryService.FORM_FIELD_NAME_PHONE)
            .setLabelText("Phone Number")
            .setValue(applicantData.getPhoneNumber().orElse("")),editTiClientInfoForm,TrustedIntermediaryService.FORM_FIELD_NAME_PHONE);
    FieldWithLabel emailField =
        setStateIfPresent(FieldWithLabel.email()
            .setId("modal-email-input")
            .setFieldName(TrustedIntermediaryService.FORM_FIELD_NAME_EMAIL_ADDRESS)
            .setLabelText("Email Address")
            .setToolTipIcon(Icons.INFO)
            .setToolTipText(
                "Add an email address for your client to receive status updates about their"
                    + " application automatically. Without an email, you or your community-based"
                    + " organization will be responsible for communicating updates to your"
                    + " client.")
            .setValue(account.getEmailAddress()),editTiClientInfoForm,TrustedIntermediaryService.FORM_FIELD_NAME_EMAIL_ADDRESS);
    FieldWithLabel dateOfBirthField =
        setStateIfPresent(FieldWithLabel.date()
            .setId("modal-date-of-birth-input")
            .setFieldName(TrustedIntermediaryService.FORM_FIELD_NAME_DOB)
            .setLabelText("Date Of Birth")
            .setRequired(true)
            .setValue(
                applicantData
                    .getDateOfBirth()
                    .map(this.dateConverter::formatIso8601Date)
                    .orElse("")),editTiClientInfoForm,TrustedIntermediaryService.FORM_FIELD_NAME_DOB);
    FieldWithLabel tiNoteField =
        FieldWithLabel.input()
            .setId("modal-ti-note-input")
            .setFieldName("tiNote")
            .setLabelText("Notes")
            .setValue(account.getTiNote());

    return formTag.with(
        firstNameField.getInputTag(),
        middleNameField.getInputTag(),
        lastNameField.getInputTag(),
        phoneNumberField.getInputTag(),
        emailField.getEmailTag(),
        dateOfBirthField.getDateTag(),
        tiNoteField.getInputTag(),
        makeCsrfTokenInputTag(request),
        submitButton("Save").withId("update-client-save").withClasses("ml-2", "mb-6"));
  }

  private TdTag renderApplicantInfoCell(AccountModel applicantAccount) {
    int applicationCount =
        applicantAccount.getApplicants().stream()
            .map(applicant -> applicant.getApplications().size())
            .collect(Collectors.summingInt(Integer::intValue));
    return td().with(
            div(String.format("Application count: %d", applicationCount))
                .withClasses("font-semibold"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  private TdTag renderActionsCell(AccountModel applicant) {
    Optional<ApplicantModel> newestApplicant = applicant.newestApplicant();
    if (newestApplicant.isEmpty()) {
      return td().withClasses(BaseStyles.TABLE_CELL_STYLES);
    }
    return td().with(
            new LinkElement()
                .setId(String.format("act-as-%d-button", newestApplicant.get().id))
                .setText("Applicant Dashboard ➔")
                .setHref(
                    controllers.applicant.routes.ApplicantProgramsController.index(
                            newestApplicant.get().id)
                        .url())
                .asAnchorText())
        .withClasses(BaseStyles.TABLE_CELL_STYLES, "pr-12");
  }

  private TdTag renderInfoCell(AccountModel ti) {
    String emailField = ti.getEmailAddress();
    if (Strings.isNullOrEmpty(emailField)) {
      emailField = "(no email address)";
    }
    return td().with(div(ti.getApplicantName()).withClasses("font-semibold"))
        .with(div(emailField).withClasses("text-xs", ReferenceClasses.BT_EMAIL))
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  private TdTag renderStatusCell(AccountModel ti) {
    String accountStatus = "OK";
    if (ti.ownedApplicantIds().isEmpty()) {
      accountStatus = "Not yet signed in.";
    }
    return td().with(div(accountStatus).withClasses("font-semibold"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  private TheadTag renderApplicantTableHeader() {
    return thead(
        tr().withClasses("border-b", "bg-gray-200", "text-left")
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/4"))
            .with(th("Applications").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/4"))
            .with(th("Actions").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/4"))
            .with(th("Date Of Birth").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/3"))
            .with(th("Edit").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/4")));
  }

  private TheadTag renderGroupTableHeader() {
    return thead(
        tr().withClasses("border-b", "bg-gray-200", "text-left")
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/3"))
            .with(th("Status").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/4")));
  }
}
