<script setup lang="ts">

import vue from "@vitejs/plugin-vue";

const username = "小花";
let age = 30;

import { ref } from "vue";
let count = ref(1);
const refusername = ref("马立群")

function addAge() {
  age = age + 1;
  console.log("age变量的值正在发生变化", age);
}

function addCount() {
  count.value = count.value + 1;
  console.log("count变量的值正在发生变化", count.value);
}

</script>

<template>
  <p>==========插值语法==========</p>
  <p>username的值{{username}}</p>
  <p>age的值{{age}}}</p>
  <p>count的值{{count}}}</p>
  <p>refusername的值{{refusername}}}</p>
  <p>==========v-on事件绑定=======</p>
  <button v-on:click="addAge()"></button>
  <button @click="addCount()"></button>
</template>

<style scoped>

</style>
