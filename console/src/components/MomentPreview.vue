<script lang="ts" setup>
import type { Moment } from "@/types";
import apiClient from "@/utils/api-client";
import { formatDatetime } from "@/utils/date";
import {
  Dialog,
  Toast,
  IconMore,
  IconArrowLeft,
  IconArrowRight,
  VDropdown,
  VDropdownItem,
  IconEyeOff,
} from "@halo-dev/components";
import { computed, ref } from "vue";
import LucideFileVideo from "~icons/lucide/file-video";
import PreviewDetailModal from "./PreviewDetailModal.vue";
import hljs from "highlight.js";

const props = defineProps<{
  moment: Moment;
}>();

const emit = defineEmits<{
  (event: "editor"): void;
  (event: "remove", moment: Moment): void;
  (event: "cancel"): void;
  (event: "dblclick"): void;
}>();

const vHighlight = {
  mounted: (el: HTMLElement) => {
    const blocks = el.querySelectorAll("pre code");
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    blocks.forEach((block: any) => {
      hljs.highlightBlock(block);
    });
  },
};

const vLazy = {
  mounted: (el: HTMLElement) => {
    // iframe
    const iframes = el.querySelectorAll<any>("iframe");
    iframes.forEach((iframe: any) => {
      iframe.loading = "lazy";
      iframe.importance = "low";
    });
    // 图片
    const imgs = el.querySelectorAll<HTMLImageElement>("img,image");
    imgs.forEach((img: HTMLImageElement) => {
      img.loading = "lazy";
    });
    // 视频，音频
    const medium = el.querySelectorAll<HTMLMediaElement>("video,audio");
    medium.forEach((media: HTMLMediaElement) => {
      media.autoplay = false;
      media.preload = "metadata";
    });
  },
};

const mediums = ref(props.moment.spec.content.medium || []);
const detailVisible = ref<boolean>(false);
const selectedIndex = ref<number>(0);
const selectedMedia = computed(() => {
  if (mediums.value.length == 0) {
    return undefined;
  }
  return mediums.value[selectedIndex.value];
});

const deleteMoment = () => {
  Dialog.warning({
    title: "确定要删除该瞬间吗？",
    description: "该操作不可逆",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        const { data } = await apiClient.delete(
          `/apis/moment.halo.run/v1alpha1/moments/${props.moment.metadata.name}`
        );

        Toast.success("删除成功");
        emit("remove", data);
      } catch (error) {
        console.error("Failed to delete comment", error);
      }
    },
  });
};

const handlerEditor = () => {
  emit("editor");
};

const handleDblclick = () => {
  emit("dblclick");
};

const handleClickMedium = (index: number) => {
  selectedIndex.value = index;
  detailVisible.value = true;
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
  <PreviewDetailModal
    v-if="selectedMedia"
    v-model:visible="detailVisible"
    :media="selectedMedia"
    @close="(selectedMedia = undefined) && (selectedIndex = 0)"
  >
    <template #actions>
      <span
        @click="
          selectedIndex = (selectedIndex + mediums.length - 1) % mediums.length
        "
      >
        <IconArrowLeft />
      </span>
      <span @click="selectedIndex = (selectedIndex + 1) % mediums.length">
        <IconArrowRight />
      </span>
    </template>
  </PreviewDetailModal>
  <div
    class="preview card moments-bg-white moments-shrink moments-border moments-rounded-md moments-p-3.5 moments-relative"
    @dblclick="handleDblclick"
  >
    <div
      class="header moments-flex moments-items-center moments-justify-between moments-h-5"
    >
      <div class="moments-flex moments-items-center">
        <div class="moments-block moments-text-xs moments-text-gray-500">
          <span>{{ formatDatetime(props.moment.spec.releaseTime) }}</span>
        </div>

        <div v-if="props.moment.spec.visible == 'PRIVATE'" class="moments-ml-2">
          <IconEyeOff class="moments-text-xs moments-text-gray-500" />
        </div>
      </div>

      <div
        v-permission="['plugin:moments:manage']"
        class="moments-absolute moments-right-3.5"
      >
        <VDropdown
          compute-transform-origin
          :triggers="['click']"
          :popper-triggers="['click']"
        >
          <IconMore class="moments-text-gray-500 moments-cursor-pointer" />
          <template #popper>
            <VDropdownItem @click="handlerEditor"> 编辑 </VDropdownItem>
            <VDropdownItem type="danger" @click="deleteMoment">
              删除
            </VDropdownItem>
          </template>
        </VDropdown>
      </div>
    </div>
    <div
      class="moment-preview-html moments-overflow-hidden moments-relative moments-pt-1"
    >
      <div v-highlight v-lazy v-html="props.moment.spec.content.html"></div>
    </div>
    <div
      v-if="
        !!props.moment.spec.content.medium &&
        props.moment.spec.content.medium.length > 0
      "
      class="img-box moments-flex moments-pt-2"
    >
      <ul role="list">
        <li
          v-for="(media, index) in props.moment.spec.content.medium"
          :key="index"
          class="moments-rounded-md moments-border moments-overflow-hidden moments-inline-block moments-mr-2 moments-mb-2 moments-w-28 moments-cursor-pointer"
        >
          <div class="aspect-w-10 aspect-h-8" @click="handleClickMedium(index)">
            <template v-if="media.type == 'PHOTO'">
              <img
                :src="media.url"
                class="moments-object-cover"
                loading="lazy"
              />
            </template>
            <template v-else-if="media.type == 'VIDEO'">
              <div
                class="moments-flex moments-h-full moments-w-full moments-flex-col moments-items-center moments-justify-center moments-space-y-1 moments-bg-gray-100"
              >
                <LucideFileVideo />
                <span
                  class="moments-font-sans moments-text-xs moments-text-gray-500"
                >
                  {{ getExtname(media.originType) }}
                </span>
              </div>
            </template>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>
