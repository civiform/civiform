package views.admin.programs;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import models.CategoryModel;
import modules.MainModule;
import play.data.validation.Constraints;
import services.program.ProgramDefinition;
import views.admin.BaseViewModel;

@Getter
@Accessors(fluent = true)
public class ProgramMetaDataEdit2PageViewModel implements BaseViewModel {
  private final ProgramEditStatus programEditStatus;
  private final ProgramDefinition program;
  private final String baseUrl;
  private final ImmutableList<CategoryModel> allCategories;

  @Builder
  public ProgramMetaDataEdit2PageViewModel(
      ProgramEditStatus programEditStatus,
      ProgramDefinition program,
      String baseUrl,
      ImmutableList<CategoryModel> allCategories) {
    this.programEditStatus = programEditStatus;
    this.program = program;
    this.baseUrl = baseUrl;
    this.allCategories = allCategories;
    this.name = program.localizedName().getDefault();
  }

  @Constraints.Required(message = "email.tiApplicationSubmittedSubject")
  private String name;

  public String getAbsoluteProgramUrl() {
    var slug = MainModule.SLUGIFIER.slugify(program.adminName());
    return "%s%s"
        .formatted(
            baseUrl, controllers.applicant.routes.ApplicantProgramsController.show(slug).url());
  }

  public String getClassName() {
    return this.getClass().getName();
  }
}
