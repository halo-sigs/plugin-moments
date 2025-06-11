<!-- TODO: 后续期望由 @halo-dev/richtext-editor 统一提供 -->
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
  <VDropdown class=":uno: inline-flex" :triggers="['click']" :distance="10">
    <button
      v-tooltip="`${editor.isActive('link') ? '修改' : '添加'}链接`"
      class=":uno: rounded-sm p-0.5 text-lg text-gray-600 hover:bg-gray-100"
      :class="{ ':uno: bg-gray-200 !text-black': editor.isActive('link') }"
    >
      <MdiLinkVariant />
    </button>

    <template #popper>
      <div
        class=":uno: relative max-h-72 w-96 overflow-hidden overflow-y-auto rounded-md bg-white p-1 drop-shadow"
      >
        <input
          v-model.lazy="href"
          placeholder="链接地址"
          class=":uno: block w-full border border-gray-300 rounded-md bg-gray-50 px-2 py-1.5 text-sm text-gray-900 focus:border-blue-500 hover:bg-gray-100 focus:ring-blue-500"
        />
        <label class=":uno: mt-2 inline-flex items-center">
          <input
            v-model="target"
            type="checkbox"
            class=":uno: form-checkbox border-gray-300 rounded text-blue-600 focus:ring-blue-500"
          />
          <span class=":uno: ml-2 text-sm text-gray-500">在新窗口打开</span>
        </label>
      </div>
    </template>
  </VDropdown>

  <button
    v-if="editor.isActive('link')"
    v-tooltip="`取消链接`"
    class=":uno: rounded-sm p-0.5 text-lg text-gray-600 hover:bg-gray-100"
    @click="handleUnSetLink"
  >
    <MdiLinkVariantOff />
  </button>

  <a
    v-if="editor.isActive('link')"
    v-tooltip="`打开链接`"
    class=":uno: rounded-sm p-0.5 text-lg text-gray-600 hover:bg-gray-100"
    :href="editor.getAttributes('link')?.href"
    target="_blank"
  >
    <MdiShare />
  </a>
</template>
