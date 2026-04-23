// USWDS typescript module declaration for imports
declare module '@uswds/uswds/js/usa-file-input' {
  export interface USWDSFileInput {
    on: (element: HTMLElement | Document) => void
    off: (element: HTMLElement | Document) => void
  }

  const fileInput: USWDSFileInput
  export default fileInput
}

declare module '@uswds/uswds/js/usa-modal' {
  export interface USWDSModal {
    toggleModal: (event: {target: HTMLElement; type: string}) => boolean
    teardown: (element: HTMLElement) => void
  }

  const modal: USWDSModal
  export default modal
}
