package views.admin.programs;

import controllers.admin.routes;
import java.util.stream.Collectors;
import models.CategoryModel;
import services.program.ProgramDefinition;
import views.ViewUtils.ProgramDisplayType;
import views.components.TextFormatter;

/**
 * This record contains values used by the {@code admin/programs/ProgramHeaderFragment.html} file.
 */
public record ProgramHeader(ProgramDefinition programDefinition, ProgramDisplayType displayType) {
  public String displayTypeName() {
    return switch (displayType) {
      case ACTIVE -> "Active";
      case DRAFT -> "Draft";
      case PENDING_DELETION -> "Archived";
      default -> "";
    };
  }

  public String programName() {
    return programDefinition.localizedName().getDefault();
  }

  public String programDescription() {
    return TextFormatter.formatTextToSanitizedHTML(
        programDefinition.localizedDescription().getDefault(),
        /* preserveEmptyLines= */ false,
        /* addRequiredIndicator= */ false,
        /* ariaLabelForNewTabs= */ "opens in a new tab");
  }

  public String adminNote() {
    return programDefinition.adminDescription();
  }

  public String categories() {
    return programDefinition.categories().isEmpty()
        ? "None"
        : programDefinition.categories().stream()
            .map(CategoryModel::getDefaultName)
            .collect(Collectors.joining(", "));
  }

  public long programId() {
    return programDefinition.id();
  }

  public String editProgramUrl() {
    return routes.AdminProgramController.edit(programDefinition.id(), ProgramEditStatus.EDIT.name())
        .url();
  }
}
