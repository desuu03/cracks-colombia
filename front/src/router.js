import { createRouter, createWebHistory } from 'vue-router'
import { session } from './stores/session'
import HomeView from './views/HomeView.vue'
import LoginView from './views/LoginView.vue'
import RegisterView from './views/RegisterView.vue'
import ProfileCreateView from './views/ProfileCreateView.vue'
import ProfileEditView from './views/ProfileEditView.vue'
import MyVideosView from './views/MyVideosView.vue'
import AthleteProfileView from './views/AthleteProfileView.vue'
import SearchView from './views/SearchView.vue'
import RequestsView from './views/RequestsView.vue'

const ATHLETE = ['DEPORTISTA']
const SCOUT = ['OJEADOR_ENTRENADOR']

/** Where each role lands after login or when opening "/". */
export function homeFor(role) {
  if (role === 'DEPORTISTA') return { name: 'my-profile' }
  if (role === 'OJEADOR_ENTRENADOR') return { name: 'search' }
  return { name: 'home' }
}

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      beforeEnter: () => {
        const target = homeFor(session.user?.role)
        if (target.name !== 'home') return target
      },
    },
    { path: '/login', name: 'login', component: LoginView, meta: { guestOnly: true } },
    { path: '/registro', name: 'register', component: RegisterView, meta: { guestOnly: true } },
    { path: '/perfil', name: 'my-profile', component: ProfileEditView, meta: { requiresAuth: true, roles: ATHLETE } },
    { path: '/perfil/crear', name: 'profile-create', component: ProfileCreateView, meta: { requiresAuth: true, roles: ATHLETE } },
    { path: '/mis-videos', name: 'my-videos', component: MyVideosView, meta: { requiresAuth: true, roles: ATHLETE } },
    { path: '/buscar', name: 'search', component: SearchView, meta: { requiresAuth: true, roles: SCOUT } },
    {
      path: '/solicitudes',
      name: 'requests',
      component: RequestsView,
      meta: { requiresAuth: true, roles: [...ATHLETE, ...SCOUT] },
    },
    {
      path: '/deportistas/:athleteId',
      name: 'athlete-profile',
      component: AthleteProfileView,
      props: true,
      meta: { fullBleed: true },
    },
    { path: '/deportistas/:athleteId/videos', redirect: (to) => ({ name: 'athlete-profile', params: to.params }) },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !session.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.roles && !to.meta.roles.includes(session.user?.role)) return { name: 'home' }
  if (to.meta.guestOnly && session.token) return homeFor(session.user?.role)
})

/** Accepts only same-app paths, so ?redirect= cannot send users to another site. */
export function safeRedirect(value) {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//') ? value : null
}

export default router
