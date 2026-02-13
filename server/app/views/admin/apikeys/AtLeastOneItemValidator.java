package views.admin.apikeys;

import java.util.List;
import javax.validation.ConstraintValidator;
import play.data.validation.Constraints;
import play.data.validation.Constraints.Validator;
import play.libs.F;

public class AtLeastOneItemValidator extends Validator<List<String>>
    implements ConstraintValidator<Constraints.ValidateWith, List<String>> {

  @Override
  public boolean isValid(List<String> value) {
    return value != null && !value.isEmpty();
  }

  @Override
  public F.Tuple<String, Object[]> getErrorMessageKey() {
    return new F.Tuple<>("error.required", new Object[] {});
  }
}
