<script lang="ts" setup>
import type { MomentMedia } from "@/types";
import MingCloseCircle from "~icons/mingcute/close-circle-fill";

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
</script>
<template>
  <div class="moments-relative moments-overflow-hidden">
    <template v-if="props.medium.type == 'PHOTO'">
      <img
        :src="props.medium.url"
        width="80"
        height="80"
        class="moments-object-cover"
      />
    </template>
    <template v-else-if="props.medium.type == 'VIDEO'">
      <video
        width="80"
        height="80"
        class="moments-object-cover moments-w-20 moments-h-20"
        preload="metadata"
      >
        <source :src="props.medium.url" :type="props.medium.originType" />
      </video>
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
