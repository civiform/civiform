package controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.api.ApiPaginationTokenPayload;
import java.io.StringWriter;
import java.util.Map;
import org.junit.Test;
import play.test.WithApplication;

public class ApiPaginationTokenSerializationTest extends WithApplication {

  @Test
  public void serializingAndDeserializingAToken() throws Exception {
    var mapper = instanceOf(ObjectMapper.class);
    var pageSpec = new ApiPaginationTokenPayload.PageSpec("foo", 10);
    var requestSpec = Map.of("fromDate", "2022/01/01");
    var token = new ApiPaginationTokenPayload(pageSpec, requestSpec);

    var writer = new StringWriter();
    mapper.writeValue(writer, token);

    var json = writer.toString();

    assertThat(json)
        .isEqualTo(
            "{\"pageSpec\":{\"offsetIdentifier\":\"foo\",\"pageSize\":10},\"requestSpec\":{\"fromDate\":\"2022/01/01\"}}");

    token = mapper.readValue(json, ApiPaginationTokenPayload.class);

    assertThat(token.getPageSpec()).isEqualTo(pageSpec);
    assertThat(token.getRequestSpec()).isEqualTo(requestSpec);
  }
}
