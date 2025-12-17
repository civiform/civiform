package forms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import services.program.ProgramType;

/** Form for updating name and description of a program. */
@Getter
@Setter
public final class ProgramForm {
  private String adminName = "";
  private String adminDescription = "";
  private String localizedDisplayName = "";
  private String localizedDisplayDescription = "";
  private String localizedShortDescription = "";
  private String localizedConfirmationMessage = "";
  private String externalLink = "";
  private String displayMode = "";
  private List<String> notificationPreferences = new ArrayList<>();
  private String programTypeValue = "default";

  // Represents whether or not the user has confirmed that they want to change which program is
  // marked as the pre-screener form.
  private Boolean confirmedChangePreScreenerForm = false;
  private Boolean eligibilityIsGating = true;
  private Boolean loginOnly = false;
  private List<Long> tiGroups = new ArrayList<>();
  private List<Long> categories = new ArrayList<>();
  private List<Map<String, String>> applicationSteps = new ArrayList<>();

  public ProgramType getProgramType() {
    return ProgramType.fromValue(programTypeValue);
  }
}
