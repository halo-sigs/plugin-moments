<script lang="ts" setup>
import type { Moment } from "@/types";
import cloneDeep from "lodash.clonedeep";
import { ref } from "vue";
import MomentEdit from "./MomentEdit.vue";
import MomentPreview from "./MomentPreview.vue";

const props = withDefaults(
  defineProps<{
    moment: Moment;
  }>(),
  {
    moment: undefined,
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

const handlerRemove = (moment: Moment) => {
  emit("remove", moment);
};
</script>
<template>
  <div
    @dblclick="editing = !editing"
    class="moments-flex moments-justify-center moments-mt-2"
  >
    <MomentEdit
      v-if="editing"
      :moment="editingMoment"
      @save="handlerSave"
      @update="handlerUpdate"
    ></MomentEdit>
    <template v-else>
      <MomentPreview :moment="previewMoment" @remove="handlerRemove" />
    </template>
  </div>
</template>
