<script lang="ts" setup>
import type { ListedMoment, Moment } from "@/types";
import { computed, ref, toRaw } from "vue";
import MomentEdit from "./MomentEdit.vue";
import MomentPreview from "@/components/MomentPreview.vue";
import { axiosInstance } from "@halo-dev/api-client";
import {
  Dialog,
  Toast,
  VAvatar,
  VDropdown,
  VDropdownItem,
  IconEyeOff,
  VStatusDot,
} from "@halo-dev/components";
import { formatDatetime, relativeTimeTo } from "@/utils/date";
import LucideMoreHorizontal from "~icons/lucide/more-horizontal";

const props = withDefaults(
  defineProps<{
    listedMoment: ListedMoment;
    editing: boolean;
  }>(),
  {
    listedMoment: undefined,
    editing: false,
  }
);

const emit = defineEmits<{
  (event: "save", moment: Moment): void;
  (event: "update", moment: Moment): void;
  (event: "remove", name: string): void;
}>();

const editing = ref(props.editing);
const editingMoment = ref<Moment>(toRaw(props.listedMoment?.moment));
const previewMoment = ref<Moment>(toRaw(props.listedMoment?.moment));
const owner = computed(() => props.listedMoment?.owner);

const deleteMoment = () => {
  Dialog.warning({
    title: "确定要删除该瞬间吗？",
    description: "该操作不可逆",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        const { data } = await axiosInstance.delete(
          `/apis/uc.api.moment.halo.run/v1alpha1/moments/${previewMoment.value.metadata.name}`
        );

        Toast.success("删除成功");
        emit("remove", data);
      } catch (error) {
        console.error("Failed to delete comment", error);
      }
    },
  });
};

const handleUpdate = (moment: Moment) => {
  editingMoment.value = toRaw(moment);
  previewMoment.value = toRaw(moment);
  editing.value = false;
};
</script>
<template>
  <div>
    <div
      class="preview card bg-white shrink py-6 relative border-t-[1px] border-gray-100"
    >
      <div class="header flex justify-between items-center">
        <div class="flex justify-center items-center space-x-3">
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
            v-if="previewMoment.spec.visible == 'PRIVATE'"
            v-tooltip="{
              content: '私有访问',
            }"
          >
            <IconEyeOff class="text-xs text-gray-500" />
          </div>
          <div>
            <VStatusDot
              v-show="!previewMoment.spec.approved"
              v-tooltip="'请等待管理员审核通过'"
              class="mr-2 cursor-default"
              state="success"
              animate
            >
              <template #text>
                <span class="text-xs text-gray-500">
                  {{ `审核中` }}
                </span>
              </template>
            </VStatusDot>
          </div>
        </div>
        <div class="absolute right-0 flex justify-center items-center">
          <div class="text-xs text-gray-500 mr-2 cursor-default">
            <span
              v-tooltip="{
                content: formatDatetime(previewMoment.spec.releaseTime),
              }"
            >
              {{ relativeTimeTo(previewMoment.spec.releaseTime) }}
            </span>
          </div>
          <HasPermission
            :permissions="[
              'uc:plugin:moments:publish',
              'uc:plugin:moments:delete',
            ]"
          >
            <VDropdown compute-transform-origin>
              <div
                class="p-2 group hover:bg-sky-600/10 cursor-pointer rounded-full flex items-center justify-center"
              >
                <LucideMoreHorizontal
                  class="size-full text-base text-gray-600 group-hover:text-sky-600 cursor-pointer"
                />
              </div>
              <template #popper>
                <HasPermission :permissions="['uc:plugin:moments:publish']">
                  <VDropdownItem @click="editing = true"> 编辑 </VDropdownItem>
                </HasPermission>
                <HasPermission :permissions="['uc:plugin:moments:delete']">
                  <VDropdownItem type="danger" @click="deleteMoment">
                    删除
                  </VDropdownItem>
                </HasPermission>
              </template>
            </VDropdown>
          </HasPermission>
        </div>
      </div>
      <div class="pl-14 pt-3">
        <MomentEdit
          v-if="editing"
          :moment="editingMoment"
          @update="handleUpdate"
          @cancel="editing = false"
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
