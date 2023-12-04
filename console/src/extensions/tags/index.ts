import {
  type Editor,
  Mark,
  VueRenderer,
  markInputRule,
  markPasteRule,
  mergeAttributes,
} from "@halo-dev/richtext-editor";
import type { Instance } from "tippy.js";
import TagsExtensionView from "./TagsExtensionView.vue";
import tippy from "tippy.js";
import Suggestion from "@tiptap/suggestion";
import { PluginKey } from "@halo-dev/richtext-editor";

export interface TagOptions {
  HTMLAttributes: Record<string, any>;
}

declare module "@halo-dev/richtext-editor" {
  interface Commands<ReturnType> {
    tag: {
      /**
       * Set a tag mark
       */
      setTag: (name: string) => ReturnType;
    };
  }
}

export const inputRegex = /#([\u4E00-\u9FFFa-zA-Z0-9]+)(?<!#\S)\s$/;
export const pasteRegex = /#([\u4E00-\u9FFFa-zA-Z0-9]+)(?<!#\S)\s/g;

export const TagsExtension = Mark.create<TagOptions>({
  name: "tag",

  addOptions() {
    return {
      HTMLAttributes: {
        class: "tag",
      },
      suggestion: {
        char: "#",
        render: () => {
          let component: VueRenderer;
          let popup: Instance[];

          return {
            onStart: (props: Record<string, any>) => {
              component = new VueRenderer(TagsExtensionView, {
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
          props: string;
        }) => {
          editor.chain().focus().deleteRange(range).setTag(props).run();
        },
      },
    };
  },

  excludes: "_",

  exitable: false,

  inclusive: false,

  addAttributes() {
    return {
      ...this.parent?.(),
      tagText: {
        default: null,
        renderHTML(attributes) {
          return {
            href: `?tag=${encodeURI(attributes.tagText)}`,
            "data-pjax": "",
          };
        },
      },
    };
  },

  parseHTML() {
    return [
      {
        tag: "a",
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      "a",
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
      0,
    ];
  },

  addCommands() {
    return {
      setTag:
        (tag) =>
        ({ commands }) => {
          return commands.insertContent({
            type: "text",
            marks: [
              {
                type: this.name,
                attrs: {
                  tagText: tag,
                },
              },
            ],
            text: tag,
          });
        },
    };
  },

  addInputRules() {
    return [
      markInputRule({
        find: inputRegex,
        type: this.type,
        getAttributes(match) {
          return {
            tagText: match[1],
          };
        },
      }),
    ];
  },

  addPasteRules() {
    return [
      markPasteRule({
        find: pasteRegex,
        type: this.type,
        getAttributes(match) {
          return {
            tagText: match[1],
          };
        },
      }),
    ];
  },

  addProseMirrorPlugins() {
    return [
      Suggestion({
        pluginKey: new PluginKey("tagsSuggestion"),
        editor: this.editor,
        ...this.options.suggestion,
      }),
    ];
  },
});
