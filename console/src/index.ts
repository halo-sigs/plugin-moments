import type { Moment } from "@/types";
import UcMomentsList from "@/uc/MomentsList.vue";
import MomentsList from "@/views/MomentsList.vue";
import type { Extension } from "@halo-dev/api-client";
import {
  type CommentSubjectRefResult,
  definePlugin,
} from "@halo-dev/console-shared";
import { markRaw } from "vue";
import MingcuteMomentsLine from "~icons/mingcute/moment-line";
import "./styles/index.scss";
import { formatDatetime } from "./utils/date";

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
  ucRoutes: [
    {
      parentName: "Root",
      route: {
        path: "/moments",
        name: "Moments",
        component: UcMomentsList,
        meta: {
          permissions: ["uc:plugin:moments:publish"],
          menu: {
            name: "瞬间",
            group: "content",
            icon: markRaw(MingcuteMomentsLine),
          },
        },
      },
    },
  ],
  extensionPoints: {
    "comment:subject-ref:create": () => {
      return [
        {
          kind: "Moment",
          group: "moment.halo.run",
          resolve: (subject: Extension): CommentSubjectRefResult => {
            const moment = subject as Moment;
            return {
              label: "瞬间",
              title: determineMomentTitle(moment),
              externalUrl: `/moments/${moment.metadata.name}`,
              route: {
                name: "Moments",
              },
            };
          },
        },
      ];
    },
  },
});

const determineMomentTitle = (moment: Moment) => {
  const pureContent = stripHtmlTags(moment.spec.content.raw);
  const title = !pureContent?.trim()
    ? formatDatetime(new Date(moment.spec.releaseTime || ""))
    : pureContent;
  return title?.substring(0, 100);
};

const stripHtmlTags = (str: string) => {
  // strip html tags
  const stripped = str?.replace(/<\/?[^>]+(>|$)/gi, "") || "";
  // strip newlines and collapse spaces
  return stripped.replace(/\n/g, " ").replace(/\s+/g, " ");
};
