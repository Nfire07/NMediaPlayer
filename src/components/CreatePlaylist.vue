<template>
  <div class="create-playlist-view">
    
    <div class="scrollable-content">
      <div class="header-section">
        <h2 class="page-title">{{ strings.createPlaylist }} <i class="bi bi-pencil"></i></h2>
        
        <div class="input-wrapper">
          <input
            class="name-input"
            type="text"
            v-model="playlistName"
            :placeholder="strings.namePlaceholder"
          />
          <div class="input-highlight"></div>
        </div>

        <div class="search-wrapper">
          <i class="bi bi-search search-icon"></i>
          <input 
            type="text" 
            class="search-input" 
            v-model="searchQuery" 
            :placeholder="strings.searchPlaceholder" 
          />
        </div>
        
        <p class="instruction-text">
          {{ strings.selectTracks }}
        </p>
      </div>

      <div class="list-container">
        <SongLoader v-slot="{ songs }">
          <div v-if="!songs || songs.length === 0" class="no-songs">{{ strings.noMusic }}</div>
          
          <ul v-else class="songs-list">
            <li
              v-for="song in filterSongs(songs)"
              :key="song.path"
              class="song-item"
              :class="{ selected: isSelected(song) }"
              @click="selectItem(song)"
            >
              <div class="song-info">
                <div class="song-title">{{ song.title }}</div>
                <div class="song-artist">{{ song.artist || strings.unknownArtist }}</div>
              </div>
              
              <div class="checkbox-custom" :class="{ checked: isSelected(song) }">
                <i class="bi bi-check-lg" v-if="isSelected(song)"></i>
              </div>
            </li>
            
            <div v-if="filterSongs(songs).length === 0" class="no-results">
              {{ strings.noMatches }}
            </div>
          </ul>
        </SongLoader>
      </div>

      <div class="footer-section">
        <button
          class="create-button"
          @click="createPlaylist"
          :disabled="!playlistName || selectedSongs.length === 0"
        >
          {{ strings.confirmCreation }} <i class="bi bi-plus-lg"></i>
        </button>
      </div>
    </div>

    <button class="back-button" @click="$emit('goBack')">
      <i class="bi bi-arrow-left"></i> {{ strings.returnToMenu }}
    </button>

  </div>
</template>

<script>
import SongLoader from './SongLoader.vue'
import { MediaPlugin } from '@/plugins/MediaPlugin'

const STRINGS = {
  it: {
    createPlaylist: 'Crea Playlist',
    namePlaceholder: 'Nome della playlist...',
    searchPlaceholder: 'Cerca brani...',
    selectTracks: 'Seleziona i brani da includere:',
    noMusic: 'Nessuna musica trovata',
    noMatches: 'Nessun risultato.',
    confirmCreation: 'Conferma Creazione',
    returnToMenu: 'Torna al menu',
    unknownArtist: 'Artista Sconosciuto',
    alertEnterName: 'Inserisci un nome per la playlist.',
    alertExists: 'Esiste già una playlist con questo nome.',
    alertSuccess: 'Playlist creata con successo!',
    alertError: 'Errore: '
  },
  en: {
    createPlaylist: 'Create Playlist',
    namePlaceholder: 'Name your playlist...',
    searchPlaceholder: 'Search tracks...',
    selectTracks: 'Select tracks to include:',
    noMusic: 'No Music Found',
    noMatches: 'No matches found.',
    confirmCreation: 'Confirm Creation',
    returnToMenu: 'Return to menu',
    unknownArtist: 'Unknown Artist',
    alertEnterName: 'Please enter a playlist name.',
    alertExists: 'A playlist with this name already exists.',
    alertSuccess: 'Playlist created successfully!',
    alertError: 'Error: '
  }
}

export default {
  name: 'CreatePlaylist',
  components: { SongLoader },
  props: {
    currentLang: {
      type: String,
      default: 'it'
    }
  },
  data() {
    return {
      selectedSongs: [],
      playlistName: '',
      searchQuery: '',
    }
  },
  computed: {
    strings() {
      return this.currentLang === 'en' ? STRINGS.en : STRINGS.it;
    }
  },
  methods: {
    filterSongs(songs) {
      if (!songs) return [];
      if (!this.searchQuery) return songs;
      
      const query = this.searchQuery.toLowerCase();
      return songs.filter(song => 
        (song.title && song.title.toLowerCase().includes(query)) ||
        (song.artist && song.artist.toLowerCase().includes(query))
      );
    },

    selectItem(song) {
      const index = this.selectedSongs.findIndex((s) => s.path === song.path)
      if (index !== -1) {
        this.selectedSongs.splice(index, 1)
      } else {
        this.selectedSongs.push(song)
      }
    },
    isSelected(song) {
      return this.selectedSongs.some((s) => s.path === song.path)
    },

    async playlistExists(name) {
      try {
        const response = await MediaPlugin.listPlaylists()
        const playlists = response.playlists || []
        return playlists.some(
          (pl) => pl.name.toLowerCase() === name.toLowerCase()
        )
      } catch (error) {
        return false
      }
    },

    async createPlaylist() {
      if (!this.playlistName.trim()) {
        alert(this.strings.alertEnterName)
        return
      }
      
      if (await this.playlistExists(this.playlistName)) {
        alert(this.strings.alertExists)
        return
      }

      try {
        await MediaPlugin.createPlaylist({
          name: this.playlistName,
          songs: this.selectedSongs.map((song) => ({
            title: song.title,
            path: song.path,
            artist: song.artist,
          })),
        })

        alert(this.strings.alertSuccess)
        this.playlistName = ''
        this.selectedSongs = []
        this.searchQuery = ''
        this.$emit('goBack') 
      } catch (err) {
        alert(this.strings.alertError + err.message)
      }
    },
  },
}
</script>

<style scoped>
.create-playlist-view {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.scrollable-content {
  flex: 1;
  overflow-y: auto;
  width: 100%;
  padding-bottom: 90px;
  display: flex;
  flex-direction: column;
}

.header-section {
  padding: 1.5rem 1.5rem 0.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.page-title {
  font-weight: 800;
  font-size: 1.6rem;
  text-align: center;
  margin-bottom: 1rem;
  text-shadow: 0 0 10px rgba(255, 255, 255, 0.2);
}

.input-wrapper {
  position: relative;
  width: 100%;
  max-width: 300px;
  margin-bottom: 1rem;
}

.name-input {
  width: 100%;
  padding: 8px 5px;
  background: transparent;
  border: none;
  border-bottom: 2px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  font-size: 1.1rem;
  text-align: center;
  transition: border-color 0.3s ease;
}

.name-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
  font-style: italic;
}

.name-input:focus {
  outline: none;
  border-bottom-color: #944ccf;
}

.search-wrapper {
  width: 100%;
  max-width: 300px;
  position: relative;
  margin-bottom: 0.5rem;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.5);
}

.search-input {
  width: 100%;
  padding: 8px 10px 8px 35px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 0.9rem;
  outline: none;
  transition: background 0.2s;
}

.search-input:focus {
  background: rgba(255, 255, 255, 0.2);
  border-color: #2f9ceb;
}

.instruction-text {
  font-size: 0.85rem;
  opacity: 0.7;
  margin-top: 0.5rem;
}

.list-container {
  flex: 0 1 auto;
  max-height: 50vh;
  width: 90%;
  margin: 0 auto;
  overflow-y: auto;
  padding: 5px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.songs-list {
  list-style: none;
  padding: 0;
  margin: 0;
  width: 100%;
}

.song-item {
  padding: 0.6rem 1rem;
  margin-bottom: 6px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid transparent;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.song-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.song-item.selected {
  background: rgba(47, 156, 235, 0.15);
  border: 1px solid #2f9ceb;
}

.song-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-right: 10px;
}

.song-title {
  font-weight: 600;
  font-size: 0.9rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-artist {
  font-size: 0.75rem;
  opacity: 0.6;
}

.checkbox-custom {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.checkbox-custom.checked {
  background-color: #2f9ceb;
  border-color: #2f9ceb;
  color: white;
  font-size: 0.8rem;
}

.no-songs, .no-results {
  text-align: center;
  padding: 2rem;
  opacity: 0.5;
  font-style: italic;
  font-size: 0.9rem;
}

.footer-section {
  display: flex;
  justify-content: center;
  padding: 1.5rem 1rem;
  flex-shrink: 0;
}

.create-button {
  width: 100%;
  max-width: 300px;
  padding: 0.9rem;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 800;
  color: white;
  cursor: pointer;
  background: linear-gradient(130deg, #2f9ceb 0%, #944ccf 100%);
  box-shadow: 0 4px 15px rgba(47, 156, 235, 0.4);
}

.create-button:disabled {
  background: #444;
  color: #888;
  cursor: not-allowed;
  box-shadow: none;
  opacity: 0.6;
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