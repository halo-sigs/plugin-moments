<script lang="ts" setup>
import { ref, watch, type PropType } from "vue";
import type { UseQueryReturnType } from "@tanstack/vue-query";
import type { useTagQueryFetchProps } from "@/composables/use-tag";

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
    type: Function as PropType<
      (props: useTagQueryFetchProps) => UseQueryReturnType<string[], unknown>
    >,
    required: true,
  },
});

const selectedIndex = ref(0);

const keyword = ref(props.query);

const inputIntervalTime = 100;
const timeInterval = ref(-1);
const inputTimestamp = ref<number>(0);

const { data: tags } = props.tagQueryFetch({
  keyword,
});

const inputFilter = (newValue: string | undefined) => {
  if (Date.now() - inputTimestamp.value < inputIntervalTime) {
    inputTimestamp.value = Date.now();
    return;
  } else {
    inputTimestamp.value = Date.now();
    clearInterval(timeInterval.value);
    keyword.value = newValue;
  }
};

watch(props, (newValue) => {
  clearInterval(timeInterval.value);
  timeInterval.value = setInterval(
    () => inputFilter(newValue.query),
    inputIntervalTime
  );
});

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
  if (!tags.value) return;
  selectedIndex.value =
    (selectedIndex.value + tags.value.length - 1) % tags.value.length;
  scrollToSelected();
};

const handleKeyDown = () => {
  if (!tags.value) return;
  selectedIndex.value = (selectedIndex.value + 1) % tags.value.length;
  scrollToSelected();
};

const handleKeyEnter = () => {
  handleSelectItem(selectedIndex.value);
};

const handleSelectItem = (index: number) => {
  if (!tags.value) return;
  const item = tags.value[index];
  if (item) {
    props.command(item);
  }
};

const scrollToSelected = () => {
  const selected = document.getElementById(
    `command-tag-item-${selectedIndex.value}`
  );
  if (selected) {
    selected.scrollIntoView({
      behavior: "smooth",
      block: "nearest",
      inline: "start",
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
      class="relative max-h-72 w-52 overflow-y-auto overflow-y-auto rounded-md bg-white p-1 drop-shadow"
    >
      <li
        v-for="(tag, index) in tags"
        :id="`command-tag-item-${index}`"
        :key="index"
        :class="{ 'is-selected': index === selectedIndex }"
        class="group flex flex-row items-center gap-4 rounded p-1 hover:bg-gray-100"
        @click="handleSelectItem(index)"
      >
        <span
          class="post-text text-xs text-gray-600 group-hover:text-gray-900 group-hover:font-medium"
        >
          {{ tag }}
        </span>
      </li>
    </ul>
  </div>
</template>
<style>
.group.is-selected {
  background-color: rgb(243 244 246);
}

.group.is-selected .post-text {
  color: rgb(17 24 39);
  font-weight: 500;
}
</style>
