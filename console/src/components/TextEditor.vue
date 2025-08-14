<script lang="ts" setup>
import type { useTagQueryFetchProps } from "@/composables/use-tag";
import MomentExtensionImage from "@/extensions/images";
import { TagsExtension } from "@/extensions/tags";
import type { PluginModule } from "@halo-dev/console-shared";
import {
  Editor,
  ExtensionAudio,
  ExtensionBlockquote,
  ExtensionBold,
  ExtensionBulletList,
  ExtensionCode,
  ExtensionCodeBlock,
  ExtensionColor,
  ExtensionColumn,
  ExtensionColumns,
  ExtensionCommands,
  ExtensionDocument,
  ExtensionDraggable,
  ExtensionDropcursor,
  ExtensionFontSize,
  ExtensionGapcursor,
  ExtensionHardBreak,
  ExtensionHighlight,
  ExtensionHistory,
  ExtensionHorizontalRule,
  ExtensionIframe,
  ExtensionIndent,
  ExtensionItalic,
  ExtensionLink,
  ExtensionListKeymap,
  ExtensionNodeSelected,
  ExtensionOrderedList,
  ExtensionParagraph,
  ExtensionPlaceholder,
  ExtensionStrike,
  ExtensionSubscript,
  ExtensionSuperscript,
  ExtensionTable,
  ExtensionTaskList,
  ExtensionText,
  ExtensionTextAlign,
  ExtensionTrailingNode,
  ExtensionUnderline,
  ExtensionVideo,
  lowlight,
  RichTextEditor,
  type Extensions,
} from "@halo-dev/richtext-editor";
import type { UseQueryReturnType } from "@tanstack/vue-query";
import { onMounted, shallowRef, watch } from "vue";
import { useExtension } from "./composables/use-extension";

const props = withDefaults(
  defineProps<{
    html: string;
    raw: string;
    isEmpty: boolean;
    tagQueryFetch: (props: useTagQueryFetchProps) => UseQueryReturnType<unknown, unknown>;
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

const editor = shallowRef<Editor>();

const supportedPluginNames = ["editor-hyperlink-card", "hybrid-edit-block"];

const presetExtensions = [
  ExtensionParagraph,
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
  ExtensionHistory,
  ExtensionHorizontalRule,
  ExtensionItalic,
  ExtensionOrderedList,
  ExtensionStrike,
  ExtensionText,
  MomentExtensionImage.configure({
    inline: true,
    allowBase64: false,
    HTMLAttributes: {
      loading: "lazy",
    },
  }),
  ExtensionTaskList,
  ExtensionLink.configure({
    autolink: false,
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
];

const { filterDuplicateExtensions } = useExtension();

onMounted(async () => {
  const enabledPlugins = window.enabledPlugins.filter((plugin) =>
    supportedPluginNames.includes(plugin.name)
  );
  const enabledPluginNames = enabledPlugins.map((plugin) => plugin.name);
  const enabledPluginModules: PluginModule[] = enabledPluginNames
    .map((name) => {
      if (window[name as keyof Window]) {
        return window[name as keyof Window];
      }
    })
    .filter(Boolean);

  const extensionsFromPlugins: Extensions = [];

  for (const pluginModule of enabledPluginModules) {
    const callbackFunction = pluginModule?.extensionPoints?.["default:editor:extension:create"];

    if (typeof callbackFunction !== "function") {
      continue;
    }

    const extensions = await callbackFunction();

    extensionsFromPlugins.push(...extensions);
  }

  const extensions = filterDuplicateExtensions([...presetExtensions, ...extensionsFromPlugins]);

  editor.value = new Editor({
    content: props.raw,
    extensions,
    autofocus: "end",
    onUpdate: () => {
      emit("update:raw", editor.value?.getHTML() + "");
      emit("update:html", editor.value?.getHTML() + "");
      emit("update:isEmpty", editor.value?.isEmpty);
      emit("update", editor.value?.getHTML() + "");
    },
  });
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
  <div v-if="editor" class=":uno: halo-moment-editor halo-rich-text-editor relative">
    <RichTextEditor :editor="editor" locale="zh-CN"> </RichTextEditor>
  </div>
</template>
