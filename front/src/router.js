import { createRouter, createWebHistory } from 'vue-router'
import { session } from './stores/session'
import HomeView from './views/HomeView.vue'
import LoginView from './views/LoginView.vue'
import RegisterView from './views/RegisterView.vue'
import MyVideosView from './views/MyVideosView.vue'
import AthleteVideosView from './views/AthleteVideosView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: LoginView, meta: { guestOnly: true } },
    { path: '/registro', name: 'register', component: RegisterView, meta: { guestOnly: true } },
    {
      path: '/mis-videos',
      name: 'my-videos',
      component: MyVideosView,
      meta: { requiresAuth: true, role: 'DEPORTISTA' },
    },
    {
      path: '/deportistas/:athleteId/videos',
      name: 'athlete-videos',
      component: AthleteVideosView,
      props: true,
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !session.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.role && session.user?.role !== to.meta.role) return { name: 'home' }
  if (to.meta.guestOnly && session.token) return { name: 'home' }
})

/** Accepts only same-app paths, so ?redirect= cannot send users to another site. */
export function safeRedirect(value) {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//') ? value : null
}

export default router
