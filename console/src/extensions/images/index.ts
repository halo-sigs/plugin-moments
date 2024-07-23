import {
  type Editor,
  ExtensionImage,
  type Range,
} from "@halo-dev/richtext-editor";
import type { ExtensionOptions } from "@halo-dev/richtext-editor/dist/types";
import { markRaw } from "vue";
import MdiFileImageBox from "~icons/mdi/file-image-box";

export interface ImageOptions {
  inline: boolean;
  allowBase64: boolean;
  HTMLAttributes: Record<string, unknown>;
}

const MomentExtensionImage = ExtensionImage.extend<
  ExtensionOptions & ImageOptions
>({
  addOptions() {
    return {
      ...this.parent?.(),
      getCommandMenuItems() {
        return {
          priority: 100,
          icon: markRaw(MdiFileImageBox),
          title: "图片",
          keywords: ["image", "tupian"],
          command: ({ editor, range }: { editor: Editor; range: Range }) => {
            editor
              .chain()
              .focus()
              .deleteRange(range)
              .insertContent([
                { type: "image", attrs: { src: "" } },
                { type: "paragraph", content: "" },
              ])
              .run();
          },
        };
      },
    };
  },
});

export default MomentExtensionImage;
