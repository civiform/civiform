package controllers.geo;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import services.Address;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;

public class AddressSuggestionJsonSerializerTest {
  private Config config;
  private AddressSuggestionJsonSerializer addressSuggestionJsonSerializer;

  @Before
  public void setup() {
    config = ConfigFactory.load();
    addressSuggestionJsonSerializer = new AddressSuggestionJsonSerializer(config);
  }

  @Test
  public void serializingAndDeserializingANode() throws Exception {
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();
    AddressLocation location =
        AddressLocation.builder()
            .setLongitude(-122.3360380354971)
            .setLatitude(47.578374020558954)
            .setWellKnownId(4326)
            .build();
    AddressSuggestion suggestion =
        AddressSuggestion.builder()
            .setAddress(address)
            .setLocation(location)
            .setSingleLineAddress("380 New York St, Redlands, California, 92373")
            .setCorrectionSource("https://some-fake-value")
            .setScore(100)
            .build();

    ImmutableList<AddressSuggestion> suggestions = ImmutableList.of(suggestion);

    String serializedSuggestions = addressSuggestionJsonSerializer.serialize(suggestions);

    ImmutableList<AddressSuggestion> deserialzedSuggestions =
        addressSuggestionJsonSerializer.deserialize(serializedSuggestions);

    assertThat(deserialzedSuggestions).isEqualTo(suggestions);
  }
}
