package views.admin.reporting;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.TableTag;
import j2html.tags.specialized.TbodyTag;
import j2html.tags.specialized.ThTag;
import j2html.tags.specialized.TrTag;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Optional;
import javax.inject.Inject;
import services.DateConverter;
import services.reporting.ApplicationSubmissionsStat;
import views.ViewUtils;
import views.components.Icons;

/** Contains shared rendering logic for reporting tables. */
public final class ReportingTableRenderer {

  // Number format used for displaying application counts
  public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,###,###");
  private final boolean useDeterministicStatsForBrowserTest;
  private final DateConverter dateConverter;

  @Inject
  ReportingTableRenderer(Config config, DateConverter dateConverter) {
    this.dateConverter = checkNotNull(dateConverter);
    this.useDeterministicStatsForBrowserTest =
        config.getBoolean("reporting_use_deterministic_stats");
  }

  /**
   * We use browser snapshot tests to assert this page rendering correctly. The submission times can
   * vary a bit, so we stub the durations so the page renders deterministically.
   */
  public String renderDurationWithTestStubbing(double durationSeconds) {
    if (useDeterministicStatsForBrowserTest) {
      return "11:11:11";
    }

    return ReportingTableRenderer.renderDuration(durationSeconds);
  }

  public static String renderDuration(double durationSeconds) {
    Duration duration = Duration.ofSeconds((long) durationSeconds);

    long days = duration.toDaysPart();
    long hours = duration.toHoursPart();
    int minutes = duration.toMinutesPart();
    int seconds = duration.toSecondsPart();

    StringBuilder result = new StringBuilder();

    if (days > 0) {
      result.append(days);
      result.append(":");
    }

    result.append(String.format("%02d:%02d:%02d", hours, minutes, seconds));

    return result.toString();
  }

  public DivTag renderTable(
      String title, ImmutableList<ReportingTableHeader> headers, Iterable<TrTag> dataRows) {
    DivTag content =
        div(h2(title)).withClasses("shadow border border-gray-300 rounded-lg p-6 mb-10");

    TbodyTag tbodyTag = tbody();

    TrTag headerRow = tr();
    for (ReportingTableHeader header : headers) {
      ThTag thTag = th(header.headerText()).withClasses("text-left pr-2 whitespace-nowrap");

      header
          .helpText()
          .ifPresent(helpText -> thTag.with(ViewUtils.makeSvgToolTip(helpText, Icons.INFO)));

      headerRow.with(thTag);
    }

    TableTag table =
        table().withClasses("border-collapse table-auto w-full").with(thead(headerRow), tbodyTag);

    for (TrTag dataRow : dataRows) {
      tbodyTag.with(dataRow.withClasses("even:bg-slate-100"));
    }

    DivTag tableInnerContainer = div(table).withClasses("shadow-sm overflow-hidden my-8");
    DivTag tableOuterContainer = div(tableInnerContainer).withClasses("relative rounded-xl");

    return content.with(tableOuterContainer);
  }

  public String getDisplayMonth(ApplicationSubmissionsStat stat) {
    if (useDeterministicStatsForBrowserTest) {
      return "MM/YY";
    }

    return dateConverter.renderAsTwoDigitMonthAndYear(stat.timestamp().get());
  }

  /** Represents a column header in a reporting view. */
  @AutoValue
  public abstract static class ReportingTableHeader {
    public static ReportingTableHeader create(String headerText) {
      return new AutoValue_ReportingTableRenderer_ReportingTableHeader(
          headerText, /* helpText= */ Optional.empty());
    }

    public static ReportingTableHeader create(String headerText, String helpText) {
      return new AutoValue_ReportingTableRenderer_ReportingTableHeader(
          headerText, Optional.of(helpText));
    }

    /** The text in the header for this column. */
    public abstract String headerText();

    /** Optional help text that provides more detail on the data in the column. */
    public abstract Optional<String> helpText();
  }
}
