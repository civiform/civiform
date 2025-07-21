package forms;

import java.util.OptionalInt;
import services.question.types.MapQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class MapQuestionForm extends QuestionForm {

  private String geoJsonEndpoint;
  private OptionalInt minChoicesRequired;
  private OptionalInt maxChoicesAllowed;

  public MapQuestionForm() {
    super();
    this.geoJsonEndpoint = "";
    this.minChoicesRequired = OptionalInt.empty();
    this.maxChoicesAllowed = OptionalInt.empty();
  }

  public MapQuestionForm(MapQuestionDefinition qd) {
    super(qd);
    this.geoJsonEndpoint = qd.getMapValidationPredicates().geoJsonEndpoint();
    this.minChoicesRequired = qd.getMapValidationPredicates().minChoicesRequired();
    this.maxChoicesAllowed = qd.getMapValidationPredicates().maxChoicesAllowed();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.MAP;
  }

  public String getGeoJsonEndpoint() {
    return geoJsonEndpoint;
  }

  public void setGeoJsonEndpoint(String geoJsonEndpoint) {
    this.geoJsonEndpoint = geoJsonEndpoint;
  }

  public OptionalInt getMinChoicesRequired() {
    return minChoicesRequired;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMinChoicesRequired(String minChoicesRequiredAsString) {
    this.minChoicesRequired =
        minChoicesRequiredAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(minChoicesRequiredAsString));
  }

  public OptionalInt getMaxChoicesAllowed() {
    return maxChoicesAllowed;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMaxChoicesAllowed(String maxChoicesAllowedAsString) {
    this.maxChoicesAllowed =
        maxChoicesAllowedAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(maxChoicesAllowedAsString));
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    MapQuestionDefinition.MapValidationPredicates.Builder predicateBuilder =
        MapQuestionDefinition.MapValidationPredicates.builder();

    predicateBuilder.setGeoJsonEndpoint(getGeoJsonEndpoint());

    if (getMinChoicesRequired().isPresent()) {
      predicateBuilder.setMinChoicesRequired(getMinChoicesRequired());
    }

    if (getMaxChoicesAllowed().isPresent()) {
      predicateBuilder.setMaxChoicesAllowed(getMaxChoicesAllowed());
    }

    return super.getBuilder().setValidationPredicates(predicateBuilder.build());
  }
}
