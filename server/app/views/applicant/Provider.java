package com.mz.coronavirus.models;

public class LocationStats {

  private String state;
  private String country;
  private int latestTotalCases;
  private int diffFromPrevDay;
  private double latitude;
  private double longitude;

  public int getDiffFromPrevDay() {
    return diffFromPrevDay;
  }

  public void setDiffFromPrevDay(int diffFromPrevDay) {
    this.diffFromPrevDay = diffFromPrevDay;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public int getLatestTotalCases() {
    return latestTotalCases;
  }

  public void setLatestTotalCases(int latestTotalCases) {
    this.latestTotalCases = latestTotalCases;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  @Override
  public String toString() {
    return "LocationStats{"
        + "state='"
        + state
        + '\''
        + ", country='"
        + country
        + '\''
        + ", latestTotalCases="
        + latestTotalCases
        + ", diffFromPrevDay="
        + diffFromPrevDay
        + ", latitude="
        + latitude
        + ", longitude="
        + longitude
        + '}';
  }
}
