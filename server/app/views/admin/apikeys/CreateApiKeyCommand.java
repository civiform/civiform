package views.admin.apikeys;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import play.data.validation.Constraints;

@Getter
@Setter
public class CreateApiKeyCommand { // } implements Constraints.Validatable<List<ValidationError>> {

  @Constraints.Required private String csrfToken;

  @Constraints.Required
  @Constraints.MinLength(1)
  private String keyName;

  @Constraints.Required private String expiration;

  @Constraints.Required private String subnet;

  @Constraints.ValidateWith(
      value = AtLeastOneItemValidator.class,
      message = "At least one grant program must be selected")
  private List<String> grantProgramRead;

  //  @Override
  //  public List<ValidationError> validate() {
  //    List<ValidationError> errors = new ArrayList<>();
  //
  //    if (grantProgramRead == null || grantProgramRead.isEmpty()) {
  //      errors.add(new ValidationError("grantProgramRead", "At least one grant program must be
  // selected"));
  //    }
  //
  //    return errors.isEmpty() ? null : errors;
  //  }
}
