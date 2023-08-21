package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;
import javax.inject.Inject;

/** A util class that wraps a JSON string inside a container object with a nextPageToken. */
public final class ApiPayloadWrapper {

  private final ApiPaginationTokenSerializer apiPaginationTokenSerializer;

  @Inject
  ApiPayloadWrapper(ApiPaginationTokenSerializer apiPaginationTokenSerializer) {
    this.apiPaginationTokenSerializer = checkNotNull(apiPaginationTokenSerializer);
  }

  /**
   * Wraps payload in another layer of JSON. Inserts a "payload" key that maps to the data in
   * payload. Adds a nextPageToken key with the paginationTokenPayload.
   *
   * @param payload the JSON string to wrap
   * @param paginationTokenPayload the pagination token to include with the payload
   * @return a JSON string of the wrapped payload and pagination token
   */
  public String wrapPayload(
      String payload, Optional<ApiPaginationTokenPayload> paginationTokenPayload) {
    var writer = new StringWriter();

    try {
      var jsonGenerator = new JsonFactory().createGenerator(writer);
      jsonGenerator.writeStartObject();
      jsonGenerator.writeFieldName("payload");
      jsonGenerator.writeRawValue(payload);

      jsonGenerator.writeFieldName("nextPageToken");
      if (paginationTokenPayload.isPresent()) {
        jsonGenerator.writeString(
            apiPaginationTokenSerializer.serialize(paginationTokenPayload.get()));
      } else {
        jsonGenerator.writeNull();
      }

      jsonGenerator.writeEndObject();
      jsonGenerator.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return writer.toString();
  }
}
