// ⚠️ IMPORTANT: Using rspack to build Halo plugins is just an attempt by the Halo team.
// Currently, it is still recommended to refer to https://github.com/halo-dev/plugin-starter
// Using Vite

import { defineConfig } from "@rspack/cli";
import { UnoCSSRspackPlugin } from "@unocss/webpack/rspack";
import path from "path";
import process from "process";
import Icons from "unplugin-icons/rspack";
import { fileURLToPath } from "url";
import { VueLoaderPlugin } from "vue-loader";
import * as sassEmbedded from "sass-embedded";

const isProduction = process.env.NODE_ENV === "production";
const dirname = path.dirname(fileURLToPath(import.meta.url));
const outDir = isProduction
  ? "../src/main/resources/console"
  : "../build/resources/main/console";

export default defineConfig({
  mode: isProduction ? "production" : "development",
  entry: {
    main: "./src/index.ts",
  },
  plugins: [
    new VueLoaderPlugin(),
    Icons({
      compiler: "vue3",
    }),
    UnoCSSRspackPlugin(),
  ],
  resolve: {
    alias: {
      "@": path.resolve(dirname, "src"),
      // TODO: Remove this to resolve editor extension
      // "@tiptap/pm/state": "@halo-dev/richtext-editor",
      // "@tiptap/core": "@halo-dev/richtext-editor",
      // "@tiptap/pm/view": "@halo-dev/richtext-editor",
    },
    extensions: [".ts", ".js"],
  },
  output: {
    publicPath: "/plugins/PluginMoments/assets/console/",
    chunkFilename: "[id]-[hash:8].js",
    cssFilename: "style.css",
    path: path.resolve(outDir),
    library: {
      type: "window",
      export: "default",
      name: "PluginMoments",
    },
    clean: true,
    iife: true,
  },
  optimization: {
    providedExports: false,
    realContentHash: true,
  },
  devtool: false,
  experiments: {
    css: true,
  },
  module: {
    rules: [
      {
        test: /\.ts$/,
        exclude: [/node_modules/],
        loader: "builtin:swc-loader",
        options: {
          jsc: {
            parser: {
              syntax: "typescript",
            },
          },
        },
        type: "javascript/auto",
      },
      {
        test: /\.vue$/,
        loader: "vue-loader",
        options: {
          experimentalInlineMatchResource: true,
        },
      },
      {
        test: /\.css$/i,
        use: ["style-loader", "css-loader"],
        type: "javascript/auto",
      },
      {
        test: /\.(sass|scss)$/,
        use: [
          {
            loader: "sass-loader",
            options: {
              api: "modern-compiler",
              implementation: sassEmbedded,
            },
          },
        ],
        type: "css/auto",
      },
    ],
  },
  externals: {
    vue: "Vue",
    "vue-router": "VueRouter",
    "@vueuse/core": "VueUse",
    "@vueuse/components": "VueUse",
    "@vueuse/router": "VueUse",
    "@halo-dev/console-shared": "HaloConsoleShared",
    "@halo-dev/components": "HaloComponents",
    "@halo-dev/api-client": "HaloApiClient",
    "@halo-dev/richtext-editor": "RichTextEditor",
    axios: "axios",
  },
});
