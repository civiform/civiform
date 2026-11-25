import {Page} from '@playwright/test'
import {AWS_EMULATOR_URL, TEST_USER_AUTH_STRATEGY} from './config'

type AwsEmulatorSesResponse = {
  messages: AwsEmulatorSesEmail[]
}

type AwsEmulatorSesEmail = {
  Body: {
    html_part: string | null
    text_part: string | null
  }
  Destination: {
    ToAddresses: string[]
  }
  Source: string
  Subject: string
}

export const supportsEmailInspection = () => {
  return TEST_USER_AUTH_STRATEGY === 'fake-oidc'
}
/**
 * Queries the emails that have been sent for a given recipient. This method requires that tests
 * run in an environment that uses the AWS emulator since it captures the emails sent using SES and makes
 * them available at a well-known endpoint). An error is thrown when the method is called from an
 * environment that does not use the AWS emulator. The supportsEmailInspection method can be used to
 * determine if the environment supports sending emails.
 */
export const extractEmailsForRecipient = async function (
  page: Page,
  recipientEmail: string,
): Promise<AwsEmulatorSesEmail[]> {
  if (!supportsEmailInspection()) {
    throw new Error('Unsupported call to extractEmailsForRecipient')
  }
  const originalPageUrl = page.url()
  await page.goto(`${AWS_EMULATOR_URL}/_aws/ses`)
  const responseJson = JSON.parse(
    await page.innerText('body'),
  ) as AwsEmulatorSesResponse

  const allEmails = responseJson.messages
  const filteredEmails = allEmails.filter((email) => {
    return email.Destination.ToAddresses.includes(recipientEmail)
  })

  await page.goto(originalPageUrl)
  return filteredEmails
}
