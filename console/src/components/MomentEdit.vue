<script lang="ts" setup>
import type { Moment, MomentMedia, MomentMediaTypeEnum } from "@/api/generated";
import MediaCard from "@/components/MediaCard.vue";
import type { TagQueryFetch } from "@/composables/use-tag";
import type {
  MomentDraft,
  MomentSubmissionAdapter,
  MomentSubmissionPayload,
} from "@/features/moment-submission/types";
import { IconEye, IconEyeOff, Toast, VButton, VLoading } from "@halo-dev/components";
import type { AttachmentLike } from "@halo-dev/ui-shared";
import { useQueryClient } from "@tanstack/vue-query";
import { isAxiosError } from "axios";
import { cloneDeep } from "es-toolkit";
import { computed, defineAsyncComponent, ref } from "vue";
import SendMoment from "~icons/ic/sharp-send";
import TablerPhoto from "~icons/tabler/photo";

const TextEditor = defineAsyncComponent({
  loader: () => import("@/components/TextEditor.vue"),
  loadingComponent: VLoading,
});

const props = withDefaults(
  defineProps<{
    adapter: MomentSubmissionAdapter;
    moment?: Moment;
    tagQueryFetch: TagQueryFetch;
  }>(),
  {
    moment: undefined,
  }
);

const emit = defineEmits<{
  (event: "update"): void;
  (event: "cancel"): void;
}>();

const queryClient = useQueryClient();

function createDraft(moment?: Moment): MomentDraft {
  if (moment) {
    return {
      content: cloneDeep(moment.spec.content),
      visible: moment.spec.visible ?? "PUBLIC",
    };
  }
  return {
    content: {
      raw: "",
      html: "",
      medium: [],
    },
    visible: "PUBLIC",
  };
}

const draft = ref<MomentDraft>(createDraft(props.moment));
const submitting = ref(false);
const attachmentSelectorModal = ref(false);
const isUpdateMode = computed(() => !!props.moment);
const isEditorEmpty = ref(!(draft.value.content.raw || draft.value.content.medium?.length));

const handlerCreateOrUpdateMoment = async () => {
  if (saveDisable.value || submitting.value) {
    return;
  }

  submitting.value = true;
  try {
    const payload: MomentSubmissionPayload = {
      content: cloneDeep(draft.value.content),
      tags: queryEditorTags(draft.value.content.raw),
      visible: draft.value.visible,
    };
    const result = await props.adapter.submit(
      props.moment
        ? {
            type: "update",
            name: props.moment.metadata.name,
            payload,
          }
        : {
            type: "create",
            payload,
          }
    );

    void queryClient
      .invalidateQueries({ queryKey: ["plugin:moments:list"] })
      .catch((error) => console.error(error));

    Toast.success(result.status === "pending-review" ? "提交成功，等待审核" : "发布成功");

    if (props.moment) {
      emit("update");
    } else {
      handleReset();
    }
  } catch (error) {
    console.error(error);
    if (!isAxiosError(error)) {
      Toast.error(error instanceof Error ? error.message : "提交失败");
    }
  } finally {
    submitting.value = false;
  }
};

const parse = new DOMParser();
const queryEditorTags = (raw = "") => {
  const tags: Set<string> = new Set();
  const document: Document = parse.parseFromString(raw, "text/html");
  const nodeList: NodeList = document.querySelectorAll("a.tag");
  for (const tagNode of nodeList) {
    if (tagNode.textContent) {
      tags.add(tagNode.textContent);
    }
  }
  return Array.from(tags);
};

const handleReset = () => {
  draft.value = createDraft();
  isEditorEmpty.value = true;
};

const supportImageTypes: string[] = [
  "image/apng",
  "image/avif",
  "image/bmp",
  "image/gif",
  "image/x-icon",
  "image/jpg",
  "image/jpeg",
  "image/png",
  "image/svg+xml",
  "image/tiff",
  "image/webp",
];

const supportVideoTypes: string[] = ["video/*"];

const supportAudioTypes: string[] = ["audio/*"];

const accepts = [...supportImageTypes, ...supportVideoTypes, ...supportAudioTypes];

const mediumWhitelist: Map<string, MomentMediaTypeEnum> = new Map([
  ["image", "PHOTO"],
  ["video", "VIDEO"],
  ["audio", "AUDIO"],
]);

const onAttachmentsSelect = async (attachments: AttachmentLike[]) => {
  const medias: {
    url: string;
    cover?: string;
    displayName?: string;
    type?: string;
  }[] = attachments
    .map((attachment) => {
      if (typeof attachment === "string") {
        return {
          url: attachment,
          cover: attachment,
        };
      }
      if ("url" in attachment) {
        return {
          url: attachment.url,
          cover: attachment.url,
        };
      }
      if ("spec" in attachment) {
        return {
          url: attachment.status?.permalink,
          cover: attachment.status?.permalink,
          displayName: attachment.spec.displayName,
          type: attachment.spec.mediaType,
        };
      }
    })
    .filter(Boolean) as {
    url: string;
    cover?: string;
    displayName?: string;
    type?: string;
  }[];
  if (!draft.value.content.medium) {
    draft.value.content.medium = [];
  }
  medias.forEach((media) => {
    if (!addMediumVerify(media)) {
      return false;
    }
    if (!media.type) {
      return false;
    }
    const fileType = media.type.split("/")[0];
    draft.value.content.medium?.push({
      type: mediumWhitelist.get(fileType),
      url: media.url,
      originType: media.type,
    } as MomentMedia);
  });
};

const saveDisable = computed(() => {
  const medium = draft.value.content.medium;
  if (medium !== undefined && medium.length > 0 && medium.length <= 9) {
    return false;
  }
  if (!isEditorEmpty.value) {
    return false;
  }

  if (isUpdateMode.value) {
    const oldVisible = props.moment?.spec.visible;
    if (oldVisible != draft.value.visible) {
      return false;
    }
  }

  return true;
});

const removeMedium = (media: MomentMedia) => {
  const formMedium = draft.value.content.medium;
  if (!formMedium) {
    return;
  }
  const index: number = formMedium.indexOf(media);
  if (index > -1) {
    formMedium.splice(index, 1);
  }
};

const handlerCancel = () => {
  emit("cancel");
};

const uploadMediumNum = 9;

const addMediumVerify = (media?: {
  url: string;
  cover?: string;
  displayName?: string;
  type?: string;
}) => {
  const formMedium = draft.value.content.medium;
  if (!formMedium || formMedium.length == 0) {
    return true;
  }

  if (formMedium.length >= uploadMediumNum) {
    Toast.warning("最多允许添加 " + uploadMediumNum + " 个附件");
    return false;
  }

  if (media) {
    if (draft.value.content.medium?.filter((item) => item.url == media.url).length != 0) {
      Toast.warning("已过滤重复添加的附件");
      return false;
    }
  }

  return true;
};

function handleToggleVisible() {
  draft.value.visible = draft.value.visible === "PUBLIC" ? "PRIVATE" : "PUBLIC";
}

function handleKeydown(event: KeyboardEvent) {
  if (event.ctrlKey && event.key === "Enter") {
    handlerCreateOrUpdateMoment();
    return false;
  }
}
</script>

<template>
  <div class=":uno: card shrink overflow-hidden border rounded-md bg-white">
    <AttachmentSelectorModal
      v-if="attachmentSelectorModal"
      v-permission="['system:attachments:view']"
      :min="1"
      :max="9"
      :accepts="accepts"
      @select="onAttachmentsSelect"
      @close="attachmentSelectorModal = false"
    />
    <TextEditor
      v-model:raw="draft.content.raw"
      v-model:html="draft.content.html"
      v-model:isEmpty="isEditorEmpty"
      :tag-query-fetch="tagQueryFetch"
      class=":uno: min-h-[9rem]"
      tabindex="-1"
      @keydown="handleKeydown"
    />
    <div v-if="draft.content.medium?.length" class=":uno: img-box flex px-3.5 py-2">
      <ul class=":uno: grid grid-cols-3 w-full gap-1.5 sm:w-1/2" role="list">
        <li
          v-for="(media, index) in draft.content.medium"
          :key="index"
          class=":uno: inline-block overflow-hidden border rounded-md"
        >
          <MediaCard :media="media" @remove="removeMedium"></MediaCard>
        </li>
      </ul>
    </div>
    <div class=":uno: flex justify-between bg-white px-3.5 py-2">
      <div class=":uno: h-fit">
        <button
          data-testid="attachment-trigger"
          type="button"
          class=":uno: group flex cursor-pointer items-center justify-center rounded-full p-2 hover:bg-sky-600/10"
          @click="addMediumVerify() && (attachmentSelectorModal = true)"
        >
          <TablerPhoto class=":uno: size-full text-base text-gray-600 group-hover:text-sky-600" />
        </button>
      </div>

      <div class=":uno: flex items-center space-x-2.5">
        <div
          v-tooltip="{
            content: draft.visible === 'PRIVATE' ? `私有访问` : '公开访问',
          }"
          class=":uno: group flex cursor-pointer items-center justify-center rounded-full p-2"
          :class="
            draft.visible === 'PRIVATE'
              ? ':uno: hover:bg-red-600/10'
              : ':uno: hover:bg-green-600/10'
          "
          @click="handleToggleVisible()"
        >
          <IconEyeOff
            v-if="draft.visible === 'PRIVATE'"
            class=":uno: size-full text-base text-gray-600 group-hover:text-red-600"
          />
          <IconEye
            v-else
            class=":uno: size-full text-base text-gray-600 group-hover:text-green-600"
          />
        </div>

        <button
          v-if="isUpdateMode"
          class=":uno: h-7 inline-flex cursor-pointer items-center rounded px-3 text-gray-600 hover:bg-sky-600/10 hover:text-sky-600"
          @click="handlerCancel"
        >
          <span class=":uno: text-xs"> 取消 </span>
        </button>

        <div class=":uno: h-fit">
          <VButton
            data-testid="submit"
            :disabled="saveDisable || submitting"
            :loading="submitting"
            size="sm"
            type="primary"
            @click="handlerCreateOrUpdateMoment"
          >
            <template #icon>
              <SendMoment class=":uno: size-full scale-[1.35]" />
            </template>
          </VButton>
        </div>
      </div>
    </div>
  </div>
</template>
