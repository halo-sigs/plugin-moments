import { axiosInstance } from "@halo-dev/api-client";
import {
  ConsoleApiMomentHaloRunV1alpha1MomentApi,
  MomentV1alpha1Api,
  UcApiMomentHaloRunV1alpha1MomentApi,
} from "./generated";

const momentsCoreApiClient = {
  moment: new MomentV1alpha1Api(undefined, "", axiosInstance),
};

const momentsConsoleApiClient = {
  moment: new ConsoleApiMomentHaloRunV1alpha1MomentApi(undefined, "", axiosInstance),
};

const momentsUcApiClient = {
  moment: new UcApiMomentHaloRunV1alpha1MomentApi(undefined, "", axiosInstance),
};

export { momentsConsoleApiClient, momentsCoreApiClient, momentsUcApiClient };
