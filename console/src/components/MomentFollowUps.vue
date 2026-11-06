<script setup lang="ts">
import type { Moment, MomentFollowUp } from "@/api/generated";
import { stores, utils } from "@halo-dev/ui-shared";
import { useQueryClient } from "@tanstack/vue-query";
import { computed, ref, watch } from "vue";
import Sprout from "~icons/lucide/sprout";
import FollowUpEditor from "./FollowUpEditor.vue";

const props = defineProps<{ moment: Moment; uc: boolean }>();
const userStore = stores.currentUser();
const queryClient = useQueryClient();
const editing = ref(false);
const followUps = ref<MomentFollowUp[]>(props.moment.spec.followUps || []);
const canAppend = computed(
  () =>
    userStore.currentUser?.user.metadata.name === props.moment.spec.owner &&
    !props.moment.metadata.deletionTimestamp &&
    utils.permission.has([props.uc ? "uc:plugin:moments:publish" : "plugin:moments:manage"])
);
watch(
  () => props.moment,
  (moment) => {
    followUps.value = moment.spec.followUps || [];
  }
);

function onSaved(moment: Moment) {
  followUps.value = moment.spec.followUps || [];
  queryClient.invalidateQueries({ queryKey: ["plugin:moments:list"] });
}
</script>

<template>
  <section
    v-if="followUps.length || canAppend"
    class=":uno: moment-follow-ups"
    aria-label="后来的我"
    @dblclick.stop
  >
    <div v-if="followUps.length" class=":uno: moment-follow-ups__timeline">
      <h3><Sprout aria-hidden="true" />后来的我</h3>
      <ol>
        <li v-for="followUp in followUps" :key="followUp.id">
          <time :datetime="followUp.createdAt" :title="utils.date.format(followUp.createdAt)">{{
            utils.date.format(followUp.createdAt, "YYYY-MM-DD HH:mm")
          }}</time>
          <p>{{ followUp.content }}</p>
        </li>
      </ol>
    </div>
    <button
      v-if="canAppend && followUps.length < 100"
      type="button"
      class=":uno: moment-follow-ups__append"
      @click="editing = true"
    >
      <Sprout aria-hidden="true" />写下后续
    </button>
    <FollowUpEditor
      v-if="editing && canAppend"
      :moment="moment"
      :uc="uc"
      @saved="onSaved"
      @close="editing = false"
    />
  </section>
</template>

<style scoped>
.moment-follow-ups {
  margin-top: 16px;
}
.moment-follow-ups__timeline {
  border-left: 2px solid #bae6fd;
  padding-left: 14px;
}
.moment-follow-ups h3 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 12px;
  color: #0284c7;
  font-size: 12px;
  font-weight: 500;
}
.moment-follow-ups svg {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
}
.moment-follow-ups ol {
  list-style: none;
  padding: 0;
  margin: 0;
}
.moment-follow-ups li + li {
  margin-top: 14px;
}
.moment-follow-ups time {
  color: #6b7280;
  font-size: 11px;
}
.moment-follow-ups p {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.moment-follow-ups__append {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
}
.moment-follow-ups__append:hover {
  color: #0284c7;
}
.moment-follow-ups__append:focus-visible {
  outline: 2px solid #0284c7;
  outline-offset: 4px;
  border-radius: 2px;
}
</style>
