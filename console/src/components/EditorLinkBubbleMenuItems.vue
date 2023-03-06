<script lang="ts" setup>
import type { Editor } from "@halo-dev/richtext-editor";
import { computed, type PropType } from "vue";
import { Dropdown as VDropdown } from "floating-vue";
import MdiLinkVariant from "~icons/mdi/link-variant";
import MdiLinkVariantOff from "~icons/mdi/link-variant-off";
import MdiShare from "~icons/mdi/share";

const props = defineProps({
  editor: {
    type: Object as PropType<Editor>,
    required: true,
  },
});

const href = computed({
  get() {
    const attrs = props.editor.getAttributes("link");
    return attrs?.href;
  },
  set(value) {
    props.editor.commands.setLink({
      href: value,
      target: target.value ? "_blank" : "_self",
    });
  },
});

const target = computed({
  get() {
    const attrs = props.editor.getAttributes("link");
    return attrs?.target === "_blank";
  },
  set(value) {
    props.editor.commands.setLink({
      href: href.value,
      target: value ? "_blank" : "_self",
    });
  },
});

function handleUnSetLink() {
  props.editor.commands.unsetLink();
}
</script>

<template>
  <VDropdown class="inline-flex" :triggers="['click']" :distance="10">
    <button
      v-tooltip="`${editor.isActive('link') ? '修改' : '添加'}链接`"
      class="text-gray-600 text-lg hover:bg-gray-100 p-0.5 rounded-sm"
      :class="{ 'bg-gray-200 !text-black': editor.isActive('link') }"
    >
      <MdiLinkVariant />
    </button>

    <template #popper>
      <div
        class="relative rounded-md bg-white overflow-hidden drop-shadow w-96 p-1 max-h-72 overflow-y-auto"
      >
        <input
          v-model.lazy="href"
          placeholder="链接地址"
          class="bg-gray-50 rounded-md hover:bg-gray-100 block px-2 w-full py-1.5 text-sm text-gray-900 border border-gray-300 focus:ring-blue-500 focus:border-blue-500"
        />
        <label class="inline-flex items-center mt-2">
          <input
            v-model="target"
            type="checkbox"
            class="form-checkbox text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
          />
          <span class="ml-2 text-sm text-gray-500">在新窗口打开</span>
        </label>
      </div>
    </template>
  </VDropdown>

  <button
    v-if="editor.isActive('link')"
    v-tooltip="`取消链接`"
    class="text-gray-600 text-lg hover:bg-gray-100 p-0.5 rounded-sm"
    @click="handleUnSetLink"
  >
    <MdiLinkVariantOff />
  </button>

  <a
    v-if="editor.isActive('link')"
    v-tooltip="`打开链接`"
    class="text-gray-600 text-lg hover:bg-gray-100 p-0.5 rounded-sm"
    :href="editor.getAttributes('link')?.href"
    target="_blank"
  >
    <MdiShare />
  </a>
</template>
