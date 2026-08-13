import presetWind3 from "@unocss/preset-wind3";
import transformerCompileClass from "@unocss/transformer-compile-class";

export default {
  presets: [presetWind3()],
  transformers: [transformerCompileClass()],
  blocklist: ["transform", "inline"],
};
