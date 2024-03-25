<script lang="ts" setup>
import {
  IconArrowDown,
  IconClose,
  VDropdown,
  VDropdownItem,
} from "@halo-dev/components";
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    items: {
      label: string;
      value?: string | boolean | number;
    }[];
    label: string;
    modelValue?: string | boolean | number;
  }>(),
  {
    modelValue: undefined,
  }
);

const emit = defineEmits<{
  (
    event: "update:modelValue",
    modelValue: string | boolean | number | undefined
  ): void;
}>();

const selectedItem = computed(() => {
  return props.items.find((item) => item.value === props.modelValue);
});

function handleSelect(item: {
  label: string;
  value?: string | boolean | number;
}) {
  if (item.value === props.modelValue) {
    emit("update:modelValue", undefined);
    return;
  }
  emit("update:modelValue", item.value);
}

const handleClear = (event: Event) => {
  emit("update:modelValue", undefined);
  event.stopPropagation();
};
</script>

<template>
  <VDropdown>
    <div
      class="filter-dropdown moments-group"
      :class="{
        'moments-font-semibold moments-text-gray-700': modelValue !== undefined,
      }"
    >
      <span v-if="!selectedItem" class="mr-0.5">
        {{ label }}
      </span>
      <span v-else class="mr-0.5"> {{ label }}：{{ selectedItem.label }} </span>
      <span class="moments-text-base">
        <IconArrowDown
          :class="{ 'group-hover:moments-hidden': modelValue !== undefined }"
        />
        <IconClose
          v-if="modelValue !== undefined"
          class="moments-hidden group-hover:moments-block"
          @click="handleClear"
        />
      </span>
    </div>
    <template #popper>
      <VDropdownItem
        v-for="(item, index) in items"
        :key="index"
        :selected="item.value === modelValue"
        @click="handleSelect(item)"
      >
        {{ item.label }}
      </VDropdownItem>
    </template>
  </VDropdown>
</template>
<style scoped lang="scss">
.filter-dropdown {
  @apply moments-flex moments-cursor-pointer moments-select-none moments-items-center moments-text-sm moments-leading-9 moments-px-3 moments-text-gray-700 moments-rounded-lg hover:moments-text-black;
  border: 1px solid rgb(209 213 219);
}
</style>
