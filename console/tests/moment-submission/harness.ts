import type { Moment } from "@/api/generated";
import MomentEdit from "@/components/MomentEdit.vue";
import type {
  MomentSubmissionAdapter,
  MomentSubmissionIntent,
  MomentSubmissionResult,
} from "@/features/moment-submission/types";
import { QueryClient, VueQueryPlugin } from "@tanstack/vue-query";
import { rs } from "@rstest/core";
import { mount, type VueWrapper } from "@vue/test-utils";
import { defineComponent } from "vue";

export type TestSubmissionResult = MomentSubmissionResult;
export type TestSubmissionAdapter = MomentSubmissionAdapter & {
  submit: ReturnType<
    typeof rs.fn<(intent: MomentSubmissionIntent) => Promise<MomentSubmissionResult>>
  >;
};

const TextEditorStub = defineComponent({
  name: "TextEditor",
  props: {
    html: {
      type: String,
      default: "",
    },
    isEmpty: {
      type: Boolean,
      default: true,
    },
    raw: {
      type: String,
      default: "",
    },
  },
  emits: ["keydown", "update:html", "update:isEmpty", "update:raw"],
  methods: {
    handleInput(event: Event) {
      const value = (event.target as HTMLTextAreaElement).value;
      this.$emit("update:raw", value);
      this.$emit("update:html", value);
      this.$emit("update:isEmpty", value.length === 0);
    },
  },
  template: `
    <textarea
      data-testid="moment-editor"
      :value="raw"
      @input="handleInput"
      @keydown="$emit('keydown', $event)"
    />
  `,
});

const VButtonStub = defineComponent({
  name: "VButton",
  props: {
    disabled: Boolean,
    loading: Boolean,
  },
  template: `
    <button
      data-testid="submit"
      :data-loading="String(loading)"
      :disabled="disabled"
    >
      <slot name="icon" />
    </button>
  `,
});

const AttachmentSelectorModalStub = defineComponent({
  name: "AttachmentSelectorModal",
  emits: ["close", "select"],
  template: "<div data-testid=\"attachment-selector\" />",
});

const MediaCardStub = defineComponent({
  name: "MediaCard",
  emits: ["remove"],
  template: "<div data-testid=\"media-card\" />",
});

export function createDeferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, reject, resolve };
}

export function createTestAdapter(
  implementation: (intent: MomentSubmissionIntent) => Promise<TestSubmissionResult>
): TestSubmissionAdapter {
  return {
    submit: rs.fn(implementation),
  };
}

interface MountWorkbenchOptions {
  adapter: TestSubmissionAdapter;
  moment?: Moment;
}

export function mountWorkbench(options: MountWorkbenchOptions): {
  queryClient: QueryClient;
  wrapper: VueWrapper;
} {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  const wrapper = mount(MomentEdit, {
    props: {
      adapter: options.adapter,
      moment: options.moment,
      tagQueryFetch: rs.fn(),
    } as never,
    global: {
      directives: {
        permission: () => undefined,
        tooltip: () => undefined,
      },
      plugins: [[VueQueryPlugin, { queryClient }]],
      stubs: {
        AttachmentSelectorModal: AttachmentSelectorModalStub,
        Button: VButtonStub,
        MediaCard: MediaCardStub,
        TextEditor: TextEditorStub,
        VButton: VButtonStub,
      },
    },
  });

  return { queryClient, wrapper };
}
