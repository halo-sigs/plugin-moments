<script lang="ts" setup>
import type { ListedMoment, Moment, MomentMedia } from "@/api/generated";
import { IconArrowLeft, IconArrowRight, IconMessage } from "@halo-dev/components";
import { computed, inject, ref } from "vue";
import LucideFileAudio from "~icons/lucide/file-audio";
import LucideFileVideo from "~icons/lucide/file-video";
import PreviewDetailModal from "./PreviewDetailModal.vue";
import RiHeart3Line from "~icons/ri/heart-3-line";
import { useQueryClient } from "@tanstack/vue-query";
import ShikiDirective from "@/plugin-supports/shiki/directive";
import { utils } from "@halo-dev/console-shared";

const props = defineProps<{
  moment: ListedMoment;
  uc: boolean;
}>();

const { updateTagQuery } = inject("tag") as {
  tag: string;
  updateTagQuery: (tag: string) => void;
};

const emit = defineEmits<{
  (event: "switchEditMode"): void;
}>();

const queryClient = useQueryClient();

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

const mediums = ref(props.moment.moment.spec.content.medium || []);
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

const commentText = computed(() => {
  const { totalComment, approvedComment } = props.moment.stats || {};

  let text = totalComment || "0";

  const pendingComments = (totalComment || 0) - (approvedComment || 0);

  if (pendingComments > 0) {
    text += `（${pendingComments} 条待审核）`;
  }
  return text;
});

const commentListVisible = ref(false);
const commentSubjectRefKey = `moment.halo.run/Moment/${props.moment.moment.metadata.name}`;

function onCommentListClose() {
  commentListVisible.value = false;

  queryClient.invalidateQueries({
    queryKey: ["plugin:moments:list"],
  });
}

function handleOpenCommentList() {
  if (props.uc) {
    window.open(`/moments/${props.moment.moment.metadata.name}`, "_blank");
    return;
  }
  commentListVisible.value = true;
}

defineOptions({
  directives: {
    shiki: ShikiDirective,
  },
});
</script>
<template>
  <PreviewDetailModal
    v-if="detailVisible && selectedMedia"
    :media="selectedMedia"
    @close="detailVisible = false"
  >
    <template #actions>
      <span @click="selectedIndex = (selectedIndex + mediums.length - 1) % mediums.length">
        <IconArrowLeft />
      </span>
      <span @click="selectedIndex = (selectedIndex + 1) % mediums.length">
        <IconArrowRight />
      </span>
    </template>
  </PreviewDetailModal>
  <div class=":uno: relative overflow-hidden" @dblclick="handleSwitchEdit">
    <div
      v-lazy
      v-tag
      v-shiki
      class=":uno: markdown-body moment-preview-html"
      v-html="moment.moment.spec.content.html"
    ></div>

    <div
      v-if="!!moment.moment.spec.content.medium && moment.moment.spec.content.medium.length > 0"
      class=":uno: img-box flex pt-2"
    >
      <ul class=":uno: grid grid-cols-3 w-full gap-1.5 sm:w-1/2 !pl-0" role="list">
        <li
          v-for="(media, index) in moment.moment.spec.content.medium"
          :key="index"
          class=":uno: inline-block cursor-pointer overflow-hidden border rounded-md"
        >
          <div class=":uno: aspect-square" @click="handleClickMedium(index)">
            <template v-if="media.type == 'PHOTO'">
              <img
                :src="utils.attachment.getThumbnailUrl(media.url!, 'S')"
                class=":uno: size-full object-cover"
                loading="lazy"
              />
            </template>
            <template v-else-if="media.type == 'VIDEO'">
              <div
                class=":uno: size-full flex flex-col items-center justify-center bg-gray-100 space-y-1"
              >
                <LucideFileVideo />
                <span class=":uno: text-xs text-gray-500 font-sans">
                  {{ getExtname(media.originType) }}
                </span>
              </div>
            </template>
            <template v-else-if="media.type == 'AUDIO'">
              <div
                class=":uno: size-full flex flex-col items-center justify-center bg-gray-100 space-y-1"
              >
                <LucideFileAudio />
                <span class=":uno: text-xs text-gray-500 font-sans">
                  {{ getExtname(media.originType) }}
                </span>
              </div>
            </template>
          </div>
        </li>
      </ul>
    </div>

    <div class=":uno: mt-4 flex items-center gap-3">
      <div class=":uno: inline-flex items-center gap-1 text-gray-600 hover:text-gray-900">
        <RiHeart3Line />
        <span class=":uno: text-sm">{{ moment.stats.upvote }}</span>
      </div>
      <div
        class=":uno: group inline-flex cursor-pointer items-center gap-1 text-gray-600 hover:text-gray-900"
        @click="handleOpenCommentList"
      >
        <IconMessage />
        <span class=":uno: text-sm group-hover:underline">{{ commentText }}</span>
      </div>
    </div>

    <SubjectQueryCommentListModal
      v-if="commentListVisible && !uc"
      :subject-ref-key="commentSubjectRefKey"
      @close="onCommentListClose"
    />
  </div>
</template>
