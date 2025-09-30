import { useScriptTag } from "@vueuse/core";
import { onMounted, onUnmounted } from "vue";
import { hasShikiPlugin, PLUGIN_SHIKI_SCRIPT_URL } from "./util";

export function usePluginShikiScriptLoader() {
  const { unload, load } = useScriptTag(
    `${PLUGIN_SHIKI_SCRIPT_URL}?version=${Date.now()}`,
    () => {
      // do nothing
    },
    {
      type: "module",
      manual: true,
    }
  );

  onMounted(() => {
    if (hasShikiPlugin()) {
      load();
    }
  });

  onUnmounted(() => {
    unload();
  });
}
