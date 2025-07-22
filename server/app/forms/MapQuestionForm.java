package forms;

import java.util.OptionalInt;
import services.question.types.MapQuestionDefinition;
import services.question.types.MapQuestionDefinition.MapValidationPredicates;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class MapQuestionForm extends QuestionForm {

  private String geoJsonEndpoint;
  private OptionalInt maxLocationSelections;

  public MapQuestionForm() {
    super();
    this.geoJsonEndpoint = "";
    this.maxLocationSelections = OptionalInt.empty();
  }

  public MapQuestionForm(MapQuestionDefinition qd) {
    super(qd);
    this.geoJsonEndpoint = qd.getMapValidationPredicates().geoJsonEndpoint();
    this.maxLocationSelections = qd.getMapValidationPredicates().maxLocationSelections();
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

  public OptionalInt getMaxLocationSelections() {
    return maxLocationSelections;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMaxLocationSelections(String maxLocationSelectionsAsString) {
    this.maxLocationSelections =
        maxLocationSelectionsAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(maxLocationSelectionsAsString));
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    MapValidationPredicates.Builder predicateBuilder =
        MapValidationPredicates.builder();

    predicateBuilder.setGeoJsonEndpoint(getGeoJsonEndpoint());

    if (getMaxLocationSelections().isPresent()) {
      predicateBuilder.setMaxLocationSelections(getMaxLocationSelections());
    }

    return super.getBuilder().setValidationPredicates(predicateBuilder.build());
  }
}
