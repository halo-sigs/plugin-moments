<script lang="ts" setup>
import type { Moment } from "@/types";
import { IconArrowLeft, IconArrowRight } from "@halo-dev/components";
import { computed, inject, ref } from "vue";
import LucideFileVideo from "~icons/lucide/file-video";
import LucideFileAudio from '~icons/lucide/file-audio';
import PreviewDetailModal from "./PreviewDetailModal.vue";
import hljs from "highlight.js/lib/common";
import xml from "highlight.js/lib/languages/xml";

hljs.registerLanguage("xml", xml);

const props = defineProps<{
  moment: Moment;
}>();

const { updateTagQuery } = inject("tag") as {
  tag: string;
  updateTagQuery: (tag: string) => void;
};

const emit = defineEmits<{
  (event: "switchEditMode"): void;
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

const vTag = {
  mounted: (el: HTMLElement) => {
    const tagNodes = el.querySelectorAll("a.tag");
    for (let node of tagNodes) {
      node.addEventListener("click", (event) => {
        event.preventDefault();
        let tagName = node.textContent;
        if (tagName) {
          updateTagQuery(node.textContent || "");
        }
      });
    }
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

const handleSwitchEdit = () => {
  emit("switchEditMode");
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
    class="moment-preview-html markdown-body moments-overflow-hidden moments-relative"
    @dblclick="handleSwitchEdit"
  >
    <div v-highlight v-lazy v-tag v-html="props.moment.spec.content.html"></div>

    <div
      v-if="
        !!props.moment.spec.content.medium &&
        props.moment.spec.content.medium.length > 0
      "
      class="img-box moments-flex moments-pt-2"
    >
      <ul
        class="moments-grid moments-grid-cols-3 moments-gap-1.5 moments-w-full sm:moments-w-1/2 !moments-pl-0"
        role="list"
      >
        <li
          v-for="(media, index) in props.moment.spec.content.medium"
          :key="index"
          class="moments-rounded-md moments-border moments-overflow-hidden moments-inline-block moments-cursor-pointer"
        >
          <div
            class="moments-aspect-w-1 moments-aspect-h-1"
            @click="handleClickMedium(index)"
          >
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
            <template v-else-if="media.type == 'AUDIO'">
              <div
                class="moments-flex moments-h-full moments-w-full moments-flex-col moments-items-center moments-justify-center moments-space-y-1 moments-bg-gray-100"
              >
                <LucideFileAudio />
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
