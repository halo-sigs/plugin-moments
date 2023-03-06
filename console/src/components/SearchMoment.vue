<script lang="ts" setup>
import { IconSearch } from "@halo-dev/components";
import { ref } from "vue";

const searchText = ref<string>("");

const emit = defineEmits<{
  (event: "search", searchText: string): void;
}>();

const handlerSearch = () => {
  emit("search", searchText.value);
};
</script>

<template>
  <div>
    <div
      class="moments-w-60 flex cursor-pointer items-center transition-all moments-relative"
    >
      <span
        class="moments-absolute moments-text-gray-400 moments-leading-10 moments-left-2"
      >
        <IconSearch />
      </span>
      <input
        type="text"
        autocomplete="off"
        placeholder="搜索"
        class="moments-bg-gray-100 moments-px-8 moments-h-10 moments-border-gray-400 moments-border moments-rounded"
        v-model="searchText"
        @keyup.enter="handlerSearch"
      />
    </div>
  </div>
</template>
