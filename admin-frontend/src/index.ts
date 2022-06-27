import { definePlugin } from "@halo-dev/admin-shared";
import DefaultView from "./views/DefaultView.vue";
import { IconGrid } from "@halo-dev/components";
import "./styles/index.css";

export default definePlugin({
  name: "PluginJournals",
  components: [],
  extensionPoints: {},
  routes: [
    {
      path: "/hello-world",
      name: "HelloWorld",
      component: DefaultView,
    },
  ],
  menus: [
    {
      name: "From PluginTemplate",
      items: [
        {
          name: "HelloWorld",
          path: "/hello-world",
          icon: IconGrid,
        },
      ],
    },
  ],
  activated() {
    // TODO
  },
  deactivated() {
    // TODO
  },
});

