<script setup lang="ts">
import "@/styles/date-picker.scss";
import { VButton } from "@halo-dev/components";
import { fromDate, getLocalTimeZone, toCalendarDate, today } from "@internationalized/date";
import { useMediaQuery } from "@vueuse/core";
import {
  DateRangePickerCalendar,
  DateRangePickerCell,
  DateRangePickerCellTrigger,
  DateRangePickerContent,
  DateRangePickerGrid,
  DateRangePickerGridBody,
  DateRangePickerGridHead,
  DateRangePickerGridRow,
  DateRangePickerHeadCell,
  DateRangePickerHeader,
  DateRangePickerHeading,
  DateRangePickerNext,
  DateRangePickerPrev,
  DateRangePickerRoot,
  DateRangePickerTrigger,
  type DateRange,
} from "reka-ui";
import { computed, ref, shallowRef } from "vue";
import Calendar from "~icons/tabler/calendar";
import ChevronLeft from "~icons/tabler/chevron-left";
import ChevronRight from "~icons/tabler/chevron-right";

const model = defineModel<Date[]>({ required: true });
const zone = getLocalTimeZone();
const wide = useMediaQuery("(min-width: 640px)");
const open = ref(false);
const draft = shallowRef<DateRange>({ start: undefined, end: undefined });
const placeholder = shallowRef(today(zone));
const label = computed(() =>
  model.value.length === 2
    ? model.value.map((date) => toCalendarDate(fromDate(date, zone)).toString()).join(" 至 ")
    : "筛选日期范围"
);
const presets = [
  { label: "今天", days: 0 },
  { label: "昨天", days: 1 },
  { label: "最近 7 天", days: 6 },
  { label: "最近 30 天", days: 29 },
];
function handleOpen(value: boolean) {
  if (value) {
    draft.value = {
      start: model.value[0] ? toCalendarDate(fromDate(model.value[0], zone)) : undefined,
      end: model.value[1] ? toCalendarDate(fromDate(model.value[1], zone)) : undefined,
    };
    placeholder.value = draft.value.start ? toCalendarDate(draft.value.start) : today(zone);
  }
  open.value = value;
}
function selectPreset(days: number) {
  const end = today(zone);
  const start = end.subtract({ days });
  draft.value = { start, end: days === 1 ? start : end };
  placeholder.value = start;
}
function apply() {
  if (!draft.value.start || !draft.value.end) return;
  model.value = [draft.value.start.toDate(zone), draft.value.end.toDate(zone)];
  open.value = false;
}
function clear() {
  model.value = [];
  open.value = false;
}
</script>
<template>
  <DateRangePickerRoot
    v-model="draft"
    v-model:placeholder="placeholder"
    :open="open"
    :number-of-months="wide ? 2 : 1"
    locale="zh-CN"
    @update:open="handleOpen"
  >
    <DateRangePickerTrigger as-child>
      <button
        type="button"
        class=":uno: inline-flex cursor-pointer items-center gap-2 border rounded-lg px-3 text-sm text-gray-700 leading-9 hover:text-black focus-visible:outline-sky-600"
        :aria-label="label"
      >
        <Calendar class=":uno: size-4 shrink-0" />
        <span
          class=":uno: whitespace-nowrap"
          :class="{ ':uno: font-semibold': model.length === 2 }"
          >{{ label }}</span
        >
      </button>
    </DateRangePickerTrigger>
    <DateRangePickerContent
      class=":uno: moments-date-popover"
      align="end"
      :side-offset="6"
      :collision-padding="8"
      aria-label="筛选日期范围"
    >
      <div class=":uno: moments-date-presets">
        <button
          v-for="preset in presets"
          :key="preset.label"
          type="button"
          class=":uno: moments-date-text-button"
          @click="selectPreset(preset.days)"
        >
          {{ preset.label }}
        </button>
      </div>
      <DateRangePickerCalendar v-slot="{ grid, weekDays }" calendar-label="日期范围">
        <DateRangePickerHeader class=":uno: moments-calendar-header">
          <DateRangePickerPrev class=":uno: moments-calendar-nav" aria-label="上个月"
            ><ChevronLeft
          /></DateRangePickerPrev>
          <DateRangePickerHeading />
          <DateRangePickerNext class=":uno: moments-calendar-nav" aria-label="下个月"
            ><ChevronRight
          /></DateRangePickerNext>
        </DateRangePickerHeader>
        <div class=":uno: moments-calendar-months">
          <DateRangePickerGrid
            v-for="month in grid"
            :key="month.value.toString()"
            class=":uno: moments-calendar-grid"
          >
            <DateRangePickerGridHead>
              <DateRangePickerGridRow>
                <DateRangePickerHeadCell
                  v-for="day in weekDays"
                  :key="day"
                  class=":uno: moments-calendar-weekday"
                  >{{ day }}</DateRangePickerHeadCell
                >
              </DateRangePickerGridRow>
            </DateRangePickerGridHead>
            <DateRangePickerGridBody>
              <DateRangePickerGridRow v-for="(week, index) in month.rows" :key="index">
                <DateRangePickerCell
                  v-for="day in week"
                  :key="day.toString()"
                  :date="day"
                  class=":uno: moments-calendar-cell"
                >
                  <DateRangePickerCellTrigger
                    :day="day"
                    :month="month.value"
                    class=":uno: moments-calendar-day"
                  />
                </DateRangePickerCell>
              </DateRangePickerGridRow>
            </DateRangePickerGridBody>
          </DateRangePickerGrid>
        </div>
      </DateRangePickerCalendar>
      <div class=":uno: moments-date-actions">
        <button type="button" class=":uno: moments-date-text-button" @click="clear">
          清除筛选
        </button>
        <div class=":uno: flex gap-2">
          <VButton size="sm" @click="open = false">取消</VButton>
          <VButton size="sm" type="secondary" :disabled="!draft.start || !draft.end" @click="apply"
            >确定</VButton
          >
        </div>
      </div>
    </DateRangePickerContent>
  </DateRangePickerRoot>
</template>
