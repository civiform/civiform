package models;

import com.google.common.collect.ImmutableList;
import io.ebean.Database;

/**
 * This is just a global constant of the list of models we have so we can truncate them in tests.
 */
public final class Models {
  private static final ImmutableList<Class<? extends BaseModel>> MODELS =
      ImmutableList.of(
          AccountModel.class,
          ApiKeyModel.class,
          ApplicantModel.class,
          ApplicationModel.class,
          ApplicationEventModel.class,
          PersistedDurableJobModel.class,
          ProgramModel.class,
          QuestionModel.class,
          StoredFileModel.class,
          TrustedIntermediaryGroupModel.class,
          VersionModel.class,
          SettingsGroupModel.class,
          ApplicationStatusesModel.class);

  /** Get the complete list of ebean models to truncate. */
  public static void truncate(Database database) {
    database.truncate(MODELS.toArray(new Class[0]));
  }
}
