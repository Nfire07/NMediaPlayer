<template>
  <div class="music-player">
    <div class="title">{{ currentSong?.title || "No song playing" }}</div>

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
      <button class="arrow-button" disabled>
        <i class="bi bi-skip-start-fill"></i>
      </button>

      <button class="pause-button" @click="togglePlayPause">
        <i class="bi bi-caret-right-fill" v-if="!isPlaying"></i>
        <i class="bi bi-pause" v-else></i>
      </button>

      <button class="arrow-button" disabled>
        <i class="bi bi-skip-end-fill"></i>
      </button>
    </div>

    <button class="return-button" @click="handleReturnToList">
      <i class="bi bi-arrow-return-left"></i>
    </button>
  </div>
</template>

<script>
import { MediaPlugin } from '@/plugins/MediaPlugin'


export default {
  name: "MusicPlayer",
  props: {
    currentSong: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      isPlaying: false,
      duration: 0,
      currentTime: 0,
      intervalId: null,
    }
  },
  computed:{},
  watch: {
    currentSong: {
      immediate: true,
      handler(newSong) {
        if (newSong) {
          this.playSong(newSong.path);
        } else {
          this.stop();
        }
      },
    },
  },
  methods: {
    async playSong(path) {
      try {
        await MediaPlugin.stop();
        await MediaPlugin.play({ path });

        this.isPlaying = true;
        this.currentTime = 0;

        const info = await MediaPlugin.getSongInfo({ path });
        this.duration = info.duration || 0;

        this.startProgress();
      } catch (e) {
        alert("An Error occured while playing" + e.message);
      }
    },
    startProgress() {
      if (this.intervalId) clearInterval(this.intervalId);
      this.intervalId = setInterval(async () => {
        try {
          const status = await MediaPlugin.getPlaybackStatus();
          this.currentTime = status.currentTime || 0;
          this.isPlaying = status.isPlaying;
          if (this.currentTime >= this.duration) {
            this.currentTime = this.duration;
            await this.stop();
          }
        } catch {}
      }, 500);
    },
    async stop() {
      if (this.intervalId) clearInterval(this.intervalId);
      this.isPlaying = false;
      this.currentTime = 0;
      this.duration = 0;
      await MediaPlugin.stop();
    },
   async togglePlayPause() {
      try {
        if (this.isPlaying) {
          await MediaPlugin.pause();
          this.isPlaying = false;
        } else {
          if (this.currentTime >= this.duration) {
            this.currentTime = 0;
            await MediaPlugin.seek({ position: 0 });
            await MediaPlugin.play({ path: this.currentSong.path });
          } else {
            try {
              await MediaPlugin.resume();
            } catch {
              await MediaPlugin.play({ path: this.currentSong.path });
              this.currentTime = 0;
            }
          }
          this.isPlaying = true;
        }
      } catch (e) {
        alert("Playback error: " + e.message);
      }
    },
    async seek() {
      const position = Math.floor(this.currentTime);

      if (
        typeof position !== "number" ||
        isNaN(position) ||
        position < 0 ||
        position > this.duration
      ) {
        console.warn("Invalid seek position:", position);
        return;
      }

      try {
        await MediaPlugin.seek({ position }); 
      } catch (e) {
        console.error("Seek failed:", e.message);
      }
    },
    formatTime(seconds) {
      const m = Math.floor(seconds / 60)
        .toString()
        .padStart(2, "0");
      const s = Math.floor(seconds % 60)
        .toString()
        .padStart(2, "0");
      return `${m}:${s}`;
    },
    handleReturnToList() {
      this.stop(); 
      this.$emit('returnToList'); 
    }
  },
  beforeUnmount() {
    if (this.intervalId) clearInterval(this.intervalId);
  },
}

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
  0%, 100% {
    background: linear-gradient(90deg, #e1e1e1, #bb00bb, #a382ba);
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
}

/* Progress Bar */
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

.time-info {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
  color: #fefefe;
}

/* Controlli Row */
.controls-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  width: 100%;
}

/* Pulsante Play/Pause Centrale (Grande) */
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
  font-size: 3rem; 
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  /* Allineamento ottico per il triangolo Play */
  margin-left: v-bind("!isPlaying ? '6px' : '0px'"); 
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
}

/* Pulsanti Avanti/Indietro */
.arrow-button {
  width: 65px;
  height: 65px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6f6f6f, #3a3a3a);
  color: #fefefe;
  border: none;
  cursor: pointer;
  opacity: 0.6;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.25);
  transition: all 0.2s ease;
}

.arrow-button i {
  font-size: 1.8rem;
  filter: drop-shadow(1px 1px 2px rgba(0,0,0,0.4));
}

.arrow-button:disabled {
  cursor: not-allowed;
  opacity: 0.3;
}

/* Pulsante Ritorno */
.return-button {
  width: 60%;
  height: 60px; 
  margin-top: 1rem;
  border: none;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  color: #fff;
}

.return-button:hover {
  background: rgba(255, 255, 255, 0.25);
}

.return-button:active {
  transform: scale(0.98);
  background: rgba(255, 255, 255, 0.1);
}

.return-button i {
  font-size: 1.4rem;
}
</style>