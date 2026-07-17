import { momentsConsoleApiClient, momentsUcApiClient } from "@/api";
import { useQuery } from "@tanstack/vue-query";
import type { Ref } from "vue";

export interface useTagQueryFetchProps {
  keyword?: Ref<string | undefined>;
}

export type TagQueryFetch = (keyword?: string) => Promise<string[]>;

export const fetchConsoleTags: TagQueryFetch = async (keyword) => {
  const { data } = await momentsConsoleApiClient.moment.listTags({
    name: keyword,
  });
  return data;
};

export const fetchUserCenterTags: TagQueryFetch = async (keyword) => {
  const { data } = await momentsUcApiClient.moment.listTags1({
    name: keyword,
  });
  return data;
};

export function useConsoleTagQueryFetch(props: useTagQueryFetchProps): ReturnType<typeof useQuery> {
  return useTagQueryFetch(fetchConsoleTags, props);
}

export function useUCTagQueryFetch(props: useTagQueryFetchProps): ReturnType<typeof useQuery> {
  return useTagQueryFetch(fetchUserCenterTags, props);
}

export function useTagQueryFetch(
  fetchTags: TagQueryFetch,
  props: useTagQueryFetchProps
): ReturnType<typeof useQuery> {
  return useQuery<string[]>({
    queryKey: ["moments-tags", props.keyword],
    queryFn: () => fetchTags(props.keyword?.value),
  });
}
