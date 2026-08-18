import { stores } from "@halo-dev/ui-shared";

export const PLUGIN_SHIKI_NAME = "shiki";
export const PLUGIN_SHIKI_SCRIPT_URL = `/plugins/${PLUGIN_SHIKI_NAME}/assets/static/shiki-code.js`;

export function hasShikiPlugin() {
  return stores.uiPlugins().isEnabled(PLUGIN_SHIKI_NAME);
}
