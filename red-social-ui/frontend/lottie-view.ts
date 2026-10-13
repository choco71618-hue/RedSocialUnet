import { LitElement, html } from 'lit';
import lottie, { type AnimationItem } from 'lottie-web';

export class LottieView extends LitElement {
  static get properties() {
    return {
      src: { type: String },
    };
  }

  src = '';
  private animation?: AnimationItem;

  createRenderRoot() {
    // Light DOM: permite que el CSS del theme apunte a .lottie-box y que
    // document.querySelector() encuentre el contenedor (necesario para Lottie).
    return this;
  }

  render() {
    return html`<div class="lottie-box"></div>`;
  }

  firstUpdated() {
    const container = this.querySelector<HTMLElement>('.lottie-box');
    if (container && this.src) {
      this.animation = lottie.loadAnimation({
        container,
        renderer: 'svg',
        loop: true,
        autoplay: true,
        path: this.src,
      });
    }
  }

  disconnectedCallback() {
    if (this.animation) {
      this.animation.destroy();
      this.animation = undefined;
    }
    super.disconnectedCallback();
  }
}

customElements.define('lottie-view', LottieView);

declare global {
  interface HTMLElementTagNameMap {
    'lottie-view': LottieView;
  }
}