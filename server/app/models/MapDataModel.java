package models;

import io.ebean.Model;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(name = "map_data")
public class MapDataModel extends Model {

  @Getter @DbJsonB public String geojson;

  public String endpoint;

  @WhenModified public Instant lastUpdated;
}
