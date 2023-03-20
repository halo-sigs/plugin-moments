import { ApiConsoleHaloRunV1alpha1PostApi } from "@halo-dev/api-client";
import axios from "axios";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
});

export const postApiClient = new ApiConsoleHaloRunV1alpha1PostApi(
  undefined,
  import.meta.env.VITE_API_URL,
  axios
);

export default apiClient;
