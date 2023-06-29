<script lang="ts" setup>
import { computed, ref } from "vue";
import { VPageHeader, VLoading, VPagination } from "@halo-dev/components";
import MingcuteMomentsLine from "~icons/mingcute/moment-line";
import type { User } from "@halo-dev/api-client";
import type { ListedMoment } from "@/types";
import { useQuery } from "@tanstack/vue-query";
import apiClient from "@/utils/api-client";
import MomentItem from "@/components/MomentItem.vue";
import MomentEdit from "@/components/MomentEdit.vue";
import DatePicker from "vue-datepicker-next";
import "vue-datepicker-next/index.css";
import "vue-datepicker-next/locale/zh-cn";
import { toISODayEndOfTime } from "@/utils/date";
import { useRouteQuery } from "@vueuse/router";
import FilterTag from "@/components/FilterTag.vue";

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
const selectedContributor = ref<User>();
const keyword = ref("");
const momentsRangeTime = ref<Array<Date>>([]);

const startDate = computed(() => {
  return momentsRangeTime.value[0] || "";
});
const endDate = computed(() => {
  let endTime: Date = momentsRangeTime.value[1] || "";
  return toISODayEndOfTime(endTime);
});

const {
  data: moments,
  isLoading,
  refetch,
} = useQuery<ListedMoment[]>({
  queryKey: [
    page,
    size,
    selectedContributor,
    selectedVisibleItem,
    selectedSortItem,
    startDate,
    endDate,
    keyword,
    tag,
  ],
  queryFn: async () => {
    let contributors: string[] | undefined;

    if (selectedContributor.value) {
      contributors = [selectedContributor.value.metadata.name];
    }

    const { data } = await apiClient.get(
      "/apis/api.plugin.halo.run/v1alpha1/plugins/PluginMoments/moments",
      {
        params: {
          page: page.value,
          size: size.value,
          visible: selectedVisibleItem.value?.value,
          sort: selectedSortItem.value?.sort,
          sortOrder: selectedSortItem.value?.sortOrder,
          keyword: keyword.value,
          startDate: startDate.value,
          endDate: endDate.value,
          contributor: contributors,
          tag: tag.value,
        },
      }
    );
    total.value = data.total;
    totalPages.value = data.value;
    hasNext.value = data.hasNext;
    hasPrevious.value = data.hasPrevious;
    return data.items;
  },
  refetchInterval: (data) => {
    const abnormalMoments = data?.filter((moment) => {
      const { metadata } = moment.moment;
      return metadata.deletionTimestamp;
    });
    return abnormalMoments?.length ? 500 : false;
  },
  refetchOnWindowFocus: false,
});

const searchText = ref("");

const handleCloseTag = () => {
  tag.value = "";
};
</script>
<template>
  <VPageHeader title="瞬间">
    <template #icon>
      <MingcuteMomentsLine class="moments-mr-2 moments-self-center" />
    </template>
  </VPageHeader>

  <div class="moments-container moments-mx-auto">
    <div
      class="moments-content moments-my-2 md:moments-my-4 moments-flex moments-flex-col moments-space-y-2"
    >
      <div class="moment-header">
        <div
          class="moments-flex moments-justify-between moments-flex-col sm:moments-flex-row moments-space-x-2"
        >
          <div
            class="moments-left-0 moments-mb-2 sm:moments-mb-0 moments-flex moments-items-center moments-justify-between"
          >
            <FormKit
              v-model="searchText"
              placeholder="输入关键词搜索"
              type="text"
              outer-class="!moments-p-0 moments-mr-2"
              @keyup.enter="keyword = searchText"
            ></FormKit>
            <FilterTag v-if="tag" @close="handleCloseTag()">
              标签：{{ tag }}
            </FilterTag>
          </div>

          <div class="moments-right-0 !moments-ml-0 moments-flex">
            <DatePicker
              v-model:value="momentsRangeTime"
              input-class=""
              class="range-time moments-max-w-[13rem] md:moments-max-w-[15rem]"
              range
              :editable="false"
              placeholder="筛选日期范围"
            />
          </div>
        </div>
      </div>

      <MomentEdit @save="refetch()" />

      <VLoading v-if="isLoading" />

      <Transition v-else appear name="fade">
        <ul
          class="box-border moments-flex moments-flex-col moments-space-y-2"
          role="list"
        >
          <li v-for="moment in moments" :key="moment.moment.metadata.name">
            <MomentItem
              :key="moment.moment.metadata.name"
              :moment="moment.moment"
              @remove="refetch()"
            />
          </li>
        </ul>
      </Transition>

      <div
        v-if="hasPrevious || hasNext"
        s
        class="moments-my-5 flex moments-justify-center"
      >
        <VPagination
          v-model:page="page"
          v-model:size="size"
          class="!moments-bg-transparent"
          :total="total"
          :size-options="[20, 30, 50, 100]"
        />
      </div>
    </div>
  </div>
</template>
