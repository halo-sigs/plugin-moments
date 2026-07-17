import TagsExtensionView from "@/extensions/tags/TagsExtensionView.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { expect, test, vi } from "vitest";

test("loads tag suggestions without depending on the host Vue Query context", async () => {
  const fetchTags = vi.fn(async (keyword?: string) =>
    keyword === "ha" ? ["halo", "happy"] : []
  );
  const wrapper = mount(TagsExtensionView, {
    props: {
      command: vi.fn(),
      query: "ha",
      tagQueryFetch: fetchTags,
    } as never,
  });

  await flushPromises();

  expect(fetchTags).toHaveBeenCalledWith("ha");
  expect(wrapper.text()).toContain("halo");
  expect(wrapper.text()).toContain("happy");
});
