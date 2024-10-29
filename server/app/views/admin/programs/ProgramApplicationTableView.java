package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.option;
import static j2html.TagCreator.select;
import static j2html.TagCreator.span;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import annotations.BindingAnnotations;
import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.TagCreator;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.SelectTag;
import j2html.tags.specialized.SpanTag;
import j2html.tags.specialized.TableTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
import java.util.Optional;
import models.ApplicationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.SubmittedApplicationFilter;
import services.AlertType;
import services.DateConverter;
import services.MessageKey;
import services.applicant.ApplicantService;
import services.pagination.PageNumberPaginationSpec;
import services.pagination.PaginationResult;
import services.program.ProgramDefinition;
import services.statuses.StatusDefinitions;
import views.AlertComponent;
import views.ApplicantUtils;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.SelectWithLabel;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

public class ProgramApplicationTableView extends BaseHtmlView {

  private static final String FROM_DATE_PARAM = "fromDate";
  private static final String UNTIL_DATE_PARAM = "untilDate";
  private static final String SEARCH_PARAM = "search";
  private static final String APPLICATION_STATUS_PARAM = "applicationStatus";
  private static final String IGNORE_FILTERS_PARAM = "ignoreFilters";
  private static final String SHOW_DOWNLOAD_MODAL = "showDownloadModal";

  private final AdminLayout layout;
  private final ApplicantUtils applicantUtils;
  private final ApplicantService applicantService;
  private final DateConverter dateConverter;
  private final Logger log = LoggerFactory.getLogger(ProgramApplicationListView.class);
  private final Messages enUsMessages;

  @Inject
  public ProgramApplicationTableView(
      AdminLayoutFactory layoutFactory,
      ApplicantUtils applicantUtils,
      ApplicantService applicantService,
      DateConverter dateConverter,
      @BindingAnnotations.EnUsLang Messages enUsMessages) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
    this.applicantUtils = checkNotNull(applicantUtils);
    this.applicantService = checkNotNull(applicantService);
    this.dateConverter = checkNotNull(dateConverter);
    this.enUsMessages = checkNotNull(enUsMessages);
  }

  public Content render(
      Http.Request request,
      CiviFormProfile profile,
      ProgramDefinition program,
      StatusDefinitions activeStatusDefinitions,
      ImmutableList<String> allPossibleProgramApplicationStatuses,
      PageNumberPaginationSpec paginationSpec,
      PaginationResult<ApplicationModel> paginatedApplications,
      ProgramApplicationListView.RenderFilterParams filterParams,
      Optional<Boolean> showDownloadModal,
      Optional<String> message) {
    Modal downloadModal =
        renderDownloadApplicationsModal(program, filterParams, showDownloadModal.orElse(false));

    DivTag applicationListDiv =
        div()
            .withData("testid", "application-list")
            .with(
                h1(program.adminName())
                    .withClasses(
                        StyleUtils.responsiveSmall("text-5xl"),
                        "font-semibold",
                        "mt-10",
                        "px-6",
                        "bg-opacity-75",
                        StyleUtils.responsiveSmall("mb-6")),
                br(),
                renderSearchForm(program, allPossibleProgramApplicationStatuses, filterParams),
                div(),
                renderApplicationTable(
                        paginatedApplications.getPageContents(),
                        /* displayStatus= */ allPossibleProgramApplicationStatuses.size() > 0,
                        activeStatusDefinitions,
                        program,
                        request,
                        message)
                    .condWith(
                        paginatedApplications.getNumPages() > 1,
                        renderPagination(
                            paginationSpec.getCurrentPage(),
                            paginatedApplications.getNumPages(),
                            pageNumber ->
                                routes.AdminApplicationController.index(
                                    program.id(),
                                    filterParams.search(),
                                    Optional.of(pageNumber),
                                    filterParams.fromDate(),
                                    filterParams.untilDate(),
                                    filterParams.selectedApplicationStatus(),
                                    /* selectedApplicationUri= */ Optional.empty(),
                                    /* showDownloadModal= */ Optional.empty(),
                                    /* message= */ Optional.empty()),
                            /* optionalMessages */ Optional.empty())));

    HtmlBundle htmlBundle =
        layout
            .setAdminType(profile)
            .getBundle(request)
            .setTitle(program.adminName() + " - Applications")
            .addModals(downloadModal)
            .addMainContent(makeCsrfTokenInputTag(request), applicationListDiv);
    return layout.renderCentered(htmlBundle);
  }

  private FormTag renderSearchForm(
      ProgramDefinition program,
      ImmutableList<String> allPossibleProgramApplicationStatuses,
      ProgramApplicationListView.RenderFilterParams filterParams) {
    String redirectUrl =
        routes.AdminApplicationController.index(
                program.id(),
                /* search= */ Optional.empty(),
                /* page= */ Optional.empty(),
                /* fromDate= */ Optional.empty(),
                /* untilDate= */ Optional.empty(),
                /* applicationStatus= */ Optional.empty(),
                /* selectedApplicationUri= */ Optional.empty(),
                /* showDownloadModal= */ Optional.empty(),
                /* message= */ Optional.empty())
            .url();
    String labelText = "Search by name, email, phone number, or application ID";
    return form()
        .withClasses("mt-6")
        .attr("data-override-disable-submit-on-enter")
        .withMethod("GET")
        .withAction(
            routes.AdminApplicationController.index(
                    program.id(),
                    /* search= */ Optional.empty(),
                    /* page= */ Optional.empty(),
                    /* fromDate= */ Optional.empty(),
                    /* untilDate= */ Optional.empty(),
                    /* applicationStatus= */ Optional.empty(),
                    /* selectedApplicationUri= */ Optional.empty(),
                    /* showDownloadModal= */ Optional.empty(),
                    /* message= */ Optional.empty())
                .url())
        .with(
            fieldset()
                .withClasses("pt-1")
                .with(
                    legend("Applications submitted").withClasses("ml-1", "text-gray-600"),
                    div()
                        .withClasses("flex", "space-x-3")
                        .with(
                            FieldWithLabel.date()
                                .setFieldName(FROM_DATE_PARAM)
                                .setValue(filterParams.fromDate().orElse(""))
                                .setLabelText("from:")
                                .getDateTag()
                                .withClasses("flex"),
                            FieldWithLabel.date()
                                .setFieldName(UNTIL_DATE_PARAM)
                                .setValue(filterParams.untilDate().orElse(""))
                                .setLabelText("until:")
                                .getDateTag()
                                .withClasses("flex"))),
            FieldWithLabel.input()
                .setFieldName(SEARCH_PARAM)
                .setValue(filterParams.search().orElse(""))
                .setLabelText(labelText)
                .getInputTag()
                .withClasses("w-full", "mt-4"))
        .condWith(
            allPossibleProgramApplicationStatuses.size() > 0,
            new SelectWithLabel()
                .setFieldName(APPLICATION_STATUS_PARAM)
                .setLabelText("Application status")
                .setValue(filterParams.selectedApplicationStatus().orElse(""))
                .setOptionGroups(
                    ImmutableList.of(
                        SelectWithLabel.OptionGroup.builder()
                            .setLabel("General")
                            .setOptions(
                                ImmutableList.of(
                                    SelectWithLabel.OptionValue.builder()
                                        .setLabel("Any application status")
                                        .setValue("")
                                        .build(),
                                    SelectWithLabel.OptionValue.builder()
                                        .setLabel("Only applications without a status")
                                        .setValue(
                                            SubmittedApplicationFilter
                                                .NO_STATUS_FILTERS_OPTION_UUID)
                                        .build()))
                            .build(),
                        SelectWithLabel.OptionGroup.builder()
                            .setLabel("Application statuses")
                            .setOptions(
                                ImmutableList.<SelectWithLabel.OptionValue>builder()
                                    .addAll(
                                        allPossibleProgramApplicationStatuses.stream()
                                            .map(
                                                status ->
                                                    SelectWithLabel.OptionValue.builder()
                                                        .setLabel(status)
                                                        .setValue(status)
                                                        .build())
                                            .collect(ImmutableList.toImmutableList()))
                                    .build())
                            .build()))
                .getSelectTag())
        .with(
            div()
                .withClasses("mt-6", "mb-8", "flex", "space-x-2")
                .with(
                    div().withClass("flex-grow"),
                    makeSvgTextButton("Download", Icons.DOWNLOAD)
                        .withClass(ButtonStyles.OUTLINED_WHITE_WITH_ICON)
                        .withName(SHOW_DOWNLOAD_MODAL)
                        .withValue("true")
                        .withType("submit"),
                    makeSvgTextButton("Filter", Icons.FILTER_ALT)
                        .withClass(ButtonStyles.SOLID_BLUE_WITH_ICON)
                        .withType("submit"),
                    a("Clear").withHref(redirectUrl).withClass(ButtonStyles.SOLID_BLUE)));
  }

  private Modal renderDownloadApplicationsModal(
      ProgramDefinition program,
      ProgramApplicationListView.RenderFilterParams filterParams,
      boolean showDownloadModal) {
    String modalId = "download-program-applications-modal";
    DivTag modalContent =
        div()
            .withClasses("px-8")
            .with(
                form()
                    .withMethod("GET")
                    .with(
                        FieldWithLabel.radio()
                            .setFieldName(IGNORE_FILTERS_PARAM)
                            .setLabelText("Current results")
                            .setChecked(true)
                            .getRadioTag(),
                        FieldWithLabel.radio()
                            .setFieldName(IGNORE_FILTERS_PARAM)
                            .setLabelText("All data")
                            .setValue("1")
                            .setChecked(false)
                            .getRadioTag(),
                        input()
                            .withName(FROM_DATE_PARAM)
                            .isHidden()
                            .withValue(filterParams.fromDate().orElse("")),
                        input()
                            .withName(UNTIL_DATE_PARAM)
                            .isHidden()
                            .withValue(filterParams.untilDate().orElse("")),
                        input()
                            .withName(SEARCH_PARAM)
                            .isHidden()
                            .withValue(filterParams.search().orElse("")),
                        input()
                            .withName(APPLICATION_STATUS_PARAM)
                            .isHidden()
                            .withValue(filterParams.selectedApplicationStatus().orElse("")),
                        div()
                            .withClasses("flex", "mt-6", "space-x-2")
                            .with(
                                TagCreator.button("Download CSV")
                                    .withClasses(
                                        ReferenceClasses.DOWNLOAD_ALL_BUTTON,
                                        ReferenceClasses.MODAL_CLOSE,
                                        ButtonStyles.SOLID_BLUE_WITH_ICON)
                                    .withFormaction(
                                        controllers.admin.routes.AdminApplicationController
                                            .downloadAll(
                                                program.id(),
                                                /* search= */ Optional.empty(),
                                                /* fromDate= */ Optional.empty(),
                                                /* untilDate= */ Optional.empty(),
                                                /* applicationStatus= */ Optional.empty(),
                                                /* ignoreFilters= */ Optional.empty())
                                            .url())
                                    .withType("submit"),
                                TagCreator.button("Download JSON")
                                    .withClasses(
                                        ReferenceClasses.DOWNLOAD_ALL_BUTTON,
                                        ReferenceClasses.MODAL_CLOSE,
                                        ButtonStyles.SOLID_BLUE_WITH_ICON)
                                    .withFormaction(
                                        controllers.admin.routes.AdminApplicationController
                                            .downloadAllJson(
                                                program.id(),
                                                /* search= */ Optional.empty(),
                                                /* fromDate= */ Optional.empty(),
                                                /* untilDate= */ Optional.empty(),
                                                /* applicationStatus= */ Optional.empty(),
                                                /* ignoreFilters= */ Optional.empty())
                                            .url())
                                    .withType("submit"))));
    return Modal.builder()
        .setModalId(modalId)
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(modalContent)
        .setModalTitle("Download application data")
        .setDisplayOnLoad(showDownloadModal)
        .build();
  }

  private DivTag renderApplicationTable(
      ImmutableList<ApplicationModel> applications,
      boolean displayStatus,
      StatusDefinitions statusDefinitions,
      ProgramDefinition program,
      Http.Request request,
      Optional<String> message) {
    boolean hasEligibilityEnabled = program.hasEligibilityEnabled();
    SelectTag dropdownTag =
        select()
            .withName("statusText")
            .withClasses(
                "outline-none",
                "px-3",
                "py-2",
                "ml-3",
                "border",
                "border-gray-500",
                "rounded-lg",
                "bg-white",
                "text-lg",
                StyleUtils.focus(BaseStyles.BORDER_CIVIFORM_BLUE));
    dropdownTag.with(option(enUsMessages.at(MessageKey.DROPDOWN_PLACEHOLDER.getKeyName())));
    String latestStatusText = "";
    statusDefinitions.getStatuses().stream()
        .map(StatusDefinitions.Status::statusText)
        .forEach(
            statusText -> {
              boolean isCurrentStatus = statusText.equals(latestStatusText);
              dropdownTag.with(
                  option(statusText).withValue(statusText).withCondSelected(isCurrentStatus));
            });
    DivTag alertTag =
        message.isPresent()
            ? div(AlertComponent.renderFullAlert(
                    AlertType.INFO,
                    /* text= */ message.get(),
                    /* title= */ Optional.of("Status Update"),
                    /* hidden= */ false))
                .withClasses("my-5")
            : div();

    TableTag applicationTable =
        table()
            .withClasses("usa-table usa-table--borderless", "w-full")
            .with(renderGroupTableHeader(displayStatus, hasEligibilityEnabled))
            .with(
                tbody(
                    each(
                        applications,
                        application ->
                            renderApplicationRowItem(
                                application,
                                displayStatus,
                                statusDefinitions.getDefaultStatus(),
                                hasEligibilityEnabled,
                                applicantService.getApplicationEligibilityStatus(
                                    application, program)))));
    if (displayStatus) {

      return div(
          form()
              .withId("bulk-status-update")
              .withMethod("POST")
              .withAction(routes.AdminApplicationController.updateStatuses(program.id()).url())
              .with(
                  div()
                      .withClass("space-x-2")
                      .with(
                          dropdownTag,
                          makeCsrfTokenInputTag(request),
                          label("Send notification"),
                          input()
                              .withType("checkbox")
                              .withName("shouldSendEmail")
                              .withClasses(BaseStyles.CHECKBOX),
                          submitButton("Status change").withClasses("usa-button"),
                          alertTag,
                          applicationTable)));
    }
    return div(applicationTable);
  }

  private TheadTag renderGroupTableHeader(boolean displayStatus, boolean hasEligibilityEnabled) {
    return thead(
        tr().condWith(
                displayStatus,
                th(input()
                        .withName("selectall")
                        .withClasses("has:checked:text-red-500")
                        .withType("checkbox")
                        .withId("selectAll")
                        .withClasses(BaseStyles.CHECKBOX))
                    .withScope("col"))
            .with(th("Name").withScope("col"))
            .condWith(hasEligibilityEnabled, th("Eligibility").withScope("col"))
            .condWith(displayStatus, th("Status").withScope("col"))
            .with(th("Submission date").withScope("col")));
  }

  private TrTag renderApplicationRowItem(
      ApplicationModel application,
      boolean displayStatus,
      Optional<StatusDefinitions.Status> defaultStatus,
      boolean hasEligibilityEnabled,
      Optional<Boolean> maybeEligibilityStatus) {
    String applicantNameWithApplicationId =
        String.format(
            "%s (%d)",
            applicantUtils.getApplicantNameEnUs(
                application.getApplicant().getApplicantDisplayName()),
            application.id);
    String applicationStatus =
        application
            .getLatestStatus()
            .map(
                s ->
                    String.format(
                        "%s%s",
                        s,
                        defaultStatus.map(defaultString -> defaultString.matches(s)).orElse(false)
                            ? " (default)"
                            : ""))
            .orElse("None");
    String eligibilityStatus =
        maybeEligibilityStatus.isPresent() && maybeEligibilityStatus.get()
            ? "Meets eligibility"
            : "Doesn't meet eligibility";

    return tr().withClasses("has:checked:text-red-500")
        .condWith(
            displayStatus,
            td(
                input()
                    .withType("checkbox")
                    .withName("applicationsIds[]")
                    .withId("current-application-selection-" + application.id)
                    .withValue(Long.toString(application.id))
                    .withClasses(BaseStyles.CHECKBOX)))
        .with(td(renderApplicationLink(applicantNameWithApplicationId, application)))
        .condWith(hasEligibilityEnabled, td(eligibilityStatus))
        .condWith(displayStatus, td(applicationStatus))
        .with(td(renderSubmitTime(application)));
  }

  private ATag renderApplicationLink(String text, ApplicationModel application) {
    String viewLink =
        controllers.admin.routes.AdminApplicationController.show(
                application.getProgram().id, application.id)
            .url();

    return new LinkElement()
        .setId("application-view-link-" + application.id)
        .setHref(viewLink)
        .setText(text)
        .opensInNewTab()
        .setStyles(
            "mr-2", ReferenceClasses.VIEW_BUTTON, "underline", ReferenceClasses.BT_APPLICATION_ID)
        .asAnchorText();
  }

  private SpanTag renderSubmitTime(ApplicationModel application) {
    try {
      return span()
          .withText(dateConverter.renderDateTimeHumanReadable(application.getSubmitTime()));
    } catch (NullPointerException e) {
      log.error("Application {} submitted without submission time marked.", application.id);
      return span();
    }
  }
}
