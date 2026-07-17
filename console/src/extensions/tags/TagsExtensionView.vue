<script lang="ts" setup>
import type { TagQueryFetch } from "@/composables/use-tag";
import { onBeforeUnmount, ref, watch, type PropType } from "vue";

const props = defineProps({
  query: {
    type: String,
    required: true,
  },
  command: {
    type: Function as PropType<(tag: string) => void>,
    required: true,
  },
  tagQueryFetch: {
    type: Function as PropType<TagQueryFetch>,
    required: true,
  },
});

const selectedIndex = ref(0);
const tags = ref<string[]>([]);
let debounceTimer: ReturnType<typeof setTimeout> | undefined;
let requestId = 0;

const loadTags = async (keyword: string) => {
  const currentRequestId = ++requestId;
  const result = await props.tagQueryFetch(keyword);
  if (currentRequestId === requestId) {
    tags.value = result;
    selectedIndex.value = 0;
  }
};

watch(
  () => props.query,
  (query, previousQuery) => {
    clearTimeout(debounceTimer);
    if (previousQuery === undefined) {
      void loadTags(query);
      return;
    }
    debounceTimer = setTimeout(() => void loadTags(query), 100);
  },
  { immediate: true }
);

onBeforeUnmount(() => clearTimeout(debounceTimer));

// TODO: 滚动条会跟随外部滚动条一起移动，需要处理
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
  if (!tags.value.length) return;
  selectedIndex.value = (selectedIndex.value + tags.value.length - 1) % tags.value.length;
  scrollToSelected();
};

const handleKeyDown = () => {
  if (!tags.value.length) return;
  selectedIndex.value = (selectedIndex.value + 1) % tags.value.length;
  scrollToSelected();
};

const handleKeyEnter = () => {
  handleSelectItem(selectedIndex.value);
};

const handleSelectItem = (index: number) => {
  if (!tags.value.length) return;
  const item = tags.value[index];
  if (item) {
    props.command(item);
  }
};

const scrollToSelected = () => {
  const selected = document.getElementById(`command-tag-item-${selectedIndex.value}`);
  if (selected) {
    selected.scrollIntoView({
      behavior: "smooth",
      // @unocss-skip-start
      block: "nearest",
      inline: "start",
      // @unocss-skip-end
    });
  }
};

defineExpose({
  onKeyDown,
});
</script>

<template>
  <div>
    <ul
      v-if="tags?.length"
      class=":uno: relative max-h-72 w-52 overflow-y-auto overflow-y-auto rounded-md bg-white p-1 drop-shadow"
    >
      <li
        v-for="(tag, index) in tags"
        :id="`command-tag-item-${index}`"
        :key="index"
        :class="{ ':uno: is-selected': index === selectedIndex }"
        class=":uno: group flex flex-row items-center gap-4 rounded p-1 hover:bg-gray-100"
        @click="handleSelectItem(index)"
      >
        <span
          class=":uno: post-text text-xs text-gray-600 group-hover:text-gray-900 group-hover:font-medium"
        >
          {{ tag }}
        </span>
      </li>
    </ul>
  </div>
</template>
<style lang="scss">
.group.is-selected {
  background-color: rgb(243 244 246);
}

.group.is-selected .post-text {
  color: rgb(17 24 39);
  font-weight: 500;
}
</style>
