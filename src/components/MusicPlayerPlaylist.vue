<template>
  <div class="music-player">
    <div class="title">{{ currentSongTitle }}</div>
    
    <div class="progress-container">
      <input
        type="range"
        min="0"
        :max="duration"
        v-model="currentTime"
        @input="onSeekInput"
        @change="onSeekChange"
        class="progress-bar"
      />
      <div class="time-info">
        <span>{{ formatTime(currentTime) }}</span>
        <span>{{ formatTime(duration) }}</span>
      </div>
    </div>

    <div class="controls-row">
      <button 
        class="arrow-button" 
        :disabled="!playerStore.hasPrev" 
        @click="handlePrevious"
      >
        <i class="bi bi-skip-start-fill"></i>
      </button>

      <button class="pause-button" @click="handlePlayPause">
        <i class="bi bi-caret-right-fill" v-if="!playerStore.isPlaying"></i>
        <i class="bi bi-pause" v-else></i>
      </button>

      <button 
        class="arrow-button" 
        :disabled="!playerStore.hasNext" 
        @click="handleNext"
      >
        <i class="bi bi-skip-end-fill"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useMusicPlayerStore } from '@/stores/musicPlayerStore.js'
import { MediaPlugin } from '@/plugins/MediaPlugin'
import { KeepAwake } from '@capacitor-community/keep-awake'

const props = defineProps({
  songs: { type: Array, default: () => [] },
  initialIndex: { type: Number, default: 0 },
  isShuffleModeOn: { type: Boolean, default: false }
})

const playerStore = useMusicPlayerStore()
const duration = ref(0)
const currentTime = ref(0)
const intervalId = ref(null)
const notificationListener = ref(null)

const isSeeking = ref(false)

const currentSongTitle = computed(() => playerStore.currentSong?.title || "No song playing")

watch(() => props.songs, async (newSongs) => {
  if (newSongs.length > 0) {
    playerStore.setPlaylist(newSongs, props.initialIndex, props.isShuffleModeOn)
    await playerStore.playSong(newSongs[props.initialIndex], props.initialIndex)
  }
}, { immediate: true })

watch(() => playerStore.currentSong, async (newSong) => {
  if (newSong) {
    if (!isSeeking.value) currentTime.value = 0
    
    try {
      const info = await MediaPlugin.getSongInfo({ path: newSong.path })
      duration.value = info.duration || 0
    } catch (e) {
      console.error('Error info:', e)
      duration.value = newSong.duration || 0; 
    }
    
    startProgress()
  }
})

function startProgress() {
  if (intervalId.value) clearInterval(intervalId.value)
  
  intervalId.value = setInterval(async () => {
    if (isSeeking.value) return;

    try {
      const status = await MediaPlugin.getPlaybackStatus()
      
      const nativeTime = status.currentTime || 0
      currentTime.value = nativeTime

      if (playerStore.isPlaying !== status.isPlaying) {
        playerStore.isPlaying = status.isPlaying
      }
    } catch (e) {
    }
  }, 1000) 
}

// Handler UI
async function handlePlayPause() {
  if (playerStore.isPlaying) {
    await playerStore.pause()
  } else {
    await playerStore.resume()
  }
}

async function handleNext() { await playerStore.next() }
async function handlePrevious() { await playerStore.previous() }

// --- Gestione SEEK (Barra di avanzamento) ---

function onSeekInput() {
  isSeeking.value = true
}

async function onSeekChange() {
  await MediaPlugin.seek({ position: Math.floor(currentTime.value) })
  
  setTimeout(() => {
    isSeeking.value = false
  }, 500)
}

function formatTime(seconds) {
  if (!seconds || isNaN(seconds)) return "00:00"
  const m = Math.floor(seconds / 60).toString().padStart(2, "0")
  const s = Math.floor(seconds % 60).toString().padStart(2, "0")
  return `${m}:${s}`
}

onMounted(async () => {
  try {
    await MediaPlugin.checkPermissions()
  } catch (e) {
    console.log("Permission check skipped or failed", e)
  }
  
  try {
    await KeepAwake.keepAwake()
  } catch(e) { console.error(e) }

  notificationListener.value = await MediaPlugin.addListener('notificationAction', (data) => {
    console.log('Action received from Native:', data.action)
    
    playerStore.handleNotificationAction(data.action)
  })
  
  startProgress()
})

onBeforeUnmount(async () => {
  if (intervalId.value) clearInterval(intervalId.value)
  if (notificationListener.value) notificationListener.value.remove()
  try {
    await KeepAwake.allowSleep()
  } catch(e) {}
})
</script>

<style scoped>
.music-player {
  width: 75%;
  height: 90vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3rem;
  user-select: none;
}

.title {
  font-weight: 700;
  font-size: 2rem;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  color: #fefefe;
  animation: fromLeftToRight 3s ease-in-out infinite;
}

@keyframes fromLeftToRight {
  0% {
    background: linear-gradient(90deg, #e1e1e1,  #bb00bb, #a382ba);
    background-size: 200% 200%;
    background-position: 0% 50%;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    text-shadow: #a382ba 1px 1px 6px;
  }
  50% {
    background: linear-gradient(90deg, #e1e1e1, #bb00bb, #a382ba);
    background-size: 200% 200%;
    background-position: 100% 50%;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    text-shadow: #a382ba 1px 1px 6px;
  }
  100% {
    background: linear-gradient(90deg, #e1e1e1, #bb00bb, #a382ba);
    background-size: 200% 200%;
    background-position: 0% 50%;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    text-shadow: #a382ba 1px 1px 6px;
  }
}

.progress-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.progress-bar {
  width: 100%;
  cursor: pointer;
  -webkit-appearance: none;
  height: 8px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.2);
  outline: none;
}

.progress-bar::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 14px;
  height: 14px;
  background: linear-gradient(135deg, #91039e57, #5a0286c1);
  cursor: pointer;
  border-radius: 50%;
  border: none;
  margin-top: -3px;
  transition: background-color 0.3s ease;
}

.progress-bar::-webkit-slider-thumb:hover {
  background:linear-gradient(135deg, #a404b365, #8106be8a);
}

.time-info {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
  color: #fefefe;
}

.pause-button {
  width: 100px; 
  height: 100px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #8a8a8a, #3a3a3a);
  color: #ffffff;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4);
  position: relative;
}

.pause-button:active {
  transform: scale(0.92);
  background: linear-gradient(135deg, #3a3a3a, #1a1a1a);
}

.pause-button i {
  font-size: 2.5rem; 
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: v-bind("!playerStore.isPlaying ? '4px' : '0px'"); 
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
}

.controls-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  width: 100%;
}

.arrow-button {
  width: 15vw;
  height: 15vw;
  max-width: 80px;
  max-height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: linear-gradient(135deg, #6f6f6f, #3a3a3a);
  color: #fefefe;
  border: none;
  font-size: 1rem;
  user-select: none;
  opacity: 0.6;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.25);
  transition: background 0.3s ease, transform 0.2s ease;
  cursor: pointer;
}

.arrow-button:active {
  background: linear-gradient(135deg, #2c2c2c, #1a1a1a);
  transform: scale(0.95);
}

.arrow-button i {
  font-size: 1.75rem; 
  filter: drop-shadow(1px 1px 2px rgba(0,0,0,0.4));
}

.arrow-button:disabled {
  cursor: not-allowed;
  opacity: 0.3;
}
</style>