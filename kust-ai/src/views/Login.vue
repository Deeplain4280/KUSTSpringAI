<script setup lang="ts">

  import { ref } from 'vue'

  let account = ref({
    email: '',
    code: ''
  })

  function sendCode() {
    console.log(account.value)
  }
  function login() {
    console.log(account.value)
  }

</script>

<template>
  <div>
    <p>用户邮箱：<input type="text" v-model = "account.email"></p>
    <p>验证码：<input type="text" v-model="account.code"></p>
    <p>
      <button @click="sendCode()">发送验证码</button>
      <button @click="login()">登录</button>
    </p>
  </div>
</template>

<style scoped>

</style>
