package services.tooling.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RequestSqlCollector {
  private static final ThreadLocal<List<Map<String, Object>>> current =
      ThreadLocal.withInitial(ArrayList::new);

  public static void start() {
    current.get().clear();
  }

  public static void add(String sql) {
    current.get().add(Map.of("sql", sql));
  }

  public static List<Map<String, Object>> get() {
    return new ArrayList<>(current.get());
  }

  public static void end() {
    current.remove();
  }
}
