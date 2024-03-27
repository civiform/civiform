package views.admin.importexport;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import services.program.ProgramDefinition;

public final class AdminProgramExportForm {
  private List<Long> programIds;
  private List<String> programNames;

  public AdminProgramExportForm() {}

  public AdminProgramExportForm(ImmutableList<ProgramDefinition> programDefinitions) {
    programIds =
        programDefinitions.stream().map(ProgramDefinition::id).collect(Collectors.toList());
    programNames =
        programDefinitions.stream()
            .map(def -> def.localizedName().getDefault())
            .collect(Collectors.toList());
  }

  public List<Long> getProgramIds() {
    return this.programIds;
  }

  @SuppressWarnings("unused") // Used by FormFactory
  public void setProgramIds(List<Long> programIds) {
    this.programIds = programIds;
  }

  public List<String> getProgramNames() {
    return this.programNames;
  }

  @SuppressWarnings("unused") // Used by FormFactory
  public void setProgramNames(List<String> programNames) {
    this.programNames = programNames;
  }
}
