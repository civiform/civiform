package services.question;

import java.util.Set;
import services.question.types.QuestionType;

public enum MapSettingType implements SettingType {
  /** Setting is used as a filter for map questions */
  LOCATION_FILTER_GEO_JSON_KEY,
  /** Setting maps to the GeoJSON field containing location name */
  LOCATION_NAME_GEO_JSON_KEY,
  /** Setting maps to the GeoJSON field containing location address */
  LOCATION_ADDRESS_GEO_JSON_KEY,
  /** Setting maps to the GeoJSON field containing location details URL */
  LOCATION_DETAILS_URL_GEO_JSON_KEY,
  /** Setting maps to the GeoJSON field containing the key to be used to tag map locations */
  LOCATION_TAG_GEO_JSON_KEY;

  @Override
  public Set<QuestionType> getSupportedQuestionTypes() {
    return Set.of(QuestionType.MAP);
  }
}
