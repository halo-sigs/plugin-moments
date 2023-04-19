import {
  Mark,
  markInputRule,
  markPasteRule,
  mergeAttributes,
} from "@halo-dev/richtext-editor";

export interface TagOptions {
  HTMLAttributes: Record<string, any>;
}

declare module "@halo-dev/richtext-editor" {
  interface Commands<ReturnType> {
    tag: {
      /**
       * Set a tag mark
       */
      setTag: () => ReturnType;
      /**
       * Toggle inline tag
       */
      toggleTag: () => ReturnType;
      /**
       * Unset a tag mark
       */
      unsetTag: () => ReturnType;
    };
  }
}

export const inputRegex = /(?:^|\s)((?:#)((?:[^#]+))(?: ))$/;
export const pasteRegex = /(?:^|\s)((?:#)((?:[^#]+))(?: ))/g;

const TagExtension = Mark.create<TagOptions>({
  name: "tagExtension",

  addOptions() {
    return {
      HTMLAttributes: {
        class: "tag",
      },
    };
  },

  excludes: "_",

  exitable: true,

  renderHTML({ HTMLAttributes }) {
    return [
      "span",
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
      0,
    ];
  },

  addCommands() {
    return {
      setCode:
        () =>
        ({ commands }) => {
          return commands.setMark(this.name);
        },
      toggleCode:
        () =>
        ({ commands }) => {
          return commands.toggleMark(this.name);
        },
      unsetCode:
        () =>
        ({ commands }) => {
          return commands.unsetMark(this.name);
        },
    };
  },

  addInputRules() {
    return [
      markInputRule({
        find: inputRegex,
        type: this.type,
      }),
    ];
  },

  addPasteRules() {
    return [
      markPasteRule({
        find: pasteRegex,
        type: this.type,
      }),
    ];
  },
});

export default TagExtension;
