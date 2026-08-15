import { presetWind3 } from "unocss";
import { transformerCompileClass } from "unocss";

export default {
  presets: [presetWind3()],
  transformers: [transformerCompileClass()],
  blocklist: ["transform", "inline"],
};
