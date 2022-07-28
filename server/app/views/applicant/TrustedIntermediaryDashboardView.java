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
import java.time.LocalDate;
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
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.ti.TrustedIntermediaryGroupListView;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.*;

/** Renders a page for a trusted intermediary to manage their clients. */
public class TrustedIntermediaryDashboardView extends BaseHtmlView {
  private final ApplicantLayout layout;

  @Inject
  public TrustedIntermediaryDashboardView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
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
                h2(tiGroup.getDescription()).withClasses(Styles.ML_2),
                hr(),
                renderHeader("Add Client"),
                renderAddNewForm(tiGroup, request),
                hr().withClasses(Styles.MT_6),
                renderHeader("Clients"),
                renderSearchForm(request, searchParameters),
                renderTIApplicantsTable(
                    managedAccounts, searchParameters, page, totalPageCount, request),
                hr().withClasses(Styles.MT_6),
                renderHeader("Trusted Intermediary Members"),
                renderTIMembersTable(tiGroup).withClasses(Styles.ML_2))
            .addMainStyles(Styles.PX_2, Styles.MAX_W_SCREEN_XL, Styles.MX_AUTO);

    if (request.flash().get("error").isPresent()) {
      LoggerFactory.getLogger(TrustedIntermediaryGroupListView.class)
          .info(request.flash().get("error").get());
      String error = request.flash().get("error").get();
      bundle.addToastMessages(
          ToastMessage.error(error)
              .setId("warning-message-ti-add-fill")
              .setIgnorable(false)
              .setDuration(0));
    }
    return layout.renderWithNav(request, userName, messages, bundle);
  }

  private FormTag renderSearchForm(Http.Request request, SearchParameters searchParameters) {
    return form()
        .withClass(Styles.W_1_4)
        .withMethod("GET")
        .withAction(
            routes.TrustedIntermediaryController.dashboard(
                    Optional.empty(), Optional.empty(), Optional.empty())
                .url())
        .with(
            FieldWithLabel.input()
                .setId("search")
                .setFieldName("search")
                .setValue(searchParameters.search().orElse(""))
                .setLabelText("SearchText")
                .getInputTag()
                .withClasses(Styles.W_FULL),
            FieldWithLabel.date()
                .setId("search-date")
                .setFieldName("searchDate")
                .setValue(searchParameters.searchDate().orElse(""))
                .setLabelText("Search Date of Birth")
                .getInputTag()
                .withClass(Styles.W_FULL),
            makeCsrfTokenInputTag(request),
            submitButton("Search").withClasses(Styles.M_2));
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
                    Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.FLEX_AUTO)
                .with(renderApplicantTableHeader())
                .with(
                    tbody(
                        each(
                            managedAccounts.stream()
                                .sorted(Comparator.comparing(Account::getApplicantName))
                                .collect(Collectors.toList()),
                            account -> renderApplicantRow(account, request)))))
            .withClasses(Styles.MB_16);
    return main.with(
        renderPaginationDiv(
            page,
            totalPageCount,
            pageNumber ->
                routes.TrustedIntermediaryController.dashboard(
                    searchParameters.search(),
                    searchParameters.searchDate(),
                    Optional.of(pageNumber))));
  }

  private DivTag renderTIMembersTable(TrustedIntermediaryGroup tiGroup) {
    return div(
        table()
            .withClasses(Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_3_4)
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
                submitButton("Add").withClasses(Styles.ML_2, Styles.MB_6)))
        .withClasses(
            Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_1_2, Styles.MT_6);
  }

  private TrTag renderTIRow(Account ti) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
            Styles.BORDER_B,
            Styles.BORDER_GRAY_300,
            StyleUtils.even(Styles.BG_GRAY_100))
        .with(renderInfoCell(ti))
        .with(renderStatusCell(ti));
  }

  private TrTag renderApplicantRow(Account applicant, Http.Request request) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
            Styles.BORDER_B,
            Styles.BORDER_GRAY_300,
            StyleUtils.even(Styles.BG_GRAY_100))
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
    Optional<LocalDate> currentDob = newestApplicant.get().getApplicantData().getDateOfBirth();
    return td().withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.FONT_SEMIBOLD)
        .with(
            form()
                .withClass(Styles.FLEX)
                .withMethod("POST")
                .withAction(
                    routes.TrustedIntermediaryController.updateDateOfBirth(account.id).url())
                .with(
                    input()
                        .withId("date-of-birth-update")
                        .withName("dob")
                        .withType("date")
                      .withValue(currentDob.isPresent()?currentDob.get().toString():""),
                    makeCsrfTokenInputTag(request),
                    submitButton("Update DOB")
                        .withClasses(Styles.UPPERCASE, Styles.TEXT_XS, Styles.ML_3)));
  }

  private TdTag renderApplicantInfoCell(Account applicantAccount) {
    int applicationCount =
        applicantAccount.getApplicants().stream()
            .map(applicant -> applicant.getApplications().size())
            .collect(Collectors.summingInt(Integer::intValue));
    return td().with(
            div(String.format("Application count: %d", applicationCount))
                .withClasses(Styles.FONT_SEMIBOLD))
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
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  private TdTag renderInfoCell(Account ti) {
    String emailField = ti.getEmailAddress();
    if (Strings.isNullOrEmpty(emailField)) {
      emailField = "(no email address)";
    }
    return td().with(div(ti.getApplicantName()).withClasses(Styles.FONT_SEMIBOLD))
        .with(div(emailField).withClasses(Styles.TEXT_XS))
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  private TdTag renderStatusCell(Account ti) {
    String accountStatus = "OK";
    if (ti.ownedApplicantIds().isEmpty()) {
      accountStatus = "Not yet signed in.";
    }
    return td().with(div(accountStatus).withClasses(Styles.FONT_SEMIBOLD))
        .withClasses(BaseStyles.TABLE_CELL_STYLES);
  }

  private TheadTag renderApplicantTableHeader() {
    return thead(
        tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_4))
            .with(th("Applications").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_4))
            .with(th("Actions").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_4))
            .with(th("Date Of Birth").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_3)));
  }

  private TheadTag renderGroupTableHeader() {
    return thead(
        tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_3))
            .with(th("Status").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_4)));
  }
}
