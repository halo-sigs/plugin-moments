<!-- TODO: 后续期望由 @halo-dev/richtext-editor 统一提供 -->
<script lang="ts" setup>
import { roundArrow } from "tippy.js";
import "tippy.js/dist/svg-arrow.css";
import type { PropType } from "vue";
import { BubbleMenu, Editor, useEditor } from "@halo-dev/richtext-editor";
import type { MenuItem } from "@/types";
import EditorLinkBubbleMenuItems from "./EditorLinkBubbleMenuItems.vue";

defineProps({
  editor: {
    type: Object as PropType<Editor>,
    required: false,
    default: () => useEditor(),
  },
  menuItems: {
    type: Array as PropType<MenuItem[]>,
    required: false,
    default: () => [],
  },
});
</script>
<template>
  <bubble-menu
    :editor="editor"
    :tippy-options="{ duration: 100, arrow: roundArrow, maxWidth: '100%' }"
  >
    <div
      class="bg-white flex items-center rounded p-1 border drop-shadow space-x-0.5"
    >
      <button
        v-for="(menuItem, index) in menuItems"
        :key="index"
        v-tooltip="menuItem.title"
        :class="{ 'bg-gray-200 !text-black': menuItem?.isActive?.() }"
        :title="menuItem.title"
        class="text-gray-600 text-lg hover:bg-gray-100 p-0.5 rounded-sm"
        @click="menuItem.action?.()"
      >
        <component :is="menuItem.icon" />
      </button>

      <div class="px-1">
        <div class="h-5 bg-gray-100" style="width: 1px"></div>
      </div>

      <EditorLinkBubbleMenuItems :editor="editor" />
    </div>
  </bubble-menu>
</template>
