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
</script>
<template>
  <VPageHeader title="瞬间">
    <template #icon>
      <MingcuteMomentsLine class="mr-2 self-center" />
    </template>
  </VPageHeader>

  <div class="moments-mt-3 moments-content">
    <div class="moment-header moments-flex moments-justify-center moments-mb-3">
      <div class="moments-w-160 moments-flex moments-justify-between">
        <FormKit
          v-model="searchText"
          placeholder="输入关键词搜索"
          type="text"
          @keyup.enter="keyword = searchText"
        ></FormKit>
        <DatePicker
          class="range-time"
          range
          v-model:value="momentsRangeTime"
          placeholder="筛选日期范围"
        />
      </div>
    </div>

    <div class="moments-flex moments-justify-center">
      <MomentEdit @save="refetch()" />
    </div>
    <VLoading v-if="isLoading" />
    <Transition v-else appear name="fade">
      <ul class="box-border divide-y divide-gray-100" role="list">
        <li v-for="moment in moments" :key="moment.moment.metadata.name">
          <MomentItem
            :key="moment.moment.metadata.name"
            :moment="moment.moment"
            @remove="refetch()"
          />
        </li>
      </ul>
    </Transition>
  </div>
  <div class="moments-flex moments-justify-center" v-if="total > 20">
    <div
      class="moments-bg-white moments-flex moments-justify-center moments-w-160 moments-my-2 moments-h-20 moments-border moments-rounded-md moments-overflow-hidden"
    >
      <VPagination
        v-model:page="page"
        v-model:size="size"
        :total="total"
        :size-options="[20, 30, 50, 100]"
      />
    </div>
  </div>
</template>
