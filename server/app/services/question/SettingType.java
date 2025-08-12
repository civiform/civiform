package services.question;

/** Defines types of question settings and how they will be used. */
public enum SettingType {
  /** Setting is used as a filter for map questions */
  FILTER,
  /** Setting maps to the GeoJSON field containing location name */
  NAME,
  /** Setting maps to the GeoJSON field containing location address */
  ADDRESS,
  /** Setting maps to the GeoJSON field containing location details URL */
  URL
}
