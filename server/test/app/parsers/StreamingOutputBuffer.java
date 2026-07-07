package parsers;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class StreamingOutputBuffer {
  private final List<String> output = new ArrayList<>();

  public void add(String message) {
    output.add(message);
  }

  public String getOutput() {
    return String.join("\n", output);
  }

  public void clear() {
    output.clear();
  }
}
