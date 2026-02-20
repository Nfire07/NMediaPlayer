<script>
import { MediaPlugin } from '@/plugins/MediaPlugin'
const STRINGS = {
  it: {
    title: 'Impostazioni',
    language: 'Lingua',
    currentLang: 'Italiano',
    theme: 'Tema',
    currentTheme: 'Modo Scuro',
    back: 'Torna al menu',
    stopPlayback: 'Ferma Servizio',
  },
  en: {
    title: 'Settings',
    language: 'Language',
    currentLang: 'English',
    theme: 'Theme',
    currentTheme: 'Dark Mode',
    back: 'Return to menu',
    stopPlayback: 'Stop Service'
  }
}

export default {
  name: 'Settings',
  
  props: {
    isDarkMode: {
      type: Boolean,
      default: false
    },
    currentLang: {
      type: String,
      default: 'it'
    }
  },
  
  data() {
    return {
      localDarkMode: this.isDarkMode,
      localEnglish: this.currentLang === 'en'
    };
  },

  watch: {
    localDarkMode(newValue) {
      this.$emit('toggle-theme', newValue);
    },

    localEnglish(newValue) {
      this.$emit('toggle-lang', newValue ? 'en' : 'it');
    },

    isDarkMode(val) {
      this.localDarkMode = val;
    },
    currentLang(val) {
      this.localEnglish = val === 'en';
    }
  },
  method:{
    async stopPlayback() {
      try {
        if (MediaPlugin.stop) await MediaPlugin.stop()
      } catch (e) {}

      try {
        await ForegroundService.stopForegroundService()
      } catch (e) {}

      this.currentPlayingPath = null
      this.currentSong = null
    },
    async stopService(){
      await this.stopPlayback();
    }
  },
  computed: {
    strings() {
        return this.localEnglish ? STRINGS.en : STRINGS.it;
    }
  }
};
</script>

<template>
    <div class="settings-view">
        <div class="header-section">
            <h2 class="page-title">{{ strings.title }} <i class="bi bi-gear"></i></h2>
        </div>

        <div class="settings-content">
            
            <div class="setting-item">
                <div class="setting-info">
                    <span class="setting-label">{{ strings.language }}</span>
                    <span class="setting-value">{{ strings.currentLang }}</span>
                </div>
                <label class="glow-switch">
                    <input type="checkbox" v-model="localEnglish">
                    <span class="slider"></span>
                </label>
            </div>

            <div class="setting-item">
                <div class="setting-info">
                    <span class="setting-label">{{ strings.theme }}</span>
                    <span class="setting-value">{{ strings.currentTheme }}</span>
                </div>
                <label class="glow-switch theme-switch">
                    <input type="checkbox" v-model="localDarkMode">
                    <span class="slider">
                        <i class="bi bi-sun-fill icon-sun"></i>
                        <i class="bi bi-moon-stars-fill icon-moon"></i>
                    </span>
                </label>
            </div>
            <div class="setting-item" @click="stopService" style="cursor:pointer;">
              <div class="setting-info">
                <span class="setting-label">{{ strings.stopPlayback }}</span>
              </div>
              <i class="bi bi-stop-circle" style="font-size: 1.5rem;"></i>
            </div>

        </div>
    </div>

    <button class="back-button" @click="$emit('goBack')">
      <i class="bi bi-arrow-left"></i> {{ strings.back }}
    </button>
</template>

<style scoped>
.settings-view {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: inherit;
}

.header-section {
  padding: 2rem 1rem 1rem;
  text-align: center;
  flex-shrink: 0;
}

.page-title {
  font-weight: 800;
  font-size: 1.8rem;
  margin: 0;
  text-shadow: 0 0 10px rgba(255, 255, 255, 0.2);
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.settings-content {
    flex: 1;
    padding: 1rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: rgba(255, 255, 255, 0.15);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    padding: 1.2rem;
    border-radius: 16px;
    box-shadow: 0 4px 6px rgba(0,0,0,0.05);
    transition: transform 0.2s ease, background 0.2s ease;
}

.setting-item:hover {
    transform: translateY(-2px);
    background: rgba(255, 255, 255, 0.25);
}

.setting-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.setting-label {
    font-size: 1.1rem;
    font-weight: 700;
}

.setting-value {
    font-size: 0.9rem;
    opacity: 0.8;
}

.glow-switch {
  position: relative;
  display: inline-block;
  width: 60px;
  height: 32px;
  flex-shrink: 0;
}

.glow-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.3);
  transition: .4s;
  border-radius: 34px;
  border: 1px solid rgba(255,255,255,0.1);
  overflow: hidden;
}

.slider:before {
  position: absolute;
  content: "";
  height: 24px;
  width: 24px;
  left: 4px;
  bottom: 3px;
  background-color: white;
  transition: .4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  border-radius: 50%;
  box-shadow: 0 2px 5px rgba(0,0,0,0.3);
  z-index: 2;
}

input:checked + .slider {
  background-color: #00d2ff;
  box-shadow: 0 0 15px #00d2ff;
  border-color: transparent;
}

input:checked + .slider:before {
  transform: translateX(28px);
}

.theme-switch input:checked + .slider {
  background-color: #7b42f6; 
  box-shadow: 0 0 15px #7b42f6;
}

.theme-switch input:not(:checked) + .slider {
    background-color: #ffaa00;
    box-shadow: 0 0 15px #ffaa00;
}

.icon-sun, .icon-moon {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    font-size: 14px;
    transition: opacity 0.3s ease;
    z-index: 1;
}

.icon-sun {
    left: 8px;
    color: #fff;
    opacity: 0; 
}

.icon-moon {
    right: 8px;
    color: #fff;
    opacity: 1;
}

.theme-switch input:not(:checked) + .slider .icon-sun {
    opacity: 1;
}
.theme-switch input:not(:checked) + .slider .icon-moon {
    opacity: 0;
}

.theme-switch input:checked + .slider .icon-sun {
    opacity: 0;
}
.theme-switch input:checked + .slider .icon-moon {
    opacity: 1;
}

.back-button {
  width: 100%;
  padding: 1.5rem 1rem;
  font-size: 1rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: background 0.3s ease;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.3);
  color: inherit;
  position: fixed;
  bottom: 0;
  left: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  z-index: 100;
}

.back-button:hover {
  background: rgba(0, 0, 0, 0.1);
}
</style>