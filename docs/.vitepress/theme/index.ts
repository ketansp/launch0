import { h } from 'vue'
import Theme from 'vitepress/theme'
import FeatureIntro from '../components/FeatureIntro.vue'
import FeatureCards from '../components/FeatureCards.vue'
import './style.css'

export default {
  extends: Theme,
  enhanceApp({ app }) {
    app.component('FeatureIntro', FeatureIntro)
    app.component('FeatureCards', FeatureCards)
  },
  // Inject a fixed starfield layer behind all page content
  Layout() {
    return h(Theme.Layout, null, {
      'layout-top': () => h('div', { class: 'starfield-bg' }),
      // Secondary install path under the hero's Play Store button
      'home-hero-actions-after': () =>
        h('p', { class: 'hero-apk-note' }, [
          'No Play Store? ',
          h('a', { href: '/download#apk' }, 'Get the APK'),
        ]),
    })
  },
}
