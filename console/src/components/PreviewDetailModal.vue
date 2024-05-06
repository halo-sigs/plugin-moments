<script setup lang="ts">
import type { MomentMedia } from "@/types";
import { VModal, VButton, VSpace } from "@halo-dev/components";

const props = withDefaults(
  defineProps<{
    visible: boolean;
    media: MomentMedia;
  }>(),
  {
    visible: false,
    media: undefined,
  }
);

const emit = defineEmits<{
  (event: "update:visible", visible: boolean): void;
  (event: "close"): void;
}>();

const onVisibleChange = (visible: boolean) => {
  emit("update:visible", visible);
  if (!visible) {
    emit("close");
  }
};
</script>
<template>
  <VModal
    title="预览"
    :visible="visible"
    :width="1000"
    :layer-closable="true"
    height="80vh"
    @update:visible="onVisibleChange"
  >
    <template #actions>
      <slot name="actions"></slot>
    </template>
    <div
      class="overflow-hidden bg-white moments-flex moments-items-center moments-justify-center moments-h-full"
    >
      <template v-if="props.media.type === 'PHOTO'">
        <img :src="media?.url" class="moments-w-auto moments-h-full" />
      </template>
      <template v-else-if="props.media.type === 'VIDEO'">
        <video controls muted :src="media?.url">
          当前浏览器不支持该视频播放
        </video>
      </template>
    </div>
    <template #footer>
      <VSpace>
        <VButton type="default" @click="onVisibleChange(false)"
          >关闭 Esc</VButton
        >
        <slot name="footer" />
      </VSpace>
    </template>
  </VModal>
</template>
