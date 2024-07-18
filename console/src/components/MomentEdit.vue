<script lang="ts" setup>
import { VButton, IconEye, IconEyeOff } from "@halo-dev/components";
import type { Moment, MomentMedia, MomentMediaTypeEnum } from "@/types";
import { Toast } from "@halo-dev/components";
import type { AttachmentLike } from "@halo-dev/console-shared";
import { computed, onMounted, ref, toRaw } from "vue";
import MediaCard from "@/components/MediaCard.vue";
import TextEditor from "@/components/TextEditor.vue";
import SendMoment from "~icons/ic/sharp-send";
import cloneDeep from "lodash.clonedeep";
import TablerPhoto from "~icons/tabler/photo";
import { axiosInstance } from "@halo-dev/api-client";
import { useConsoleTagQueryFetch } from "@/composables/use-tag";

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
    approved: true,
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
      handleUpdate(formState.value);
    } else {
      handleSave(formState.value);
      handleReset();
    }
  } catch (error) {
    console.error(error);
  } finally {
    saving.value = false;
  }
};

const handleSave = async (moment: Moment) => {
  moment.spec.releaseTime = new Date().toISOString();
  moment.spec.approved = true;
  const { data } = await axiosInstance.post<Moment>(
    `/apis/console.api.moment.halo.run/v1alpha1/moments`,
    moment
  );
  emit("save", data);
  Toast.success("发布成功");
};

const handleUpdate = async (moment: Moment) => {
  const { data } = await axiosInstance.get<Moment>(
    `/apis/moment.halo.run/v1alpha1/moments/${moment.metadata.name}`
  );
  // 更新当前需要提交的 moment spec 为最新
  data.spec = moment.spec;
  const updated = await axiosInstance.put<Moment>(
    `/apis/moment.halo.run/v1alpha1/moments/${moment.metadata.name}`,
    data
  );
  emit("update", updated.data);
  Toast.success("发布成功");
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

const supportAudioTypes: string[] = ["audio/*"];

const accepts = [
  ...supportImageTypes,
  ...supportVideoTypes,
  ...supportAudioTypes,
];

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
  <div class="card bg-white shrink border rounded-md overflow-hidden">
    <AttachmentSelectorModal
      v-model:visible="attachmentSelectorModal"
      v-permission="['system:attachments:view']"
      :min="1"
      :max="9"
      :accepts="accepts"
      @select="onAttachmentsSelect"
    />
    <TextEditor
      v-model:raw="formState.spec.content.raw"
      v-model:html="formState.spec.content.html"
      v-model:isEmpty="isEditorEmpty"
      :tag-query-fetch="useConsoleTagQueryFetch"
      class="min-h-[9rem] p-3.5"
      tabindex="-1"
      @keydown="handleKeydown"
    />
    <div
      v-if="formState.spec.content.medium?.length"
      class="img-box flex px-3.5 py-2"
    >
      <ul class="grid grid-cols-3 gap-1.5 w-full sm:w-1/2" role="list">
        <li
          v-for="(media, index) in formState.spec.content.medium"
          :key="index"
          class="rounded-md border overflow-hidden inline-block"
        >
          <MediaCard :media="media" @remove="removeMedium"></MediaCard>
        </li>
      </ul>
    </div>
    <div class="bg-white flex justify-between px-3.5 py-2">
      <div class="h-fit">
        <div
          class="p-2 group hover:bg-sky-600/10 cursor-pointer rounded-full flex items-center justify-center"
        >
          <TablerPhoto
            class="size-full text-base text-gray-600 group-hover:text-sky-600"
            @click="addMediumVerify() && (attachmentSelectorModal = true)"
          />
        </div>
      </div>

      <div class="flex items-center space-x-2.5">
        <div
          v-tooltip="{
            content:
              formState.spec.visible === 'PRIVATE' ? `私有访问` : '公开访问',
          }"
          class="p-2 group cursor-pointer rounded-full flex items-center justify-center"
          :class="
            formState.spec.visible === 'PRIVATE'
              ? 'hover:bg-red-600/10'
              : 'hover:bg-green-600/10'
          "
          @click="handleToggleVisible()"
        >
          <IconEyeOff
            v-if="formState.spec.visible === 'PRIVATE'"
            class="size-full text-base text-gray-600 group-hover:text-red-600"
          />
          <IconEye
            v-else
            class="size-full text-base text-gray-600 group-hover:text-green-600"
          />
        </div>

        <button
          v-if="isUpdateMode"
          class="cursor-pointer text-gray-600 hover:text-sky-600 inline-flex items-center rounded h-7 hover:bg-sky-600/10 px-3"
          @click="handlerCancel"
        >
          <span class="text-xs"> 取消 </span>
        </button>

        <div
          v-permission="
            ['plugin:moments:manage'] || ['uc:plugin:moments:publish']
          "
          class="h-fit"
        >
          <VButton
            v-model:disabled="saveDisable"
            :loading="saving"
            size="sm"
            type="primary"
            @click="handlerCreateOrUpdateMoment"
          >
            <template #icon>
              <SendMoment class="scale-[1.35] size-full" />
            </template>
          </VButton>
        </div>
      </div>
    </div>
  </div>
</template>
