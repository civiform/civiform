package controllers.dev;

import static j2html.TagCreator.h2;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.geojson.GeoJsonClient;

public final class GeoJsonApiController {
  private static final Logger logger = LoggerFactory.getLogger(GeoJsonApiController.class);

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

    try {
      new URL(geoJsonEndpoint);
    } catch (MalformedURLException e) {
      return CompletableFuture.completedFuture(ok("Invalid GeoJSON endpoint"));
    }

    return geoJsonClient
        .getGeoJsonData(geoJsonEndpoint)
        .thenApply(
            geoJsonResponse -> {
              // parse response
              return ok(h2("Success!").withClass("text-green-500").toString());
            })
        .exceptionally(
            ex -> {
              logger.error("An error occurred trying to retrieve GeoJSON", ex);
              return ok(
                  h2("An error occurred trying to retrieve GeoJSON")
                      .withClass("text-red-500")
                      .toString());
            });
  }
}
