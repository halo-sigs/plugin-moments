<script lang="ts" setup>
import type { MomentMedia } from "@/types";
import MingCloseCircle from "~icons/mingcute/close-circle-fill";
import LucideFileVideo from "~icons/lucide/file-video";

const props = withDefaults(
  defineProps<{
    media: MomentMedia;
  }>(),
  {
    media: undefined,
  }
);

const emit = defineEmits<{
  (event: "remove", media: MomentMedia): void;
}>();

const handleRemoveClick = () => {
  emit("remove", props.media);
};

const canPlayType = (type: string) => {
  let obj = document.createElement("video");
  return !!obj.canPlayType(type);
};

const getExtname = (type: string) => {
  if (!type) {
    return "";
  }
  const ext = type.split("/");
  if (ext.length > 1) {
    if (ext) return ext[1].toLowerCase();
  }
  return "";
};
</script>
<template>
  <div class="moments-relative moments-overflow-hidden">
    <template v-if="props.media.type == 'PHOTO'">
      <div class="aspect-w-10 aspect-h-8">
        <img
          :src="props.media.url"
          class="moments-object-cover"
          loading="lazy"
        />
      </div>
    </template>
    <template v-else-if="props.media.type == 'VIDEO'">
      <div class="aspect-w-10 aspect-h-8">
        <video
          v-if="canPlayType(props.media.originType)"
          class="moments-object-cover"
          preload="metadata"
        >
          <source :src="props.media.url" :type="props.media.originType" />
        </video>
        <div
          v-else
          class="flex h-full w-full flex-col items-center justify-center moments-space-y-1 moments-bg-gray-100"
        >
          <LucideFileVideo />
          <span class="font-sans text-xs text-gray-500">
            {{ getExtname(props.media.originType) }}
          </span>
        </div>
      </div>
    </template>
    <label
      class="moments-absolute moments-cursor-pointer moments-right-1 moments-top-1"
    >
      <MingCloseCircle
        class="moments-text-gray-700"
        @click="handleRemoveClick"
      />
    </label>
  </div>
</template>
