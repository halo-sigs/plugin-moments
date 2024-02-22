<script lang="ts" setup>
import type { Moment } from "@/types";
import cloneDeep from "lodash.clonedeep";
import { ref } from "vue";
import MomentEdit from "./MomentEdit.vue";
import MomentPreview from "./MomentPreview.vue";
import type { Contributor } from "@halo-dev/api-client/index";
import {
  Dialog,
  Toast,
  VAvatar,
  VDropdown,
  VDropdownItem,
  IconEyeOff,
} from "@halo-dev/components";
import { formatDatetime, relativeTimeTo } from "@/utils/date";
import LucideMoreHorizontal from "~icons/lucide/more-horizontal";

import apiClient from "@/utils/api-client";

const props = withDefaults(
  defineProps<{
    moment: Moment;
    owner: Contributor;
  }>(),
  {
    moment: undefined,
    owner: undefined,
  }
);

const emit = defineEmits<{
  (event: "save", moment: Moment): void;
  (event: "update", moment: Moment): void;
  (event: "remove", moment: Moment): void;
}>();

const editing = ref(false);
const editingMoment = ref<Moment>(cloneDeep(props.moment));
const previewMoment = ref<Moment>(cloneDeep(props.moment));

const handlerSave = (moment: Moment) => {
  emit("save", moment);
};

const handlerUpdate = (moment: Moment) => {
  editingMoment.value = cloneDeep(moment);
  previewMoment.value = cloneDeep(moment);
  editing.value = false;
  emit("update", moment);
};

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

const handlerCancel = () => {
  editing.value = false;
};
</script>
<template>
  <div>
    <div
      class="preview card moments-bg-white moments-shrink moments-py-6 moments-relative moments-border-t-[1px] moments-border-gray-300"
    >
      <div
        class="header moments-flex moments-justify-between moments-items-center"
      >
        <div
          class="moments-flex moments-justify-center moments-items-center moments-space-x-3"
        >
          <VAvatar
            :alt="owner?.displayName"
            :src="owner?.avatar"
            size="md"
            circle
          ></VAvatar>
          <div>
            <b> {{ owner?.displayName }} </b>
          </div>
          <div
            v-if="props.moment.spec.visible == 'PRIVATE'"
            v-tooltip="{
              content: '私有访问',
            }"
          >
            <IconEyeOff class="moments-text-xs moments-text-gray-500" />
          </div>
        </div>
        <div
          class="moments-absolute moments-right-0 moments-flex moments-justify-center moments-items-center"
        >
          <div
            class="moments-text-xs moments-text-gray-500 moments-mr-2 moments-cursor-default"
          >
            <span
              v-tooltip="{
                content: formatDatetime(moment?.spec.releaseTime),
              }"
            >
              {{ relativeTimeTo(moment?.spec.releaseTime) }}
            </span>
          </div>
          <VDropdown
            v-permission="['plugin:moments:manage']"
            compute-transform-origin
          >
            <div
              class="moments-p-2 moments-group hover:moments-bg-sky-600/10 moments-cursor-pointer moments-rounded-full moments-flex moments-items-center moments-justify-center"
            >
              <LucideMoreHorizontal
                class="h-full w-full moments-text-md moments-text-gray-600 group-hover:moments-text-sky-600 moments-cursor-pointer"
              />
            </div>
            <template #popper>
              <VDropdownItem @click="editing = true"> 编辑 </VDropdownItem>
              <VDropdownItem type="danger" @click="deleteMoment">
                删除
              </VDropdownItem>
            </template>
          </VDropdown>
        </div>
      </div>
      <div class="moments-pl-14 moments-pt-3">
        <MomentEdit
          v-if="editing"
          :moment="editingMoment"
          @save="handlerSave"
          @update="handlerUpdate"
          @cancel="handlerCancel"
        ></MomentEdit>
        <MomentPreview
          v-else
          :moment="previewMoment"
          @switch-edit-mode="editing = true"
        />
      </div>
    </div>
  </div>
</template>
