package views.admin.versions;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.p;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.TableTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import models.LifecycleStage;
import models.Version;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DateConverter;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.LinkElement;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page for viewing all versions. */
public class VersionListView extends BaseHtmlView {

  private final AdminLayout layout;
  private final DateConverter dateConverter;

  @Inject
  public VersionListView(
      AdminLayoutFactory layoutFactory, Config config, DateConverter dateConverter) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.VERSIONS);
    this.dateConverter = checkNotNull(dateConverter);
  }

  public Content render(List<Version> allVersions, Http.Request request) {
    Optional<Version> draftVersion =
        allVersions.stream()
            .filter(version -> version.getLifecycleStage().equals(LifecycleStage.DRAFT))
            .findAny();
    Optional<Version> activeVersion =
        allVersions.stream()
            .filter(version -> version.getLifecycleStage().equals(LifecycleStage.ACTIVE))
            .findAny();
    ImmutableList<Version> olderVersions =
        allVersions.stream()
            .filter(version -> version.getLifecycleStage().equals(LifecycleStage.OBSOLETE))
            // Sort from newest to oldest. IDs are DB-generated and increment monotonically.
            .sorted((a, b) -> a.id.compareTo(b.id))
            .collect(ImmutableList.toImmutableList())
            .reverse();

    String title = "Program Versions";
    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                renderHeader("Current Versions"),
                renderVersionCard(draftVersion),
                renderVersionCard(activeVersion),
                renderHeader("Older Versions"),
                renderPastVersionTable(olderVersions, request));

    return layout.renderCentered(htmlBundle);
  }

  private TableTag renderPastVersionTable(
      ImmutableList<Version> olderVersions, Http.Request request) {
    return table()
        .withClasses("border", "border-gray-300", "shadow-md", "w-full")
        .with(renderVersionTableHeaderRow())
        .with(
            tbody(
                each(
                    olderVersions,
                    (olderVersion) -> renderOlderVersionRow(olderVersion, request))));
  }

  private TheadTag renderVersionTableHeaderRow() {
    return thead(
        tr().withClasses("border-b", "bg-gray-200", "text-left")
            .with(
                th("ID").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-2/5"),
                th("Publish Time").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-2/5"),
                th("Programs").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-2/5"),
                th("Questions").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-2/5"),
                th("Publish").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-2/5")));
  }

  private TrTag renderOlderVersionRow(Version olderVersion, Http.Request request) {
    return tr().withClasses("border-b", "bg-gray-200", "text-left")
        .with(
            td(olderVersion.id.toString()),
            td(dateConverter.renderDateTime(olderVersion.getSubmitTime())),
            td(String.valueOf(olderVersion.getPrograms().size())),
            td(String.valueOf(olderVersion.getQuestions().size())),
            td(
                new LinkElement()
                    .setId("set-version-live-" + olderVersion.id)
                    .setHref(routes.AdminVersionController.setVersionLive(olderVersion.id).url())
                    .setText("Set Live")
                    .setStyles("mr-2")
                    .asHiddenForm(request)));
  }

  private DivTag renderVersionCard(Optional<Version> versionMaybe) {
    if (versionMaybe.isEmpty()) {
      return div();
    }
    Version version = versionMaybe.get();
    DivTag topContent =
        div(
                div(
                    div(String.format("%s: Version %d", version.getLifecycleStage(), version.id))
                        .withClasses(
                            "text-black", "font-bold", "text-xl", "mb-2")),
                p().withClasses("flex-grow"),
                div(
                        p("Programs: " + version.getPrograms().size()),
                        p("Questions: " + version.getQuestions().size()))
                    .withClasses(
                        "text-right",
                        "text-xs",
                        "text-gray-700",
                        "mr-2",
                        StyleUtils.applyUtilityClass(StyleUtils.RESPONSIVE_MD, "mr-4")))
            .withClasses("flex");

    String listOfPrograms =
        version.getPrograms().stream()
            .limit(5)
            .map(program -> program.getProgramDefinition().adminName())
            .collect(
                Collectors.joining(
                    ", ",
                    "Contains: (",
                    version.getPrograms().size() > 5
                        ? String.format("... + %d more)", version.getPrograms().size() - 5)
                        : ")"));

    DivTag midContent =
        div(listOfPrograms)
            .withClasses("text-gray-700", "text-base", "mb-8", "line-clamp-3");

    DivTag bottomContent =
        div(
            p(String.format(
                    "Last updated: " + dateConverter.renderDateTime(version.getSubmitTime())))
                .withClasses("text-gray-700", "italic"),
            p().withClasses("flex-grow"));

    DivTag innerDiv =
        div(div(topContent, midContent, bottomContent)
                .withClasses(
                    "border",
                    "border-gray-300",
                    "bg-white",
                    "rounded",
                    "p-4"))
            .withClasses(
                ReferenceClasses.ADMIN_VERSION_CARD, "w-full", "shadow-lg", "mb-4");
    return innerDiv;
  }
}
