<script lang="ts" setup>
import type { ListedMoment, Moment } from "@/types";
import { formatDatetime, relativeTimeTo } from "@/utils/date";
import { axiosInstance } from "@halo-dev/api-client";
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
import MomentPreview from "./MomentPreview.vue";

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
      class="preview card bg-white shrink py-6 relative border-t border-gray-100"
    >
      <div class="flex items-start gap-3">
        <VAvatar
          :alt="owner?.displayName"
          :src="owner?.avatar"
          size="md"
          circle
          class="flex-none"
        ></VAvatar>
        <div class="flex-1 shrink min-w-0">
          <div class="flex justify-between items-center">
            <div class="flex items-center space-x-3">
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
                  v-show="previewMoment.spec.approved === false"
                  class="mr-2 cursor-default"
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

            <div class="flex items-center">
              <div class="text-xs text-gray-500 mr-2 cursor-default">
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
                  class="p-2 group hover:bg-sky-600/10 cursor-pointer rounded-full flex items-center justify-center"
                >
                  <LucideMoreHorizontal
                    class="size-full text-base text-gray-600 group-hover:text-sky-600 cursor-pointer"
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

          <div class="mt-3">
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
