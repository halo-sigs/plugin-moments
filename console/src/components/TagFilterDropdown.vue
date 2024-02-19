<script lang="ts" setup>
import {
  VEntity,
  VDropdown,
  IconArrowDown,
  VEntityField,
} from "@halo-dev/components";
import { computed, ref } from "vue";
import { useQuery } from "@tanstack/vue-query";
import apiClient from "@/utils/api-client";

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
  (event: "clearFilter"): void;
}>();

const page = ref(1);
const size = ref(20);
const keyword = ref<string | undefined>(undefined);

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
</script>

<template>
  <VDropdown ref="dropdown" :classes="['!p-0']">
    <div
      class="tag-filter-label"
      :class="{ 'font-semibold text-gray-700': modelValue !== undefined }"
    >
      <span v-if="!modelValue" class="mr-0.5">
        {{ label }}
      </span>
      <span v-else class="mr-0.5"> {{ label }}：{{ modelValue }} </span>
      <span>
        <IconArrowDown />
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
          <ul
            class="box-border h-full w-full divide-y divide-gray-100"
            role="list"
          >
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
<style scoped lang="scss">
.tag-filter-label {
  @apply moments-flex moments-cursor-pointer moments-select-none moments-items-center moments-text-sm moments-leading-9 moments-px-3 moments-text-gray-700 moments-rounded-lg hover:moments-text-black;
  border: 1px solid rgb(209 213 219);
}
</style>
