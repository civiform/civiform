package controllers.dev;

import static j2html.TagCreator.div;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.geojson.FeatureCollection;
import services.geojson.GeoJsonClient;

public final class GeoJsonApiController {
  private final FormFactory formFactory;
  private final GeoJsonClient geoJsonClient;

  @Inject
  GeoJsonApiController(FormFactory formFactory, GeoJsonClient geoJsonClient) {
    this.formFactory = formFactory;
    this.geoJsonClient = geoJsonClient;
  }

  public CompletionStage<Result> hxGetData(Http.Request request) {
    DynamicForm formData = formFactory.form().bindFromRequest(request);
    String geoJsonEndpoint = formData.get("geoJsonEndpoint");

    if (geoJsonEndpoint == null || geoJsonEndpoint.isEmpty()) {
      return CompletableFuture.completedFuture(badRequest("Missing geoJsonEndpoint"));
    }

    CompletionStage<Optional<FeatureCollection>> geoJsonResponseStage =
        geoJsonClient.fetchGeoJsonData(geoJsonEndpoint);

    return geoJsonResponseStage.thenApply(
        optionalGeoJsonResponse -> {
          // String geoJsonString = optionalGeoJsonResponse.orElse("");
          return ok(div("Success!").toString());
        });
  }
}
