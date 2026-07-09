import { h } from 'vue'
import Theme from 'vitepress/theme'
import './style.css'

export default {
  extends: Theme,
  // Inject a fixed starfield layer behind all page content
  Layout() {
    return h(Theme.Layout, null, {
      'layout-top': () => h('div', { class: 'starfield-bg' }),
    })
  },
}
