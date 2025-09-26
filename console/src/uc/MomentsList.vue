<script lang="ts" setup>
import { momentsUcApiClient } from "@/api";
import { toISODayEndOfTime } from "@/utils/date";
import { VCard, VLoading, VPageHeader, VPagination } from "@halo-dev/components";
import { useQuery } from "@tanstack/vue-query";
import { useRouteQuery } from "@vueuse/router";
import { computed, provide, ref, watch } from "vue";
import DatePicker from "vue-datepicker-next";
import "vue-datepicker-next/index.css";
import "vue-datepicker-next/locale/zh-cn.es";
import MingcuteMomentsLine from "~icons/mingcute/moment-line";
import MomentEdit from "./MomentEdit.vue";
import MomentItem from "./MomentItem.vue";
import TagFilterDropdown from "./TagFilterDropdown.vue";
import { usePluginShikiScriptLoader } from "@/plugin-supports/shiki/use-plugin-shiki-script-loader";

interface VisibleItem {
  label: string;
  value?: "PUBLIC" | "PRIVATE";
}

interface SortItem {
  label: string;
  sort: "RELEASE_TIME";
  sortOrder: boolean;
}

const VisibleItems: VisibleItem[] = [
  {
    label: "全部",
    value: undefined,
  },
  {
    label: "公开",
    value: "PUBLIC",
  },
  {
    label: "私有",
    value: "PRIVATE",
  },
];

const tag = useRouteQuery<string>("tag");

const page = ref(1);
const size = ref(20);
const total = ref(0);
const totalPages = ref(1);
const hasPrevious = ref(false);
const hasNext = ref(false);

const selectedVisibleItem = ref<VisibleItem>(VisibleItems[0]);
const selectedSortItem = ref<SortItem>();
const keyword = ref("");
const momentsRangeTime = ref<Array<Date>>([]);

const startDate = computed(() => {
  const date = momentsRangeTime.value[0];
  return toISODayEndOfTime(date);
});
const endDate = computed(() => {
  let endTime: Date = momentsRangeTime.value[1];
  return toISODayEndOfTime(endTime);
});

const {
  data: moments,
  isLoading,
  refetch,
} = useQuery({
  queryKey: [
    "plugin:moments:list",
    page,
    size,
    selectedVisibleItem,
    selectedSortItem,
    startDate,
    endDate,
    keyword,
    tag,
  ],
  queryFn: async () => {
    const { data } = await momentsUcApiClient.moment.listMyMoments({
      page: page.value,
      size: size.value,
      // @unocss-skip-start
      visible: selectedVisibleItem.value?.value,
      // @unocss-skip-end
      keyword: keyword.value,
      startDate: startDate.value,
      endDate: endDate.value,
      tag: tag.value,
    });

    total.value = data.total;
    totalPages.value = data.totalPages;
    hasNext.value = data.hasNext;
    hasPrevious.value = data.hasPrevious;
    return data.items;
  },
  refetchInterval: (data) => {
    const hasDeletingData = data?.some((moment) => {
      return !!moment.moment.metadata.deletionTimestamp;
    });
    return hasDeletingData ? 1000 : false;
  },
  refetchOnWindowFocus: false,
});

function updateTagQuery(tagQuery: string) {
  tag.value = tagQuery;
}

provide("tag", {
  tag: tag.value,
  updateTagQuery,
});

watch([tag, momentsRangeTime], () => {
  page.value = 1;
  size.value = 20;
  refetch();
});

usePluginShikiScriptLoader();
</script>
<template>
  <VPageHeader title="瞬间">
    <template #icon>
      <MingcuteMomentsLine class=":uno: mr-2 self-center" />
    </template>
  </VPageHeader>
  <VCard class=":uno: m-0 flex-1 md:m-4">
    <div class=":uno: mx-auto max-w-4xl px-4 md:px-8">
      <div class=":uno: moments-content my-2 flex flex-col md:my-4 space-y-2">
        <MomentEdit />

        <div class=":uno: moment-header pb-2 pt-8">
          <div class=":uno: flex flex-col justify-between sm:flex-row space-x-2">
            <div class=":uno: left-0 mb-2 mr-2 flex items-center sm:mb-0">
              <TagFilterDropdown v-model="tag" :label="'标签'"></TagFilterDropdown>
            </div>

            <div class=":uno: right-0 flex !ml-0">
              <DatePicker
                v-model:value="momentsRangeTime"
                input-class=":uno: mx-input rounded"
                class=":uno: date-picker range-time max-w-[13rem] cursor-pointer md:max-w-[15rem]"
                range
                :editable="false"
                placeholder="筛选日期范围"
              />
            </div>
          </div>
        </div>

        <VLoading v-if="isLoading" />

        <Transition v-else appear name="fade">
          <ul
            v-if="moments && moments.length > 0"
            class=":uno: box-border flex flex-col space-y-2"
            role="list"
          >
            <li v-for="moment in moments" :key="moment.moment.metadata.name">
              <MomentItem
                :key="moment.moment.metadata.name"
                :listed-moment="moment"
                @remove="refetch()"
              />
            </li>
          </ul>
          <template v-else>
            <div class=":uno: h-full flex items-center justify-center">
              <span class=":uno: text-gray-500">暂无数据</span>
            </div>
          </template>
        </Transition>

        <div v-if="hasPrevious || hasNext" class=":uno: my-5 flex justify-center">
          <VPagination
            v-model:page="page"
            v-model:size="size"
            class=":uno: !bg-transparent"
            :total="total"
            :size-options="[20, 30, 50, 100]"
          />
        </div>
      </div>
    </div>
  </VCard>
</template>
<style lang="scss">
.date-picker {
  & input {
    border-radius: 0.375rem;
  }
}
</style>
