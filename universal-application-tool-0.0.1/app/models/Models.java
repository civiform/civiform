package models;

import com.google.common.collect.ImmutableList;

public class Models {
  public static final ImmutableList<Class<? extends BaseModel>> MODELS =
      ImmutableList.of(
          Account.class,
          Applicant.class,
          Application.class,
          Program.class,
          Question.class,
          StoredFile.class,
          TrustedIntermediaryGroup.class,
          Version.class);
}
