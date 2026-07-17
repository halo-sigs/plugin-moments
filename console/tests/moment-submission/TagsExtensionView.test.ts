import TagsExtensionView from "@/extensions/tags/TagsExtensionView.vue";
import { expect, rs, test } from "@rstest/core";
import { flushPromises, mount } from "@vue/test-utils";

test("loads tag suggestions without depending on the host Vue Query context", async () => {
  const fetchTags = rs.fn(async (keyword?: string) =>
    keyword === "ha" ? ["halo", "happy"] : []
  );
  const wrapper = mount(TagsExtensionView, {
    props: {
      command: rs.fn(),
      query: "ha",
      tagQueryFetch: fetchTags,
    } as never,
  });

  await flushPromises();

  expect(fetchTags).toHaveBeenCalledWith("ha");
  expect(wrapper.text()).toContain("halo");
  expect(wrapper.text()).toContain("happy");
});
