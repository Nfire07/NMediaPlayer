<template>
  <div class="layout">
    <div class="main">
      <Navigator 
        v-if="currentView === 'navigator'" 
        :current-lang="language"
        @navigate="setView" 
      />
      
      <AvailableMedia 
        v-if="currentView === 'media'" 
        :current-lang="language"
        @goBack="setView('navigator')" 
      />
      
      <Playlists 
        v-if="currentView === 'playlists'" 
        :current-lang="language"
        @goBack="setView('navigator')" 
      />
      
      <CreatePlaylist 
        v-if="currentView === 'create'" 
        :current-lang="language"
        @goBack="setView('navigator')" 
      /> 
      
      <Settings 
        v-if="currentView === 'settings'" 
        :is-dark-mode="isDarkMode"
        :current-lang="language"
        @toggle-theme="handleThemeChange"
        @toggle-lang="handleLangChange"
        @goBack="setView('navigator')" 
      /> 
      
      <Credits 
        v-if="currentView === 'credits'" 
        :current-lang="language"
        @goBack="setView('navigator')" 
      /> 
    </div>
  </div>
</template>

<script>
import Navigator from './components/Navigator.vue'
import AvailableMedia from './components/AvailableMedia.vue'
import Playlists from './components/Playlists.vue'
import CreatePlaylist from './components/CreatePlaylist.vue'
import Credits from './components/Credits.vue'
import Settings from './components/Settings.vue'
import { useMusicPlayerStore } from '@/stores/musicPlayerStore'
import { onMounted } from 'vue'
import { Preferences } from '@capacitor/preferences';

export default {
  name: 'App',
  components: {
    Navigator,
    AvailableMedia,
    Playlists,
    CreatePlaylist,
    Settings,
    Credits,
  },
  setup() {
    const musicStore = useMusicPlayerStore()
    
    onMounted(async () => {
      try {
        await musicStore.initListener() 
      } catch (e) {}
    })

    return { musicStore }
  },

  data() {
    return {
      currentView: 'navigator',
      isDarkMode: false,
      language: 'it'
    }
  },

  mounted() {
    this.initSettings();
  },

  methods: {
    setView(view) {
      this.currentView = view
    },

    async initSettings() {
      const { value: savedTheme } = await Preferences.get({ key: 'theme' });
      
      if (savedTheme) {
        this.isDarkMode = savedTheme === 'dark';
      } else {
        this.isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
      }
      this.applyThemeClass();

      const { value: savedLang } = await Preferences.get({ key: 'lang' });
      this.language = savedLang || 'it';
    },

    async handleThemeChange(isDark) {
      this.isDarkMode = isDark;
      this.applyThemeClass();
      
      await Preferences.set({
        key: 'theme',
        value: isDark ? 'dark' : 'light',
      });
    },

    applyThemeClass() {
      document.body.classList.toggle('dark', this.isDarkMode);
    },

    async handleLangChange(newLang) {
      if (typeof newLang === 'boolean') {
          this.language = newLang ? 'en' : 'it';
      } else {
          this.language = newLang;
      }
      
      await Preferences.set({
        key: 'lang',
        value: this.language,
      });
    }
  }
}
</script>

<style>
    :root {
        --card-bg-color: rgba(255, 255, 255, 0.85);
        --card-text-color: #2c3e50;
        --title-color: #fff;
    }

    body.dark {
        background: linear-gradient(30deg, #121211 0%, #363636 100%);
        color: #fff;
        
        --card-bg-color: #1a1a1a;
        --card-text-color: #fff;
        --title-color: #fff;
    }

    html, body {
        margin: 0;
        padding: 0;
        width: 100vw;
        height: 100vh;
        overflow-x: hidden;
        background: linear-gradient(30deg, #EEAECA 0%, #94BBE9 100%);
        font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
        flex-direction: column;
    }   
    
    .layout {
        display: flex;
        flex-direction: column;
        min-height: 100vh;
    }
    .main {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: flex-start;
    }

    .footer {
        margin-top: auto;
        width: 100%;
        padding: 1.5rem;
        text-align: center;
        background: rgba(255, 255, 255, 0.2);
        backdrop-filter: blur(10px);
        -webkit-backdrop-filter: blur(10px);
        border-top: 1px solid rgba(255, 255, 255, 0.3);
        color: #000;
        font-weight: 600;
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 0.5rem;
    }

    body.dark .footer {
        background: rgba(0, 0, 0, 0.3);
        border-top: 1px solid rgba(255, 255, 255, 0.2);
        color: #fff;
    }

</style>