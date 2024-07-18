<script lang="ts" setup>
import type { MomentMedia } from "@/types";
import MingCloseCircle from "~icons/mingcute/close-circle-fill";
import LucideFileVideo from "~icons/lucide/file-video";
import LucideFileAudio from "~icons/lucide/file-audio";

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
const audioType = (type: string) => {
  let obj = document.createElement("audio");
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
  <div class="relative overflow-hidden">
    <template v-if="props.media.type == 'PHOTO'">
      <div class="aspect-square">
        <img
          :src="props.media.url"
          class="object-cover size-full"
          loading="lazy"
        />
      </div>
    </template>
    <template v-else-if="props.media.type == 'VIDEO'">
      <div class="aspect-square">
        <video
          v-if="canPlayType(props.media.originType)"
          class="object-cover size-full"
          preload="metadata"
        >
          <source :src="props.media.url" :type="props.media.originType" />
        </video>
        <div
          v-else
          class="flex size-full flex-col items-center justify-center space-y-1 bg-gray-100"
        >
          <LucideFileVideo />
          <span class="font-sans text-xs text-gray-500">
            {{ getExtname(props.media.originType) }}
          </span>
        </div>
      </div>
    </template>
    <template v-else-if="props.media.type == 'AUDIO'">
      <div class="aspect-square">
        <audio
          v-if="audioType(props.media.originType)"
          class="object-cover"
          preload="metadata"
        >
          <source :src="props.media.url" :type="props.media.originType" />
        </audio>
        <div
          v-else
          class="flex size-full flex-col items-center justify-center space-y-1 bg-gray-100"
        >
          <LucideFileAudio />
          <span class="font-sans text-xs text-gray-500">
            {{ getExtname(props.media.originType) }}
          </span>
        </div>
      </div>
    </template>
    <label class="absolute cursor-pointer right-1 top-1">
      <MingCloseCircle class="text-gray-700" @click="handleRemoveClick" />
    </label>
  </div>
</template>
