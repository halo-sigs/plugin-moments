<script lang="ts" setup>
import { VButton, IconEye, IconEyeOff } from "@halo-dev/components";
import type { Moment, MomentMedia, MomentMediaTypeEnum } from "@/types";
import apiClient from "@/utils/api-client";
import { Toast } from "@halo-dev/components";
import type { AttachmentLike } from "@halo-dev/console-shared";
import { computed, onMounted, ref, toRaw } from "vue";
import MediaCard from "./MediaCard.vue";
import TextEditor from "./TextEditor.vue";
import SendMoment from "~icons/ic/sharp-send";
import cloneDeep from "lodash.clonedeep";
import TablerPhoto from "~icons/tabler/photo";

const props = withDefaults(
  defineProps<{
    moment?: Moment;
  }>(),
  {
    moment: undefined,
  }
);

const emit = defineEmits<{
  (event: "save", moment: Moment): void;
  (event: "update", moment: Moment): void;
  (event: "cancel"): void;
}>();

const initMoment: Moment = {
  spec: {
    content: {
      raw: "",
      html: "",
      medium: [],
    },
    releaseTime: new Date().toISOString(),
    owner: "",
    visible: "PUBLIC",
    tags: [],
  },
  metadata: {
    generateName: "moment-",
  },
  kind: "Moment",
  apiVersion: "moment.halo.run/v1alpha1",
};

onMounted(() => {
  if (props.moment) {
    formState.value = cloneDeep(props.moment);
  }
});

const formState = ref<Moment>(cloneDeep(initMoment));
const saving = ref<boolean>(false);
const attachmentSelectorModal = ref(false);
const isUpdateMode = computed(
  () => !!formState.value.metadata.creationTimestamp
);
const isEditorEmpty = ref<boolean>(true);
const handlerCreateOrUpdateMoment = async () => {
  if (saveDisable.value) {
    return;
  }
  try {
    saving.value = true;
    queryEditorTags();
    if (isUpdateMode.value) {
      updateMoment();
    } else {
      createMoment();
    }
  } catch (error) {
    console.error(error);
  } finally {
    saving.value = false;
  }
};

const parse = new DOMParser();
const queryEditorTags = function () {
  let tags: Set<string> = new Set();
  let document: Document = parse.parseFromString(
    formState.value.spec.content.raw,
    "text/html"
  );
  let nodeList: NodeList = document.querySelectorAll("a.tag");
  if (nodeList) {
    for (let tagNode of nodeList) {
      if (tagNode.textContent) {
        tags.add(tagNode.textContent);
      }
    }
  }
  formState.value.spec.tags = Array.from(tags);
};

const createMoment = async () => {
  formState.value.spec.releaseTime = new Date().toISOString();
  const { data } = await apiClient.post<Moment>(
    `/apis/api.plugin.halo.run/v1alpha1/plugins/PluginMoments/moments`,
    formState.value
  );
  emit("save", data);
  handleReset();
  Toast.success("发布成功");
};

const updateMoment = async () => {
  const { data } = await apiClient.get<Moment>(
    `/apis/moment.halo.run/v1alpha1/moments/${formState.value.metadata.name}`
  );
  // 更新当前需要提交的 moment spec 为最新
  data.spec = formState.value.spec;
  const updated = await apiClient.put<Moment>(
    `/apis/moment.halo.run/v1alpha1/moments/${formState.value.metadata.name}`,
    data
  );
  emit("update", updated.data);
  Toast.success("发布成功");
};

const handleReset = () => {
  formState.value = toRaw(cloneDeep(initMoment));
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

const accepts = [...supportImageTypes, ...supportVideoTypes];

const mediumWhitelist: Map<string, MomentMediaTypeEnum> = new Map([
  ["image", "PHOTO"],
  ["video", "VIDEO"],
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
  if (!formState.value.spec.content.medium) {
    formState.value.spec.content.medium = [];
  }
  medias.forEach((media) => {
    if (!addMediumVerify(media)) {
      return false;
    }
    if (!media.type) {
      return false;
    }
    let fileType = media.type.split("/")[0];
    formState.value.spec.content.medium?.push({
      type: mediumWhitelist.get(fileType),
      url: media.url,
      originType: media.type,
    } as MomentMedia);
  });
};

const saveDisable = computed(() => {
  let medium = formState.value.spec.content.medium;
  if (medium !== undefined && medium.length > 0 && medium.length <= 9) {
    return false;
  }
  if (!isEditorEmpty.value) {
    return false;
  }

  if (isUpdateMode.value) {
    let oldVisible = props.moment?.spec.visible;
    if (oldVisible != formState.value.spec.visible) {
      return false;
    }
  }

  return true;
});

const removeMedium = (media: MomentMedia) => {
  let formMedium = formState.value.spec.content.medium;
  if (!formMedium) {
    return;
  }
  let index: number = formMedium.indexOf(media);
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
  let formMedium = formState.value.spec.content.medium;
  if (!formMedium || formMedium.length == 0) {
    return true;
  }

  if (formMedium.length >= uploadMediumNum) {
    Toast.warning("最多允许添加 " + uploadMediumNum + " 个附件");
    return false;
  }

  if (media) {
    if (
      formState.value.spec.content.medium?.filter(
        (item) => item.url == media.url
      ).length != 0
    ) {
      Toast.warning("已过滤重复添加的附件");
      return false;
    }
  }

  return true;
};

function handleToggleVisible() {
  const { visible: currentVisible } = formState.value.spec;
  formState.value.spec.visible =
    currentVisible === "PUBLIC" ? "PRIVATE" : "PUBLIC";
}

function handleKeydown(event: KeyboardEvent) {
  if (event.ctrlKey && event.key === "Enter") {
    handlerCreateOrUpdateMoment();
    return false;
  }
}
</script>

<template>
  <div
    class="card moments-bg-white moments-shrink moments-border moments-rounded-md moments-overflow-hidden focus-within:shadow-lg"
  >
    <AttachmentSelectorModal
      v-model:visible="attachmentSelectorModal"
      :min="1"
      :max="9"
      :accepts="accepts"
      @select="onAttachmentsSelect"
    />
    <TextEditor
      v-model:raw="formState.spec.content.raw"
      v-model:html="formState.spec.content.html"
      v-model:isEmpty="isEditorEmpty"
      class="moments-min-h-[9rem] moments-p-3.5"
      tabindex="-1"
      @keydown="handleKeydown"
    />
    <div
      v-if="formState.spec.content.medium?.length"
      class="img-box moments-flex moments-px-3.5 moments-py-2"
    >
      <ul
        class="moments-grid moments-grid-cols-3 moments-gap-1.5 moments-w-full sm:moments-w-1/2"
        role="list"
      >
        <li
          v-for="(media, index) in formState.spec.content.medium"
          :key="index"
          class="moments-rounded-md moments-border moments-overflow-hidden moments-inline-block"
        >
          <MediaCard :media="media" @remove="removeMedium"></MediaCard>
        </li>
      </ul>
    </div>
    <div
      class="moments-bg-white moments-flex moments-justify-between moments-px-3.5 moments-py-2"
    >
      <div class="moments-h-fit">
        <div
          class="moments-p-2 moments-group hover:moments-bg-sky-600/10 moments-cursor-pointer moments-rounded-full moments-flex moments-items-center moments-justify-center"
        >
          <TablerPhoto
            class="h-full w-full moments-text-md moments-text-gray-600 group-hover:moments-text-sky-600"
            @click="addMediumVerify() && (attachmentSelectorModal = true)"
          />
        </div>
      </div>

      <div class="moments-flex moments-items-center moments-space-x-2.5">
        <div
          v-tooltip="{
            content:
              formState.spec.visible === 'PRIVATE' ? `私有访问` : '公开访问',
          }"
          class="moments-p-2 moments-group moments-cursor-pointer moments-rounded-full moments-flex moments-items-center moments-justify-center"
          :class="
            formState.spec.visible === 'PRIVATE'
              ? 'hover:moments-bg-red-600/10'
              : 'hover:moments-bg-green-600/10'
          "
          @click="handleToggleVisible()"
        >
          <IconEyeOff
            v-if="formState.spec.visible === 'PRIVATE'"
            class="h-full w-full moments-text-md moments-text-gray-600 group-hover:moments-text-red-600"
          />
          <IconEye
            v-else
            class="h-full w-full moments-text-md moments-text-gray-600 group-hover:moments-text-green-600"
          />
        </div>

        <button
          v-if="isUpdateMode"
          class="moments-cursor-pointer moments-text-gray-600 hover:moments-text-sky-600 moments-inline-flex moments-items-center moments-rounded moments-h-7 hover:moments-bg-sky-600/10 moments-px-3"
          @click="handlerCancel"
        >
          <span class="moments-text-xs"> 取消 </span>
        </button>

        <div v-permission="['plugin:moments:manage']" class="moments-h-fit">
          <VButton
            v-model:disabled="saveDisable"
            :loading="saving"
            size="sm"
            type="primary"
            @click="handlerCreateOrUpdateMoment"
          >
            <template #icon>
              <SendMoment class="moments-scale-[1.35] h-full w-full" />
            </template>
          </VButton>
        </div>
      </div>
    </div>
  </div>
</template>
