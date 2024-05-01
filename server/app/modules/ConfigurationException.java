package modules;

/** Exception representing an error in service configuration. */
public class ConfigurationException extends RuntimeException {
  public ConfigurationException(String errorMessage) {
    super(errorMessage);
  }
}
