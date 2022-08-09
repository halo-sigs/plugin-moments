import "./styles/index.css";
import type { PagesPublicState } from "@halo-dev/admin-shared";
import { definePlugin } from "@halo-dev/admin-shared";
import JournalList from "@/views/JournalList.vue";
import type { Ref } from "vue";

export default definePlugin({
  name: "PluginJournals",
  components: [],
  routes: [
    {
      parentName: "BasePages",
      route: {
        path: "functional/journals",
        name: "Journals",
        component: JournalList,
        meta: {
          permissions: ["plugin:journals:view"],
        },
      },
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
