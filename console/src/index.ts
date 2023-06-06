import "./styles/tailwind.css";
import "./styles/index.css";
import { definePlugin } from "@halo-dev/console-shared";
import MomentsList from "@/views/MomentsList.vue";
import { markRaw } from "vue";
import MingcuteMomentsLine from "~icons/mingcute/moment-line";
import type { Moment } from "@/types";
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
  extensionPoints: {
    "comment:subject-ref:create": () => {
      return [
        {
          Moment: (subject: Extension): CommentSubjectRefResult => {
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
  const title = stripHtmlTags(moment.spec.content.raw);
  
  return !title?.trim() ? formatDatetime(new Date(moment.spec.releaseTime || "")) : title;
};

const stripHtmlTags = (str: string) => {
  return str?.replace(/<\/?[^>]+(>|$)/gi, "") || ""
};
