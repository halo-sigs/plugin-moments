<script lang="ts" setup>
import type { MomentMedia } from "@/api/generated";
import { computed } from "vue";
import LucideFileAudio from "~icons/lucide/file-audio";
import LucideFileVideo from "~icons/lucide/file-video";
import MingCloseCircle from "~icons/mingcute/close-circle-fill";

const props = withDefaults(
  defineProps<{
    media: MomentMedia;
  }>(),
  {}
);

const emit = defineEmits<{
  (event: "remove", media: MomentMedia): void;
}>();

const handleRemoveClick = () => {
  emit("remove", props.media);
};

const canPlayType = (type?: string) => {
  if (!type) {
    return false;
  }
  let obj = document.createElement("video");
  return !!obj.canPlayType(type);
};
const audioType = (type?: string) => {
  if (!type) {
    return false;
  }
  let obj = document.createElement("audio");
  return !!obj.canPlayType(type);
};

const getExtname = (type?: string) => {
  if (!type) {
    return "";
  }
  const ext = type.split("/");
  if (ext.length > 1) {
    if (ext) return ext[1].toLowerCase();
  }
  return "";
};

const imageThumbnailUrl = computed(() => {
  const { url } = props.media || {};
  return `/apis/api.storage.halo.run/v1alpha1/thumbnails/-/via-uri?uri=${encodeURI(
    url || ""
  )}&size=s`;
});
</script>
<template>
  <div class=":uno: relative overflow-hidden">
    <template v-if="media.type == 'PHOTO'">
      <div class=":uno: aspect-square">
        <img :src="imageThumbnailUrl" class=":uno: size-full object-cover" loading="lazy" />
      </div>
    </template>
    <template v-else-if="media.type == 'VIDEO'">
      <div class=":uno: aspect-square">
        <video
          v-if="canPlayType(media.originType)"
          class=":uno: size-full object-cover"
          preload="metadata"
        >
          <source :src="media.url" :type="media.originType" />
        </video>
        <div
          v-else
          class=":uno: size-full flex flex-col items-center justify-center bg-gray-100 space-y-1"
        >
          <LucideFileVideo />
          <span class=":uno: text-xs text-gray-500 font-sans">
            {{ getExtname(media.originType) }}
          </span>
        </div>
      </div>
    </template>
    <template v-else-if="media.type == 'AUDIO'">
      <div class=":uno: aspect-square">
        <audio v-if="audioType(media.originType)" class=":uno: object-cover" preload="metadata">
          <source :src="media.url" :type="media.originType" />
        </audio>
        <div
          v-else
          class=":uno: size-full flex flex-col items-center justify-center bg-gray-100 space-y-1"
        >
          <LucideFileAudio />
          <span class=":uno: text-xs text-gray-500 font-sans">
            {{ getExtname(media.originType) }}
          </span>
        </div>
      </div>
    </template>
    <label class=":uno: absolute right-1 top-1 cursor-pointer">
      <MingCloseCircle class=":uno: text-gray-700" @click="handleRemoveClick" />
    </label>
  </div>
</template>
