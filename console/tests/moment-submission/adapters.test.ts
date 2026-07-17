import { momentsConsoleApiClient, momentsCoreApiClient, momentsUcApiClient } from "@/api";
import type { Moment } from "@/api/generated";
import {
  consoleMomentSubmissionAdapter,
  userCenterMomentSubmissionAdapter,
} from "@/features/moment-submission/adapters";
import type { MomentSubmissionIntent } from "@/features/moment-submission/types";
import { beforeEach, describe, expect, test, vi } from "vitest";

const payload = {
  content: {
    html: '<p>Hello <a class="tag">#Halo</a></p>',
    medium: [],
    raw: '<p>Hello <a class="tag">#Halo</a></p>',
  },
  tags: ["#Halo"],
  visible: "PRIVATE" as const,
};

function persistedMoment(overrides: Partial<Moment["spec"]> = {}): Moment {
  return {
    apiVersion: "moment.halo.run/v1alpha1",
    kind: "Moment",
    metadata: {
      creationTimestamp: "2026-07-17T00:00:00Z",
      name: "moment-test",
    },
    spec: {
      approved: true,
      approvedTime: "2026-07-17T00:00:00Z",
      content: payload.content,
      owner: "admin",
      releaseTime: "2026-07-17T00:00:00Z",
      tags: payload.tags,
      visible: payload.visible,
      ...overrides,
    },
  };
}

describe("Console Moment submission adapter", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  test("maps create intent and owns Console-managed publication fields", async () => {
    const create = vi
      .spyOn(momentsConsoleApiClient.moment, "createMoment")
      .mockResolvedValue({ data: persistedMoment() } as never);

    const result = await consoleMomentSubmissionAdapter.submit({
      type: "create",
      payload,
    });

    expect(create).toHaveBeenCalledWith({
      moment: expect.objectContaining({
        apiVersion: "moment.halo.run/v1alpha1",
        kind: "Moment",
        metadata: { generateName: "moment-", name: "" },
        spec: expect.objectContaining({
          approved: true,
          content: payload.content,
          owner: "",
          tags: payload.tags,
          visible: payload.visible,
        }),
      }),
    });
    expect(create.mock.calls[0][0].moment.spec.releaseTime).toEqual(expect.any(String));
    expect(result).toEqual({ name: "moment-test", status: "published" });
  });

  test("patches only editable fields for update", async () => {
    const patch = vi
      .spyOn(momentsCoreApiClient.moment, "patchMoment")
      .mockResolvedValue({ data: persistedMoment() } as never);
    const intent: MomentSubmissionIntent = {
      name: "moment-test",
      payload,
      type: "update",
    };

    await consoleMomentSubmissionAdapter.submit(intent);

    expect(patch).toHaveBeenCalledWith({
      jsonPatchInner: [
        { op: "add", path: "/spec/tags", value: payload.tags },
        { op: "add", path: "/spec/content", value: payload.content },
        { op: "add", path: "/spec/visible", value: payload.visible },
      ],
      name: "moment-test",
    });
  });
});

describe("User Center Moment submission adapter", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  test("leaves approval and release time to the server on create", async () => {
    const create = vi
      .spyOn(momentsUcApiClient.moment, "createMyMoment")
      .mockResolvedValue({ data: persistedMoment({ approved: false }) } as never);

    const result = await userCenterMomentSubmissionAdapter.submit({
      type: "create",
      payload,
    });

    const submitted = create.mock.calls[0][0].moment;
    expect(submitted.spec).toEqual({
      content: payload.content,
      owner: "",
      tags: payload.tags,
      visible: payload.visible,
    });
    expect(result).toEqual({ name: "moment-test", status: "pending-review" });
  });

  test("preserves server-managed fields while replacing editable fields on update", async () => {
    const current = persistedMoment();
    vi.spyOn(momentsUcApiClient.moment, "getMyMoment").mockResolvedValue({
      data: current,
    } as never);
    const update = vi
      .spyOn(momentsUcApiClient.moment, "updateMyMoment")
      .mockResolvedValue({ data: persistedMoment({ approved: false }) } as never);

    const result = await userCenterMomentSubmissionAdapter.submit({
      name: "moment-test",
      payload,
      type: "update",
    });

    expect(update).toHaveBeenCalledWith({
      moment: {
        ...current,
        spec: {
          ...current.spec,
          content: payload.content,
          tags: payload.tags,
          visible: payload.visible,
        },
      },
      name: "moment-test",
    });
    expect(result.status).toBe("pending-review");
  });
});
