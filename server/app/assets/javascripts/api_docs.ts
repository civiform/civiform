import {addEventListenerToElements} from './util'

class ApiDocs {
  // Redirects the user to the active version of the program slug they just selected upon
  // clicking the select program slug.
  static attachSlugEventListener() {
    addEventListenerToElements('#select-slug', 'change', (event: Event) => {
      const programDropdown = event.currentTarget as HTMLSelectElement
      const programValue = programDropdown.value
      window.location.href = `/api/docs/v1/${programValue}/active`
    })
  }

  // Redirects the user to the selected version of the program currently being viewed, upon
  // clicking the select version slug.
  static attachVersionEventListener() {
    addEventListenerToElements('#select-version', 'change', (event: Event) => {
      const slugDropdown = document.getElementById(
        'select-slug',
      ) as HTMLSelectElement
      const slugValue: string = slugDropdown.value

      const versionDropdown = event.currentTarget as HTMLSelectElement
      const versionValue: string = versionDropdown.value

      const encodedSlug = encodeURIComponent(slugValue);
      const encodedVersion = encodeURIComponent(versionValue);

      window.location.href = `/api/docs/v1/${encodedSlug}/${encodedVersion}`;

    })
  }
}
export function init() {
  ApiDocs.attachSlugEventListener()
  ApiDocs.attachVersionEventListener()
}
