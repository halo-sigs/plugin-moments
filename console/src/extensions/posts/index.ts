import { Editor, Extension, VueRenderer } from "@halo-dev/richtext-editor";
import Suggestion from "@tiptap/suggestion";
import { PluginKey } from "@tiptap/pm/state";
import PostsView from "./PostsView.vue";
import type { Instance } from "tippy.js";
import tippy from "tippy.js";
import type { Post } from "@halo-dev/api-client/index";

export interface Item {
  post: Post;
  command: ({ editor, range }: { editor: Editor; range: Range }) => void;
}

const PostsExtension = Extension.create({
  name: "postsExtension",
  addOptions() {
    return {
      suggestion: {
        char: "#",
        render: () => {
          let component: VueRenderer;
          let popup: Instance[];

          return {
            onStart: (props: Record<string, any>) => {
              component = new VueRenderer(PostsView, {
                props,
                editor: props.editor,
              });

              if (!props.clientRect) {
                return;
              }

              popup = tippy("body", {
                getReferenceClientRect: props.clientRect,
                appendTo: () => document.body,
                content: component.element,
                showOnCreate: true,
                interactive: true,
                trigger: "manual",
                placement: "bottom-start",
              });
            },

            onUpdate(props: Record<string, any>) {
              component.updateProps(props);

              if (!props.clientRect) {
                return;
              }

              popup[0].setProps({
                getReferenceClientRect: props.clientRect,
              });
            },

            onKeyDown(props: Record<string, any>) {
              if (props.event.key === "Escape") {
                popup[0].hide();

                return true;
              }

              return component.ref?.onKeyDown(props);
            },

            onExit() {
              popup[0].destroy();
              component.destroy();
            },
          };
        },
        command: ({
          editor,
          range,
          props,
        }: {
          editor: Editor;
          range: Range;
          props: Record<string, any>;
        }) => {
          props.command({ editor, range });
        },
      },
    };
  },

  addProseMirrorPlugins() {
    return [
      Suggestion({
        pluginKey: new PluginKey("postsSuggestion"),
        editor: this.editor,
        ...this.options.suggestion,
      }),
    ];
  },
});

export default PostsExtension;
