<script lang="ts" setup>
import { ref, watch, type PropType } from "vue";
import { useQuery } from "@tanstack/vue-query";
import apiClient from "@/utils/api-client";

const props = defineProps({
  query: {
    type: String,
  },
  command: {
    type: Function as PropType<(tag: string) => void>,
    required: true,
  },
});

const selectedIndex = ref(0);

const page = ref(1);
const size = ref(20);
const keyword = ref(props.query);

const inputIntervalTime = 100;
const timeInterval = ref(-1);
const inputTimestamp = ref<number>(0);

const { data: tags } = useQuery<string[]>({
  queryKey: ["tags", page, size, keyword],
  queryFn: async () => {
    const { data } = await apiClient.get(
      "/apis/api.plugin.halo.run/v1alpha1/plugins/PluginMoments/tags",
      {
        params: {
          name: keyword.value,
        },
      }
    );
    return data;
  },
  refetchOnWindowFocus: false,
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
  <div>
    <ul
      v-if="tags?.length"
      class="relative rounded-md bg-white overflow-y-auto drop-shadow w-52 p-1 max-h-72"
    >
      <li
        v-for="(tag, index) in tags"
        :id="`command-item-${index}`"
        :key="index"
        :class="{ 'is-selected': index === selectedIndex }"
        class="group hover:bg-gray-100 flex flex-row items-center rounded gap-4 p-1"
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
