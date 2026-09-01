import {createRouter, createWebHistory} from "vue-router";

const router = createRouter({
  history:createWebHistory(import.meta.env.BASE_URL),
  routes: [{
    path:"/",
    name:"登录",
    component: () => import("@/views/Login.vue")
  },
    {
      path:"/index",
      name:"主页",
      component:() => import("@/components/Index.vue")
    }

  ],
})

export default router
