package views.admin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Takes in a stream of objects, and a function that extracts a string key from those objects, and
 * returns a map of keys to lists of objects. Implements the Collector interface so can be used on
 * streams.
 *
 * <p>For example: [{"foo": "bar"}, {"foo": "bar"}, {"foo": "bar2"}], plus (e -> e.foo), gives
 * {"bar": [object1, object2], "bar2": [object3]}.
 */
public class GroupByKeyCollector<T>
    implements Collector<T, Map<String, List<T>>, ImmutableMap<String, ImmutableList<T>>> {
  private final Function<T, String> keyExtractor;

  public GroupByKeyCollector(Function<T, String> keyExtractor) {
    this.keyExtractor = keyExtractor;
  }

  @Override
  public Supplier<Map<String, List<T>>> supplier() {
    return () -> new HashMap<>();
  }

  @Override
  public BiConsumer<Map<String, List<T>>, T> accumulator() {
    return (map, element) -> {
      String key = keyExtractor.apply(element);
      if (map.containsKey(key)) {
        map.get(key).add(element);
      } else {
        List<T> listWithElement = new ArrayList<>();
        listWithElement.add(element);
        map.put(key, listWithElement);
      }
    };
  }

  @Override
  public BinaryOperator<Map<String, List<T>>> combiner() {
    return (map1, map2) -> {
      for (Map.Entry<String, List<T>> entry : map1.entrySet()) {
        if (map2.containsKey(entry.getKey())) {
          map2.get(entry.getKey()).addAll(entry.getValue());
        } else {
          map2.put(entry.getKey(), entry.getValue());
        }
      }
      return map1;
    };
  }

  @Override
  public Function<Map<String, List<T>>, ImmutableMap<String, ImmutableList<T>>> finisher() {
    return (map) -> {
      ImmutableMap.Builder<String, ImmutableList<T>> builder = new ImmutableMap.Builder<>();
      for (Map.Entry<String, List<T>> entry : map.entrySet()) {
        builder.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
      }
      return builder.build();
    };
  }

  @Override
  public Set<Characteristics> characteristics() {
    return Set.of(Characteristics.UNORDERED);
  }
}
