import type { Moment } from "@/api/generated";
import { Toast } from "@halo-dev/components";
import { beforeEach, describe, expect, rs, test } from "@rstest/core";
import { flushPromises } from "@vue/test-utils";
import {
  createDeferred,
  createTestAdapter,
  mountWorkbench,
  type TestSubmissionResult,
} from "./harness";

function existingMoment(): Moment {
  return {
    apiVersion: "moment.halo.run/v1alpha1",
    kind: "Moment",
    metadata: {
      creationTimestamp: "2026-07-17T00:00:00Z",
      name: "moment-existing",
    },
    spec: {
      approved: true,
      content: {
        html: "<p>Existing</p>",
        medium: [],
        raw: "<p>Existing</p>",
      },
      owner: "admin",
      releaseTime: "2026-07-17T00:00:00Z",
      tags: [],
      visible: "PUBLIC",
    },
  };
}

describe("Moment submission workbench", () => {
  beforeEach(() => {
    rs.spyOn(Toast, "success").mockImplementation(() => undefined);
    rs.spyOn(Toast, "error").mockImplementation(() => undefined);
    rs.spyOn(Toast, "warning").mockImplementation(() => undefined);
    rs.spyOn(console, "error").mockImplementation(() => undefined);
  });

  test("retains the draft and allows only one submission while persistence is pending", async () => {
    const deferred = createDeferred<TestSubmissionResult>();
    const adapter = createTestAdapter(() => deferred.promise);
    const { wrapper } = mountWorkbench({ adapter });

    await wrapper.get("[data-testid='moment-editor']").setValue("<p>Pending draft</p>");
    await wrapper.get("[data-testid='submit']").trigger("click");
    await wrapper.get("[data-testid='submit']").trigger("click");

    expect(adapter.submit).toHaveBeenCalledTimes(1);
    expect(wrapper.get("[data-testid='submit']").attributes("data-loading")).toBe("true");
    expect(
      (wrapper.get("[data-testid='moment-editor']").element as HTMLTextAreaElement).value
    ).toBe("<p>Pending draft</p>");

    deferred.resolve({ name: "moment-new", status: "published" });
    await flushPromises();
  });

  test("resets a new draft only after persistence succeeds", async () => {
    const deferred = createDeferred<TestSubmissionResult>();
    const adapter = createTestAdapter(() => deferred.promise);
    const { wrapper } = mountWorkbench({ adapter });

    await wrapper.get("[data-testid='moment-editor']").setValue("<p>New draft</p>");
    await wrapper.get("[data-testid='submit']").trigger("click");

    expect(
      (wrapper.get("[data-testid='moment-editor']").element as HTMLTextAreaElement).value
    ).toBe("<p>New draft</p>");

    deferred.resolve({ name: "moment-new", status: "published" });
    await flushPromises();

    expect(
      (wrapper.get("[data-testid='moment-editor']").element as HTMLTextAreaElement).value
    ).toBe("");
  });

  test("emits update completion only after an existing Moment is persisted", async () => {
    const deferred = createDeferred<TestSubmissionResult>();
    const adapter = createTestAdapter(() => deferred.promise);
    const { wrapper } = mountWorkbench({
      adapter,
      moment: existingMoment(),
    });

    await wrapper.get("[data-testid='moment-editor']").setValue("<p>Updated draft</p>");
    await wrapper.get("[data-testid='submit']").trigger("click");

    expect(wrapper.emitted("update")).toBeUndefined();

    deferred.resolve({ name: "moment-existing", status: "published" });
    await flushPromises();

    expect(wrapper.emitted("update")).toHaveLength(1);
  });

  test("retains a failed draft and allows retry", async () => {
    const adapter = createTestAdapter(
      rs
        .fn()
        .mockRejectedValueOnce(new Error("Submission failed"))
        .mockResolvedValueOnce({ name: "moment-retry", status: "published" })
    );
    const { wrapper } = mountWorkbench({ adapter });

    await wrapper.get("[data-testid='moment-editor']").setValue("<p>Retry draft</p>");
    await wrapper.get("[data-testid='submit']").trigger("click");
    await flushPromises();

    expect(
      (wrapper.get("[data-testid='moment-editor']").element as HTMLTextAreaElement).value
    ).toBe("<p>Retry draft</p>");

    await wrapper.get("[data-testid='submit']").trigger("click");
    await flushPromises();

    expect(adapter.submit).toHaveBeenCalledTimes(2);
  });

  test.each([
    ["published" as const, "发布成功"],
    ["pending-review" as const, "提交成功，等待审核"],
  ])("shows outcome-specific feedback for %s", async (status, message) => {
    const adapter = createTestAdapter(async () => ({ name: "moment-new", status }));
    const { wrapper } = mountWorkbench({ adapter });

    await wrapper.get("[data-testid='moment-editor']").setValue("<p>Outcome</p>");
    await wrapper.get("[data-testid='submit']").trigger("click");
    await flushPromises();

    expect(Toast.success).toHaveBeenCalledWith(message);
  });

  test("derives unique tags from the editor content at submission time", async () => {
    const adapter = createTestAdapter(async () => ({
      name: "moment-tags",
      status: "published",
    }));
    const { wrapper } = mountWorkbench({ adapter });
    const content =
      '<p><a class="tag">#Halo</a> and <a class="tag">#Halo</a> <a class="tag">#Vue</a></p>';

    await wrapper.get("[data-testid='moment-editor']").setValue(content);
    await wrapper.get("[data-testid='submit']").trigger("click");
    await flushPromises();

    expect(adapter.submit).toHaveBeenCalledWith({
      payload: {
        content: {
          html: content,
          medium: [],
          raw: content,
        },
        tags: ["#Halo", "#Vue"],
        visible: "PUBLIC",
      },
      type: "create",
    });
  });

  test("submits with Ctrl+Enter", async () => {
    const adapter = createTestAdapter(async () => ({
      name: "moment-shortcut",
      status: "published",
    }));
    const { wrapper } = mountWorkbench({ adapter });

    await wrapper.get("[data-testid='moment-editor']").setValue("<p>Shortcut</p>");
    await wrapper.get("[data-testid='moment-editor']").trigger("keydown", {
      ctrlKey: true,
      key: "Enter",
    });
    await flushPromises();

    expect(adapter.submit).toHaveBeenCalledTimes(1);
  });

  test("filters duplicate attachments and enforces the nine-attachment limit", async () => {
    const adapter = createTestAdapter(async () => ({
      name: "moment-media",
      status: "published",
    }));
    const { wrapper } = mountWorkbench({ adapter });
    const attachment = (index: number) =>
      ({
        spec: {
          displayName: `image-${index}`,
          mediaType: "image/png",
        },
        status: {
          permalink: `/image-${index}.png`,
        },
      }) as never;

    await wrapper.get("[data-testid='attachment-trigger']").trigger("click");
    const selector = wrapper.getComponent({ name: "AttachmentSelectorModal" });
    selector.vm.$emit("select", [attachment(0), attachment(0)]);
    await wrapper.vm.$nextTick();

    expect(wrapper.findAll("[data-testid='media-card']")).toHaveLength(1);
    expect(Toast.warning).toHaveBeenCalledWith("已过滤重复添加的附件");

    selector.vm.$emit(
      "select",
      Array.from({ length: 10 }, (_, index) => attachment(index + 1))
    );
    await wrapper.vm.$nextTick();

    expect(wrapper.findAll("[data-testid='media-card']")).toHaveLength(9);
    expect(Toast.warning).toHaveBeenCalledWith("最多允许添加 9 个附件");
  });

  test("keeps persistence success when cache invalidation fails", async () => {
    const adapter = createTestAdapter(async () => ({
      name: "moment-cache",
      status: "published",
    }));
    const { queryClient, wrapper } = mountWorkbench({ adapter });
    rs.spyOn(queryClient, "invalidateQueries").mockRejectedValue(new Error("Cache failed"));

    await wrapper.get("[data-testid='moment-editor']").setValue("<p>Cache</p>");
    await wrapper.get("[data-testid='submit']").trigger("click");
    await flushPromises();

    expect(Toast.success).toHaveBeenCalledWith("发布成功");
    expect(
      (wrapper.get("[data-testid='moment-editor']").element as HTMLTextAreaElement).value
    ).toBe("");
  });
});
