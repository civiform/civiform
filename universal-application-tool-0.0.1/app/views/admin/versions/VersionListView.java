package views.admin.versions;

import static j2html.TagCreator.body;
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
import controllers.admin.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import models.LifecycleStage;
import models.Version;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class VersionListView extends BaseHtmlView {

  private final AdminLayout layout;

  @Inject
  public VersionListView(AdminLayout layout) {
    this.layout = layout;
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
            .collect(ImmutableList.toImmutableList());
    ContainerTag bodyContent =
        body(
            renderHeader("Current Versions"),
            renderVersionCard(draftVersion),
            renderVersionCard(activeVersion),
            renderHeader("Older Versions"),
            renderPastVersionTable(olderVersions, request));

    return layout.render(bodyContent);
  }

  private String renderDateTime(Instant time) {
    LocalDateTime datetime = LocalDateTime.ofInstant(time, ZoneId.of("America/Los_Angeles"));
    return datetime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd 'at' h:mm a"));
  }

  private Tag renderPastVersionTable(ImmutableList<Version> olderVersions, Http.Request request) {
    return table()
        .withClasses(Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_FULL)
        .with(renderVersionTableHeaderRow())
        .with(
            tbody(
                each(
                    olderVersions,
                    (olderVersion) -> renderOlderVersionRow(olderVersion, request))));
  }

  private Tag renderVersionTableHeaderRow() {
    return thead(
        tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
            .with(
                th("ID").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5),
                th("Publish Time").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5),
                th("Programs").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5),
                th("Questions").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5),
                th("Publish").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5)));
  }

  private Tag renderOlderVersionRow(Version olderVersion, Http.Request request) {
    return tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
        .with(
            td(olderVersion.id.toString()),
            td(renderDateTime(olderVersion.getSubmitTime())),
            td(String.valueOf(olderVersion.getPrograms().size())),
            td(String.valueOf(olderVersion.getQuestions().size())),
            td(
                new LinkElement()
                    .setId("view-question-link-" + olderVersion.id)
                    .setHref(routes.AdminVersionController.setVersionLive(olderVersion.id).url())
                    .setText("Set Live")
                    .setStyles(Styles.MR_2)
                    .asHiddenForm(request)));
  }

  private Tag renderVersionCard(Optional<Version> versionMaybe) {
    if (versionMaybe.isEmpty()) {
      return div();
    }
    Version version = versionMaybe.get();
    Tag topContent =
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

    Tag midContent =
        div(listOfPrograms)
            .withClasses(
                Styles.TEXT_GRAY_700,
                Styles.TEXT_BASE,
                Styles.MB_8,
                "line-clamp-3" /* TODO: Add tailwind plugin for line clamping. */);

    Tag bottomContent =
        div(
            p(String.format("Last updated: " + renderDateTime(version.getSubmitTime())))
                .withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC),
            p().withClasses(Styles.FLEX_GROW));

    Tag innerDiv =
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
