<script setup lang="ts">

import vue from "@vitejs/plugin-vue";

let username = "小花";
let age = 30;

import { ref } from "vue";
let count = ref(1);
let refusername = ref("马立群")

function addAge() {
  age = age + 1;
  console.log("age变量的值正在发生变化", age);
}

function addCount() {
  count.value = count.value + 1;
  console.log("count变量的值正在发生变化", count.value);
}

let dict = ref({username:"马加爵", age:100, phone:123456789});
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
  <p>=========输入框，通过v-model进行双向绑定=========</p>
  <p>username变量： <input type="text" v-model = "username"></p>
  <p>refusername变量：<input type="text" v-model ="refusername"></p>

  dict的值：{{dict}}<br>
  dict的值username:{{dict.username}}
</template>

<style scoped>

</style>
