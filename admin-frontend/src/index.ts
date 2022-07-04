import "./styles/index.css";
import type { PagesPublicState } from "@halo-dev/admin-shared";
import { BasicLayout, definePlugin } from "@halo-dev/admin-shared";
import JournalList from "@/views/JournalList.vue";
import type { Ref } from "vue";

export default definePlugin({
  name: "PluginJournals",
  components: [],
  routes: [
    {
      path: "/pages/functional/journals",
      component: BasicLayout,
      children: [
        {
          path: "",
          name: "Journals",
          component: JournalList,
        },
      ],
    },
  ],
  menus: [],
  extensionPoints: {
    PAGES: (state: Ref<PagesPublicState>) => {
      state.value.functionalPages.push({
        name: "日志",
        url: "/journals",
        path: "/pages/functional/journals",
      });
    },
  },
});
