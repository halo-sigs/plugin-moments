import { defineConfig, presetWind3, transformerCompileClass } from "unocss";

export default defineConfig({
  presets: [presetWind3()],
  transformers: [transformerCompileClass()],
  blocklist: ["transform", "inline"],
});
