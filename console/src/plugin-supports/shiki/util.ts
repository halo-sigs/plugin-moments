export const PLUGIN_SHIKI_NAME = "shiki";
export const PLUGIN_SHIKI_SCRIPT_URL = `/plugins/${PLUGIN_SHIKI_NAME}/assets/static/shiki-code.js`;

export function hasShikiPlugin() {
  return window.enabledPlugins.find((p) => p.name === PLUGIN_SHIKI_NAME);
}
