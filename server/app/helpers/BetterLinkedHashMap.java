package helpers;

import java.util.LinkedHashMap;
import java.util.Map;

/** LinkedHashMap, but better, more batteries */
public class BetterLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

  public BetterLinkedHashMap() {
    super();
  }

  public BetterLinkedHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  public BetterLinkedHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public BetterLinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
    super(initialCapacity, loadFactor, accessOrder);
  }

  /**
   * Puts the key-value pair into the map if the condition evaluates to true
   *
   * @param condition the boolean supplier to evaluate
   * @param key the key
   * @param value the value
   * @return the previous value associated with key, or null if there was no mapping for key or if
   *     the condition was false
   */
  public V putIf(boolean condition, K key, V value) {
    if (condition) {
      return put(key, value);
    }
    return null;
  }

  /** Puts a {@link Map.Entry } into the map */
  public V put(Map.Entry<K, V> entry) {
    if (entry == null) {
      return null;
    }

    return put(entry.getKey(), entry.getValue());
  }
}
