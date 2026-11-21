import {HtmlValidate} from 'html-validate'
import config from '../.htmlvalidate.mjs'

const htmlvalidate = new HtmlValidate(config)

/** Lints a template with the project config and returns the messages. */
export async function lintMessages(markup) {
  const report = await htmlvalidate.validateString(markup, 'template.html')

  return report.results.flatMap((result) => result.messages)
}

/** Lints a template with the project config and returns the rules it broke. */
export async function lint(markup) {
  const messages = await lintMessages(markup)

  return messages.map((message) => message.ruleId)
}
