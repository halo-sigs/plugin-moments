import apiClient from "@/utils/api-client";
import { useQuery } from "@tanstack/vue-query";
import type { Ref } from "vue";

export interface useTagQueryFetchProps {
  keyword?: Ref<string | undefined>;
}

export function useConsoleTagQueryFetch(
  props: useTagQueryFetchProps
): ReturnType<typeof useQuery> {
  return useTagQueryFetch("console", props);
}

export function useUCTagQueryFetch(
  props: useTagQueryFetchProps
): ReturnType<typeof useQuery> {
  return useTagQueryFetch("uc", props);
}

export function useTagQueryFetch(
  group: "console" | "uc",
  props: useTagQueryFetchProps
): ReturnType<typeof useQuery> {
  return useQuery<string[]>({
    queryKey: ["moments-tags", props.keyword],
    queryFn: async () => {
      const { data } = await apiClient.get(
        `/apis/${group}.api.moment.halo.run/v1alpha1/tags`,
        {
          params: {
            name: props.keyword?.value,
          },
        }
      );
      return data;
    },
  });
}
