<script setup lang="ts">
import type { MomentEmotion } from "@/api/generated";
import {
  fetchLocation,
  fetchWeather,
  getCurrentPosition,
  moods,
  normalizeEmotion,
} from "@/utils/emotional-attachment";
import { VButton } from "@halo-dev/components";
import { PopoverClose, PopoverContent, PopoverPortal, PopoverRoot, PopoverTrigger } from "reka-ui";
import { onBeforeUnmount, ref, watch } from "vue";
import Heart from "~icons/lucide/heart";
import LocateFixed from "~icons/lucide/locate-fixed";
import X from "~icons/lucide/x";

const model = defineModel<MomentEmotion>();
const expanded = ref(false);
const props = defineProps<{ disabled?: boolean }>();
const emit = defineEmits<{ (event: "update:loading", value: boolean): void }>();
const loading = ref(false);
const message = ref("");
let requestId = 0;
let controller: AbortController | undefined;

function cancelLookup() {
  requestId++;
  controller?.abort();
  loading.value = false;
  emit("update:loading", false);
}

watch(model, (value) => {
  if (!value) {
    cancelLookup();
    message.value = "";
    expanded.value = false;
  }
});
onBeforeUnmount(cancelLookup);

function updateField(field: "mood" | "location" | "weather", value: string) {
  const next = { ...model.value, [field]: value };
  // A manual weather edit is no longer an unmodified provider reading.
  if (field === "weather") {
    delete next.weatherSource;
  }
  model.value = next;
}

function clear() {
  cancelLookup();
  model.value = undefined;
  message.value = "";
  expanded.value = false;
}

async function locate() {
  if (loading.value || props.disabled) return;
  const currentRequest = ++requestId;
  const requestController = new AbortController();
  controller = requestController;
  const { signal } = requestController;
  loading.value = true;
  emit("update:loading", true);
  message.value = "";
  let timeout: ReturnType<typeof setTimeout> | undefined;
  try {
    const { coords } = await getCurrentPosition();
    if (currentRequest !== requestId) return;
    timeout = setTimeout(() => requestController.abort(), 10000);
    const [location, weather] = await Promise.allSettled([
      fetchLocation(coords, signal),
      fetchWeather(coords, signal),
    ]);
    if (currentRequest !== requestId) return;
    const next = { ...model.value };
    const failures: string[] = [];
    if (location.status === "fulfilled") {
      next.location = location.value;
    } else {
      failures.push("位置");
    }
    if (weather.status === "fulfilled") {
      next.weather = weather.value;
      next.weatherSource = "open-meteo";
    } else {
      failures.push("天气");
    }
    // Do not replace the form when both services failed.
    if (failures.length < 2) model.value = normalizeEmotion(next);
    message.value = failures.length
      ? `${failures.join("和")}获取失败，原有内容已保留，可重试或手动填写`
      : "已获取此刻的位置和天气";
  } catch (error) {
    if (currentRequest === requestId) {
      message.value = error instanceof Error ? error.message : "获取失败，请重试或手动填写";
    }
  } finally {
    clearTimeout(timeout);
    if (currentRequest === requestId) {
      loading.value = false;
      emit("update:loading", false);
    }
  }
}
</script>

<template>
  <PopoverRoot v-model:open="expanded">
    <PopoverTrigger as-child>
      <button
        v-tooltip="{ content: '情感寄托' }"
        type="button"
        aria-label="情感寄托"
        :aria-busy="loading"
        :disabled="disabled"
        class=":uno: group flex cursor-pointer items-center justify-center rounded-full p-2 hover:bg-sky-600/10 focus-visible:outline-2 focus-visible:outline-sky-600"
        :class="{ ':uno: bg-sky-600/10': expanded || !!normalizeEmotion(model) }"
      >
        <Heart
          aria-hidden="true"
          class=":uno: text-base"
          :class="
            expanded || normalizeEmotion(model)
              ? ':uno: text-sky-600'
              : ':uno: text-gray-600 group-hover:text-sky-600'
          "
        />
      </button>
    </PopoverTrigger>
    <PopoverPortal>
      <PopoverContent
        class=":uno: emotion-editor"
        side="top"
        align="start"
        :side-offset="8"
        :collision-padding="12"
        aria-label="情感寄托"
      >
        <div class=":uno: emotion-editor__bar">
          <span class=":uno: emotion-editor__title">情感寄托</span>
          <PopoverClose as-child>
            <button type="button" class=":uno: emotion-editor__close" aria-label="关闭情感寄托">
              <X aria-hidden="true" />
            </button>
          </PopoverClose>
        </div>

        <div class=":uno: emotion-editor__panel">
          <p class=":uno: emotion-editor__intro">记下此刻的心情和身边的天气</p>
          <fieldset :disabled="disabled || loading">
            <legend>此刻心情</legend>
            <div class=":uno: emotion-editor__moods">
              <button
                v-for="mood in moods"
                :key="mood"
                type="button"
                :aria-pressed="model?.mood === mood"
                @click="updateField('mood', model?.mood === mood ? '' : mood)"
              >
                {{ mood }}
              </button>
            </div>
            <div class=":uno: emotion-editor__fields">
              <label>
                <span>位置</span>
                <input
                  :value="model?.location || ''"
                  type="text"
                  maxlength="100"
                  placeholder="你在哪里？"
                  @input="updateField('location', ($event.target as HTMLInputElement).value)"
                />
              </label>
              <label>
                <span>天气</span>
                <input
                  :value="model?.weather || ''"
                  type="text"
                  maxlength="64"
                  placeholder="例如：晴 · 22°C"
                  @input="updateField('weather', ($event.target as HTMLInputElement).value)"
                />
              </label>
            </div>
          </fieldset>
          <button
            type="button"
            class=":uno: emotion-editor__locate"
            :disabled="disabled || loading"
            :aria-busy="loading"
            @click="locate"
          >
            <LocateFixed aria-hidden="true" />
            {{ loading ? "正在获取位置和天气…" : "获取位置和天气" }}
          </button>
          <p v-if="message" class=":uno: emotion-editor__message" role="status">{{ message }}</p>
          <p class=":uno: emotion-editor__note">
            点击获取会向
            <a href="https://www.bigdatacloud.com/" target="_blank" rel="noopener noreferrer"
              >BigDataCloud</a
            >
            和
            <a href="https://open-meteo.com/" target="_blank" rel="noopener noreferrer"
              >Open-Meteo</a
            >
            发送当前位置，仅保存地名和天气。发布后随瞬间的可见性展示。
          </p>
        </div>
        <div class=":uno: emotion-editor__actions">
          <button
            type="button"
            class=":uno: emotion-editor__clear"
            :disabled="disabled"
            @click="clear"
          >
            移除
          </button>
          <PopoverClose as-child>
            <VButton size="sm" type="secondary">完成</VButton>
          </PopoverClose>
        </div>
      </PopoverContent>
    </PopoverPortal>
  </PopoverRoot>
</template>

<style>
.emotion-editor {
  z-index: 1000;
  display: flex;
  flex-direction: column;
  width: min(390px, calc(100vw - 24px));
  max-height: var(--reka-popover-content-available-height);
  box-sizing: border-box;
  overflow: hidden;
  padding: 14px;
  border: 1px solid #eaecf0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 12px rgb(0 0 0 / 0.12);
  color: #374151;
  font-size: 12px;
}
.emotion-editor button {
  font: inherit;
  cursor: pointer;
}
.emotion-editor button:disabled {
  cursor: wait;
  opacity: 0.55;
}
.emotion-editor button:focus-visible,
.emotion-editor input:focus-visible {
  outline: 2px solid #0284c7;
  outline-offset: 3px;
}
.emotion-editor__bar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.emotion-editor__title {
  color: #0284c7;
  font-weight: 500;
}
.emotion-editor__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #6b7280;
}
.emotion-editor__close:hover {
  background: #f3f4f6;
}
.emotion-editor__close svg {
  width: 16px;
  height: 16px;
}
.emotion-editor__clear {
  border: 0;
  background: transparent;
  color: #6b7280;
}
.emotion-editor__clear:hover {
  color: #0284c7;
}
.emotion-editor__panel {
  min-height: 0;
  overflow-y: auto;
  padding: 8px 0 12px;
}
.emotion-editor__actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #eaecf0;
}
.emotion-editor__intro {
  margin: 0 0 14px;
  color: #6b7280;
}
.emotion-editor fieldset {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}
.emotion-editor legend {
  margin-bottom: 8px;
  font-weight: 500;
}
.emotion-editor__moods {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}
.emotion-editor__moods button {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
  padding: 5px 11px;
  color: #4b5563;
}
.emotion-editor__moods button:hover,
.emotion-editor__moods button[aria-pressed="true"] {
  border-color: #7dd3fc;
  background: #f0f9ff;
  color: #0284c7;
}
.emotion-editor__fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0 12px;
}
.emotion-editor__fields label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.emotion-editor__fields input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 10px;
  color: #374151;
  background: #fff;
  font: inherit;
}
.emotion-editor__fields input::placeholder {
  color: #9ca3af;
}
.emotion-editor__locate {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  padding: 7px 10px;
  background: #f0f9ff;
  color: #0284c7;
}
.emotion-editor__locate svg {
  width: 14px;
  height: 14px;
}
.emotion-editor__message {
  margin: 10px 0 0;
  color: #0284c7;
}
.emotion-editor__note {
  margin: 10px 0 0;
  font-size: 11px;
  line-height: 1.7;
  color: #6b7280;
}
.emotion-editor__note a {
  color: inherit;
  text-decoration: underline;
}
@media (max-width: 480px) {
  .emotion-editor__fields {
    grid-template-columns: 1fr;
  }
}
</style>
