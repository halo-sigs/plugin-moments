<script lang="ts" setup>
import { ref, watch, type PropType } from "vue";
import type { Item } from "./index";

const props = defineProps({
  items: {
    type: Array as PropType<Item[]>,
    required: true,
  },
  command: {
    type: Function as PropType<(item: Item) => void>,
    required: true,
  },
});

const selectedIndex = ref(0);

watch(
  () => props.items,
  () => {
    selectedIndex.value = 0;
  }
);

const onKeyDown = ({ event }: { event: KeyboardEvent }) => {
  if (event.key === "ArrowUp" || (event.key === "k" && event.ctrlKey)) {
    handleKeyUp();
    return true;
  }
  if (event.key === "ArrowDown" || (event.key === "j" && event.ctrlKey)) {
    handleKeyDown();
    return true;
  }
  if (event.key === "Enter") {
    handleKeyEnter();
    return true;
  }
  return false;
};

const handleKeyUp = () => {
  selectedIndex.value =
    (selectedIndex.value + props.items.length - 1) % props.items.length;
  scrollToSelected();
};

const handleKeyDown = () => {
  selectedIndex.value = (selectedIndex.value + 1) % props.items.length;
  scrollToSelected();
};

const handleKeyEnter = () => {
  handleSelectItem(selectedIndex.value);
};

const handleSelectItem = (index: number) => {
  const item = props.items[index];
  if (item) {
    props.command(item);
  }
};

const scrollToSelected = () => {
  const selected = document.getElementById(
    `command-item-${selectedIndex.value}`
  );
  if (selected) {
    selected.scrollIntoView({
      behavior: "smooth",
    });
  }
};

defineExpose({
  onKeyDown,
});
</script>

<template>
  <div
    class="relative rounded-md bg-white overflow-hidden drop-shadow w-52 p-1 max-h-72 overflow-y-auto;"
  >
    <template v-if="items.length">
      <div
        v-for="(item, index) in items"
        :id="`command-item-${index}`"
        :key="index"
        :class="{ 'is-selected': index === selectedIndex }"
        class="group hover:bg-gray-100 flex flex-row items-center rounded gap-4 p-1 peer-[.is-selected]:bg-gray-100"
        @click="handleSelectItem(index)"
      >
        <span
          class="text-xs text-gray-600 group-hover:text-gray-900 group-hover:font-medium peer-[.is-selected]:text-gray-900:font-medium"
        >
          {{ item.post.spec.title }}
        </span>
      </div>
    </template>
    <div
      v-else
      class="flex justify-center items-center p-1 text-xs text-gray-600"
    >
      <span>没有搜索结果</span>
    </div>
  </div>
</template>
