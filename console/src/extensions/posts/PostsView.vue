<script lang="ts" setup>
import { ref, watch, type PropType } from "vue";
import { useQuery } from "@tanstack/vue-query";
import type { ListedPost, Post } from "@halo-dev/api-client/index";
import { postApiClient } from "@/utils/api-client";

const props = defineProps({
  query: {
    type: String,
  },
  command: {
    type: Function as PropType<(post: Post) => void>,
    required: true,
  },
});

const selectedIndex = ref(0);

const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasPrevious = ref(false);
const hasNext = ref(false);

const keyword = ref(props.query);

const inputIntervalTime = 1000;
const timeInterval = ref(-1);
const inputTimestamp = ref<number>(0);

const {
  data: posts,
  isLoading,
  isFetching,
  refetch,
} = useQuery<ListedPost[]>({
  queryKey: ["posts", page, size, keyword],
  queryFn: async () => {
    const labelSelector: string[] = ["content.halo.run/deleted=false"];

    const { data } = await postApiClient.listPosts({
      labelSelector,
      page: page.value,
      size: size.value,
      keyword: keyword.value,
    });

    total.value = data.total;
    hasNext.value = data.hasNext;
    hasPrevious.value = data.hasPrevious;

    return data.items;
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

watch(props, function (newValue) {
  clearInterval(timeInterval.value);
  timeInterval.value = setInterval(
    () => inputFilter(newValue.query),
    inputIntervalTime
  );
});

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
  if (!posts.value) return;
  selectedIndex.value =
    (selectedIndex.value + posts.value.length - 1) % posts.value.length;
  scrollToSelected();
};

const handleKeyDown = () => {
  if (!posts.value) return;
  selectedIndex.value = (selectedIndex.value + 1) % posts.value.length;
  scrollToSelected();
};

const handleKeyEnter = () => {
  handleSelectItem(selectedIndex.value);
};

const handleSelectItem = (index: number) => {
  if (!posts.value) return;
  const item = posts.value[index];
  if (item) {
    props.command(item.post);
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
    class="relative rounded-md bg-white overflow-y-auto drop-shadow w-52 p-1 max-h-72"
  >
    <template v-if="posts?.length">
      <div
        v-for="(post, index) in posts"
        :id="`command-item-${index}`"
        :key="index"
        :class="{ 'is-selected': index === selectedIndex }"
        class="group hover:bg-gray-100 flex flex-row items-center rounded gap-4 p-1"
        @click="handleSelectItem(index)"
      >
        <span
          class="post-text text-xs text-gray-600 group-hover:text-gray-900 group-hover:font-medium"
        >
          {{ post.post.spec.title }}
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
<style>
.group.is-selected {
  background-color: rgb(243 244 246);
}

.group.is-selected .post-text {
  color: rgb(17 24 39);
  font-weight: 500;

}
</style>
