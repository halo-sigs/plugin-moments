import "./styles/tailwind.css";
import "./styles/index.css";
import { definePlugin } from "@halo-dev/console-shared";
import MomentsList from "@/views/MomentsList.vue";
import { markRaw } from "vue";
import MingcuteMomentsLine from "~icons/mingcute/moment-line";

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/moments",
        name: "Moments",
        component: MomentsList,
        meta: {
          permissions: ["plugin:moments:view"],
          menu: {
            name: "瞬间",
            group: "content",
            icon: markRaw(MingcuteMomentsLine),
          },
        },
      },
    },
  ],
});
