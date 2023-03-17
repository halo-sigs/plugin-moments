<script lang="ts" setup>
import type { Moment } from "@/types";
import apiClient from "@/utils/api-client";
import { formatDatetime } from "@/utils/date";
import { Dialog, Toast, VButton } from "@halo-dev/components";
import { ref } from "vue";
import MdiRemoveOctagon from "~icons/mdi/remove-octagon";
import MdiHide from "~icons/mdi/hide";

const props = defineProps<{
  moment: Moment;
}>();

const emit = defineEmits<{
  (event: "remove", moment: Moment): void;
}>();

const saving = ref<boolean>(false);
const removeBtnVisible = ref<boolean>(false);

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
</script>
<template>
  <div
    class="card moments-bg-white moments-shrink moments-border moments-rounded-md moments-w-160 moments-p-3.5 moments-relative"
    @mouseover="removeBtnVisible = true"
    @mouseout="removeBtnVisible = false"
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
        v-show="removeBtnVisible"
        class="moments-absolute moments-right-3.5"
        v-permission="['plugin:moments:manage']"
      >
        <VButton
          :loading="saving"
          size="sm"
          type="danger"
          @click="removeMoment"
        >
          <template #icon>
            <MdiRemoveOctagon class="h-full w-full" />
          </template>
        </VButton>
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
          class="moments-rounded-md moments-border moments-overflow-hidden moments-inline-block moments-mr-2 moments-mb-2"
        >
          <template v-if="medium.type == 'PHOTO'">
            <img
              :src="medium.url"
              width="112"
              height="112"
              class="moments-object-cover"
            />
          </template>
          <template v-else-if="medium.type == 'VIDEO'">
            <video
              width="112"
              height="112"
              class="moments-object-cover moments-w-28 moments-h-28"
              preload="metadata"
            >
              <source :src="medium.url" :type="medium.originType" />
            </video>
          </template>
        </li>
      </ul>
    </div>
  </div>
</template>
