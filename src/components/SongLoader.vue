<script setup>
import { ref, onMounted, computed } from 'vue'
import { MediaPlugin } from '@/plugins/MediaPlugin'

const props = defineProps({
  currentLang: {
    type: String,
    default: 'it'
  }
})

const STRINGS = {
  it: {
    loading: 'Caricamento brani...',
    errorPrefix: 'Errore:',
    noData: 'Nessun brano trovato.'
  },
  en: {
    loading: 'Loading songs...',
    errorPrefix: 'Error:',
    noData: 'No songs found.'
  }
}

const songs = ref([])
const loading = ref(true)
const error = ref(null)

const ui = computed(() => props.currentLang === 'en' ? STRINGS.en : STRINGS.it)

async function loadSongs() {
  loading.value = true
  error.value = null
  try {
    const result = await MediaPlugin.getSongs()
    songs.value = result.songs || []
  } catch (e) {
    error.value = e.message || 'Unknown error'
  } finally {
    loading.value = false
  }
}

onMounted(loadSongs)
</script>

<template>
  <div class="song-loader">
    <div v-if="loading" class="status-msg loading">
        <i class="bi bi-hourglass-split spin"></i> {{ ui.loading }}
    </div>
    
    <div v-else-if="error" class="status-msg error">
        <i class="bi bi-exclamation-triangle-fill"></i> {{ ui.errorPrefix }} {{ error }}
    </div>
    
    <div v-else class="content-wrapper">
      <slot v-if="Array.isArray(songs) && songs.length > 0" :songs="songs"></slot>
      
      <div v-else class="status-msg empty">
        <i class="bi bi-music-note-list"></i> {{ ui.noData }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.song-loader {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
}

.content-wrapper {
    width: 100%;
}

.status-msg {
  font-size: 1.1rem;
  font-weight: 500;
  padding: 1.5rem 2rem;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: inherit;
  margin: 1rem;
}

.error {
  background: rgba(255, 59, 48, 0.15);
  border: 1px solid rgba(255, 59, 48, 0.3);
  color: #ff4d4d;
}

.empty {
    opacity: 0.8;
}

.spin {
    animation: spin 1.5s linear infinite;
}

@keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}
</style>