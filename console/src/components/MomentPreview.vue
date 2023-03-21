<script lang="ts" setup>
import type { Moment } from "@/types";
import apiClient from "@/utils/api-client";
import { formatDatetime } from "@/utils/date";
import { Dialog, Toast, IconMore } from "@halo-dev/components";
import { ref } from "vue";
import MdiHide from "~icons/mdi/hide";

const props = defineProps<{
  moment: Moment;
}>();

const emit = defineEmits<{
  (event: "editor"): void;
  (event: "remove", moment: Moment): void;
  (event: "cancel"): void;
}>();

const saving = ref<boolean>(false);

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
</script>
<template>
  <div
    class="preview card moments-bg-white moments-shrink moments-border moments-rounded-md moments-w-160 moments-p-3.5 moments-relative"
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

      <div class="moments-absolute moments-right-3.5">
        <FloatingDropdown
          compute-transform-origin
          :triggers="['hover', 'focus']"
          :popperTriggers="['hover', 'focus']"
        >
          <IconMore
            class="h-full w-full moments-text-gray-500 moments-cursor-pointer"
          />
          <template #popper>
            <div class="moments-w-48 moments-p-2">
              <ul class="space-y-1">
                <li
                  class="flex cursor-pointer items-center rounded px-3 py-2 text-sm text-gray-600 hover:bg-gray-100 hover:text-gray-900"
                  @click="handlerEditor"
                >
                  <span class="truncate">编辑</span>
                </li>
                <li
                  class="flex cursor-pointer items-center rounded px-3 py-2 text-sm text-gray-600 hover:bg-gray-100"
                  @click="removeMoment"
                >
                  <span class="truncate moments-text-red-500">删除</span>
                </li>
              </ul>
            </div>
          </template>
        </FloatingDropdown>
      </div>
    </div>
    <div class="moments-overflow-hidden moments-relative">
      <div v-html="props.moment.spec.content.html"></div>
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
          class="moments-rounded-md moments-border moments-overflow-hidden moments-inline-block moments-mr-2 moments-mb-2 moments-w-28"
        >
          <div class="aspect-w-10 aspect-h-8">
            <template v-if="medium.type == 'PHOTO'">
              <img
                :src="medium.url"
                class="moments-object-cover"
                loading="lazy"
              />
            </template>
            <template v-else-if="medium.type == 'VIDEO'">
              <video class="moments-object-cover" preload="metadata">
                <source :src="medium.url" :type="medium.originType" />
              </video>
            </template>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>
