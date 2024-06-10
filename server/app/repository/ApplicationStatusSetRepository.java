package repository;

import io.ebean.DB;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import models.ApplicationStatusesModel;
import models.StatusLifecycleStage;

/** A repository class used to interact with the application_statuses table. */
public final class ApplicationStatusSetRepository {
  private final Database database;

  @Inject
  public ApplicationStatusSetRepository() {
    this.database = DB.getDefault();
  }

  public Optional<ApplicationStatusesModel> lookupActiveStatuDefinitions(String programName) {
    return database
        .find(ApplicationStatusesModel.class)
        .setLabel("ApplicationStatusesModel.findByProgramName")
        .where()
        .eq("program_name", programName)
        .and()
        .eq("status_lifecycle_stage", StatusLifecycleStage.ACTIVE)
        .findOneOrEmpty();
  }

  public List<ApplicationStatusesModel> lookupListOfObsoleteStatusDefinitions(String programName) {
    return database
        .find(ApplicationStatusesModel.class)
        .setLabel("ApplicationStatusesModel.findByProgramName")
        .where()
        .eq("program_name", programName)
        .and()
        .eq("status_lifecycle_stage", StatusLifecycleStage.OBSOLETE)
        .findList();
  }
}
