<script lang="ts" setup>
import {
  VEntity,
  VDropdown,
  IconArrowDown,
  IconClose,
  VEntityField,
  VEntityContainer,
} from "@halo-dev/components";
import { computed, ref } from "vue";
import { useUCTagQueryFetch } from "@/composables/use-tag";

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

const { data: tags, refetch } = useUCTagQueryFetch({
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
  <VDropdown ref="dropdown" :classes="[':uno: !p-0']" @show="refetch">
    <div
      class=":uno: group flex cursor-pointer select-none items-center border rounded-lg px-3 text-sm text-gray-700 leading-9 hover:text-black"
      :class="{ ':uno: font-semibold text-gray-700': modelValue !== undefined }"
    >
      <span v-if="!modelValue" class=":uno: mr-0.5">
        {{ label }}
      </span>
      <span v-else class=":uno: mr-0.5"> {{ label }}：{{ modelValue }} </span>
      <span class=":uno: text-base">
        <IconArrowDown :class="{ ':uno: group-hover:hidden': modelValue }" />
        <IconClose
          v-if="modelValue"
          class=":uno: hidden group-hover:block"
          @click="handleCloseTag"
        />
      </span>
    </div>
    <template #popper>
      <div class=":uno: h-96 w-80">
        <div class=":uno: border-b border-b-gray-100 bg-white p-4">
          <FormKit
            id="tagFilterDropdownInput"
            v-model="keyword"
            :placeholder="`输入${label}搜索`"
            type="text"
          ></FormKit>
        </div>
        <div>
          <VEntityContainer>
            <VEntity
              v-for="(tag, index) in searchResults"
              :key="index"
              @click="handleSelect(tag)"
              :is-selected="modelValue === tag"
            >
              <template #start>
                <VEntityField>
                  <template #title>
                    {{ tag }}
                  </template>
                </VEntityField>
              </template>
            </VEntity>
          </VEntityContainer>
        </div>
      </div>
    </template>
  </VDropdown>
</template>
