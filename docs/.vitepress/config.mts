import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'Launch0',
  description: 'OS for the busy — a fast, text-only home screen',

  // Dark mode only — hides the appearance toggle in the nav bar
  appearance: 'force-dark',

  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }],
    ['meta', { name: 'theme-color', content: '#f97316' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:title', content: 'Launch0 - OS for the busy' }],
    ['meta', { property: 'og:description', content: 'An OS for the busy — a fast, text-only home screen. Launch apps, capture notes, hold distractions, track your time. No ads, no data collection.' }],
    ['meta', { property: 'og:url', content: 'https://launch0.app' }],
  ],

  themeConfig: {
    nav: [
      { text: 'Features', link: '/features/', activeMatch: '^/features/' },
      { text: 'FAQ', link: '/faq' },
      { text: 'Download', link: '/download' },
      { text: 'Privacy', link: '/privacy' },
      {
        text: 'Open source',
        items: [
          { text: 'Build from Source', link: '/build' },
          { text: 'Contributing', link: '/contributing' },
        ],
      },
    ],

    sidebar: {
      '/features/': [
        {
          text: 'Features',
          items: [
            { text: 'Overview', link: '/features/' },
            { text: 'Setup & first run', link: '/features/setup' },
          ],
        },
        {
          text: 'Home',
          items: [
            { text: 'Home screen', link: '/features/home-screen' },
            { text: 'Home widgets', link: '/features/widgets' },
            { text: 'Gestures', link: '/features/gestures' },
          ],
        },
        {
          text: 'Apps',
          items: [
            { text: 'Search & launch', link: '/features/search' },
            { text: 'App drawer', link: '/features/app-drawer' },
            { text: 'Work profile', link: '/features/work-profile' },
          ],
        },
        {
          text: 'Capture',
          items: [
            { text: 'Notes', link: '/features/notes' },
          ],
        },
        {
          text: 'Focus',
          items: [
            { text: 'Distraction timer', link: '/features/distraction-timer' },
            { text: 'Do Not Disturb', link: '/features/do-not-disturb' },
            { text: 'Screen time', link: '/features/screen-time' },
          ],
        },
        {
          text: 'Make it yours',
          items: [
            { text: 'Appearance & wallpapers', link: '/features/appearance' },
            { text: 'Settings reference', link: '/features/settings' },
          ],
        },
      ],
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/ketansp/Olauncher' },
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
