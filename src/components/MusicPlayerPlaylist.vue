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
      <button 
            class="arrow-button" 
            :disabled="!hasPrev" 
            @click="prevSong"
            >
        <img :src="leftArrowIcon" class="arrow-icon" />
        </button>

    <button class="pause-button" @click="togglePlayPause">
        <img :src="currentIcon" class="pause-button-icon" />
    </button>

    <button 
        class="arrow-button" 
        :disabled="!hasNext" 
        @click="nextSong"
        >
        <img :src="rightArrowIcon" class="arrow-icon" />
    </button>
  </div>
</div>
</template>

<script>
import { MediaPlugin } from '@/plugins/MediaPlugin'
import pauseIcon from '@/assets/PAUSE.png'
import playIcon from '@/assets/PLAY.png'
import rightArrowIcon from '@/assets/RIGHT_ARROW.png'
import leftArrowIcon from '@/assets/LEFT_ARROW.png'
import ReturnIcon from '@/assets/RETURN.png'

export default {
  name: "MusicPlayerPlaylist",
  props: {
    songs: {
      type: Array,
      default: () => [],
    },
    initialIndex: {
        type: Number,
        default: 0,
    }
  },
  data() {
    return {
      currentIndex: this.initialIndex,
      isPlaying: false,
      duration: 0,
      currentTime: 0,
      intervalId: null,
      pauseIcon,
      playIcon,
      rightArrowIcon,
      leftArrowIcon,
      ReturnIcon,
    }
  },
  computed:{
    currentSong() {
      return this.songs[this.currentIndex] || null;
    },
    currentIcon() {
      return this.isPlaying ? this.pauseIcon : this.playIcon;
    },
    hasPrev() {
      return this.currentIndex > 0;
    },
    hasNext() {
      return this.currentIndex < this.songs.length - 1;
    }
  },
  watch: {
    initialIndex(newVal) {
        this.currentIndex = newVal;
    },
    songs: {
        immediate: true,
        handler(newSongs) {
        if (newSongs.length > 0) {
            if(this.currentIndex >= newSongs.length) this.currentIndex = 0;
            this.playSong(this.songs[this.currentIndex].path);
        } else {
            this.stop();
        }
        },
    },
    currentIndex(newIndex) {
        if (this.songs.length > 0 && this.songs[newIndex]) {
        this.playSong(this.songs[newIndex].path);
        }
    }
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
        alert("An Error occured while playing " + e.message);
      }
    },
    startProgress() {
        if (this.intervalId) clearInterval(this.intervalId);

        this.intervalId = setInterval(async () => {
            try {
            const status = await MediaPlugin.getPlaybackStatus();
            this.currentTime = status.currentTime || 0;
            this.isPlaying = status.isPlaying;

            if (this.duration > 0 && this.currentTime >= this.duration - 0.5) {
                clearInterval(this.intervalId); 
                this.currentTime = this.duration;
                await this.nextSong();
            }
            } catch (e) {
            console.error("Playback status error:", e.message);
            }
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
        if (!this.currentSong) return;

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
    async nextSong() {
        if (this.hasNext) {
            this.currentIndex++;
        } else {
            this.currentIndex = 0;
        }
    },
    async prevSong() {
      if (this.hasPrev) {
        this.currentIndex--;
      }
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
  width: 30vw;
  height: 30vw;
  max-width: 200px;
  max-height: 200px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: linear-gradient(135deg, #6f6f6f, #3a3a3a);
  color: #fefefe;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.3s ease, transform 0.2s ease;
  user-select: none;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.pause-button:active {
  background: linear-gradient(135deg, #2c2c2c, #1a1a1a);
  transform: scale(0.96);
}


.stop-button{
  width: 100%;
  padding: 0.75rem 1rem;
  background-color: #bb0000;
  color: #fefefe;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s ease;
  user-select: none;
}

.pause-button-icon{
  width:30%;
  height:30%;
  filter: blur(0.05px);
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
}

.arrow-button:active {
  background: linear-gradient(135deg, #2c2c2c, #1a1a1a);
  transform: scale(0.95);
}

.arrow-icon {
  width: 50%;
  height: 50%;
  object-fit: contain;
  filter: drop-shadow(1px 1px 2px rgba(0,0,0,0.4));
}

.arrow-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.return-button {
  width: 60%;
  height: 60px; 
  padding: 0 1.5rem;
  border: none;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.3s ease, transform 0.2s ease;
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
  user-select: none;
}

.return-button:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-1px);
}

.return-button:active {
  background: rgba(255, 255, 255, 0.1);
  transform: scale(0.98);
}

.return-icon {
  width: 24px;
  height: 35px;
  object-fit: contain;
  filter: drop-shadow(0 0 2px rgba(0, 0, 0, 0.3));
}


</style>
