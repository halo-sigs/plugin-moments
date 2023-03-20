<script lang="ts" setup>
import { VButton } from "@halo-dev/components";
import type { Moment, MomentMedia, MomentMediaTypeEnum } from "@/types";
import apiClient from "@/utils/api-client";
import { Toast } from "@halo-dev/components";
import type { AttachmentLike } from "@halo-dev/console-shared";
import { computed, nextTick, onMounted, ref, toRaw } from "vue";
import MediumCard from "./MediumCard.vue";
import TextEditor from "./TextEditor.vue";
import { v4 as uuid } from "uuid";
import SendMoment from "~icons/ic/sharp-send";
import MdiFileImageBox from "~icons/mdi/file-image-box";
import MdiShow from "~icons/mdi/show";
import MdiHide from "~icons/mdi/hide";
import cloneDeep from "lodash.clonedeep";

const props = withDefaults(
  defineProps<{
    moment: Moment;
  }>(),
  {
    moment: undefined,
  }
);

const emit = defineEmits<{
  (event: "save", moment: Moment): void;
  (event: "update", moment: Moment): void;
}>();

const initMoment: Moment = {
  spec: {
    content: {
      raw: "",
      html: "",
      medium: [],
    },
    releaseTime: new Date(),
    owner: "",
  },
  metadata: {
    generateName: "moment-",
  },
  kind: "Moment",
  apiVersion: "moment.halo.run/v1alpha1",
};

onMounted(() => {
  if (props.moment) {
    formState.value = toRaw(props.moment);
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
    if (isUpdateMode.value) {
      updateMoment();
    } else {
      insertMoment();
    }
  } catch (error) {
    console.error(error);
  } finally {
    saving.value = false;
  }
};

const insertMoment = async () => {
  formState.value.spec.releaseTime = new Date();
  const { data } = await apiClient.post<Moment>(
    `/apis/api.plugin.halo.run/v1alpha1/moments`,
    formState.value
  );
  emit("save", data);
  handleReset();
  Toast.success("发布成功");
  return data;
};

const updateMoment = async () => {
  const { data } = await apiClient.put<Moment>(
    `/apis/moment.halo.run/v1alpha1/moments/${formState.value.metadata.name}`,
    formState.value
  );
  emit("update", data);
  Toast.success("发布成功");
  return data;
};

const handleReset = () => {
  formState.value = toRaw(cloneDeep(initMoment));
  isEditorEmpty.value = true;
};

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

  //目前只能弹窗进行提醒，无法做到选择的时候就指定特定类型文件
  for (let media of medias) {
    let type = media.type;
    if (!type) {
      Toast.error("不支持未知类型的文件");
      nextTick(() => {
        attachmentSelectorModal.value = true;
      });

      return;
    }
    let fileType = type.split("/")[0];
    if (!MediumWhitelist.has(fileType)) {
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
    let fileType = media.type!.split("/")[0];
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
  // TODO 更新状态下初始化默认可发送状态有问题
  if (!isEditorEmpty.value) {
    return false;
  }
  return true;
});

const removeMedium = (medium: MomentMedia) => {
  let formMedium = formState.value.spec.content.medium;
  if (!formMedium) {
    return;
  }
  let index: number | undefined = formMedium.indexOf(medium);
  if (index != -1) {
    formMedium.splice(index, 1);
  }
};
</script>

<template>
  <div
    class="card moments-bg-white moments-shrink moments-border moments-rounded-md moments-w-160 moments-overflow-hidden"
  >
    <AttachmentSelectorModal
      v-model:visible="attachmentSelectorModal"
      @select="onAttachmentsSelect"
    />
    <TextEditor
      v-model:raw="formState.spec.content.raw"
      v-model:html="formState.spec.content.html"
      v-model:isEmpty="isEditorEmpty"
      class="moments-min-h-36 moments-p-3.5"
    />
    <div
      v-if="
        !!formState.spec.content.medium &&
        formState.spec.content.medium.length > 0
      "
      class="img-box moments-flex moments-px-3.5 moments-py-2"
    >
      <ul role="list">
        <li
          v-for="(medium, index) in formState.spec.content.medium"
          :key="index"
          class="moments-rounded-md moments-border moments-overflow-hidden moments-inline-block moments-mr-2 moments-mb-2"
        >
          <MediumCard :medium="medium" @remove="removeMedium"></MediumCard>
        </li>
        <li class="moments-inline-block">
          <div></div>
        </li>
      </ul>
    </div>
    <div
      class="moments-h-8 moments-bg-white moments-flex moments-justify-between moments-mb-1"
    >
      <div class="moments-left-0 moments-h-fit moments-ml-3.5">
        <VButton
          size="sm"
          type="primary"
          @click="attachmentSelectorModal = true"
        >
          <template #icon>
            <MdiFileImageBox class="h-full w-full" />
          </template>
        </VButton>
      </div>

      <div class="moments-flex moments-items-center">
        <div class="moments-right-0 moments-mr-3.5 moments-cursor-pointer">
          <MdiHide
            v-if="formState.spec.visible === 'PRIVATE'"
            @click="formState.spec.visible = 'PUBLIC'"
          />
          <MdiShow v-else @click="formState.spec.visible = 'PRIVATE'" />
        </div>

        <div
          class="moments-right-0 moments-h-fit moments-mr-3.5"
          v-permission="['plugin:moments:manage']"
        >
          <VButton
            :loading="saving"
            size="sm"
            type="primary"
            v-model:disabled="saveDisable"
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
