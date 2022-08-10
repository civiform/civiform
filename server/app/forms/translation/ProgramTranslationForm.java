package forms.translation;

import java.util.Locale;
import services.program.ProgramDefinition;

/** Form for updating translation for programs. */
public class ProgramTranslationForm {

  private String displayName;
  private String displayDescription;

  public ProgramTranslationForm() {
    this.displayName = "";
    this.displayDescription = "";
  }

  /** Initializes a ProgramStatusesForm from the given status object. */
  public static ProgramTranslationForm fromProgram(ProgramDefinition program, Locale locale) {
    ProgramTranslationForm form = new ProgramTranslationForm();
    form.setDisplayName(program.localizedName().maybeGet(locale).orElse(""));
    form.setDisplayDescription(program.localizedDescription().maybeGet(locale).orElse(""));
    return form;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayDescription() {
    return displayDescription;
  }

  public void setDisplayDescription(String displayDescription) {
    this.displayDescription = displayDescription;
  }
}
