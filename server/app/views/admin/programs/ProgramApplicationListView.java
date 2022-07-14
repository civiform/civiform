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
                h1(program.adminName()).withClasses(Styles.MY_4),
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
                    .withClasses(Styles.MB_2),
                br(),
                renderSearchForm(program, downloadModal.getButton(), filterParams),
                each(paginatedApplications.getPageContents(), this::renderApplicationListItem))
            .withClasses(
                Styles.MT_6,
                StyleUtils.responsiveLarge(Styles.MT_12),
                Styles.MB_16,
                Styles.ML_6,
                Styles.MR_2);

    DivTag applicationShowDiv =
        div()
            .withClasses(
                Styles.MT_6, StyleUtils.responsiveLarge(Styles.MT_12), Styles.W_FULL, Styles.H_FULL)
            .with(
                iframe()
                    .withId("application-display-frame")
                    .withClasses(Styles.W_FULL, Styles.H_FULL));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(program.adminName() + " - Applications")
            .addFooterScripts(layout.viewUtils.makeLocalJsTag("admin_applications"))
            .addModals(downloadModal)
            .addMainStyles(Styles.FLEX)
            .addMainContent(applicationListDiv, applicationShowDiv);

    return layout.renderCentered(htmlBundle);
  }

  private FormTag renderSearchForm(
      ProgramDefinition program, ButtonTag downloadButton, RenderFilterParams filterParams) {
    return form()
        .withClasses(Styles.MT_6)
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
                .withClasses(Styles.PT_1)
                .with(
                    legend("Application submitted").withClasses(Styles.ML_1, Styles.TEXT_GRAY_600),
                    div()
                        .withClasses(Styles.FLEX, Styles.SPACE_X_3)
                        .with(
                            FieldWithLabel.date()
                                .setFieldName(FROM_DATE_PARAM)
                                .setValue(filterParams.fromDate().orElse(""))
                                .setLabelText("From:")
                                .getDateTag()
                                .withClasses(Styles.FLEX),
                            FieldWithLabel.date()
                                .setFieldName(UNTIL_DATE_PARAM)
                                .setValue(filterParams.untilDate().orElse(""))
                                .setLabelText("Until:")
                                .getDateTag()
                                .withClasses(Styles.FLEX))),
            FieldWithLabel.input()
                .setFieldName(SEARCH_PARAM)
                .setValue(filterParams.search().orElse(""))
                .setLabelText("Search by name, email, or application ID")
                .getInputTag()
                .withClasses(Styles.W_FULL, Styles.MT_4),
            div()
                .withClasses(Styles.MT_6, Styles.MB_8, Styles.FLEX, Styles.SPACE_X_2)
                .with(
                    div().withClass(Styles.FLEX_GROW),
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
            .withClasses(Styles.PX_8)
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
                            .withClasses(Styles.FLEX, Styles.MT_6, Styles.SPACE_X_2)
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
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4)
            .with(
                p(applicantNameWithApplicationId)
                    .withClasses(Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_XL, Styles.MB_1))
            .condWith(
                application.getSubmitterEmail().isPresent(),
                p(application.getSubmitterEmail().orElse(""))
                    .withClasses(Styles.TEXT_LG, Styles.TEXT_GRAY_800, Styles.MB_2))
            .with(
                div()
                    .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL)
                    .with(
                        p(renderSubmitTime(application))
                            .withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC),
                        p().withClasses(Styles.FLEX_GROW),
                        renderViewLink(viewLinkText, application)));

    return div(cardContent)
        .withClasses(
            ReferenceClasses.ADMIN_APPLICATION_CARD, Styles.W_FULL, Styles.SHADOW_LG, Styles.MT_4);
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
        .setStyles(Styles.MR_2, ReferenceClasses.VIEW_BUTTON)
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
