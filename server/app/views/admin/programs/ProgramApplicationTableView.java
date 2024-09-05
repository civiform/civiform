package views.admin.programs;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import j2html.tags.specialized.DivTag;


import j2html.tags.specialized.TrTag;

import play.mvc.Http;
import play.twirl.api.Content;
import controllers.admin.routes;


import services.program.ProgramApplicationTableRow;
import services.program.ProgramDefinition;

import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;

import views.components.LinkElement;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

import static com.google.common.base.Preconditions.checkNotNull;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;

import static j2html.TagCreator.form;
import static j2html.TagCreator.input;

import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

public class ProgramApplicationTableView extends BaseHtmlView {

  private final AdminLayout layout;

  @Inject
  public ProgramApplicationTableView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
  }
  public Content render(
    Http.Request request,
    CiviFormProfile profile,
    ProgramDefinition program,
    ImmutableList<String> allPossibleProgramApplicationStatuses,
    ImmutableList<ProgramApplicationTableRow> programApplicationTableRows ){
    DivTag applicationTable = div(createApplicationsTable(programApplicationTableRows,allPossibleProgramApplicationStatuses)).withClasses("w-full");
    HtmlBundle htmlBundle =
      layout
        .setAdminType(profile)
        .getBundle(request)
        .setTitle(program.adminName())
        .addMainStyles("flex")
        .addMainContent(makeCsrfTokenInputTag(request),applicationTable);
    return layout.renderCentered(htmlBundle);

  }
  private j2html.tags.specialized.TheadTag renderGroupTableHeader(boolean displayStatus) {
    return thead(
      tr()  .with(th(input().withName("selectall").withClasses("has:checked:text-red-500").withType("checkbox").withClasses(BaseStyles.CHECKBOX)).withScope("col"))
        .with(th("Name").withScope("col"))
        .with(th("Eligibility").withScope("col"))
        .condWith(displayStatus, th("Status").withScope("col"))
        .with(th("Submission date").withScope("col")));
  }
  private DivTag createApplicationsTable(  ImmutableList<ProgramApplicationTableRow> programApplicationTableRows,  ImmutableList<String> allPossibleProgramApplicationStatuses){
    boolean displayStatus = allPossibleProgramApplicationStatuses.size() > 0;
    DivTag table =
        div(form()
          .withId("bulk-status-update")
          .withMethod("POST")
         // .withAction(routes.AdminApplicationController.updateStatuses(program.id()).url())
          .with(
            table()
              .withClasses("usa-table usa-table--borderless", "w-full")
              .with(renderGroupTableHeader(displayStatus))
              .with(
                tbody(
                  each(
                    programApplicationTableRows,
                    applicationRow ->
                      renderApplicationRowItem(
                        applicationRow,displayStatus)))))
          );
    return table;
  }
  private TrTag renderApplicationRowItem(
    ProgramApplicationTableRow applicationRow, boolean displayStatus) {
    String applicantNameWithApplicationId =
      String.format(
        "%s (%d)",
        applicationRow.applicantName(),
        applicationRow.applicationId());
    return tr().withClasses("has:checked:text-red-500")
      .with(
        td(
          input()
            .withType("checkbox")
            .withName("applicationsIds[]")
            .withValue(Long.toString(applicationRow.applicationId()))
            .withClasses(BaseStyles.CHECKBOX)))
      .with(td(renderApplicationLink(applicantNameWithApplicationId, applicationRow)))
      .with(td(applicationRow.eligibilityStatus()))
      .condWith(displayStatus, td(applicationRow.applicationStatus()))
      .with(td(applicationRow.submitTime()));
  }
  private j2html.tags.specialized.ATag renderApplicationLink(
    String text,  ProgramApplicationTableRow applicationRow) {
    String viewLink =
      controllers.admin.routes.AdminApplicationController.show(
          applicationRow.applicationProgramId(), applicationRow.applicationId())
        .url();

    return new LinkElement()
      .setId("application-view-link-" + applicationRow.applicationId())
      .setHref(viewLink)
      .setText(text)
      .setStyles(
        "mr-2", ReferenceClasses.VIEW_BUTTON, "underline", ReferenceClasses.BT_APPLICATION_ID)
      .asAnchorText();
  }

}
