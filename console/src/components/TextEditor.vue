<script lang="ts" setup>
import {
  Extension,
  EditorContent,
  useEditor,
  ExtensionBlockquote,
  ExtensionBold,
  ExtensionBulletList,
  ExtensionCode,
  ExtensionDocument,
  ExtensionDropcursor,
  ExtensionGapcursor,
  ExtensionHardBreak,
  ExtensionHistory,
  ExtensionHorizontalRule,
  ExtensionItalic,
  ExtensionListItem,
  ExtensionOrderedList,
  ExtensionParagraph,
  ExtensionStrike,
  ExtensionText,
  ExtensionImage,
  ExtensionTaskList,
  ExtensionTaskItem,
  ExtensionLink,
  ExtensionTextAlign,
  ExtensionUnderline,
  ExtensionTable,
  ExtensionSubscript,
  ExtensionSuperscript,
  ExtensionPlaceholder,
  ExtensionHighlight,
  ExtensionHeading,
  ExtensionCommands,
  ExtensionIframe,
  ExtensionVideo,
  ExtensionAudio,
  CommandsSuggestion,
  CommentParagraph,
  CommandHeader1,
  CommandHeader2,
  CommandHeader3,
  CommandHeader4,
  CommandHeader5,
  CommandHeader6,
  CommandCodeBlock,
  CommandIframe,
  CommandVideo,
  CommandAudio,
  CommandTable,
  CommandBulletList,
  CommandOrderedList,
  CommandTaskList,
  ExtensionCodeBlock,
  lowlight,
  BoldMenuItem,
  ItalicMenuItem,
  UnderlineMenuItem,
  StrikeMenuItem,
  QuoteMenuItem,
  CodeMenuItem,
  SuperScriptMenuItem,
  SubScriptMenuItem,
  CodeBlockMenuItem,
  AlignLeftMenuItem,
  AlignCenterMenuItem,
  AlignRightMenuItem,
  HighlightMenuItem,
} from "@halo-dev/richtext-editor";
import { computed, watch } from "vue";
import EditorBubbleMenu from "./EditorBubbleMenu.vue";

const props = withDefaults(
  defineProps<{
    html: string;
    raw: string;
    isEmpty: boolean;
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
    ExtensionDropcursor,
    ExtensionGapcursor,
    ExtensionHardBreak,
    ExtensionHeading,
    ExtensionHistory,
    ExtensionHorizontalRule,
    ExtensionItalic,
    ExtensionListItem,
    ExtensionOrderedList,
    ExtensionParagraph,
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
    ExtensionTaskItem,
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
    ExtensionPlaceholder.configure({
      placeholder: "有什么想说的吗...",
    }),
    ExtensionHighlight,
    ExtensionCommands.configure({
      suggestion: {
        ...CommandsSuggestion,
        items: ({ query }: { query: string }) => {
          return [
            CommentParagraph,
            CommandHeader1,
            CommandHeader2,
            CommandHeader3,
            CommandHeader4,
            CommandHeader5,
            CommandHeader6,
            CommandCodeBlock,
            CommandTable,
            CommandBulletList,
            CommandOrderedList,
            CommandTaskList,
            CommandIframe,
            CommandVideo,
            CommandAudio,
          ].filter((item) =>
            [...item.keywords, item.title].some((keyword) =>
              keyword.includes(query)
            )
          );
        },
      },
    }),
    ExtensionCodeBlock.configure({
      lowlight,
    }),
    ExtensionIframe,
    ExtensionVideo,
    ExtensionAudio,
    Extension.create({
      addGlobalAttributes() {
        return [
          {
            types: ["heading"],
            attributes: {
              id: {
                default: null,
              },
            },
          },
        ];
      },
    }),
  ],
  autofocus: "start",
  onUpdate: () => {
    emit("update:raw", editor.value?.getHTML() + "");
    emit("update:html", editor.value?.getHTML() + "");
    emit("update:isEmpty", editor.value?.isEmpty);
    emit("update", editor.value?.getHTML() + "");
  },
});

const bubbleMenuItems = computed(() => {
  if (!editor.value) return [];
  return [
    BoldMenuItem(editor.value),
    ItalicMenuItem(editor.value),
    UnderlineMenuItem(editor.value),
    StrikeMenuItem(editor.value),
    HighlightMenuItem(editor.value),
    QuoteMenuItem(editor.value),
    CodeMenuItem(editor.value),
    CodeBlockMenuItem(editor.value),
    SuperScriptMenuItem(editor.value),
    SubScriptMenuItem(editor.value),
    AlignLeftMenuItem(editor.value),
    AlignCenterMenuItem(editor.value),
    AlignRightMenuItem(editor.value),
  ];
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
  <div class="halo-moment-editor moments-relative" v-if="editor">
    <div class="flex moments-flex-col w-full h-full">
      <EditorBubbleMenu
        :editor="editor"
        :menu-items="bubbleMenuItems"
      ></EditorBubbleMenu>
      <EditorContent
        :editor="editor"
        :content-styles="{
          width: '100%',
          maxHeight: '50vh',
        }"
        class="editor-content prose prose-base !max-w-none prose-pre:p-0 bg-white prose-p:mt-3 prose-p:mb-3 prose-img:mt-0 prose-img:mb-0"
      />
    </div>
  </div>
</template>
