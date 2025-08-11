package forms;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import services.question.types.MapQuestionDefinition;
import services.question.types.MapQuestionDefinition.MapValidationPredicates;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

// TODO(#11001): Add settings for filters
public class MapQuestionForm extends QuestionForm {

  public static final String LOCATION_NAME_DISPLAY = "Name";
  public static final String LOCATION_ADDRESS_DISPLAY = "Address";
  public static final String LOCATION_DETAILS_URL_DISPLAY = "URL";
  public static final List<String> DEFAULT_MAP_QUESTION_KEYS =
      Arrays.asList(LOCATION_NAME_DISPLAY, LOCATION_ADDRESS_DISPLAY, LOCATION_DETAILS_URL_DISPLAY);
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
   * Sets the maximum number of locations an applicant can select.
   *
   * <p>We use a string parameter here so that if the field is empty (i.e., unset), we can correctly
   * set it to an empty {@code OptionalInt}.
   *
   * @param maxLocationSelectionsAsString the max number of locations an applicant can select
   */
  public void setMaxLocationSelections(String maxLocationSelectionsAsString) {
    this.maxLocationSelections =
        maxLocationSelectionsAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(maxLocationSelectionsAsString));
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    MapValidationPredicates.Builder predicateBuilder = MapValidationPredicates.builder();

    predicateBuilder.setGeoJsonEndpoint(getGeoJsonEndpoint());

    if (getMaxLocationSelections().isPresent()) {
      predicateBuilder.setMaxLocationSelections(getMaxLocationSelections());
    }

    return super.getBuilder().setValidationPredicates(predicateBuilder.build());
  }
}
