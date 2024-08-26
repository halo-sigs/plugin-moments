<script lang="ts" setup>
import type { Moment, MomentMedia } from "@/types";
import { IconArrowLeft, IconArrowRight } from "@halo-dev/components";
import hljs from "highlight.js/lib/common";
import xml from "highlight.js/lib/languages/xml";
import { computed, inject, ref } from "vue";
import LucideFileAudio from "~icons/lucide/file-audio";
import LucideFileVideo from "~icons/lucide/file-video";
import PreviewDetailModal from "./PreviewDetailModal.vue";

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

function getImageThumbnailUrl(media: MomentMedia) {
  const { url } = media || {};
  return `/apis/api.storage.halo.run/thumbnails/-/via-uri?uri=${url}&width=w400`;
}
</script>
<template>
  <PreviewDetailModal
    v-if="detailVisible && selectedMedia"
    :media="selectedMedia"
    @close="detailVisible = false"
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
  <div class="relative overflow-hidden" @dblclick="handleSwitchEdit">
    <div
      v-highlight
      v-lazy
      v-tag
      class="markdown-body moment-preview-html"
      v-html="props.moment.spec.content.html"
    ></div>

    <div
      v-if="
        !!props.moment.spec.content.medium &&
        props.moment.spec.content.medium.length > 0
      "
      class="img-box flex pt-2"
    >
      <ul class="grid grid-cols-3 w-full gap-1.5 sm:w-1/2 !pl-0" role="list">
        <li
          v-for="(media, index) in props.moment.spec.content.medium"
          :key="index"
          class="inline-block cursor-pointer overflow-hidden border rounded-md"
        >
          <div class="aspect-square" @click="handleClickMedium(index)">
            <template v-if="media.type == 'PHOTO'">
              <img
                :src="getImageThumbnailUrl(media)"
                class="size-full object-cover"
                loading="lazy"
              />
            </template>
            <template v-else-if="media.type == 'VIDEO'">
              <div
                class="size-full flex flex-col items-center justify-center bg-gray-100 space-y-1"
              >
                <LucideFileVideo />
                <span class="text-xs text-gray-500 font-sans">
                  {{ getExtname(media.originType) }}
                </span>
              </div>
            </template>
            <template v-else-if="media.type == 'AUDIO'">
              <div
                class="size-full flex flex-col items-center justify-center bg-gray-100 space-y-1"
              >
                <LucideFileAudio />
                <span class="text-xs text-gray-500 font-sans">
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
