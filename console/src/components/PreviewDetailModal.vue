<script setup lang="ts">
import type { MomentMedia } from "@/api/generated";
import { VButton, VModal } from "@halo-dev/components";
import { ref } from "vue";

const props = withDefaults(
  defineProps<{
    media: MomentMedia;
  }>(),
  {
    media: undefined,
  }
);

const emit = defineEmits<{
  (event: "close"): void;
}>();

const modal = ref<InstanceType<typeof VModal> | null>(null);
</script>
<template>
  <VModal
    ref="modal"
    title="预览"
    :width="1000"
    :layer-closable="true"
    height="80vh"
    @close="emit('close')"
  >
    <template #actions>
      <slot name="actions"></slot>
    </template>
    <div
      class="h-full flex items-center justify-center overflow-hidden bg-white"
    >
      <template v-if="props.media.type === 'PHOTO'">
        <img :src="media?.url" class="h-full w-auto" />
      </template>
      <template v-else-if="props.media.type === 'VIDEO'">
        <video controls muted :src="media?.url">
          当前浏览器不支持该视频播放
        </video>
      </template>
    </div>
    <template #footer>
      <VButton type="default" @click="modal?.close()">关闭 Esc</VButton>
    </template>
  </VModal>
</template>
