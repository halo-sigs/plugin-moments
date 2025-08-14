<script lang="ts" setup>
import { momentsUcApiClient } from "@/api";
import type { Moment, MomentMedia, MomentMediaTypeEnum } from "@/api/generated";
import MediaCard from "@/components/MediaCard.vue";
import { useUCTagQueryFetch } from "@/composables/use-tag";
import { IconEye, IconEyeOff, Toast, VButton, VLoading } from "@halo-dev/components";
import type { AttachmentLike } from "@halo-dev/console-shared";
import { useQueryClient } from "@tanstack/vue-query";
import { cloneDeep } from "es-toolkit";
import { computed, defineAsyncComponent, onMounted, ref, toRaw } from "vue";
import SendMoment from "~icons/ic/sharp-send";
import TablerPhoto from "~icons/tabler/photo";

const TextEditor = defineAsyncComponent({
  loader: () => import("@/components/TextEditor.vue"),
  loadingComponent: VLoading,
});

const props = withDefaults(
  defineProps<{
    moment?: Moment;
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

const initMoment: Moment = {
  spec: {
    content: {
      raw: "",
      html: "",
      medium: [],
    },
    releaseTime: new Date().toISOString(),
    owner: "",
    // @unocss-skip-start
    visible: "PUBLIC",
    // @unocss-skip-end
    tags: [],
    approved: false,
  },
  metadata: {
    generateName: "moment-",
    name: "",
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
const isUpdateMode = computed(() => !!formState.value.metadata.creationTimestamp);
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

  await momentsUcApiClient.moment.createMyMoment({
    moment: moment,
  });

  queryClient.invalidateQueries(["plugin:moments:list"]);

  Toast.success("发布成功");
};

const handleUpdate = async (moment: Moment) => {
  const { data } = await momentsUcApiClient.moment.getMyMoment({
    name: moment.metadata.name,
  });

  data.spec = moment.spec;

  await momentsUcApiClient.moment.updateMyMoment({
    name: moment.metadata.name,
    moment: data,
  });

  emit("update");

  queryClient.invalidateQueries(["plugin:moments:list"]);

  Toast.success("发布成功");
};

const parse = new DOMParser();
const queryEditorTags = function () {
  let tags: Set<string> = new Set();
  let document: Document = parse.parseFromString(formState.value.spec.content.raw!, "text/html");
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
    if (formState.value.spec.content.medium?.filter((item) => item.url == media.url).length != 0) {
      Toast.warning("已过滤重复添加的附件");
      return false;
    }
  }

  return true;
};

function handleToggleVisible() {
  // @unocss-skip-start
  const { visible: currentVisible } = formState.value.spec;
  // @unocss-skip-end
  formState.value.spec.visible = currentVisible === "PUBLIC" ? "PRIVATE" : "PUBLIC";
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
      :tag-query-fetch="useUCTagQueryFetch"
      class=":uno: min-h-[9rem] p-3.5"
      tabindex="-1"
      @keydown="handleKeydown"
    />
    <div v-if="formState.spec.content.medium?.length" class=":uno: img-box flex px-3.5 py-2">
      <ul class=":uno: grid grid-cols-3 w-full gap-1.5 sm:w-1/2" role="list">
        <li
          v-for="(media, index) in formState.spec.content.medium"
          :key="index"
          class=":uno: inline-block overflow-hidden border rounded-md"
        >
          <MediaCard :media="media" @remove="removeMedium"></MediaCard>
        </li>
      </ul>
    </div>
    <div class=":uno: flex justify-between bg-white px-3.5 py-2">
      <div class=":uno: h-fit">
        <div
          class=":uno: group flex cursor-pointer items-center justify-center rounded-full p-2 hover:bg-sky-600/10"
        >
          <TablerPhoto
            class=":uno: size-full text-base text-gray-600 group-hover:text-sky-600"
            @click="addMediumVerify() && (attachmentSelectorModal = true)"
          />
        </div>
      </div>

      <div class=":uno: flex items-center space-x-2.5">
        <div
          v-tooltip="{
            content: formState.spec.visible === 'PRIVATE' ? `私有访问` : '公开访问',
          }"
          class=":uno: group flex cursor-pointer items-center justify-center rounded-full p-2"
          :class="
            formState.spec.visible === 'PRIVATE'
              ? ':uno: hover:bg-red-600/10'
              : ':uno: hover:bg-green-600/10'
          "
          @click="handleToggleVisible()"
        >
          <IconEyeOff
            v-if="formState.spec.visible === 'PRIVATE'"
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

        <div v-permission="['uc:plugin:moments:publish']" class=":uno: h-fit">
          <VButton
            v-model:disabled="saveDisable"
            :loading="saving"
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
