<script lang="ts" setup>
import { computed, provide, ref, watch, type Ref } from "vue";
import {
  VPageHeader,
  VLoading,
  VPagination,
  VCard,
} from "@halo-dev/components";
import MingcuteMomentsLine from "~icons/mingcute/moment-line";
import type { User } from "@halo-dev/api-client";
import type { ListedMoment } from "@/types";
import { useQuery } from "@tanstack/vue-query";
import { axiosInstance } from "@halo-dev/api-client";
import MomentItem from "./MomentItem.vue";
import DatePicker from "vue-datepicker-next";
import "vue-datepicker-next/index.css";
import "vue-datepicker-next/locale/zh-cn.es";
import { toISODayEndOfTime } from "@/utils/date";
import { useRouteQuery } from "@vueuse/router";
import TagFilterDropdown from "./TagFilterDropdown.vue";
import MomentEdit from "./MomentEdit.vue";

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

    const { data } = await axiosInstance.get(
      "/apis/uc.api.moment.halo.run/v1alpha1/moments",
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
</script>
<template>
  <VPageHeader title="瞬间">
    <template #icon>
      <MingcuteMomentsLine class="mr-2 self-center" />
    </template>
  </VPageHeader>
  <VCard class="m-0 md:m-4 flex-1">
    <div class="max-w-4xl px-4 md:px-8 mx-auto">
      <div class="moments-content my-2 md:my-4 flex flex-col space-y-2">
        <MomentEdit @save="refetch()" />

        <div class="moment-header pt-8 pb-2">
          <div class="flex flex-col justify-between sm:flex-row space-x-2">
            <div class="left-0 mb-2 sm:mb-0 flex items-center mr-2">
              <TagFilterDropdown
                v-model="tag"
                :label="'标签'"
              ></TagFilterDropdown>
            </div>

            <div class="right-0 !ml-0 flex">
              <DatePicker
                v-model:value="momentsRangeTime"
                input-class="mx-input rounded"
                class="cursor-pointer date-picker range-time max-w-[13rem] md:max-w-[15rem]"
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
            class="box-border flex flex-col space-y-2"
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
            <div class="flex justify-center items-center h-full">
              <span class="text-gray-500">暂无数据</span>
            </div>
          </template>
        </Transition>

        <div v-if="hasPrevious || hasNext" s class="my-5 flex justify-center">
          <VPagination
            v-model:page="page"
            v-model:size="size"
            class="!bg-transparent"
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
