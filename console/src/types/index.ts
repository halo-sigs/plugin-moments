import type { Contributor } from "@halo-dev/api-client/index";
import type { Component } from "vue";

export interface MenuItem {
  type: "button" | "separator";
  icon?: Component;
  title?: string;
  action?: () => void;
  isActive?: () => boolean;
  children?: MenuItem[];
}

export interface Metadata {
  name: string;
  generateName: string;
  labels?: {
    [key: string]: string;
  } | null;
  annotations?: {
    [key: string]: string;
  } | null;
  version?: number | null;
  creationTimestamp?: string | null;
  deletionTimestamp?: string | null;
}

export interface MomentSpec {
  content: MomentContent;
  releaseTime?: Date;
  visible?: MomentVisibleEnum;
  owner: string;
}

export interface Moment {
  spec: MomentSpec;
  apiVersion: string;
  kind: string;
  metadata: Metadata;
}

export interface MomentContent {
  raw: string;
  html: string;
  medium?: MomentMedia[];
}

export interface MomentMedia {
  type: MomentMediaTypeEnum;
  url: string;
  originType: string;
}

declare const MomentMediaTypeEnum: {
  readonly Photo: "PHOTO";
  readonly Video: "VIDEO";
  readonly Post: "POST";
};

export type MomentMediaTypeEnum =
  (typeof MomentMediaTypeEnum)[keyof typeof MomentMediaTypeEnum];

export declare const MomentVisibleEnum: {
  readonly Public: "PUBLIC";
  readonly Private: "PRIVATE";
};

export type MomentVisibleEnum =
  (typeof MomentVisibleEnum)[keyof typeof MomentVisibleEnum];

export interface MomentList {
  page: number;
  size: number;
  total: number;
  items: Array<ListedMoment>;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  totalPages: number;
}

export interface ListedMoment {
  moment: Moment;
  owner: Contributor;
  stats: Stats;
}

export interface Stats {
  upvote: number;
  totalComment: number;
  approvedComment: number;
}
