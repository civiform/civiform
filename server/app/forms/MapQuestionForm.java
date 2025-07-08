package forms;

import java.util.Optional;
import services.question.types.MapQuestionDefinition;
import services.question.types.QuestionType;

/** Form for updating a map question. */
public class MapQuestionForm extends QuestionForm {

  private Optional<String> geoJSONEndpoint;

  public MapQuestionForm() {
    super();
    this.geoJSONEndpoint = Optional.empty();
  }

  public MapQuestionForm(MapQuestionDefinition qd) {
    super(qd);
    this.geoJSONEndpoint = Optional.empty();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.MAP;
  }

  public Optional<String> getGeoJSONEndpoint() {
    return geoJSONEndpoint;
  }

  public void setGeoJSONEndpoint(Optional<String> geoJSONEndpoint) {
    this.geoJSONEndpoint = geoJSONEndpoint;
  }
}
