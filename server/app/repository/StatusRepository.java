package repository;

import io.ebean.DB;
import io.ebean.Database;
import models.LifecycleStage;
import models.StatusesModel;
import models.VersionModel;
import services.program.StatusDefinitions;

import java.util.Optional;

public class StatusRepository {
  private final Database database;

  public StatusRepository(){
    this.database = DB.getDefault();
  }

  public Optional<StatusesModel> getLatestStatus(String programName){
    StatusesModel statusesModel = database
      .find(StatusesModel.class)
      .where()
      .eq("name", programName)
      .and()
      .eq("isarchieved",false)
      .setLabel("StatusRepository.getLatestStatus")
      .findOne();
    return Optional.ofNullable(statusesModel);
  }
  public StatusesModel updateStatus(String programName, StatusDefinitions statusDefinitions){
    database.sqlQuery("Update statuses set isarchieved = true where name = :programName and isarchieved = false")
      .setLabel("StatusRepository.updateStatus")
      .setParameter("programName", programName);
    StatusesModel newStatus = new StatusesModel(programName,statusDefinitions,false);
    return newStatus;
  }
  public StatusesModel createStatus(String programName)
  {
    StatusDefinitions statusDefinitions = new StatusDefinitions();
    StatusesModel statusesModel =  new StatusesModel(programName,statusDefinitions,false);
    return statusesModel;
  }

}
