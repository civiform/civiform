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
          ApiBridgeConfigurationModel.class,
          ApplicantModel.class,
          ApplicationModel.class,
          ApplicationEventModel.class,
          CategoryModel.class,
          PersistedDurableJobModel.class,
          ProgramModel.class,
          QuestionModel.class,
          StoredFileModel.class,
          TrustedIntermediaryGroupModel.class,
          VersionModel.class,
          SettingsGroupModel.class,
          ApplicationStatusesModel.class,
          GeoJsonDataModel.class);

  /** Get the complete list of ebean models to truncate. */
  public static void truncate(Database database) {
    // Truncate the relational tables we don't want to have models for.
    // Do them first just in case something slips in before the second truncate.
    database.truncate("programs_categories", "versions_programs", "versions_questions");
    database.truncate(MODELS.toArray(new Class[0]));
  }
}
