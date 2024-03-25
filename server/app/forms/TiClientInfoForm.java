package forms;

/** Form for editing TI client information. */
public final class TiClientInfoForm {
  private String firstName;
  private String middleName;
  private String lastName;

  private String dayQuery;
  private String monthQuery;
  private String yearQuery;
  private String emailAddress;
  private String tiNote;
  private String phoneNumber;

  public String getDayQuery() {
    return dayQuery;
  }

  public void setDayQuery(String dayQuery) {
    this.dayQuery = dayQuery;
  }

  public String getMonthQuery() {
    return monthQuery;
  }

  public void setMonthQuery(String monthQuery) {
    this.monthQuery = monthQuery;
  }

  public String getYearQuery() {
    return yearQuery;
  }

  public void setYearQuery(String yearQuery) {
    this.yearQuery = yearQuery;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public void setMiddleName(String middleName) {
    this.middleName = middleName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public String getTiNote() {
    return tiNote;
  }

  public void setTiNote(String tiNote) {
    this.tiNote = tiNote;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }
}
