import { createApp } from 'vue'
import router from './router'
import {createPinia} from "pinia";

import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from './App.vue'
//createApp(App).mount('#app')

const app = createApp(App)

app.use(router)
app.use(createPinia())
app.use(ElementPlus)
app.mount("#app")
