<script lang="ts" setup>
import { VButton, IconEye, IconEyeOff } from "@halo-dev/components";
import type { Moment, MomentMedia, MomentMediaTypeEnum } from "@/types";
import apiClient from "@/utils/api-client";
import { Toast } from "@halo-dev/components";
import type { AttachmentLike } from "@halo-dev/console-shared";
import { computed, nextTick, onMounted, ref, toRaw } from "vue";
import MediaCard from "./MediaCard.vue";
import TextEditor from "./TextEditor.vue";
import SendMoment from "~icons/ic/sharp-send";
import MdiFileImageBox from "~icons/mdi/file-image-box";
import cloneDeep from "lodash.clonedeep";

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
const MediumWhitelist: Map<string, MomentMediaTypeEnum> = new Map([
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

  //TODO 目前只能弹窗进行提醒，无法做到选择的时候就指定特定类型文件, 期望后续文件选择组件能支持传入类型
  for (let media of medias) {
    let type = media.type;
    if (!type) {
      Toast.error("不支持未知类型的文件");
      nextTick(() => {
        attachmentSelectorModal.value = true;
      });
      return;
    }
    const isImage = supportImageTypes.includes(type);
    let fileType = type.split("/")[0];
    if (!MediumWhitelist.has(fileType) && !isImage) {
      Toast.error("暂不支持【" + type + "】类型的文件");
      nextTick(() => {
        attachmentSelectorModal.value = true;
      });
      return;
    }
  }

  if (!formState.value.spec.content.medium) {
    formState.value.spec.content.medium = [];
  }
  medias.forEach((media) => {
    if (!addMediumVerify()) {
      return false;
    }
    if (!media.type) {
      return false;
    }
    let fileType = media.type.split("/")[0];
    formState.value.spec.content.medium?.push({
      type: MediumWhitelist.get(fileType),
      url: media.url,
      originType: media.type,
    } as MomentMedia);
  });
};

const saveDisable = computed(() => {
  let medium = formState.value.spec.content.medium;
  if (medium !== undefined && medium.length > 0) {
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

const addMediumVerify = () => {
  let formMedium = formState.value.spec.content.medium;
  if (!formMedium || formMedium.length == 0) {
    return true;
  }

  if (formMedium.length >= uploadMediumNum) {
    Toast.warning("最多允许添加 " + uploadMediumNum + " 个附件");
    return false;
  }

  return true;
};

function handleToggleVisible() {
  const { visible: currentVisible } = formState.value.spec;
  formState.value.spec.visible =
    currentVisible === "PUBLIC" ? "PRIVATE" : "PUBLIC";
}
</script>

<template>
  <div
    class="card moments-bg-white moments-shrink moments-border moments-rounded-md moments-overflow-hidden focus-within:shadow-lg"
  >
    <AttachmentSelectorModal
      v-model:visible="attachmentSelectorModal"
      @select="onAttachmentsSelect"
    />
    <TextEditor
      v-model:raw="formState.spec.content.raw"
      v-model:html="formState.spec.content.html"
      v-model:isEmpty="isEditorEmpty"
      class="moments-min-h-[9rem] moments-p-3.5"
    />
    <div
      v-if="formState.spec.content.medium?.length"
      class="img-box moments-flex moments-px-3.5 moments-py-2"
    >
      <ul role="list">
        <li
          v-for="(media, index) in formState.spec.content.medium"
          :key="index"
          class="moments-rounded-md moments-border moments-overflow-hidden moments-inline-block moments-mr-2 moments-mb-2 moments-w-20"
        >
          <MediaCard :media="media" @remove="removeMedium"></MediaCard>
        </li>
      </ul>
    </div>
    <div
      class="moments-bg-white moments-flex moments-justify-between moments-px-3.5 moments-py-2"
    >
      <div class="moments-h-fit">
        <VButton
          size="sm"
          type="primary"
          @click="addMediumVerify() && (attachmentSelectorModal = true)"
        >
          <template #icon>
            <MdiFileImageBox class="h-full w-full" />
          </template>
        </VButton>
      </div>

      <div class="moments-flex moments-items-center moments-space-x-2.5">
        <button
          v-tooltip="{
            content:
              formState.spec.visible === 'PRIVATE' ? `私有访问` : '公开访问',
          }"
          class="moments-cursor-pointer moments-inline-flex moments-text-gray-500 hover:moments-text-gray-900 moments-items-center moments-rounded moments-h-7 hover:moments-bg-teal-100 moments-px-3"
          @click="handleToggleVisible()"
        >
          <IconEyeOff
            v-if="formState.spec.visible === 'PRIVATE'"
            class="moments-h-4 moments-w-4"
          />
          <IconEye v-else class="moments-h-4 moments-w-4" />
        </button>

        <button
          v-if="isUpdateMode"
          class="moments-cursor-pointer moments-text-gray-500 hover:moments-text-gray-900 moments-inline-flex moments-items-center moments-rounded moments-h-7 hover:moments-bg-teal-100 moments-px-3"
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
              <SendMoment class="h-full w-full" />
            </template>
          </VButton>
        </div>
      </div>
    </div>
  </div>
</template>
