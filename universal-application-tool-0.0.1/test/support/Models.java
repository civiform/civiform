package support;

import com.google.common.collect.ImmutableList;
import models.Account;
import models.Applicant;
import models.Application;
import models.BaseModel;
import models.Program;
import models.Question;
import models.StoredFile;
import models.TrustedIntermediaryGroup;
import models.Version;

/**
 * This is just a global constant of the list of models we have so we can truncate them in tests.
 */
public class Models {
  private static final ImmutableList<Class<? extends BaseModel>> MODELS =
      ImmutableList.of(
          Account.class,
          Applicant.class,
          Application.class,
          Program.class,
          Question.class,
          StoredFile.class,
          TrustedIntermediaryGroup.class,
          Version.class);

  /** Get the complete list of ebean models to truncate. */
  public static Class[] modelsToTruncate() {
    return MODELS.toArray(new Class[0]);
  }
}
