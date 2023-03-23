<script lang="ts" setup>
import type { MomentMedia } from "@/types";
import MingCloseCircle from "~icons/mingcute/close-circle-fill";
import LucideFileVideo from "~icons/lucide/file-video";

const props = withDefaults(
  defineProps<{
    medium: MomentMedia;
  }>(),
  {
    medium: undefined,
  }
);

const emit = defineEmits<{
  (event: "remove", medium: MomentMedia): void;
}>();

const handleRemoveClick = () => {
  emit("remove", props.medium);
};

const canPlayType = (type: string) => {
  let obj = document.createElement("video");
  return !!obj.canPlayType(type);
};

const getExtname = (type: string) => {
  const ext = type.split("/")[1];
  if (ext) return ext.toLowerCase();
  return undefined;
};
</script>
<template>
  <div class="moments-relative moments-overflow-hidden">
    <template v-if="props.medium.type == 'PHOTO'">
      <div class="aspect-w-10 aspect-h-8">
        <img
          :src="props.medium.url"
          class="moments-object-cover"
          loading="lazy"
        />
      </div>
    </template>
    <template v-else-if="props.medium.type == 'VIDEO'">
      <div class="aspect-w-10 aspect-h-8">
        <video
          v-if="canPlayType(props.medium.originType)"
          class="moments-object-cover"
          preload="metadata"
        >
          <source :src="props.medium.url" :type="props.medium.originType" />
        </video>
        <div
          v-else
          class="flex h-full w-full flex-col items-center justify-center gap-1 moments-bg-gray-100"
        >
          <LucideFileVideo />
          <span class="font-sans text-xs text-gray-500">
            {{ getExtname(props.medium.originType) }}
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
