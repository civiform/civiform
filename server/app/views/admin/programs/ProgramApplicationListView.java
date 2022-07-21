package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
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

import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.TagCreator;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.SpanTag;
import java.util.Optional;
import models.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DateConverter;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.program.ProgramDefinition;
import views.ApplicantUtils;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.style.AdminStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page for viewing applications to a program. */
public final class ProgramApplicationListView extends BaseHtmlView {
  private static final String FROM_DATE_PARAM = "fromDate";
  private static final String UNTIL_DATE_PARAM = "untilDate";
  private static final String SEARCH_PARAM = "search";
  private static final String IGNORE_FILTERS_PARAM = "ignoreFilters";

  private final AdminLayout layout;
  private final ApplicantUtils applicantUtils;
  private final DateConverter dateConverter;
  private final Logger log = LoggerFactory.getLogger(ProgramApplicationListView.class);

  @Inject
  public ProgramApplicationListView(
      AdminLayoutFactory layoutFactory,
      ApplicantUtils applicantUtils,
      DateConverter dateConverter) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS).setOnlyProgramAdminType();
    this.applicantUtils = checkNotNull(applicantUtils);
    this.dateConverter = checkNotNull(dateConverter);
  }

  public Content render(
      Http.Request request,
      ProgramDefinition program,
      PageNumberBasedPaginationSpec paginationSpec,
      PaginationResult<Application> paginatedApplications,
      RenderFilterParams filterParams) {

    Modal downloadModal = renderDownloadApplicationsModal(program, filterParams);
    DivTag applicationListDiv =
        div()
            .with(
                h1(program.adminName()).withClasses("my-4"),
                renderPaginationDiv(
                        paginationSpec.getCurrentPage(),
                        paginatedApplications.getNumPages(),
                        pageNumber ->
                            routes.AdminApplicationController.index(
                                program.id(),
                                filterParams.search(),
                                Optional.of(pageNumber),
                                filterParams.fromDate(),
                                filterParams.untilDate()))
                    .withClasses("mb-2"),
                br(),
                renderSearchForm(program, downloadModal.getButton(), filterParams),
                each(paginatedApplications.getPageContents(), this::renderApplicationListItem))
            .withClasses(
                "mt-6",
                StyleUtils.responsiveLarge("mt-12"),
                "mb-16",
                "ml-6",
                "mr-2");

    DivTag applicationShowDiv =
        div()
            .withClasses(
                "mt-6", StyleUtils.responsiveLarge("mt-12"), "w-full", "h-full")
            .with(
                iframe()
                    .withId("application-display-frame")
                    .withClasses("w-full", "h-full"));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(program.adminName() + " - Applications")
            .addFooterScripts(layout.viewUtils.makeLocalJsTag("admin_applications"))
            .addModals(downloadModal)
            .addMainStyles("flex")
            .addMainContent(applicationListDiv, applicationShowDiv);

    return layout.renderCentered(htmlBundle);
  }

  private FormTag renderSearchForm(
      ProgramDefinition program, ButtonTag downloadButton, RenderFilterParams filterParams) {
    return form()
        .withClasses("mt-6")
        .withMethod("GET")
        .withAction(
            routes.AdminApplicationController.index(
                    program.id(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty())
                .url())
        .with(
            fieldset()
                .withClasses("pt-1")
                .with(
                    legend("Application submitted").withClasses("ml-1", "text-gray-600"),
                    div()
                        .withClasses("flex", "space-x-3")
                        .with(
                            FieldWithLabel.date()
                                .setFieldName(FROM_DATE_PARAM)
                                .setValue(filterParams.fromDate().orElse(""))
                                .setLabelText("From:")
                                .getDateTag()
                                .withClasses("flex"),
                            FieldWithLabel.date()
                                .setFieldName(UNTIL_DATE_PARAM)
                                .setValue(filterParams.untilDate().orElse(""))
                                .setLabelText("Until:")
                                .getDateTag()
                                .withClasses("flex"))),
            FieldWithLabel.input()
                .setFieldName(SEARCH_PARAM)
                .setValue(filterParams.search().orElse(""))
                .setLabelText("Search by name, email, or application ID")
                .getInputTag()
                .withClasses("w-full", "mt-4"),
            div()
                .withClasses("mt-6", "mb-8", "flex", "space-x-2")
                .with(
                    div().withClass("flex-grow"),
                    downloadButton,
                    makeSvgTextButton("Filter", Icons.FILTER_ALT)
                        .withClass(AdminStyles.PRIMARY_BUTTON_STYLES)
                        .withType("submit")));
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
                            .withValue(filterParams.fromDate().orElse(""))
                            .withType("hidden"),
                        input()
                            .withName(UNTIL_DATE_PARAM)
                            .withValue(filterParams.untilDate().orElse(""))
                            .withType("hidden"),
                        input()
                            .withName(SEARCH_PARAM)
                            .withValue(filterParams.search().orElse(""))
                            .withType("hidden"),
                        div()
                            .withClasses("flex", "mt-6", "space-x-2")
                            .with(
                                TagCreator.button("Download CSV")
                                    .withClasses(
                                        ReferenceClasses.DOWNLOAD_ALL_BUTTON,
                                        AdminStyles.PRIMARY_BUTTON_STYLES)
                                    .withFormaction(
                                        controllers.admin.routes.AdminApplicationController
                                            .downloadAll(
                                                program.id(),
                                                Optional.empty(),
                                                Optional.empty(),
                                                Optional.empty(),
                                                Optional.empty())
                                            .url())
                                    .withType("submit"),
                                TagCreator.button("Download JSON")
                                    .withClasses(
                                        ReferenceClasses.DOWNLOAD_ALL_BUTTON,
                                        AdminStyles.PRIMARY_BUTTON_STYLES)
                                    .withFormaction(
                                        controllers.admin.routes.AdminApplicationController
                                            .downloadAllJson(
                                                program.id(),
                                                Optional.empty(),
                                                Optional.empty(),
                                                Optional.empty(),
                                                Optional.empty())
                                            .url())
                                    .withType("submit"))));
    return Modal.builder(modalId, modalContent)
        .setModalTitle("Download application data")
        .setTriggerButtonContent(
            makeSvgTextButton("Download", Icons.DOWNLOAD)
                .withClass(AdminStyles.SECONDARY_BUTTON_STYLES)
                .withType("button"))
        .build();
  }

  private DivTag renderApplicationListItem(Application application) {
    String applicantNameWithApplicationId =
        String.format(
            "%s (%d)",
            applicantUtils.getApplicantNameEnUs(application.getApplicantData().getApplicantName()),
            application.id);
    String viewLinkText = "View →";

    DivTag cardContent =
        div()
            .withClasses(
                "border", "border-gray-300", "bg-white", "rounded", "p-4")
            .with(
                p(applicantNameWithApplicationId)
                    .withClasses("text-black", "font-bold", "text-xl", "mb-1"))
            .condWith(
                application.getSubmitterEmail().isPresent(),
                p(application.getSubmitterEmail().orElse(""))
                    .withClasses("text-lg", "text-gray-800", "mb-2"))
            .with(
                div()
                    .withClasses("flex", "text-sm", "w-full")
                    .with(
                        p(renderSubmitTime(application))
                            .withClasses("text-gray-700", "italic"),
                        p().withClasses("flex-grow"),
                        renderViewLink(viewLinkText, application)));

    return div(cardContent)
        .withClasses(
            ReferenceClasses.ADMIN_APPLICATION_CARD, "w-full", "shadow-lg", "mt-4");
  }

  private SpanTag renderSubmitTime(Application application) {
    try {
      return span().withText(dateConverter.renderDateTime(application.getSubmitTime()));
    } catch (NullPointerException e) {
      log.error("Application {} submitted without submission time marked.", application.id);
      return span();
    }
  }

  private ATag renderViewLink(String text, Application application) {
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

    public static Builder builder() {
      return new AutoValue_ProgramApplicationListView_RenderFilterParams.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setSearch(Optional<String> search);

      public abstract Builder setFromDate(Optional<String> fromDate);

      public abstract Builder setUntilDate(Optional<String> untilDate);

      public abstract RenderFilterParams build();
    }
  }
}
