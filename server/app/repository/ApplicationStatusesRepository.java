package repository;

import io.ebean.DB;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.ApplicationStatusesModel;
import models.StatusDefinitionsLifecycleStage;
import services.program.StatusDefinitions;

/** A repository class used to interact with the application_statuses table. */
public final class ApplicationStatusesRepository {
  private final Database database;

  @Inject
  public ApplicationStatusesRepository() {
    this.database = DB.getDefault();
  }

  public StatusDefinitions lookupActiveStatusDefinitions(String programName) {
    Optional<ApplicationStatusesModel> optionalApplicationStatusesModel =
        database
            .find(ApplicationStatusesModel.class)
            .setLabel("ApplicationStatusesModel.findByProgramName")
            .where()
            .eq("program_name", programName)
            .and()
            .eq("status_definitions_lifecycle_stage", StatusDefinitionsLifecycleStage.ACTIVE)
            .findOneOrEmpty();
    if (optionalApplicationStatusesModel.isEmpty()) {
      throw new RuntimeException("No active status found for program " + programName);
    }
    return optionalApplicationStatusesModel.get().getStatusDefinitions();
  }

  public List<StatusDefinitions> lookupListOfObsoleteStatusDefinitions(String programName) {
    List<ApplicationStatusesModel> optionalApplicationStatusesModelList =
        database
            .find(ApplicationStatusesModel.class)
            .setLabel("ApplicationStatusesModel.findByProgramName")
            .where()
            .eq("program_name", programName)
            .and()
            .eq("status_definitions_lifecycle_stage", StatusDefinitionsLifecycleStage.OBSOLETE)
            .findList();
    if (optionalApplicationStatusesModelList.isEmpty()) {
      throw new RuntimeException("No obsolete status found for program " + programName);
    }
    return optionalApplicationStatusesModelList.stream()
        .map(ApplicationStatusesModel::getStatusDefinitions)
        .collect(Collectors.toList());
  }
  public void updateStatusDefinitions(String programName, StatusDefinitions statusDefinitions) {
    //archive the previous active one
    database.sqlQuery("UPDATE application_statuses SET status_definitions_lifecycle_stage=:obsolete_stage WHERE program_name =:programName AND status_definitions_lifecycle_stage=:active_stage")
      .setLabel("ApplicationStatusRepository.updateStatus")
      .setParameter("obsolete_stage",StatusDefinitionsLifecycleStage.OBSOLETE)
      .setParameter("programName", programName)
      .setParameter("active_stage",StatusDefinitionsLifecycleStage.ACTIVE);
    //create new status
    ApplicationStatusesModel newStatusDefinition = new ApplicationStatusesModel(programName,statusDefinitions,StatusDefinitionsLifecycleStage.ACTIVE);
    newStatusDefinition.save();
  }

}
