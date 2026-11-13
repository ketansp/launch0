import { defineConfig, type DefaultTheme } from 'vitepress'

const SITE = 'https://launch0.app'
const PLAY = 'https://play.google.com/store/apps/details?id=app.launch0'
const REPO = 'https://github.com/ketansp/launch0'

// The one-line definition used as the default meta description and in the
// structured data. Pages override the description in their frontmatter.
const DEFINITION =
  'An OS for the busy: a free, open-source minimalist launcher for Android with notes, to-dos, a distraction timer and screen time. No ads, no tracking.'

const APP_SCHEMA = {
  '@context': 'https://schema.org',
  '@graph': [
    { '@type': 'WebSite', '@id': `${SITE}/#website`, name: 'Launch0', url: `${SITE}/` },
    {
      '@type': 'MobileApplication',
      '@id': `${SITE}/#app`,
      name: 'Launch0',
      description:
        'Launch0 is an OS for the busy: a free, open-source minimalist launcher for Android with built-in notes, to-dos, a distraction timer, held notifications and screen time.',
      operatingSystem: 'Android 7.0 or later',
      applicationCategory: 'UtilitiesApplication',
      offers: { '@type': 'Offer', price: 0, priceCurrency: 'USD' },
      installUrl: PLAY,
      downloadUrl: PLAY,
      screenshot: `${SITE}/screenshots/home.webp`,
      image: `${SITE}/og.png`,
      license: 'https://www.gnu.org/licenses/gpl-3.0.html',
      isAccessibleForFree: true,
      isBasedOn: 'https://github.com/tanujnotes/Olauncher',
      sameAs: [PLAY, REPO],
    },
  ],
}

// Shared by the guide, the per-feature pages and the comparison page.
const SIDEBAR: DefaultTheme.SidebarItem[] = [
  {
    text: 'Getting Started',
    items: [
      { text: 'What is Launch0?', link: '/guide/' },
      { text: 'Features', link: '/guide/features' },
      { text: 'FAQs', link: '/guide/faq' },
    ],
  },
  {
    text: 'Features in Depth',
    items: [
      { text: 'Distraction Timer', link: '/features/distraction-timer' },
      { text: 'Notes & To-Dos', link: '/features/notes' },
      { text: 'Hold Notifications', link: '/features/hold-notifications' },
      { text: 'Calendar Widget', link: '/features/calendar-widget' },
    ],
  },
  {
    text: 'Compare',
    items: [{ text: 'Launch0 vs Olauncher', link: '/compare/olauncher' }],
  },
  {
    text: 'Contributing',
    items: [
      { text: 'Build from Source', link: '/guide/build' },
      { text: 'Contributing', link: '/guide/contributing' },
    ],
  },
]

export default defineConfig({
  title: 'Launch0',
  description: DEFINITION,

  // Dark mode only — hides the appearance toggle in the nav bar
  appearance: 'force-dark',

  // /download instead of /download.html. GitHub Pages serves foo.html at /foo.
  cleanUrls: true,
  lastUpdated: true,
  sitemap: { hostname: SITE },

  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }],
    ['meta', { name: 'theme-color', content: '#f97316' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:site_name', content: 'Launch0' }],
    ['meta', { property: 'og:image', content: `${SITE}/og.png` }],
    ['meta', { property: 'og:image:width', content: '1200' }],
    ['meta', { property: 'og:image:height', content: '630' }],
    ['meta', { property: 'og:image:alt', content: 'Launch0 — OS for the busy. Minimalist launcher for Android.' }],
    ['meta', { name: 'twitter:card', content: 'summary_large_image' }],
  ],

  // Per-page canonical URL, Open Graph tags and (homepage only) JSON-LD.
  transformPageData(pageData) {
    const path = pageData.relativePath
      .replace(/(^|\/)index\.md$/, '$1')
      .replace(/\.md$/, '')
    const url = `${SITE}/${path}`
    const isHome = pageData.relativePath === 'index.md'
    // Match the <title>: pages with `titleTemplate: false` don't get the suffix.
    const title = pageData.frontmatter.titleTemplate === false
      ? pageData.title
      : `${pageData.title} | Launch0`
    const description = pageData.frontmatter.description ?? DEFINITION

    pageData.frontmatter.head ??= []
    pageData.frontmatter.head.push(
      ['link', { rel: 'canonical', href: url }],
      ['meta', { property: 'og:url', content: url }],
      ['meta', { property: 'og:title', content: title }],
      ['meta', { property: 'og:description', content: description }],
    )
    if (isHome) {
      pageData.frontmatter.head.push([
        'script', { type: 'application/ld+json' }, JSON.stringify(APP_SCHEMA),
      ])
    }
  },

  themeConfig: {
    nav: [
      { text: 'Guide', link: '/guide/' },
      { text: 'Features', link: '/guide/features' },
      { text: 'Download', link: '/download' },
      { text: 'Privacy', link: '/privacy' },
    ],

    sidebar: {
      '/guide/': SIDEBAR,
      '/features/': SIDEBAR,
      '/compare/': SIDEBAR,
    },

    socialLinks: [
      { icon: 'github', link: REPO },
    ],

    footer: {
      message: 'A fork of <a href="https://github.com/tanujnotes/Olauncher">Olauncher</a>. Released under the GPLv3 License.',
      copyright: 'Copyright Launch0 contributors',
    },

    search: {
      provider: 'local',
    },
  },
})
