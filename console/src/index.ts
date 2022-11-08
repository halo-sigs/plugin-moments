import "./styles/index.css";
import type { PagesPublicState } from "@halo-dev/console-shared";
import { definePlugin } from "@halo-dev/console-shared";
import MomentsList from "@/views/MomentsList.vue";
import type { Ref } from "vue";

export default definePlugin({
  name: "PluginMoments",
  components: [],
  routes: [
    {
      parentName: "Root",
      route: {
        path: "functional/moments",
        name: "Moments",
        component: MomentsList,
        meta: {
          permissions: ["plugin:moments:view"],
        },
      },
    },
  ],
  extensionPoints: {
    PAGES: (state: Ref<PagesPublicState>) => {
      state.value.functionalPages.push({
        name: "瞬间",
        url: "/moments",
        path: "/pages/functional/moments",
      });
    },
  },
});
