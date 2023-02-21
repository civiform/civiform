package views.admin.programs;

import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;

import featureflags.FeatureFlags;
import j2html.tags.specialized.FormTag;
import play.mvc.Http.Request;
import services.program.ProgramDefinition;
import views.BaseHtmlView;

/**
 * Builds a program form for rendering. If the program was previously created, the {@code adminName}
 * field is disabled, since it cannot be edited once set.
 */
abstract class ProgramSettingsFormBuilder extends BaseHtmlView {

  private final FeatureFlags featureFlags;

  ProgramSettingsFormBuilder(FeatureFlags featureFlags) {
    this.featureFlags = featureFlags;
  }

  /** Builds the form using program form data. */
  protected final FormTag buildProgramSettingsForm(Request request, ProgramDefinition program) {
    FormTag formTag = form().withMethod("POST");
    formTag.with(h2("Visible to applicants" + featureFlags.isNongatedEligibilityEnabled(request)));
    return formTag;
  }
}
