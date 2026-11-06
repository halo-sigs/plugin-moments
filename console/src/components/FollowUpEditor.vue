<script setup lang="ts">
import { momentsConsoleApiClient, momentsUcApiClient } from "@/api";
import type { Moment } from "@/api/generated";
import { Toast, VButton, VModal } from "@halo-dev/components";
import { computed, ref, useId } from "vue";

const props = defineProps<{ moment: Moment; uc: boolean }>();
const emit = defineEmits<{
  (event: "saved", moment: Moment): void;
  (event: "close"): void;
}>();
const content = ref("");
const saving = ref(false);
const inputId = useId();
const valid = computed(() => !!content.value.trim() && content.value.trim().length <= 1000);

async function save() {
  if (!valid.value || saving.value) return;
  saving.value = true;
  try {
    const params = {
      name: props.moment.metadata.name,
      followUpRequest: { content: content.value.trim() },
    };
    const { data } = props.uc
      ? await momentsUcApiClient.moment.appendMyMomentFollowUp(params)
      : await momentsConsoleApiClient.moment.appendMomentFollowUp(params);
    emit("saved", data);
    Toast.success(data.spec.approved === false ? "后续已追加，等待审核" : "后续已追加");
    emit("close");
  } catch (error) {
    // Halo's API client shows request errors; retain the draft for retry.
    console.error("Failed to append moment follow-up", error);
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <VModal title="后来的我" :width="520" :layer-closable="!saving" @close="emit('close')">
    <form class=":uno: follow-up-editor" @submit.prevent="save">
      <p class=":uno: follow-up-editor__intro">给当时的自己，留一句现在想说的话。</p>
      <label :for="inputId">写下后续</label>
      <textarea
        :id="inputId"
        v-model="content"
        :disabled="saving"
        maxlength="1000"
        rows="5"
        placeholder="当时很难过的事，现在已经放下了。"
        autofocus
        @keydown.ctrl.enter.prevent="save"
        @keydown.meta.enter.prevent="save"
      />
      <div class=":uno: follow-up-editor__hint">
        <span>追加后会记录当前时间，原来的瞬间会保留。</span>
        <span>{{ content.length }}/1000</span>
      </div>
    </form>
    <template #footer>
      <div class=":uno: flex justify-end gap-2">
        <VButton :disabled="saving" @click="emit('close')">取消</VButton>
        <VButton type="secondary" :disabled="!valid || saving" :loading="saving" @click="save">
          追加后续
        </VButton>
      </div>
    </template>
  </VModal>
</template>

<style scoped>
.follow-up-editor {
  padding: 18px;
  color: #374151;
  font-size: 13px;
}
.follow-up-editor__intro {
  margin: 0 0 18px;
  color: #6b7280;
}
.follow-up-editor label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
}
.follow-up-editor textarea {
  display: block;
  box-sizing: border-box;
  width: 100%;
  min-height: 140px;
  resize: vertical;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fff;
  color: inherit;
  font: inherit;
  line-height: 1.8;
}
.follow-up-editor textarea:focus {
  outline: 2px solid #bae6fd;
  outline-offset: 1px;
  border-color: #0284c7;
}
.follow-up-editor textarea::placeholder {
  color: #9ca3af;
}
.follow-up-editor__hint {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  color: #6b7280;
  font-size: 12px;
}
</style>
