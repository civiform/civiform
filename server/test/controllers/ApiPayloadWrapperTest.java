package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static services.export.JsonPrettifier.asPrettyJsonString;

import com.google.common.collect.ImmutableMap;
import controllers.api.ApiPaginationTokenPayload;
import controllers.api.ApiPaginationTokenSerializer;
import controllers.api.ApiPayloadWrapper;
import java.util.Optional;
import org.junit.Test;
import repository.ResetPostgres;

public class ApiPayloadWrapperTest extends ResetPostgres {

  @Test
  public void wrapPayloadJson_wrapsPayloadWithNoToken() {
    String payload = "{\"United States\":{\"New York State\":[\"New York City\", \"Albany\"]}}";

    ApiPayloadWrapper apiPayloadWrapper = instanceOf(ApiPayloadWrapper.class);

    String result =
        apiPayloadWrapper.wrapPayload(payload, /* paginationTokenPayload= */ Optional.empty());

    assertThat(asPrettyJsonString(result))
        .isEqualTo(
            "{\n"
                + "  \"nextPageToken\" : null,\n"
                + "  \"payload\" : {\n"
                + "    \"United States\" : {\n"
                + "      \"New York State\" : [ \"New York City\", \"Albany\" ]\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void wrapPayloadJson_wrapsPayloadWithProvidedToken() {
    String payload = "{\"United States\":{\"New York State\":[\"New York City\", \"Albany\"]}}";

    ApiPaginationTokenPayload fakeToken =
        new ApiPaginationTokenPayload(
            new ApiPaginationTokenPayload.PageSpec("fakeOffsetId", 1),
            ImmutableMap.of("key", "value"));
    ApiPaginationTokenSerializer tokenSerializer = instanceOf(ApiPaginationTokenSerializer.class);
    String expectedToken = tokenSerializer.serialize(fakeToken);

    ApiPayloadWrapper apiPayloadWrapper = instanceOf(ApiPayloadWrapper.class);

    String result =
        apiPayloadWrapper.wrapPayload(
            payload, /* paginationTokenPayload= */ Optional.of(fakeToken));

    assertThat(asPrettyJsonString(result))
        .isEqualTo(
            "{\n"
                + "  \"nextPageToken\" : \""
                + expectedToken
                + "\",\n"
                + "  \"payload\" : {\n"
                + "    \"United States\" : {\n"
                + "      \"New York State\" : [ \"New York City\", \"Albany\" ]\n"
                + "    }\n"
                + "  }\n"
                + "}");
  }
}
