import { fileURLToPath, URL } from "url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import Icons from "unplugin-icons/vite";
import { HaloUIPluginBundlerKit } from "@halo-dev/ui-plugin-bundler-kit";

export default defineConfig({
  plugins: [vue(), Icons({ compiler: "vue3" }), HaloUIPluginBundlerKit()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      // To resolve conflict between @tiptap and @halo-dev/richtext-editor
      "@tiptap/pm/state": "@halo-dev/richtext-editor",
      "@tiptap/core": "@halo-dev/richtext-editor",
      "@tiptap/pm/view": "@halo-dev/richtext-editor",
    },
  },
});
