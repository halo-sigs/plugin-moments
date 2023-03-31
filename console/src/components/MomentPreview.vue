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
} from "@halo-dev/components";
import { computed, ref } from "vue";
import MdiHide from "~icons/mdi/hide";
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

const mediums = ref(props.moment.spec.content.medium || []);
const detailVisible = ref<boolean>(false);
const selectedIndex = ref<number>(0);
const selectedMedium = computed(() => {
  if (mediums.value.length == 0) {
    return undefined;
  }
  return mediums.value[selectedIndex.value];
});

const removeMoment = () => {
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
    v-if="selectedMedium"
    v-model:visible="detailVisible"
    :medium="selectedMedium"
    @close="(selectedMedium = undefined) && (selectedIndex = 0)"
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
    class="preview card moments-bg-white moments-shrink moments-border moments-rounded-md moments-w-160 moments-p-3.5 moments-relative"
    @dblclick="handleDblclick"
  >
    <div
      class="header moments-flex moments-items-center moments-justify-between"
    >
      <div class="moments-flex moments-items-center">
        <div class="moments-block moments-text-xs moments-text-gray-500">
          <span>{{ formatDatetime(props.moment.spec.releaseTime) }}</span>
        </div>

        <div v-if="props.moment.spec.visible == 'PRIVATE'" class="moments-ml-2">
          <MdiHide class="moments-text-xs moments-text-gray-500" />
        </div>
      </div>

      <div
        class="moments-absolute moments-right-3.5"
        v-permission="['plugin:moments:manage']"
      >
        <VDropdown
          compute-transform-origin
          :triggers="['click']"
          :popperTriggers="['click']"
        >
          <IconMore
            class="h-full w-full moments-text-gray-500 moments-cursor-pointer"
          />
          <template #popper>
            <VDropdownItem @click="handlerEditor"> 编辑 </VDropdownItem>
            <VDropdownItem type="danger" @click="removeMoment">
              删除
            </VDropdownItem>
          </template>
        </VDropdown>
      </div>
    </div>
    <div class="moments-overflow-hidden moments-relative">
      <div v-highlight v-html="props.moment.spec.content.html"></div>
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
          v-for="(medium, index) in props.moment.spec.content.medium"
          :key="index"
          class="moments-rounded-md moments-border moments-overflow-hidden moments-inline-block moments-mr-2 moments-mb-2 moments-w-28 moments-cursor-pointer"
        >
          <div class="aspect-w-10 aspect-h-8" @click="handleClickMedium(index)">
            <template v-if="medium.type == 'PHOTO'">
              <img
                :src="medium.url"
                class="moments-object-cover"
                loading="lazy"
              />
            </template>
            <template v-else-if="medium.type == 'VIDEO'">
              <div
                class="flex h-full w-full flex-col items-center justify-center gap-1 moments-bg-gray-100"
              >
                <LucideFileVideo />
                <span class="font-sans text-xs text-gray-500">
                  {{ getExtname(medium.originType) }}
                </span>
              </div>
            </template>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>
