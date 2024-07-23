import { HaloUIPluginBundlerKit } from "@halo-dev/ui-plugin-bundler-kit";
import vue from "@vitejs/plugin-vue";
import UnoCSS from "unocss/vite";
import Icons from "unplugin-icons/vite";
import { fileURLToPath, URL } from "url";
import { defineConfig, type Plugin } from "vite";

export default defineConfig({
  plugins: [
    vue(),
    Icons({ compiler: "vue3" }),
    UnoCSS({
      mode: "vue-scoped",
      configFile: "./uno.config.ts",
    }),
    HaloUIPluginBundlerKit() as Plugin,
  ],
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
