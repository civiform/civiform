package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.hr;
import static j2html.TagCreator.input;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.ti.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.TdTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
import java.util.Comparator;
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
import services.applicant.ApplicantPersonalInfo;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.ti.TrustedIntermediaryGroupListView;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page for a trusted intermediary to manage their clients. */
public class TrustedIntermediaryDashboardView extends BaseHtmlView {
  private final ApplicantLayout layout;
  private final DateConverter dateConverter;
  public static final String OPTIONAL_INDICATOR = " (optional)";

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
                //                renderHeader("Add Client").withId("add-client"),
                //                requiredFieldsExplanationContent(),

                //                renderAddNewForm(tiGroup, request),
                //                hr().withClasses("mt-6"),
                div(renderHeader("Clients").withClass("mb-0"), createModal(tiGroup, request))
                    .withClasses("flex", "justify-between", "items-center", "mb-4"),
                renderSearchForm(request, searchParameters),
                renderTIApplicantsTable(
                    managedAccounts, searchParameters, page, totalPageCount, request),
                hr().withClasses("mt-6"),
                renderHeader("Trusted Intermediary Members"),
                renderTIMembersTable(tiGroup).withClasses("ml-2"))
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

  private DivTag createModal(TrustedIntermediaryGroupModel tiGroup, Http.Request request) {
    String modalId = "new-client-modal";
    String headingId = "new-client-heading";
    String descriptionId = "new-client-description";

    DivTag modalContent =
        div()
            .withClass("usa-modal")
            .withId(modalId)
            .attr("aria-labelledby", headingId)
            .attr("aria-describedby", descriptionId)
            .with(
                div()
                    .withClass("usa-modal__content")
                    .with(
                        div()
                            .withClasses("mx-4", "usa-modal__main")
                            .with(
                                h2("Add new client")
                                    .withClass("usa-modal__heading")
                                    .withId(headingId))
                            .with(
                                div()
                                    .withClasses("my-6", "usa-prose")
                                    .with(renderAddNewForm(tiGroup, request))
                                    .withId(descriptionId)))
                    //          .with(div().withClass("usa-modal__footer")
                    //            .with(ul().withClass("usa-button-group")
                    //              .with(li().withClass("usa-button-group__item")
                    //
                    // .with(button("Save").withType("button").withClass("usa-button").attr("data-close-modal")))
                    //              .with(li().withClass("usa-button-group__item")
                    //
                    // .with(button("Cancel").withType("button").withClass("usa-button
                    // usa-button--unstyled padding-105 text-center").attr("data-close-modal")))))
                    .with(
                        BaseHtmlView.iconOnlyButton("Close this window")
                            .withClasses(
                                "usa-button usa-modal__close", ButtonStyles.CLEAR_WITH_ICON, "pt-4")
                            .attr("data-close-modal")
                            .with(
                                Icons.svg(Icons.CLOSE)
                                    .withClasses("usa-icon")
                                    .attr("aria-hidden", "true")
                                    .attr("focusable", "false")
                                    .attr("role", "img"))));

    DivTag container =
        div()
            .withClass("margin-y-3")
            .with(
                a("Add new client")
                    .withHref("#" + modalId)
                    .withClasses("usa-button", "bg-blue-600")
                    .attr("aria-controls", modalId)
                    .attr("data-open-modal"))
            .with(modalContent);

    return container;
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
      int totalPageCount,
      Http.Request request) {
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
                            account -> renderApplicantRow(account, request)))))
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

  private FormTag renderAddNewForm(TrustedIntermediaryGroupModel tiGroup, Http.Request request) {
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
    return formTag.with(
        emailField.getEmailTag(),
        firstNameField.getInputTag(),
        middleNameField.getInputTag(),
        lastNameField.getInputTag(),
        dateOfBirthField.getDateTag(),
        makeCsrfTokenInputTag(request),
        div()
            .withClasses("mt-10", "flex", "flex-col")
            .with(
                submitButton("Save").attr("data-close-modal").withClasses(ButtonStyles.SOLID_BLUE),
                button("Cancel")
                    .attr("data-close-modal")
                    .withClasses("mt-2", ButtonStyles.OUTLINED_TRANSPARENT)));
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

  private TrTag renderApplicantRow(AccountModel applicant, Http.Request request) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
            "border-b",
            "border-gray-300",
            StyleUtils.even("bg-gray-100"))
        .with(renderInfoCell(applicant))
        .with(renderApplicantInfoCell(applicant))
        .with(renderActionsCell(applicant))
        .with(renderDateOfBirthCell(applicant, request));
  }

  private TdTag renderDateOfBirthCell(AccountModel account, Http.Request request) {
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
    return td().withClasses(BaseStyles.TABLE_CELL_STYLES, "font-semibold")
        .with(
            form()
                .withClass("flex")
                .withMethod("POST")
                .withAction(
                    routes.TrustedIntermediaryController.updateDateOfBirth(account.id).url())
                .with(
                    input()
                        .withId("date-of-birth-update")
                        .withName("dob")
                        .withType("date")
                        .withValue(currentDob),
                    makeCsrfTokenInputTag(request),
                    submitButton("Update DOB").withClasses("text-xs", "ml-3")));
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
            .with(th("Date Of Birth").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/3")));
  }

  private TheadTag renderGroupTableHeader() {
    return thead(
        tr().withClasses("border-b", "bg-gray-200", "text-left")
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/3"))
            .with(th("Status").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/4")));
  }
}
