package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
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
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.SearchParameters;
import services.DateConverter;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.ti.TrustedIntermediaryGroupListView;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;


/** Renders a page for a trusted intermediary to manage their clients. */
public class TrustedIntermediaryDashboardView extends BaseHtmlView {
  private final ApplicantLayout layout;
  private final DateConverter dateConverter;

  @Inject
  public TrustedIntermediaryDashboardView(ApplicantLayout layout, DateConverter dateConverter) {
    this.layout = checkNotNull(layout);
    this.dateConverter = checkNotNull(dateConverter);
  }

  public Content render(
      TrustedIntermediaryGroup tiGroup,
      Optional<String> userName,
      ImmutableList<Account> managedAccounts,
      int totalPageCount,
      int page,
      SearchParameters searchParameters,
      Http.Request request,
      Messages messages) {
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle("CiviForm")
            .addMainContent(
                renderHeader(tiGroup.getName()),
                h2(tiGroup.getDescription()).withClasses("ml-2"),
                hr(),
                renderHeader("Add Client"),
                renderAddNewForm(tiGroup, request),
                hr().withClasses("mt-6"),
                renderHeader("Clients"),
                renderSearchForm(request, searchParameters),
                renderTIApplicantsTable(
                    managedAccounts, searchParameters, page, totalPageCount, request),
                hr().withClasses("mt-6"),
                renderHeader("Trusted Intermediary Members"),
                renderTIMembersTable(tiGroup).withClasses("ml-2"))
            .addMainStyles("px-2", "max-w-screen-xl", "mx-auto");

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      LoggerFactory.getLogger(TrustedIntermediaryGroupListView.class)
          .info(request.flash().get("error").get());
      bundle.addToastMessages(ToastMessage.error(flash.get("error").get()).setDuration(-1));
    } else if (flash.get("success").isPresent()) {
      bundle.addToastMessages(ToastMessage.success(flash.get("success").get()).setDuration(-1));
    }
    return layout.renderWithNav(request, userName, messages, bundle);
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
      ImmutableList<Account> managedAccounts,
      SearchParameters searchParameters,
      int page,
      int totalPageCount,
      Http.Request request) {
    DivTag main =
        div(table()
                .withClasses(
                    "border", "border-gray-300", "shadow-md", "flex-auto")
                .with(renderApplicantTableHeader())
                .with(
                    tbody(
                        each(
                            managedAccounts.stream()
                                .sorted(Comparator.comparing(Account::getApplicantName))
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

  private DivTag renderTIMembersTable(TrustedIntermediaryGroup tiGroup) {
    return div(
        table()
            .withClasses("border", "border-gray-300", "shadow-md", "w-3/4")
            .with(renderGroupTableHeader())
            .with(
                tbody(
                    each(
                        tiGroup.getTrustedIntermediaries().stream()
                            .sorted(Comparator.comparing(Account::getApplicantName))
                            .collect(Collectors.toList()),
                        account -> renderTIRow(account)))));
  }

  private DivTag renderAddNewForm(TrustedIntermediaryGroup tiGroup, Http.Request request) {
    FormTag formTag =
        form()
            .withMethod("POST")
            .withAction(routes.TrustedIntermediaryController.addApplicant(tiGroup.id).url());
    FieldWithLabel firstNameField =
        FieldWithLabel.input()
            .setId("first-name-input")
            .setFieldName("firstName")
            .setLabelText("First Name")
            .setValue(request.flash().get("providedFirstName").orElse(""))
            .setPlaceholderText("Applicant first name (Required)");
    FieldWithLabel middleNameField =
        FieldWithLabel.input()
            .setId("middle-name-input")
            .setFieldName("middleName")
            .setLabelText("Middle Name")
            .setValue(request.flash().get("providedMiddleName").orElse(""))
            .setPlaceholderText("Applicant middle name (Optional)");
    FieldWithLabel lastNameField =
        FieldWithLabel.input()
            .setId("last-name-input")
            .setFieldName("lastName")
            .setLabelText("Last Name")
            .setValue(request.flash().get("providedLastName").orElse(""))
            .setPlaceholderText("Applicant last name (Required)");
    // TODO: do something with this field.  currently doesn't do anything. Add a Path
    // to WellKnownPaths referencing the canonical date of birth question.
    FieldWithLabel dateOfBirthField =
        FieldWithLabel.date()
            .setId("date-of-birth-input")
            .setFieldName("dob")
            .setLabelText("Date Of Birth")
            .setValue(request.flash().get("providedDob").orElse(""))
            .setPlaceholderText("Applicant Date of Birth");
    FieldWithLabel emailField =
        FieldWithLabel.input()
            .setId("email-input")
            .setFieldName("emailAddress")
            .setLabelText("Email Address")
            .setValue(request.flash().get("providedEmail").orElse(""))
            .setPlaceholderText(
                "Email address (if provided, applicant can access account by logging in)");
    return div()
        .with(
            formTag.with(
                emailField.getInputTag(),
                firstNameField.getInputTag(),
                middleNameField.getInputTag(),
                lastNameField.getInputTag(),
                dateOfBirthField.getDateTag(),
                makeCsrfTokenInputTag(request),
                submitButton("Add").withClasses("ml-2", "mb-6")))
        .withClasses(
            "border", "border-gray-300", "shadow-md", "w-1/2", "mt-6");
  }

  private TrTag renderTIRow(Account ti) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
            "border-b",
            "border-gray-300",
            StyleUtils.even("bg-gray-100"))
        .with(renderInfoCell(ti))
        .with(renderStatusCell(ti));
  }

  private TrTag renderApplicantRow(Account applicant, Http.Request request) {
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

  private TdTag renderDateOfBirthCell(Account account, Http.Request request) {
    Optional<Applicant> newestApplicant = account.newestApplicant();
    if (newestApplicant.isEmpty()) {
      return td().withClasses(BaseStyles.TABLE_CELL_STYLES);
    }
    String currentDob =
        newestApplicant
            .get()
            .getApplicantData()
            .getDateOfBirth()
            .map(localDate -> this.dateConverter.formatIso8601Date(localDate))
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
                    submitButton("Update DOB")
                        .withClasses("uppercase", "text-xs", "ml-3")));
  }

  private TdTag renderApplicantInfoCell(Account applicantAccount) {
    int applicationCount =
        applicantAccount.getApplicants().stream()
            .map(applicant -> applicant.getApplications().size())
            .collect(Collectors.summingInt(Integer::intValue));
    return td().with(
            div(String.format("Application count: %d", applicationCount))
                .withClasses("font-semibold"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  private TdTag renderActionsCell(Account applicant) {
    Optional<Applicant> newestApplicant = applicant.newestApplicant();
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

  private TdTag renderInfoCell(Account ti) {
    String emailField = ti.getEmailAddress();
    if (Strings.isNullOrEmpty(emailField)) {
      emailField = "(no email address)";
    }
    return td().with(div(ti.getApplicantName()).withClasses("font-semibold"))
        .with(div(emailField).withClasses("text-xs"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  private TdTag renderStatusCell(Account ti) {
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
