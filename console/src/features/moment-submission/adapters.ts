import { momentsConsoleApiClient, momentsCoreApiClient, momentsUcApiClient } from "@/api";
import type { Moment } from "@/api/generated";
import { cloneDeep } from "es-toolkit";
import type {
  MomentSubmissionAdapter,
  MomentSubmissionPayload,
  MomentSubmissionResult,
} from "./types";

function createMoment(
  payload: MomentSubmissionPayload,
  systemFields: Pick<Moment["spec"], "approved" | "releaseTime">
): Moment {
  return {
    apiVersion: "moment.halo.run/v1alpha1",
    kind: "Moment",
    metadata: {
      generateName: "moment-",
      name: "",
    },
    spec: {
      content: cloneDeep(payload.content),
      owner: "",
      tags: [...payload.tags],
      visible: payload.visible,
      ...systemFields,
    },
  };
}

function normalizeResult(moment: Moment): MomentSubmissionResult {
  return {
    name: moment.metadata.name,
    status: moment.spec.approved === false ? "pending-review" : "published",
  };
}

export const consoleMomentSubmissionAdapter: MomentSubmissionAdapter = {
  async submit(intent) {
    if (intent.type === "create") {
      const { data } = await momentsConsoleApiClient.moment.createMoment({
        moment: createMoment(intent.payload, {
          approved: true,
          releaseTime: new Date().toISOString(),
        }),
      });
      return normalizeResult(data);
    }

    const { data } = await momentsCoreApiClient.moment.patchMoment({
      name: intent.name,
      jsonPatchInner: [
        {
          op: "add",
          path: "/spec/tags",
          value: intent.payload.tags,
        },
        {
          op: "add",
          path: "/spec/content",
          value: intent.payload.content,
        },
        {
          op: "add",
          path: "/spec/visible",
          value: intent.payload.visible,
        },
      ],
    });
    return normalizeResult(data);
  },
};

export const userCenterMomentSubmissionAdapter: MomentSubmissionAdapter = {
  async submit(intent) {
    if (intent.type === "create") {
      const { data } = await momentsUcApiClient.moment.createMyMoment({
        moment: createMoment(intent.payload, {}),
      });
      return normalizeResult(data);
    }

    const { data: current } = await momentsUcApiClient.moment.getMyMoment({
      name: intent.name,
    });
    const moment = cloneDeep(current);
    moment.spec.content = cloneDeep(intent.payload.content);
    moment.spec.tags = [...intent.payload.tags];
    moment.spec.visible = intent.payload.visible;

    const { data } = await momentsUcApiClient.moment.updateMyMoment({
      name: intent.name,
      moment,
    });
    return normalizeResult(data);
  },
};
