package controllers.geo;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import play.test.WithApplication;
import services.Address;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;

public class AddressSuggestionJsonSerializerTest extends WithApplication {
  @Test
  public void serializingAndDeserializingANode() {
    AddressSuggestionJsonSerializer addressSuggestionJsonSerializer =
        instanceOf(AddressSuggestionJsonSerializer.class);

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
            .setScore(100)
            .build();

    ImmutableList<AddressSuggestion> suggestions = ImmutableList.of(suggestion);

    String serializedSuggestions = addressSuggestionJsonSerializer.serialize(suggestions);

    ImmutableList<AddressSuggestion> deserializedSuggestions =
        addressSuggestionJsonSerializer.deserialize(serializedSuggestions);

    assertThat(deserializedSuggestions).isEqualTo(suggestions);
  }
}
