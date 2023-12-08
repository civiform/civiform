package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.hr;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.ti.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.TdTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.SearchParameters;
import services.DateConverter;
import services.applicant.ApplicantData;
import services.applicant.ApplicantPersonalInfo;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.ti.TrustedIntermediaryGroupListView;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page for a trusted intermediary to manage their clients. */
public class TrustedIntermediaryDashboardView extends BaseHtmlView {
  private final ApplicantLayout layout;
  private final DateConverter dateConverter;
  public static final String OPTIONAL_INDICATOR = " (optional)";
  private static final String DEFAULT_CLIENT_MODAL_CONTENT = "No client selected.";
  private final List<Modal> editClientModals = new ArrayList<>();

  @Inject
  public TrustedIntermediaryDashboardView(ApplicantLayout layout, DateConverter dateConverter) {
    this.layout = checkNotNull(layout);
    this.dateConverter = checkNotNull(dateConverter);
  }

  public Content render(
      TrustedIntermediaryGroupModel tiGroup,
      ApplicantPersonalInfo personalInfo,
      ImmutableList<AccountModel> managedAccounts,
      int totalPageCount,
      int page,
      SearchParameters searchParameters,
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
                renderHeader("Add Client").withId("add-client"),
                requiredFieldsExplanationContent(),
                renderAddNewForm(tiGroup, request),
                hr().withClasses("mt-6"),
                renderHeader("Clients"),
                renderSearchForm(request, searchParameters),
                renderTIApplicantsTable(managedAccounts, searchParameters, page, totalPageCount),
                hr().withClasses("mt-6"),
                renderHeader("Trusted Intermediary Members"),
                renderTIMembersTable(tiGroup).withClasses("ml-2"))
            .addMainStyles("px-20", "max-w-screen-xl")
            .addModals(generateModals(managedAccounts, request));

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

  private List<Modal> generateModals(
      ImmutableList<AccountModel> managedAccounts, Http.Request request) {
    for (AccountModel account : managedAccounts) {
      ApplicantData applicantData = account.newestApplicant().get().getApplicantData();
      FormTag formTag =
          form()
              .withId("edit-ti")
              .withMethod("POST")
              .withAction(routes.TrustedIntermediaryController.updateClientInfo(account.id).url());
      List<String> names =
          Splitter.onPattern(",").splitToList(applicantData.getApplicantFullName().get());
      FieldWithLabel firstNameField =
          FieldWithLabel.input()
              .setId("first-name-input")
              .setFieldName("firstName")
              .setLabelText("First Name")
              .setRequired(true)
              .setValue(names.get(0));
      FieldWithLabel middleNameField =
          FieldWithLabel.input()
              .setId("middle-name-input")
              .setFieldName("middleName")
              .setLabelText("Middle Name")
              .setValue(names.get(1));
      FieldWithLabel lastNameField =
          FieldWithLabel.input()
              .setId("last-name-input")
              .setFieldName("lastName")
              .setLabelText("Last Name")
              .setRequired(true)
              .setValue(names.get(2));
      FieldWithLabel phoneNumberField =
          FieldWithLabel.input()
              .setId("current-phone-number-input")
              .setPlaceholderText("(xxx) xxx-xxxx")
              .setFieldName("phoneNumber")
              .setLabelText("Phone Number")
              .setValue(applicantData.getPhoneNumber().orElse(""));
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
      FieldWithLabel tiNoteField =
          FieldWithLabel.input()
              .setId("ti-note-input")
              .setFieldName("tiNote")
              .setLabelText("Notes")
              .setValue(account.getTiNote());
      editClientModals.add(
          Modal.builder()
              .setModalId("edit-" + account.id + "-modal")
              .setLocation(Modal.Location.ADMIN_FACING)
              .setContent(
                  div()
                      .with(
                          formTag.with(
                              firstNameField.getInputTag(),
                              middleNameField.getInputTag(),
                              lastNameField.getInputTag(),
                              phoneNumberField.getInputTag(),
                              emailField.getEmailTag(),
                              dateOfBirthField.getDateTag(),
                              tiNoteField.getInputTag(),
                              makeCsrfTokenInputTag(request),
                              submitButton("Save").withClasses("ml-2", "mb-6"))))
              .setModalTitle("Edit Client")
              .setWidth(Modal.Width.THIRD)
              .build());
    }
    return editClientModals;
  }

  private FormTag renderSearchForm(Http.Request request, SearchParameters searchParameters) {
    return form()
        .withClass("w-1/4")
        .withMethod("GET")
        .withAction(
            routes.TrustedIntermediaryController.dashboard(
                    /* paramName=  nameQuery */
                    Optional.empty(),
                    /* paramName=  dateQuery */
                    Optional.empty(),
                    /* paramName=  page */
                    Optional.empty())
                .url())
        .with(
            FieldWithLabel.input()
                .setId("name-query")
                .setFieldName("nameQuery")
                .setValue(searchParameters.nameQuery().orElse(""))
                .setLabelText("Search by Name")
                .getInputTag()
                .withClasses("w-full"),
            FieldWithLabel.date()
                .setId("search-date")
                .setFieldName("dateQuery")
                .setValue(searchParameters.dateQuery().orElse(""))
                .setLabelText("Search Date of Birth")
                .getInputTag()
                .withClass("w-full"),
            makeCsrfTokenInputTag(request),
            submitButton("Search").withClasses("m-2"));
  }

  private DivTag renderTIApplicantsTable(
      ImmutableList<AccountModel> managedAccounts,
      SearchParameters searchParameters,
      int page,
      int totalPageCount) {
    DivTag main =
        div(table()
                .withClasses("border", "border-gray-300", "shadow-md", "flex-auto")
                .with(renderApplicantTableHeader())
                .with(
                    tbody(
                        each(
                            managedAccounts.stream()
                                .sorted(Comparator.comparing(AccountModel::getApplicantName))
                                .collect(Collectors.toList()),
                            account -> renderApplicantRow(account)))))
            .withClasses("mb-16");
    return main.with(
        renderPaginationDiv(
            page,
            totalPageCount,
            pageNumber ->
                routes.TrustedIntermediaryController.dashboard(
                    searchParameters.nameQuery(),
                    searchParameters.dateQuery(),
                    Optional.of(pageNumber))));
  }

  private DivTag renderTIMembersTable(TrustedIntermediaryGroupModel tiGroup) {
    return div(
        table()
            .withClasses("border", "border-gray-300", "shadow-md", "w-3/4")
            .with(renderGroupTableHeader())
            .with(
                tbody(
                    each(
                        tiGroup.getTrustedIntermediaries().stream()
                            .sorted(Comparator.comparing(AccountModel::getApplicantName))
                            .collect(Collectors.toList()),
                        this::renderTIRow))));
  }

  private DivTag renderAddNewForm(TrustedIntermediaryGroupModel tiGroup, Http.Request request) {
    FormTag formTag =
        form()
            .withMethod("POST")
            .withAction(routes.TrustedIntermediaryController.addApplicant(tiGroup.id).url());
    FieldWithLabel firstNameField =
        FieldWithLabel.input()
            .setId("first-name-input")
            .setFieldName("firstName")
            .setLabelText("First Name")
            .setRequired(true)
            .setValue(request.flash().get("providedFirstName").orElse(""));
    FieldWithLabel middleNameField =
        FieldWithLabel.input()
            .setId("middle-name-input")
            .setFieldName("middleName")
            .setLabelText("Middle Name" + OPTIONAL_INDICATOR)
            .setValue(request.flash().get("providedMiddleName").orElse(""));
    FieldWithLabel lastNameField =
        FieldWithLabel.input()
            .setId("last-name-input")
            .setFieldName("lastName")
            .setLabelText("Last Name")
            .setRequired(true)
            .setValue(request.flash().get("providedLastName").orElse(""));
    // TODO: do something with this field.  currently doesn't do anything. Add a Path
    // to WellKnownPaths referencing the canonical date of birth question.
    FieldWithLabel dateOfBirthField =
        FieldWithLabel.date()
            .setId("date-of-birth-input")
            .setFieldName("dob")
            .setLabelText("Date Of Birth")
            .setRequired(true)
            .setValue(request.flash().get("providedDob").orElse(""));
    FieldWithLabel emailField =
        FieldWithLabel.email()
            .setId("email-input")
            .setFieldName("emailAddress")
            .setLabelText("Email Address" + OPTIONAL_INDICATOR)
            .setToolTipIcon(Icons.INFO)
            .setToolTipText(
                "Add an email address for your client to receive status updates about their"
                    + " application automatically. Without an email, you or your community-based"
                    + " organization will be responsible for communicating updates to your"
                    + " client.")
            .setValue(request.flash().get("providedEmail").orElse(""));
    return div()
        .with(
            formTag.with(
                emailField.getEmailTag(),
                firstNameField.getInputTag(),
                middleNameField.getInputTag(),
                lastNameField.getInputTag(),
                dateOfBirthField.getDateTag(),
                makeCsrfTokenInputTag(request),
                submitButton("Add").withClasses("ml-2", "mb-6")))
        .withClasses("border", "border-gray-300", "shadow-md", "w-1/2", "mt-6");
  }

  private TrTag renderTIRow(AccountModel ti) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
            "border-b",
            "border-gray-300",
            StyleUtils.even("bg-gray-100"))
        .with(renderInfoCell(ti))
        .with(renderStatusCell(ti));
  }

  private TrTag renderApplicantRow(AccountModel applicant) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
            "border-b",
            "border-gray-300",
            StyleUtils.even("bg-gray-100"))
        .with(renderInfoCell(applicant))
        .with(renderApplicantInfoCell(applicant))
        .with(renderActionsCell(applicant))
        .with(renderDateOfBirthCell(applicant))
        .with(renderUpdateClientInfoCell(applicant));
  }

  private TdTag renderUpdateClientInfoCell(AccountModel account) {
    Modal modal = getModal(account.id);
    return td().with(
            a().with(
                    button("Edit")
                        .withId("edit" + account.id + "modal")
                        .withClasses("text-xs", "ml-3")
                        .withId(modal.getTriggerButtonId())));
  }

  private Modal getModal(Long id) {
    for (Modal modal : editClientModals) {
      if (modal.modalId().contains(id.toString())) {
        return modal;
      }
    }
    return Modal.builder()
        .setModalId("edit-modal")
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(div().with(h2(DEFAULT_CLIENT_MODAL_CONTENT)))
        .setModalTitle("Edit Client")
        .setWidth(Modal.Width.THIRD)
        .build();
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
