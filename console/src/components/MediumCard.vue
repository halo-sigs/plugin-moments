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
        <video class="moments-object-cover" preload="metadata">
          <source :src="props.medium.url" :type="props.medium.originType" />
        </video>
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
