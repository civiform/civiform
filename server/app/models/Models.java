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
          Applicant.class,
          Application.class,
          ApplicationEvent.class,
          PersistedDurableJob.class,
          ProgramModel.class,
          Question.class,
          StoredFile.class,
          TrustedIntermediaryGroup.class,
          VersionModel.class,
          SettingsGroup.class);

  /** Get the complete list of ebean models to truncate. */
  public static void truncate(Database database) {
    database.truncate(MODELS.toArray(new Class[0]));
  }
}
