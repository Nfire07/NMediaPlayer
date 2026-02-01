<template>
  <div class="music-player">
    <div class="title">{{ currentSongTitle }}</div>
    <div class="progress-container">
      <input
        type="range"
        min="0"
        :max="duration"
        v-model="currentTime"
        @input="seek"
        class="progress-bar"
      />
      <div class="time-info">
        <span>{{ formatTime(currentTime) }}</span>
        <span>{{ formatTime(duration) }}</span>
      </div>
    </div>

    <div class="controls-row">
      <button class="arrow-button" :disabled="!playerStore.hasPrev" @click="handlePrevious">
        <i class="bi bi-chevron-bar-left"></i>
      </button>

      <button class="pause-button" @click="handlePlayPause">
        <i class="bi bi-pause-fill" v-if="!isPlaying"></i>
        <i class="bi bi-caret-right-fill" v-else></i>
      </button>

      <button class="arrow-button" :disabled="!playerStore.hasNext" @click="handleNext">
        <i class="bi bi-chevron-bar-right"></i>
      </button>
    </div>
  </div>
</template>

<script>
import { useMusicPlayerStore } from '@/stores/musicPlayerStore.js'
import { MediaPlugin } from '@/plugins/MediaPlugin'
import { KeepAwake } from '@capacitor-community/keep-awake'
import pauseIcon from '@/assets/PAUSE.png'
import playIcon from '@/assets/PLAY.png'
import rightArrowIcon from '@/assets/RIGHT_ARROW.png'
import leftArrowIcon from '@/assets/LEFT_ARROW.png'

export default {
  props: {
    songs: Array,
    initialIndex: Number,
    isShuffleModeOn: Boolean
  },
  data() {
    return {
      playerStore: useMusicPlayerStore(),
      duration: 0,
      currentTime: 0,
      intervalId: null,
      pauseIcon,
      playIcon,
      rightArrowIcon,
      leftArrowIcon,
      notificationListener: null
    }
  },
  computed: {
    currentSongTitle() {
      return this.playerStore.currentSong?.title || "No song playing"
    }
  },
  watch: {
    'playerStore.currentSong': {
      immediate: true,
      async handler(newSong) {
        if (newSong) {
          try {
            const info = await MediaPlugin.getSongInfo({ path: newSong.path })
            this.duration = info.duration || 0
            this.currentTime = 0
            this.startProgress()
          } catch (e) {
            console.error(e)
          }
        }
      }
    }
  },
  async mounted() {
    await KeepAwake.keepAwake()
    
    if (this.songs.length > 0) {
      this.playerStore.setPlaylist(this.songs, this.initialIndex, this.isShuffleModeOn)
      await this.playerStore.playSong(this.songs[this.initialIndex], this.initialIndex)
    }

    this.notificationListener = await MediaPlugin.addListener('notificationAction', (data) => {
      this.playerStore.handleNotificationAction(data.action)
    })
  },
  async beforeUnmount() {
    if (this.intervalId) clearInterval(this.intervalId)
    await KeepAwake.allowSleep()
    if (this.notificationListener) this.notificationListener.remove()
  },
  methods: {
    startProgress() {
      if (this.intervalId) clearInterval(this.intervalId)
      this.intervalId = setInterval(async () => {
        const status = await MediaPlugin.getPlaybackStatus()
        this.currentTime = status.currentTime || 0
        if (this.duration > 0 && this.currentTime >= this.duration - 0.8) {
          clearInterval(this.intervalId)
          await this.playerStore.next()
        }
      }, 1000)
    },
    async handlePlayPause() {
      if (this.playerStore.isPlaying) {
        await this.playerStore.pause()
      } else {
        await this.playerStore.resume()
      }
    },
    async seek() {
      await MediaPlugin.seek({ position: Math.floor(this.currentTime) })
    },
    async handleNext() {
      await this.playerStore.next()
    },
    async handlePrevious() {
      await this.playerStore.previous()
    },
    formatTime(seconds) {
      const m = Math.floor(seconds / 60).toString().padStart(2, "0")
      const s = Math.floor(seconds % 60).toString().padStart(2, "0")
      return `${m}:${s}`
    }
  }
}
</script>

<style scoped>
.music-player { width: 75%; height: 90vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3rem; user-select: none; }
.title { font-weight: 700; font-size: 2rem; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; color: #fefefe; animation: fromLeftToRight 3s ease-in-out infinite; }
@keyframes fromLeftToRight { 0%, 100% { background: linear-gradient(90deg, #e1e1e1, #bb00bb, #a382ba); background-size: 200% 200%; background-position: 0% 50%; -webkit-background-clip: text; -webkit-text-fill-color: transparent; text-shadow: #a382ba 1px 1px 6px; } 50% { background: linear-gradient(90deg, #e1e1e1, #bb00bb, #a382ba); background-size: 200% 200%; background-position: 100% 50%; -webkit-background-clip: text; -webkit-text-fill-color: transparent; text-shadow: #a382ba 1px 1px 6px; } }
.progress-container { width: 100%; display: flex; flex-direction: column; gap: 0.25rem; }
.progress-bar { width: 100%; cursor: pointer; -webkit-appearance: none; height: 8px; border-radius: 8px; background: rgba(0, 0, 0, 0.2); outline: none; }
.progress-bar::-webkit-slider-thumb { -webkit-appearance: none; appearance: none; width: 14px; height: 14px; background: linear-gradient(135deg, #91039e57, #5a0286c1); cursor: pointer; border-radius: 50%; border: none; margin-top: -3px; }
.time-info { display: flex; justify-content: space-between; font-size: 0.9rem; color: #fefefe; }
.pause-button { width: 30vw; height: 30vw; max-width: 200px; max-height: 200px; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #6f6f6f, #3a3a3a); border: none; cursor: pointer; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3); }
.pause-button-icon { width: 30%; height: 30%; }
.controls-row { display: flex; align-items: center; justify-content: center; gap: 2rem; width: 100%; }
.arrow-button { width: 15vw; height: 15vw; max-width: 80px; max-height: 80px; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #6f6f6f, #3a3a3a); border: none; opacity: 0.6; cursor: pointer; }
.arrow-icon { width: 50%; height: 50%; object-fit: contain; }
.arrow-button:disabled { cursor: not-allowed; opacity: 0.3; }
</style>