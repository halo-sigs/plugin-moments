import { pluginSass } from "@rsbuild/plugin-sass";
import { pluginVue } from "@rsbuild/plugin-vue";
import { defineConfig } from "@rstest/core";
import Icons from "unplugin-icons/rspack";

export default defineConfig({
  clearMocks: true,
  include: ["tests/**/*.test.ts"],
  plugins: [pluginVue(), pluginSass()],
  tools: {
    rspack: {
      plugins: [
        Icons({
          compiler: "vue3",
        }),
      ],
    },
  },
  restoreMocks: true,
  testEnvironment: "happy-dom",
});
