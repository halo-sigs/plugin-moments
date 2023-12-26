<script lang="ts" setup>
import type { ListedMoment, Moment } from "@/types";
import cloneDeep from "lodash.clonedeep";
import { ref } from "vue";
import MomentEdit from "@/components/MomentEdit.vue";
import MomentPreview from "@/components/MomentPreview.vue";
import apiClient from "@/utils/api-client";
import { Dialog, Toast, VDropdownItem } from "@halo-dev/components";

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
const editingMoment = ref<Moment>(cloneDeep(props.listedMoment?.moment));
const previewMoment = ref<Moment>(cloneDeep(props.listedMoment?.moment));

const handleSave = async (moment: Moment) => {
  moment.spec.releaseTime = new Date().toISOString();
  const { data } = await apiClient.post<Moment>(
    `/apis/uc.api.plugin.halo.run/v1alpha1/plugins/PluginMoments/moments`,
    moment
  );
  emit("save", data);
  Toast.success("发布成功");
};

const handleUpdate = async (moment: Moment) => {
  const { data } = await apiClient.get<Moment>(
    `/apis/uc.api.plugin.halo.run/v1alpha1/plugins/PluginMoments/moments/${moment.metadata.name}`
  );
  // 更新当前需要提交的 moment spec 为最新
  data.spec = moment.spec;
  const updated = await apiClient.put<Moment>(
    `/apis/uc.api.plugin.halo.run/v1alpha1/plugins/PluginMoments/moments/${moment.metadata.name}`,
    data
  );
  editingMoment.value = cloneDeep(updated.data);
  previewMoment.value = cloneDeep(updated.data);
  editing.value = false;
  emit("update", updated.data);
  Toast.success("发布成功");
};

const handleCancel = () => {
  editing.value = false;
};

const handleRemove = (name: string) => {
  Dialog.warning({
    title: "确定要删除该瞬间吗？",
    description: "该操作不可逆",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        apiClient
          .delete(
            `/apis/uc.api.plugin.halo.run/v1alpha1/plugins/PluginMoments/moments/${name}`
          )
          .then(() => {
            Toast.success("删除成功");
            emit("remove", name);
          });
      } catch (error) {
        console.error("Failed to delete comment", error);
      }
    },
  });
};
</script>
<template>
  <div>
    <MomentEdit
      v-if="editing"
      :moment="editingMoment"
      @insert="handleSave"
      @update="handleUpdate"
      @cancel="handleCancel"
    ></MomentEdit>
    <template v-else>
      <MomentPreview :moment="previewMoment" @dblclick="editing = true">
        <template #popper>
          <VDropdownItem @click="editing = true"> 编辑 </VDropdownItem>
          <VDropdownItem type="danger" @click="handleRemove">
            删除
          </VDropdownItem>
        </template>
      </MomentPreview>
    </template>
  </div>
</template>
