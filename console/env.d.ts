/// <reference types="vite/client" />
/// <reference types="unplugin-icons/types/vue" />

export {};

declare module "*.vue" {
  import Vue from "vue";
  export default Vue;
}
