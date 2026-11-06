<script setup lang="ts">
import type { MomentEmotion } from "@/api/generated";
import { normalizeEmotion } from "@/utils/emotional-attachment";
import { computed } from "vue";
import CloudSun from "~icons/lucide/cloud-sun";
import MapPin from "~icons/lucide/map-pin";

const props = defineProps<{ value?: MomentEmotion }>();
const emotion = computed(() => normalizeEmotion(props.value));
</script>

<template>
  <div v-if="emotion" class=":uno: moment-emotion" aria-label="情感寄托">
    <span v-if="emotion.mood" class=":uno: moment-emotion__mood">{{ emotion.mood }}</span>
    <span
      v-if="emotion.weather"
      :title="
        emotion.weatherSource === 'open-meteo'
          ? '天气数据来自 Open-Meteo（https://open-meteo.com/）'
          : undefined
      "
    >
      <CloudSun aria-hidden="true" />{{ emotion.weather }}
    </span>
    <span v-if="emotion.location"><MapPin aria-hidden="true" />{{ emotion.location }}</span>
  </div>
</template>

<style scoped>
.moment-emotion {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 14px;
  margin-top: 14px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.7;
}
.moment-emotion > span {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
  overflow-wrap: anywhere;
}
.moment-emotion svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}
.moment-emotion__mood {
  padding: 2px 9px;
  border-radius: 12px;
  background: #f8f5fc;
  color: #785c94;
}
</style>
