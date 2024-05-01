package auth.oidc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** Class that manages the mapping between a jsonb db column and a Java Map. */
public final class SerializedIdTokens {
  @JsonProperty("idTokens")
  private Map<String, String> idTokens;

  public SerializedIdTokens() {
    this.idTokens = new HashMap<>();
  }

  @JsonCreator
  public SerializedIdTokens(@Nullable @JsonProperty("idTokens") Map<String, String> idTokens) {
    if (idTokens == null) {
      this.idTokens = new HashMap<>();
    } else {
      this.idTokens = idTokens;
    }
  }

  public int size() {
    return idTokens.size();
  }

  public boolean containsKey(Object key) {
    return idTokens.containsKey(key);
  }

  public String get(Object key) {
    return idTokens.get(key);
  }

  public String getOrDefault(Object key, String defaultValue) {
    return idTokens.getOrDefault(key, defaultValue);
  }

  public String put(String key, String value) {
    return idTokens.put(key, value);
  }

  public String remove(Object key) {
    return idTokens.remove(key);
  }

  public Set<Map.Entry<String, String>> entrySet() {
    return idTokens.entrySet();
  }
}
