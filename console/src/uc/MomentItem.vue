<script lang="ts" setup>
import { momentsUcApiClient } from "@/api";
import type { ListedMoment, Moment } from "@/api/generated";
import MomentPreview from "@/components/MomentPreview.vue";
import { formatDatetime, relativeTimeTo } from "@/utils/date";
import {
  Dialog,
  IconEyeOff,
  Toast,
  VAvatar,
  VDropdown,
  VDropdownItem,
  VStatusDot,
} from "@halo-dev/components";
import { computed, ref, toRaw } from "vue";
import LucideMoreHorizontal from "~icons/lucide/more-horizontal";
import MomentEdit from "./MomentEdit.vue";

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
  (event: "remove"): void;
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
        await momentsUcApiClient.moment.deleteMyMoment({
          name: previewMoment.value.metadata.name,
        });

        Toast.success("删除成功");
        emit("remove");
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
      class=":uno: preview card relative shrink border-t-[1px] border-gray-100 bg-white py-6"
    >
      <div class=":uno: flex items-start gap-3">
        <VAvatar
          :alt="owner?.displayName"
          :src="owner?.avatar"
          size="md"
          circle
          class=":uno: flex-none"
        ></VAvatar>
        <div class=":uno: min-w-0 flex-1 shrink">
          <div class=":uno: flex items-center justify-between">
            <div class=":uno: flex items-center space-x-3">
              <div>
                <b> {{ owner?.displayName }} </b>
              </div>
              <div
                v-if="previewMoment.spec.visible == 'PRIVATE'"
                v-tooltip="{
                  content: '私有访问',
                }"
              >
                <IconEyeOff class=":uno: text-xs text-gray-500" />
              </div>
              <div>
                <VStatusDot
                  v-show="!previewMoment.spec.approved"
                  v-tooltip="'请等待管理员审核通过'"
                  class=":uno: mr-2 cursor-default"
                  state="success"
                  animate
                >
                  <template #text>
                    <span class=":uno: text-xs text-gray-500">
                      {{ `审核中` }}
                    </span>
                  </template>
                </VStatusDot>
              </div>
            </div>
            <div class=":uno: flex items-center">
              <div class=":uno: mr-2 cursor-default text-xs text-gray-500">
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
                    class=":uno: group flex cursor-pointer items-center justify-center rounded-full p-2 hover:bg-sky-600/10"
                  >
                    <LucideMoreHorizontal
                      class=":uno: size-full cursor-pointer text-base text-gray-600 group-hover:text-sky-600"
                    />
                  </div>
                  <template #popper>
                    <HasPermission :permissions="['uc:plugin:moments:publish']">
                      <VDropdownItem @click="editing = true">
                        编辑
                      </VDropdownItem>
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

          <div class=":uno: mt-3">
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
    </div>
  </div>
</template>
