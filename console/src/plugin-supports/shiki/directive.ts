import type { Directive } from "vue";
import { hasShikiPlugin } from "./util";

const ShikiDirective: Directive = {
  mounted: (el: HTMLElement) => {
    renderShikiCode(el);
  },
  updated: (el: HTMLElement) => {
    renderShikiCode(el);
  },
};

function renderShikiCode(el: HTMLElement) {
  if (!hasShikiPlugin()) {
    return;
  }

  const blocks = el.querySelectorAll("pre>code");
  blocks.forEach((block) => {
    const preElement = block.parentElement;

    if (!preElement) {
      return;
    }

    preElement.style.padding = "1rem";
    preElement.style.filter = "blur(10px)";

    const shikiElement = document.createElement("shiki-code");
    shikiElement.style.padding = "0 1px";
    const parent = preElement?.parentElement;
    if (!parent) {
      return;
    }
    parent.insertBefore(shikiElement, preElement);
    shikiElement.appendChild(preElement);
  });
}

export default ShikiDirective;
