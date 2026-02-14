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
        
        <div class="search-bar-container">
          <div class="search-input-wrapper">
            <i class="bi bi-search search-icon"></i>
            <input 
              type="text" 
              v-model="searchQuery" 
              :placeholder="strings.searchPlaceholder" 
              class="search-input"
            />
            <i 
              v-if="searchQuery" 
              class="bi bi-x-circle-fill clear-icon" 
              @click="searchQuery = ''"
            ></i>
          </div>
        </div>

      </div>

      <div class="scroll-container">
        <SongLoader :key="refreshTrigger" v-slot="{ songs }">
          
          <div v-if="!songs || songs.length === 0" class="no-songs">{{ strings.noMusic }}</div>
          <div v-else-if="getFilteredSongs(songs).length === 0" class="no-songs">{{ strings.noResults }}</div>
          
          <ul v-else class="songs-list">
            <li
              v-for="song in getFilteredSongs(songs)"
              :key="song.path" 
              class="song-item"
              @click="askAndPlay(song)"
            >
              <div class="song-icon">
                <i class="bi bi-music-note-beamed"></i>
              </div>
              
              <div class="song-info">
                <div class="song-title" v-html="highlightText(song.title)"></div>
                <div class="song-artist">{{ song.artist || strings.unknownArtist }}</div>
              </div>

              <div class="song-actions">
                <button class="action-btn rename-btn" @click.stop="handleRename(song)">
                  <i class="bi bi-pencil-square"></i>
                </button>
                <button class="action-btn delete-btn" @click.stop="handleDelete(song)">
                  <i class="bi bi-trash"></i>
                </button>
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
    noResults: 'Nessun risultato trovato',
    searchPlaceholder: 'Cerca brano o artista...',
    unknownArtist: 'Artista Sconosciuto',
    returnToMenu: 'Torna al menu',
    currentlyPlaying: 'In riproduzione',
    errorPlaying: 'Errore durante la riproduzione: ',
    confirmDelete: 'Sei sicuro di voler eliminare questo brano?',
    renamePrompt: 'Inserisci il nuovo nome del file:',
    renameError: 'Errore durante la rinomina',
    deleteError: 'Errore durante l\'eliminazione',
    fileDeleted: 'File eliminato',
    fileRenamed: 'File rinominato'
  },
  en: {
    availableMedia: 'Available Media',
    noMusic: 'No Music Found',
    noResults: 'No matching songs found',
    searchPlaceholder: 'Search song or artist...',
    unknownArtist: 'Unknown Artist',
    returnToMenu: 'Return to menu',
    currentlyPlaying: 'Currently playing',
    errorPlaying: 'Error playing media: ',
    confirmDelete: 'Are you sure you want to delete this song?',
    renamePrompt: 'Enter new filename:',
    renameError: 'Error renaming file',
    deleteError: 'Error deleting file',
    fileDeleted: 'File deleted',
    fileRenamed: 'File renamed'
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
      refreshTrigger: 0,
      searchQuery: '',
    }
  },
  computed: {
    strings() {
      return this.currentLang === 'en' ? STRINGS.en : STRINGS.it;
    }
  },
  methods: {
    getFilteredSongs(songs) {
      if (!songs) return [];
      if (!this.searchQuery) return songs;

      const query = this.searchQuery.toLowerCase();
      return songs.filter(song => {
        const titleMatch = (song.title || '').toLowerCase().includes(query);
        const artistMatch = (song.artist || '').toLowerCase().includes(query);
        return titleMatch || artistMatch;
      });
    },

    highlightText(text) {
      if (!this.searchQuery) return text;
      const regex = new RegExp(`(${this.searchQuery})`, 'gi');
      return text.replace(regex, '<span style="color: #00cbcf; font-weight:bold;">$1</span>');
    },

    async handleDelete(song) {
      const confirmed = confirm(this.strings.confirmDelete);
      if (!confirmed) return;

      if (this.currentPlayingPath === song.path) {
        await this.stopPlayback();
      }

      try {
        await MediaPlugin.deleteSong({ path: song.path });
        this.refreshTrigger++;
      } catch (e) {
        console.error(e);
        alert(this.strings.deleteError + ": " + e.message);
      }
    },

    async handleRename(song) {
      const newName = prompt(this.strings.renamePrompt, song.title);
      
      if (!newName || newName.trim() === "") return;
      if (newName === song.title) return;

      if (this.currentPlayingPath === song.path) {
        await this.stopPlayback();
      }

      try {
        await MediaPlugin.renameSong({ 
          path: song.path, 
          newName: newName.trim() 
        });
        
        this.refreshTrigger++;
      } catch (e) {
        console.error(e);
        alert(this.strings.renameError + ": " + e.message);
      }
    },

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
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.page-title {
  font-weight: 800;
  font-size: 1.8rem;
  margin: 0;
  text-shadow: 0 0 10px rgba(255, 255, 255, 0.2);
}

.search-bar-container {
  width: 100%;
  max-width: 400px;
  padding: 0 0.5rem;
}

.search-input-wrapper {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
}

.search-input {
  width: 100%;
  padding: 12px 40px 12px 40px;
  border-radius: 25px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  color: white;
  font-size: 1rem;
  outline: none;
  transition: all 0.3s ease;
}

.search-input:focus {
  background: rgba(255, 255, 255, 0.15);
  border-color: #00cbcf;
  box-shadow: 0 0 10px rgba(0, 203, 207, 0.2);
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.search-icon {
  position: absolute;
  left: 15px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 1.1rem;
  pointer-events: none;
}

.clear-icon {
  position: absolute;
  right: 15px;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  font-size: 1.1rem;
  transition: color 0.2s;
}

.clear-icon:hover {
  color: white;
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
  position: relative; 
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
  flex-shrink: 0;
}

.song-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-right: 10px;
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

.song-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 10px;
}

.action-btn {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 8px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #fff;
  font-size: 1rem;
}

.action-btn:hover {
  transform: scale(1.1);
}

.action-btn:active {
  transform: scale(0.95);
}

.rename-btn:hover {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.delete-btn:hover {
  background: rgba(220, 53, 69, 0.2);
  color: #dc3545;
}

.play-indicator {
  opacity: 0;
  color: #00cbcf;
  font-size: 1.5rem;
  transition: opacity 0.2s;
  flex-shrink: 0; 
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