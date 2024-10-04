package services.geo.esri.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Error object from ESRI's findAddressCandidates results
 *
 * <p>@see <a
 * href="https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm">Find
 * Address Candidates</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EsriError {
  private final int code;
  private final int extendedCode;
  private final String message;
  private final List<String> details;

  public EsriError(
      @JsonProperty("code") int code,
      @JsonProperty("extendedCode") int extendedCode,
      @JsonProperty("message") String message,
      @JsonProperty("details") List<String> details) {
    this.code = code;
    this.extendedCode = extendedCode;
    this.message = message;
    this.details = details;
  }

  public int code() {
    return code;
  }

  public int extendedCode() {
    return extendedCode;
  }

  public String message() {
    return message;
  }

  public List<String> details() {
    return details;
  }

  public String errorMessage() {
    return String.format(
        "Esri Error Response: Code: %d, Extended Code: %d, Message: %s, Details: %s",
        code, extendedCode, message, details);
  }
}
