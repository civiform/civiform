package views.admin.migration;

import static j2html.TagCreator.div;
import static views.ViewUtils.makeAlert;
import static views.style.BaseStyles.ALERT_ERROR;

import com.google.inject.Inject;
import j2html.tags.DomContent;
import java.util.Optional;

/** An HTMX partial for portions of the page rendered by {@link AdminImportView}. */
public class AdminImportViewPartial {
  @Inject
  AdminImportViewPartial() {}

  /**
   * The ID for the div containing the imported program data. Must be applied to the top-level DOM
   * element of each partial so that replacement works correctly.
   */
  public static final String PROGRAM_DATA_ID = "program-data";

  /** Renders an error that occurred while trying to parse the program data. */
  public DomContent renderError(String errorMessage) {
    return div()
        .withId(PROGRAM_DATA_ID)
        .with(
            makeAlert(
                /* text= */ errorMessage,
                /* hidden= */ false,
                /* title= */ Optional.of("Error processing file"),
                /* classes...= */ ALERT_ERROR));
  }

  /** Renders the correctly parsed program data. */
  public DomContent renderProgramData(String programData) {
    return div(programData).withId(PROGRAM_DATA_ID);
  }
}
