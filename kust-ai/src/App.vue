<template>
  <div class="h2">
    <h2>你好，我是vue3的根组件</h2>
    <h3>你好，我是你的智能助手YY</h3>
  </div>

  <demo1></demo1>
</template>

<script lang="ts" setup>
import demo1 from "@/demo/demo1.vue"
</script>

<style>
.h2 {
  background-color: blue;
  border-radius: 20px;
  padding: 20px;
}
</style>
