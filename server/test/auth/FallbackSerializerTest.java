package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.pac4j.core.util.serializer.Serializer;

public class FallbackSerializerTest {

  /** Minimal stub that records calls and returns a configured value or throws. */
  private static final class StubSerializer implements Serializer {
    int serializeCalls = 0;
    int deserializeCalls = 0;
    private final byte[] serializeResult;
    private final Object deserializeResult;
    private final RuntimeException deserializeThrow;

    static StubSerializer returning(byte[] serialized, Object deserialized) {
      return new StubSerializer(serialized, deserialized, null);
    }

    static StubSerializer throwingOnDeserialize(RuntimeException toThrow) {
      return new StubSerializer(new byte[0], null, toThrow);
    }

    private StubSerializer(byte[] serializeResult, Object deserializeResult, RuntimeException ex) {
      this.serializeResult = serializeResult;
      this.deserializeResult = deserializeResult;
      this.deserializeThrow = ex;
    }

    @Override
    public byte[] serializeToBytes(Object o) {
      serializeCalls++;
      return serializeResult;
    }

    @Override
    public String serializeToString(Object o) {
      serializeCalls++;
      return new String(serializeResult, StandardCharsets.UTF_8);
    }

    @Override
    public Object deserializeFromBytes(byte[] bytes) {
      deserializeCalls++;
      if (deserializeThrow != null) {
        throw deserializeThrow;
      }
      return deserializeResult;
    }

    @Override
    public Object deserializeFromString(String value) {
      deserializeCalls++;
      if (deserializeThrow != null) {
        throw deserializeThrow;
      }
      return deserializeResult;
    }
  }

  @Test
  public void serialize_alwaysUsesPrimary() {
    StubSerializer primary = StubSerializer.returning(new byte[] {1, 2, 3}, "primary");
    StubSerializer fallback = StubSerializer.returning(new byte[] {9, 9, 9}, "fallback");
    FallbackSerializer combined = new FallbackSerializer(primary, fallback);

    byte[] result = combined.serializeToBytes(new Object());

    assertThat(result).isEqualTo(new byte[] {1, 2, 3});
    assertThat(primary.serializeCalls).isEqualTo(1);
    assertThat(fallback.serializeCalls).isEqualTo(0);
  }

  @Test
  public void deserialize_returnsPrimaryResultWhenPrimarySucceeds() {
    StubSerializer primary = StubSerializer.returning(new byte[0], "primary-result");
    StubSerializer fallback = StubSerializer.returning(new byte[0], "fallback-result");
    FallbackSerializer combined = new FallbackSerializer(primary, fallback);

    Object result = combined.deserializeFromBytes(new byte[] {1, 2, 3});

    assertThat(result).isEqualTo("primary-result");
    assertThat(primary.deserializeCalls).isEqualTo(1);
    assertThat(fallback.deserializeCalls).isEqualTo(0);
  }

  @Test
  public void deserialize_fallsBackWhenPrimaryThrowsRuntimeException() {
    StubSerializer primary = StubSerializer.throwingOnDeserialize(new RuntimeException("primary"));
    StubSerializer fallback = StubSerializer.returning(new byte[0], "fallback-result");
    FallbackSerializer combined = new FallbackSerializer(primary, fallback);

    Object result = combined.deserializeFromBytes(new byte[] {1, 2, 3});

    assertThat(result).isEqualTo("fallback-result");
    assertThat(primary.deserializeCalls).isEqualTo(1);
    assertThat(fallback.deserializeCalls).isEqualTo(1);
  }

  @Test
  public void deserialize_fallsBackWhenPrimaryReturnsNull() {
    // pac4j's AbstractSerializer.deserializeFromBytes catches parse errors internally and
    // returns null rather than throwing — this test pins that path.
    StubSerializer primary = StubSerializer.returning(new byte[0], null);
    StubSerializer fallback = StubSerializer.returning(new byte[0], "fallback-result");
    FallbackSerializer combined = new FallbackSerializer(primary, fallback);

    Object result = combined.deserializeFromBytes(new byte[] {1, 2, 3});

    assertThat(result).isEqualTo("fallback-result");
    assertThat(primary.deserializeCalls).isEqualTo(1);
    assertThat(fallback.deserializeCalls).isEqualTo(1);
  }

  @Test
  public void deserialize_propagatesFallbackExceptionWhenBothThrow() {
    StubSerializer primary = StubSerializer.throwingOnDeserialize(new RuntimeException("primary"));
    StubSerializer fallback =
        StubSerializer.throwingOnDeserialize(new RuntimeException("fallback"));
    FallbackSerializer combined = new FallbackSerializer(primary, fallback);

    assertThatThrownBy(() -> combined.deserializeFromBytes(new byte[] {1, 2, 3}))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("fallback");
  }
}
