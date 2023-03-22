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
      class="flex cursor-pointer items-center transition-all moments-relative sm:max-w-lg inline-flex items-center w-full relative box-border border border-gray-300 h-9 rounded-base overflow-hidden focus-within:border-primary focus-within:shadow-sm sm:max-w-lg"
    >
      <span class="moments-absolute moments-leading-10 moments-left-2">
        <slot name="left"></slot>
      </span>
      <input
        type="text"
        autocomplete="off"
        placeholder="输入关键词搜索"
        class="outline-0 bg-white antialiased resize-none w-full text-black block transition-all appearance-none h-full px-3 text-sm"
        v-model="searchText"
        @keyup.enter="handlerSearch"
      />
    </div>
  </div>
</template>
