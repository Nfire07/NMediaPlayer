<template>
  <div class="media-view">
    
    <div v-if="currentSong" class="player-wrapper">
      <MusicPlayer 
        :currentSong="currentSong"
        :currentLang="currentLang"
        @returnToList="stopPlayback"
      />
    </div>
    
    <div v-else class="list-wrapper">
      <div class="header-section">
        <h2 class="page-title">{{ strings.availableMedia }} <i class="bi bi-music-note-list"></i></h2>
      </div>

      <div class="scroll-container">
        <SongLoader v-slot="{ songs }">
          <div v-if="!songs || songs.length === 0" class="no-songs">{{ strings.noMusic }}</div>
          <ul v-else class="songs-list">
            <li
              v-for="song in songs"
              :key="song.title"
              class="song-item"
              @click="askAndPlay(song)"
            >
              <div class="song-icon">
                <i class="bi bi-music-note-beamed"></i>
              </div>
              <div class="song-info">
                <div class="song-title">{{ song.title }}</div>
                <div class="song-artist">{{ song.artist || strings.unknownArtist }}</div>
              </div>
              <div class="play-indicator">
                <i class="bi bi-play-circle"></i>
              </div>
            </li>
          </ul>
        </SongLoader>
      </div>
    </div>

    <button v-if="!currentSong" class="back-button" @click="handleGoBack">
      <i class="bi bi-arrow-left"></i> {{ strings.returnToMenu }}
    </button>
  </div>
</template>

<script>
import SongLoader from './SongLoader.vue'
import MusicPlayer from './MusicPlayer.vue'
import { MediaPlugin } from '@/plugins/MediaPlugin'
import { ForegroundService, ServiceType } from '@capawesome-team/capacitor-android-foreground-service'

const STRINGS = {
  it: {
    availableMedia: 'Media Disponibili',
    noMusic: 'Nessuna musica trovata',
    unknownArtist: 'Artista Sconosciuto',
    returnToMenu: 'Torna al menu',
    currentlyPlaying: 'In riproduzione',
    errorPlaying: 'Errore durante la riproduzione: '
  },
  en: {
    availableMedia: 'Available Media',
    noMusic: 'No Music Found',
    unknownArtist: 'Unknown Artist',
    returnToMenu: 'Return to menu',
    currentlyPlaying: 'Currently playing',
    errorPlaying: 'Error playing media: '
  }
}

export default {
  name: "AvailableMedia",
  components: {
    SongLoader,
    MusicPlayer,
  },
  props: {
    currentLang: {
      type: String,
      default: 'it'
    }
  },
  data() {
    return {
      currentPlayingPath: null,
      currentSong: null,
    }
  },
  computed: {
    strings() {
      return this.currentLang === 'en' ? STRINGS.en : STRINGS.it;
    }
  },
  methods: {
    async playSong(song) {
      try {
        await ForegroundService.startForegroundService({
          id: 1,
          title: song.title || this.strings.currentlyPlaying,
          body: song.artist || this.strings.unknownArtist,
          smallIcon: 'ic_launcher',
          serviceType: ServiceType.MediaPlayback
        })
      } catch (e) {}

      try {
        await MediaPlugin.play({ 
          path: song.path,
          title: song.title,
          artist: song.artist || this.strings.unknownArtist
        })
        this.currentPlayingPath = song.path
        this.currentSong = song
      } catch (e) {
        alert(this.strings.errorPlaying + e.message)
      }
    },

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

    async askAndPlay(song) {
      if (this.currentPlayingPath === song.path) {
        await this.stopPlayback()
      } else {
        if (this.currentPlayingPath) {
          await this.stopPlayback()
        }
        await this.playSong(song)
      }
    },

    async handleGoBack() {
      await this.stopPlayback()
      this.$emit('goBack')
    }
  }
}
</script>

<style scoped>
.media-view {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.player-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.list-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  width: 100%;
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
}

.scroll-container {
  flex: 1;
  overflow-y: auto;
  width: 100%;
  padding: 0 1rem;
  box-sizing: border-box;
  padding-bottom: 80px; 
}

.songs-list {
  padding: 0;
  margin: 0;
  list-style: none;
  width: 100%;
}

.song-item {
  display: flex;
  align-items: center;
  padding: 1rem;
  margin-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.song-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
  transform: translateX(5px);
  box-shadow: 0 0 10px rgba(0, 203, 207, 0.1); 
}

.song-item:active {
  transform: scale(0.98);
}

.song-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  color: #00cbcf; 
  font-size: 1.2rem;
}

.song-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.song-title {
  font-weight: 600;
  font-size: 1rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: inherit;
}

.song-artist {
  font-size: 0.85rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  opacity: 0.7;
  color: inherit;
}

.play-indicator {
  opacity: 0;
  color: #00cbcf;
  font-size: 1.5rem;
  transition: opacity 0.2s;
  margin-left: 10px;
}

.song-item:hover .play-indicator {
  opacity: 1;
}

.no-songs {
  text-align: center;
  padding: 3rem;
  font-style: italic;
  opacity: 0.6;
}

.back-button {
  width: 100%;
  padding: 1.5rem 1rem;
  font-size: 1rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.3);
  color: inherit;
  position: fixed;
  bottom: 0;
  left: 0;
  z-index: 100;
  transition: background 0.3s;
}

.back-button:hover {
  background: rgba(0, 0, 0, 0.1);
}
</style>