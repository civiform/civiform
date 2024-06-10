package repository;

import io.ebean.DB;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.ApplicationStatusesModel;
import models.StatusLifecycleStage;
import services.program.StatusDefinitions;

/** A repository class used to interact with the application_statuses table. */
public final class ApplicationStatusesRepository {
  private final Database database;

  @Inject
  public ApplicationStatusesRepository() {
    this.database = DB.getDefault();
  }

  public StatusDefinitions lookupActiveStatuDefinitions(String programName) {
    Optional<ApplicationStatusesModel> mayBeapplicationStatusesModel =
        database
            .find(ApplicationStatusesModel.class)
            .setLabel("ApplicationStatusesModel.findByProgramName")
            .where()
            .eq("program_name", programName)
            .and()
            .eq("status_lifecycle_stage", StatusLifecycleStage.ACTIVE)
            .findOneOrEmpty();
    if (mayBeapplicationStatusesModel.isEmpty()) {
      throw new RuntimeException("No active status found for program " + programName);
    }
    return mayBeapplicationStatusesModel.get().getStatusDefinitions();
  }

  public List<StatusDefinitions> lookupListOfObsoleteStatusDefinitions(String programName) {
    List<ApplicationStatusesModel> mayBeapplicationStatusesModelList =
        database
            .find(ApplicationStatusesModel.class)
            .setLabel("ApplicationStatusesModel.findByProgramName")
            .where()
            .eq("program_name", programName)
            .and()
            .eq("status_lifecycle_stage", StatusLifecycleStage.OBSOLETE)
            .findList();
    if (mayBeapplicationStatusesModelList.isEmpty()) {
      throw new RuntimeException("No obsolete status found for program " + programName);
    }
    return mayBeapplicationStatusesModelList.stream()
        .map(ApplicationStatusesModel::getStatusDefinitions)
        .collect(Collectors.toList());
  }
}
