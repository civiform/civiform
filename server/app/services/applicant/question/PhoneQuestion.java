package services.applicant.question;

import com.google.common.collect.ImmutableList;
import services.Path;

public class PhoneQuestion extends Question
{
  private Optional<String> phoneNumberValue;
  private Optional<String> countryCodeValue;

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getPhoneNumberPath(), getCountryCodePath());
  }

  @Override
  public String getAnswerString() {
    String number = getNumberValue().orElse("");
    String countryCode = getCountryCodeValue().orElse("");
    if(number.isPresent && countryCode.isPresent)
    {
      PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
      try {
        return phoneNumberUtil.parse(number, countryCode);
      }
      catch(NumberParseException e)
      {
        //appropriate error handling
      }
    }
    return "-";
    }
  }
}
