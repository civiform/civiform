package controllers.dev;

import static j2html.TagCreator.h2;
import static play.mvc.Results.ok;

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

    return geoJsonClient
        .fetchGeoJson(geoJsonEndpoint)
        .thenApply(geoJsonResponse -> ok(h2("Success!").toString()))
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
