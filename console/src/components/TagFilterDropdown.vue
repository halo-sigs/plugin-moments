<script lang="ts" setup>
import {
  VEntity,
  VDropdown,
  IconArrowDown,
  IconClose,
  VEntityField,
} from "@halo-dev/components";
import { computed, ref } from "vue";
import { useConsoleTagQueryFetch } from "@/composables/use-tag";

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    label: string;
  }>(),
  {
    modelValue: undefined,
  }
);

const emit = defineEmits<{
  (event: "update:modelValue", value?: string): void;
}>();

const keyword = ref(undefined);

const { data: tags, refetch } = useConsoleTagQueryFetch({
  keyword,
});

const searchResults = computed(() => tags.value || []);

const dropdown = ref();

const handleSelect = (tag: string) => {
  if (tag === props.modelValue) {
    emit("update:modelValue", undefined);
  } else {
    emit("update:modelValue", tag);
  }

  dropdown.value.hide();
};

const handleCloseTag = (event: Event) => {
  emit("update:modelValue", undefined);
  event.stopPropagation();
};
</script>

<template>
  <VDropdown ref="dropdown" :classes="['!p-0']" @show="refetch">
    <div
      class="border group flex cursor-pointer select-none items-center text-sm leading-9 px-3 text-gray-700 rounded-lg hover:text-black"
      :class="{ 'font-semibold text-gray-700': modelValue !== undefined }"
    >
      <span v-if="!modelValue" class="mr-0.5">
        {{ label }}
      </span>
      <span v-else class="mr-0.5"> {{ label }}：{{ modelValue }} </span>
      <span class="text-base">
        <IconArrowDown :class="{ 'group-hover:hidden': modelValue }" />
        <IconClose
          v-if="modelValue"
          class="hidden group-hover:block"
          @click="handleCloseTag"
        />
      </span>
    </div>
    <template #popper>
      <div class="h-96 w-80">
        <div class="border-b border-b-gray-100 bg-white p-4">
          <FormKit
            id="tagFilterDropdownInput"
            v-model="keyword"
            :placeholder="`输入${label}搜索`"
            type="text"
          ></FormKit>
        </div>
        <div>
          <ul class="box-border size-full divide-y divide-gray-100" role="list">
            <li
              v-for="(tag, index) in searchResults"
              :key="index"
              @click="handleSelect(tag)"
            >
              <VEntity :is-selected="modelValue === tag">
                <template #start>
                  <VEntityField>
                    <template #title>
                      {{ tag }}
                    </template>
                  </VEntityField>
                </template>
              </VEntity>
            </li>
          </ul>
        </div>
      </div>
    </template>
  </VDropdown>
</template>
