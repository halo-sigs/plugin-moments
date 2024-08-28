import { momentsConsoleApiClient, momentsUcApiClient } from "@/api";
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
      if (group === "console") {
        const { data } = await momentsConsoleApiClient.moment.listTags({
          name: props.keyword?.value,
        });
        return data;
      }

      if (group === "uc") {
        const { data } = await momentsUcApiClient.moment.listTags1({
          name: props.keyword?.value,
        });
        return data;
      }

      throw new Error("Invalid group");
    },
  });
}
