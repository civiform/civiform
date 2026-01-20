package helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class BetterLinkedHashMapTest {

  @Test
  public void testPutIf_conditionTrue() {
    BetterLinkedHashMap<String, String> map = new BetterLinkedHashMap<>();
    var result = map.putIf(true, "key", "value");

    assertThat(map).containsEntry("key", "value");
    assertThat(result).isNull();
  }

  @Test
  public void testPutIf_conditionFalse() {
    BetterLinkedHashMap<String, String> map = new BetterLinkedHashMap<>();
    var result = map.putIf(false, "key", "value");

    assertThat(map).isEmpty();
    assertThat(result).isNull();
  }

  @Test
  public void testPutIf_returnsPreviousValue() {
    BetterLinkedHashMap<String, String> map = new BetterLinkedHashMap<>();
    map.put("key", "oldValue");

    var result = map.putIf(true, "key", "newValue");

    assertThat(map).containsEntry("key", "newValue");
    assertThat(result).isEqualTo("oldValue");
  }

  @Test
  public void testPutIf_withVariousConditions() {
    boolean isAdmin = true;
    boolean isPremium = false;

    BetterLinkedHashMap<String, String> map = new BetterLinkedHashMap<>();
    map.put("name", "John");
    map.putIf(isAdmin, "role", "Admin");
    map.putIf(isPremium, "subscription", "Premium");

    assertThat(map).containsEntry("name", "John");
    assertThat(map).containsEntry("role", "Admin");
    assertThat(map).doesNotContainKey("subscription");
  }
}
