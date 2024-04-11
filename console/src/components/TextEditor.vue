<script lang="ts" setup>
import {
  RichTextEditor,
  ExtensionBlockquote,
  ExtensionBold,
  ExtensionBulletList,
  ExtensionCode,
  ExtensionDocument,
  ExtensionDropcursor,
  ExtensionGapcursor,
  ExtensionHardBreak,
  ExtensionHeading,
  ExtensionHistory,
  ExtensionHorizontalRule,
  ExtensionItalic,
  ExtensionOrderedList,
  ExtensionStrike,
  ExtensionText,
  ExtensionImage,
  ExtensionTaskList,
  ExtensionLink,
  ExtensionTextAlign,
  ExtensionUnderline,
  ExtensionTable,
  ExtensionSubscript,
  ExtensionSuperscript,
  ExtensionPlaceholder,
  ExtensionHighlight,
  ExtensionCommands,
  ExtensionIframe,
  ExtensionVideo,
  ExtensionAudio,
  ExtensionCodeBlock,
  ExtensionFontSize,
  ExtensionColor,
  ExtensionIndent,
  lowlight,
  ExtensionDraggable,
  ExtensionColumns,
  ExtensionColumn,
  ExtensionNodeSelected,
  ExtensionTrailingNode,
  useEditor,
  ExtensionListKeymap,
} from "@halo-dev/richtext-editor";
import { watch } from "vue";
import { TagsExtension } from "@/extensions/tags";
import type { useTagQueryFetchProps } from "@/composables/use-tag";
import type { UseQueryReturnType } from "@tanstack/vue-query";

const props = withDefaults(
  defineProps<{
    html: string;
    raw: string;
    isEmpty: boolean;
    tagQueryFetch: (
      props: useTagQueryFetchProps
    ) => UseQueryReturnType<unknown, unknown>;
  }>(),
  {
    html: "",
    raw: "",
    isEmpty: true,
  }
);

const emit = defineEmits<{
  (event: "update:raw", value: string): void;
  (event: "update:html", value: string): void;
  (event: "update", value: string): void;
  (event: "update:isEmpty", value: boolean | undefined): void;
}>();

const editor = useEditor({
  content: props.raw,
  extensions: [
    ExtensionBlockquote,
    ExtensionBold,
    ExtensionBulletList,
    ExtensionCode,
    ExtensionDocument,
    ExtensionDropcursor.configure({
      width: 2,
      class: "dropcursor",
      color: "skyblue",
    }),
    ExtensionGapcursor,
    ExtensionHardBreak,
    ExtensionHeading,
    ExtensionHistory,
    ExtensionHorizontalRule,
    ExtensionItalic,
    ExtensionOrderedList,
    ExtensionStrike,
    ExtensionText,
    ExtensionImage.configure({
      inline: true,
      allowBase64: false,
      HTMLAttributes: {
        loading: "lazy",
      },
    }),
    ExtensionTaskList,
    ExtensionLink.configure({
      autolink: true,
      openOnClick: false,
    }),
    ExtensionTextAlign.configure({
      types: ["heading", "paragraph"],
    }),
    ExtensionUnderline,
    ExtensionTable.configure({
      resizable: true,
    }),
    ExtensionSubscript,
    ExtensionSuperscript,
    ExtensionHighlight,
    ExtensionCommands,
    ExtensionCodeBlock.configure({
      lowlight,
    }),
    ExtensionIframe,
    ExtensionVideo,
    ExtensionAudio,
    ExtensionFontSize,
    ExtensionColor,
    ExtensionIndent,
    ExtensionDraggable,
    ExtensionColumns,
    ExtensionColumn,
    ExtensionNodeSelected,
    ExtensionTrailingNode,
    ExtensionPlaceholder.configure({
      placeholder: "有什么想说的吗...",
    }),
    ExtensionHighlight,
    ExtensionListKeymap,
    TagsExtension.configure({
      tagQueryFetch: props.tagQueryFetch,
    }),
  ],
  autofocus: "end",
  onUpdate: () => {
    emit("update:raw", editor.value?.getHTML() + "");
    emit("update:html", editor.value?.getHTML() + "");
    emit("update:isEmpty", editor.value?.isEmpty);
    emit("update", editor.value?.getHTML() + "");
  },
});

watch(
  () => props.raw,
  () => {
    if (props.raw !== editor.value?.getHTML()) {
      editor.value?.commands.setContent(props.raw);
    }
  }
);
</script>
<template>
  <div
    v-if="editor"
    class="halo-moment-editor halo-rich-text-editor moments-relative"
  >
    <RichTextEditor :editor="editor" locale="zh-CN "> </RichTextEditor>
  </div>
</template>
