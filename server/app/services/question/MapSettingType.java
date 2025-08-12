package services.question;

import services.question.types.QuestionType;

import java.util.Set;

public enum MapSettingType implements SettingType{
  /** Setting is used as a filter for map questions */
  LOCATION_FILTER_GEO_JSON_KEY,
  /** Setting maps to the GeoJSON field containing location name */
  LOCATION_NAME_GEO_JSON_KEY,
  /** Setting maps to the GeoJSON field containing location address */
  LOCATION_ADDRESS_GEO_JSON_KEY,
  /** Setting maps to the GeoJSON field containing location details URL */
  LOCATION_DETAILS_URL_GEO_JSON_KEY;


  @Override
  public Set<QuestionType> getSupportedQuestionTypes() {
    return Set.of(QuestionType.MAP);
  }
}
