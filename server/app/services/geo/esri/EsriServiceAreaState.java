package services.geo.esri;

/** Defines the states of inclusion for an address in a service area. */
public enum EsriServiceAreaState {
  IN_AREA,
  NOT_IN_AREA,
  // FAILED indicates that the service area validation check failed.
  // This can happen if the service area valiation option config does not load properly,
  // or if the external Esri service returns a non 200 status
  FAILED
}
