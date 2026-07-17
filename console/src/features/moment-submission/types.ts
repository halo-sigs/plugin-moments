import type { MomentContent } from "@/api/generated";

export interface MomentDraft {
  content: MomentContent;
  visible: "PUBLIC" | "PRIVATE";
}

export interface MomentSubmissionPayload extends MomentDraft {
  tags: string[];
}

export type MomentSubmissionIntent =
  | {
      type: "create";
      payload: MomentSubmissionPayload;
    }
  | {
      type: "update";
      name: string;
      payload: MomentSubmissionPayload;
    };

export interface MomentSubmissionResult {
  name: string;
  status: "published" | "pending-review";
}

export interface MomentSubmissionAdapter {
  submit(intent: MomentSubmissionIntent): Promise<MomentSubmissionResult>;
}
