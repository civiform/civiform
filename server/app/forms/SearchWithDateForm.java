package forms;

public class SearchWithDateForm {
  private String dob;
  private String searchText;
  public String getSearchText() {return searchText;}
  public void setSearchText(String searchText) {this.searchText = searchText;}
  public String getDob() {
    return dob;
  }

  public void setDob(String dob) {
    this.dob = dob;
  }
}
