import type { Plugin } from "@halo-dev/admin-shared/src/types";
import DefaultView from "./views/DefaultView.vue";
import { IconGrid } from "@halo-dev/components";
import "./styles/index.css";

const plugin: Plugin = {
  name: "PluginTemplate",
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
};
export default plugin;
