/** The number controller provides basic client-side validation of number input fields. */

class NumberController {

  /** Add listeners to all number inputs to update validation on changes. */
  private addNumberListeners() {
    const numberQuestions = Array.from(
      <NodeListOf<HTMLInputElement>>document.querySelectorAll('input[type=number]'));
    numberQuestions.forEach(numberQuestion => {
      numberQuestion.addEventListener('input', () => {
        // Only allow integer input.
        const n = parseInt(numberQuestion.value.replace(/[^.\d]/g, ''));
        numberQuestion.value = Number.isNaN(n) ? '' : String(n);
      });
      numberQuestion.addEventListener('keydown', (e) => {
        // Override default behavior of number input to prevent user from entering
        // special characters that break validation
        ['e', 'E', '+', '-'].includes(e.key) && e.preventDefault();
      });
    });
  }

  constructor() {
    this.addNumberListeners();
  }
}

let numberController = new NumberController();
