<script lang="ts">
import { defineComponent, ref } from "vue";

export default defineComponent({
  name: "JournalList",
  setup() {
    const name = ref("Ryan");
    return {
      name,
    };
  },
});
</script>
<template>
  <div>Hello {{ name }}</div>
</template>
