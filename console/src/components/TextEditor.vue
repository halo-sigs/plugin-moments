<script lang="ts" setup>
import type { useTagQueryFetchProps } from "@/composables/use-tag";
import { TagsExtension } from "@/extensions/tags";
import { ucApiClient } from "@halo-dev/api-client";
import { VLoading } from "@halo-dev/components";
import {
  ExtensionsKit,
  RichTextEditor,
  VueEditor,
  type Extensions,
} from "@halo-dev/richtext-editor";
import { utils, type PluginModule } from "@halo-dev/ui-shared";
import type { UseQueryReturnType } from "@tanstack/vue-query";
import type { AxiosRequestConfig } from "axios";
import { onMounted, ref, shallowRef, watch } from "vue";

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

const editor = shallowRef<VueEditor>();

const supportedPluginNames = ["editor-hyperlink-card", "hybrid-edit-block", "shiki"];

const customExtensions = [
  TagsExtension.configure({
    tagQueryFetch: props.tagQueryFetch,
  }),
];

const isInitialized = ref(false);

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

  editor.value = new VueEditor({
    content: props.raw,
    extensions: [
      ExtensionsKit.configure({
        placeholder: {
          placeholder: "有什么想说的吗...",
        },
        image: {
          uploadImage: handleUpload,
        },
        video: {
          uploadVideo: handleUpload,
        },
        audio: {
          uploadAudio: handleUpload,
        },
        gallery: {
          uploadImage: handleUpload,
        },
        customExtensions: [...customExtensions, ...extensionsFromPlugins],
      }),
    ],
    autofocus: "end",
    onUpdate: () => {
      emit("update:raw", editor.value?.getHTML() + "");
      emit("update:html", editor.value?.getHTML() + "");
      emit("update:isEmpty", editor.value?.isEmpty);
      emit("update", editor.value?.getHTML() + "");
    },
    onCreate: () => {
      isInitialized.value = true;
    },
  });
});

async function handleUpload(file: File, options?: AxiosRequestConfig) {
  if (!utils.permission.has(["uc:attachments:manage"])) {
    throw new Error("Permission denied");
  }
  const { data } = await ucApiClient.storage.attachment.uploadUcAttachment(
    {
      file,
    },
    options
  );
  return data;
}

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
  <div class=":uno: halo-moment-editor relative">
    <VLoading v-if="!isInitialized" />
    <RichTextEditor v-else-if="editor" :editor="editor" locale="zh-CN"> </RichTextEditor>
  </div>
</template>

<style lang="scss">
.halo-moment-editor {
  .ProseMirror {
    padding-bottom: 2rem !important;
    padding-top: 2rem !important;
  }
}
</style>
