import { rsbuildConfig } from "@halo-dev/ui-plugin-bundler-kit";
import { pluginSass } from "@rsbuild/plugin-sass";
import { UnoCSSRspackPlugin } from "@unocss/webpack/rspack";
import Icons from "unplugin-icons/rspack";

const OUT_DIR_PROD = "../src/main/resources/console";
const OUT_DIR_DEV = "../build/resources/main/console";

export default rsbuildConfig({
  rsbuild: ({ envMode }) => {
    const isProduction = envMode === "production";
    const outDir = isProduction ? OUT_DIR_PROD : OUT_DIR_DEV;

    return {
      plugins: [pluginSass()],
      resolve: {
        alias: {
          "@": "./src",
        },
      },
      output: {
        distPath: {
          root: outDir,
        },
      },
      tools: {
        rspack: {
          cache: false,
          plugins: [
            Icons({
              compiler: "vue3",
            }),
            UnoCSSRspackPlugin(),
          ],
        },
      },
    };
  },
});
