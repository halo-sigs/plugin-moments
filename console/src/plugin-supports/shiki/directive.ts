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

  const preElements = el.querySelectorAll<HTMLPreElement>("pre:has(code)");
  for (const preElement of preElements) {
    const parent = preElement.parentElement;
    if (!parent) {
      continue;
    }

    if (preElement.parentElement.tagName === "SHIKI-CODE") {
      continue;
    }

    preElement.style.padding = "1rem";
    preElement.style.filter = "blur(10px)";

    const shikiElement = document.createElement("shiki-code");
    shikiElement.style.padding = "0 1px";

    parent.insertBefore(shikiElement, preElement);
    shikiElement.appendChild(preElement);
  }
}

export default ShikiDirective;
