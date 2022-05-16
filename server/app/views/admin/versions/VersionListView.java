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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import models.LifecycleStage;
import models.Version;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page for viewing all versions. */
public class VersionListView extends BaseHtmlView {

  private final AdminLayout layout;
  private final ZoneId zoneId;

  @Inject
  public VersionListView(AdminLayout layout, Config config, ZoneId zoneId) {
    this.layout = checkNotNull(layout);
    this.zoneId = checkNotNull(zoneId);
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
                div(renderHeader("Current Versions")),
                renderVersionCard(draftVersion),
                renderVersionCard(activeVersion),
                div(renderHeader("Older Versions")),
                div(renderPastVersionTable(olderVersions, request)));

    return layout.renderCentered(htmlBundle);
  }

  private TableTag renderPastVersionTable(
      ImmutableList<Version> olderVersions, Http.Request request) {
    return table()
        .withClasses(Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_FULL)
        .with(renderVersionTableHeaderRow())
        .with(
            tbody(
                each(
                    olderVersions,
                    (olderVersion) -> renderOlderVersionRow(olderVersion, request))));
  }

  private TheadTag renderVersionTableHeaderRow() {
    return thead(
        tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
            .with(
                th("ID").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5),
                th("Publish Time").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5),
                th("Programs").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5),
                th("Questions").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5),
                th("Publish").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5)));
  }

  private TrTag renderOlderVersionRow(Version olderVersion, Http.Request request) {
    return tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
        .with(
            td(olderVersion.id.toString()),
            td(renderDateTime(olderVersion.getSubmitTime(), zoneId)),
            td(String.valueOf(olderVersion.getPrograms().size())),
            td(String.valueOf(olderVersion.getQuestions().size())),
            td(
                new LinkElement()
                    .setId("set-version-live-" + olderVersion.id)
                    .setHref(routes.AdminVersionController.setVersionLive(olderVersion.id).url())
                    .setText("Set Live")
                    .setStyles(Styles.MR_2)
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
                            Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_XL, Styles.MB_2)),
                p().withClasses(Styles.FLEX_GROW),
                div(
                        p("Programs: " + version.getPrograms().size()),
                        p("Questions: " + version.getQuestions().size()))
                    .withClasses(
                        Styles.TEXT_RIGHT,
                        Styles.TEXT_XS,
                        Styles.TEXT_GRAY_700,
                        Styles.MR_2,
                        StyleUtils.applyUtilityClass(StyleUtils.RESPONSIVE_MD, Styles.MR_4)))
            .withClasses(Styles.FLEX);

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
            .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_BASE, Styles.MB_8, Styles.LINE_CLAMP_3);

    DivTag bottomContent =
        div(
            p(String.format("Last updated: " + renderDateTime(version.getSubmitTime(), zoneId)))
                .withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC),
            p().withClasses(Styles.FLEX_GROW));

    DivTag innerDiv =
        div(div(topContent, midContent, bottomContent)
                .withClasses(
                    Styles.BORDER,
                    Styles.BORDER_GRAY_300,
                    Styles.BG_WHITE,
                    Styles.ROUNDED,
                    Styles.P_4))
            .withClasses(
                ReferenceClasses.ADMIN_VERSION_CARD, Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4);
    return innerDiv;
  }
}
