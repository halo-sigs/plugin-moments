<script lang="ts" setup>
import type { ListedMoment, Moment } from "@/types";
import { computed, ref, toRaw } from "vue";
import MomentEdit from "./MomentEdit.vue";
import MomentPreview from "./MomentPreview.vue";
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
          `/apis/moment.halo.run/v1alpha1/moments/${previewMoment.value.metadata.name}`
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

const handleApproved = async () => {
  const { data } = await axiosInstance.get<Moment>(
    `/apis/moment.halo.run/v1alpha1/moments/${editingMoment.value.metadata.name}`
  );
  // 更新当前需要提交的 moment spec 为最新
  data.spec.approved = true;
  await axiosInstance.put<Moment>(
    `/apis/moment.halo.run/v1alpha1/moments/${editingMoment.value.metadata.name}`,
    data
  );
  editingMoment.value.spec.approved = true;
  previewMoment.value.spec.approved = true;
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
            v-if="previewMoment.spec.visible == 'PRIVATE'"
            v-tooltip="{
              content: '私有访问',
            }"
          >
            <IconEyeOff class="moments-text-xs moments-text-gray-500" />
          </div>
          <div>
            <VStatusDot
              v-show="previewMoment.spec.approved === false"
              class="moments-mr-2 moments-cursor-default"
              state="success"
              animate
            >
              <template #text>
                <span class="text-xs text-gray-500">
                  {{ `待审核` }}
                </span>
              </template>
            </VStatusDot>
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
                content: formatDatetime(previewMoment.spec.releaseTime),
              }"
            >
              {{ relativeTimeTo(previewMoment.spec.releaseTime) }}
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
              <VDropdownItem
                v-if="previewMoment.spec.approved == false"
                @click="handleApproved"
              >
                审核通过
              </VDropdownItem>
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
