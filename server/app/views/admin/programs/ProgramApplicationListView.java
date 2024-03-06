package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.iframe;
import static j2html.TagCreator.input;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.TagCreator;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.SpanTag;
import java.util.Optional;
import models.ApplicationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.SubmittedApplicationFilter;
import services.DateConverter;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.UrlUtils;
import services.applicant.ApplicantService;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;
import services.settings.SettingsManifest;
import views.ApplicantUtils;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.SelectWithLabel;
import views.components.ToastMessage;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page for viewing applications to a program. */
public final class ProgramApplicationListView extends BaseHtmlView {
  private static final String FROM_DATE_PARAM = "fromDate";
  private static final String UNTIL_DATE_PARAM = "untilDate";
  private static final String SEARCH_PARAM = "search";
  private static final String APPLICATION_STATUS_PARAM = "applicationStatus";
  private static final String IGNORE_FILTERS_PARAM = "ignoreFilters";

  private final AdminLayout layout;
  private final ApplicantUtils applicantUtils;
  private final ApplicantService applicantService;
  private final DateConverter dateConverter;
  private final SettingsManifest settingsManifest;
  private final Logger log = LoggerFactory.getLogger(ProgramApplicationListView.class);

  @Inject
  public ProgramApplicationListView(
      AdminLayoutFactory layoutFactory,
      ApplicantUtils applicantUtils,
      ApplicantService applicantService,
      DateConverter dateConverter,
      SettingsManifest settingsManifest) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.applicantUtils = checkNotNull(applicantUtils);
    this.applicantService = checkNotNull(applicantService);
    this.dateConverter = checkNotNull(dateConverter);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public Content render(
      Http.Request request,
      CiviFormProfile profile,
      ProgramDefinition program,
      ImmutableList<String> allPossibleProgramApplicationStatuses,
      PageNumberBasedPaginationSpec paginationSpec,
      PaginationResult<ApplicationModel> paginatedApplications,
      RenderFilterParams filterParams,
      Optional<String> selectedApplicationUri) {
    Modal downloadModal = renderDownloadApplicationsModal(program, filterParams);
    boolean hasEligibilityEnabled = program.hasEligibilityEnabled();
    Optional<StatusDefinitions.Status> defaultStatus = program.toProgram().getDefaultStatus();

    DivTag applicationListDiv =
        div()
            .with(
                h1(program.adminName()).withClasses("my-4"),
                renderPaginationDivOldWay(
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
                                /* selectedApplicationUri= */ Optional.empty()))
                    .withClasses("mb-2"),
                br(),
                renderSearchForm(
                    program,
                    allPossibleProgramApplicationStatuses,
                    downloadModal.getButton(),
                    filterParams,
                    request),
                each(
                    paginatedApplications.getPageContents(),
                    application ->
                        renderApplicationListItem(
                            application,
                            /* displayStatus= */ allPossibleProgramApplicationStatuses.size() > 0,
                            hasEligibilityEnabled
                                ? applicantService.getApplicationEligibilityStatus(
                                    application, program)
                                : Optional.empty(),
                            defaultStatus)))
            .withClasses("mt-6", StyleUtils.responsiveLarge("mt-12"), "mb-16", "ml-6", "mr-2");

    DivTag applicationShowDiv =
        div()
            .withClasses("mt-6", StyleUtils.responsiveLarge("mt-12"), "w-full")
            .with(
                iframe()
                    .withName("application-display-frame")
                    // Only allow relative URLs to ensure that we redirect to the same domain.
                    .withSrc(UrlUtils.checkIsRelativeUrl(selectedApplicationUri.orElse("")))
                    .withClasses("w-full", "h-full"));

    HtmlBundle htmlBundle =
        layout
            .setAdminType(profile)
            .getBundle(request)
            .setTitle(program.adminName() + " - Applications")
            .addModals(downloadModal)
            .addMainStyles("flex")
            .addMainContent(makeCsrfTokenInputTag(request), applicationListDiv, applicationShowDiv);

    Optional<String> maybeSuccessMessage = request.flash().get("success");
    if (maybeSuccessMessage.isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(maybeSuccessMessage.get()));
    }
    Optional<String> maybeErrorMessage = request.flash().get("error");
    if (maybeErrorMessage.isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.errorNonLocalized(maybeErrorMessage.get()));
    }
    return layout.renderCentered(htmlBundle);
  }

  private FormTag renderSearchForm(
      ProgramDefinition program,
      ImmutableList<String> allPossibleProgramApplicationStatuses,
      ButtonTag downloadButton,
      RenderFilterParams filterParams,
      Http.Request request) {
    String redirectUrl =
        routes.AdminApplicationController.index(
                program.id(),
                /* search= */ Optional.empty(),
                /* page= */ Optional.empty(),
                /* fromDate= */ Optional.empty(),
                /* untilDate= */ Optional.empty(),
                /* applicationStatus= */ Optional.empty(),
                /* selectedApplicationUri= */ Optional.empty())
            .url();
    String labelText =
        settingsManifest.getPrimaryApplicantInfoQuestionsEnabled(request)
            ? "Search by name, email, phone number, or application ID"
            : "Search by name, email, or application ID";
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
                    /* selectedApplicationUri= */ Optional.empty())
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
                    downloadButton,
                    makeSvgTextButton("Filter", Icons.FILTER_ALT)
                        .withClass(ButtonStyles.SOLID_BLUE_WITH_ICON)
                        .withType("submit"),
                    a("Clear").withHref(redirectUrl).withClass(ButtonStyles.SOLID_BLUE)));
  }

  private Modal renderDownloadApplicationsModal(
      ProgramDefinition program, RenderFilterParams filterParams) {
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
        .setTriggerButtonContent(
            makeSvgTextButton("Download", Icons.DOWNLOAD)
                .withClass(ButtonStyles.OUTLINED_WHITE_WITH_ICON)
                .withType("button"))
        .build();
  }

  private DivTag renderApplicationListItem(
      ApplicationModel application,
      boolean displayStatus,
      Optional<Boolean> maybeEligibilityStatus,
      Optional<StatusDefinitions.Status> defaultStatus) {
    String applicantNameWithApplicationId =
        String.format(
            "%s (%d)",
            applicantUtils.getApplicantNameEnUs(application.getApplicantData().getApplicantName()),
            application.id);
    String viewLinkText = "View →";

    String statusString =
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

    DivTag cardContent =
        div()
            .withClasses("border", "border-gray-300", "bg-white", "rounded", "p-4")
            .with(
                p(applicantNameWithApplicationId)
                    .withClasses(
                        "text-black",
                        "font-bold",
                        "text-xl",
                        "mb-1",
                        ReferenceClasses.BT_APPLICATION_ID))
            .condWith(
                application.getSubmitterEmail().isPresent(),
                p(application.getSubmitterEmail().orElse(""))
                    .withClasses("text-lg", "text-gray-800", "mb-2"))
            .condWith(
                displayStatus,
                p().withClasses("text-sm", "text-gray-700")
                    .with(span("Status: "), span(statusString).withClass("font-semibold")))
            .condWith(
                maybeEligibilityStatus.isPresent(),
                p().withClasses("text-sm", "text-gray-700")
                    .with(
                        span(maybeEligibilityStatus.isPresent() && maybeEligibilityStatus.get()
                                ? "Meets eligibility"
                                : "Doesn't meet eligibility")
                            .withClass("font-semibold")))
            .with(
                div()
                    .withClasses("flex", "text-sm", "w-full")
                    .with(
                        p(renderSubmitTime(application))
                            .withClasses("text-gray-700", "italic", ReferenceClasses.BT_DATE),
                        div().withClasses("flex-grow"),
                        renderViewLink(viewLinkText, application)));

    return div(cardContent)
        .withClasses(ReferenceClasses.ADMIN_APPLICATION_CARD, "w-full", "shadow-lg", "mt-4");
  }

  private SpanTag renderSubmitTime(ApplicationModel application) {
    try {
      return span().withText(dateConverter.renderDateTime(application.getSubmitTime()));
    } catch (NullPointerException e) {
      log.error("Application {} submitted without submission time marked.", application.id);
      return span();
    }
  }

  private ATag renderViewLink(String text, ApplicationModel application) {
    String viewLink =
        controllers.admin.routes.AdminApplicationController.show(
                application.getProgram().id, application.id)
            .url();

    return new LinkElement()
        .setId("application-view-link-" + application.id)
        .setHref(viewLink)
        .setText(text)
        .setStyles("mr-2", ReferenceClasses.VIEW_BUTTON)
        .asAnchorText();
  }

  @AutoValue
  public abstract static class RenderFilterParams {
    public abstract Optional<String> search();

    public abstract Optional<String> fromDate();

    public abstract Optional<String> untilDate();

    public abstract Optional<String> selectedApplicationStatus();

    public static Builder builder() {
      return new AutoValue_ProgramApplicationListView_RenderFilterParams.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setSearch(Optional<String> search);

      public abstract Builder setFromDate(Optional<String> fromDate);

      public abstract Builder setUntilDate(Optional<String> untilDate);

      public abstract Builder setSelectedApplicationStatus(
          Optional<String> selectedApplicationStatus);

      public abstract RenderFilterParams build();
    }
  }
}
