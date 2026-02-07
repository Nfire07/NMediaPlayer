<template>
  <div class="content">
    <template v-if="!showPlayer">
      
      <div v-if="!selectedPlaylist" class="view-container">
        <h2 class="page-title">
          Your Playlists <i class="bi bi-view-list"></i>
        </h2>

        <div class="playlist-container">
          <div v-if="loading" class="loading-text">Loading playlists...</div>
          <div v-else-if="playlists.length === 0" class="no-playlists">No playlists found.</div>
          <ul v-else class="playlist-list">
            <li v-for="playlist in playlists" :key="playlist.path" class="playlist-item">
              <span class="playlist-inline-title">{{ playlist.name }}</span>
              <div class="playlist-actions">
                <button class="action-btn open-button" @click="openPlaylist(playlist)">
                  <i class="bi bi-arrow-right"></i>
                </button>
                
                <button class="action-btn shuffle-button" @click="playInShuffleMode(playlist)">
                  <i class="bi bi-shuffle"></i>
                </button>

                <button class="action-btn edit-button" @click="openEditMode(playlist)">
                  <i class="bi bi-pencil-square"></i>
                </button>

                <button class="action-btn remove-button" @click="removePlaylist(playlist.name)">
                  <i class="bi bi-trash-fill"></i>
                </button>
              </div>
            </li>
          </ul>
        </div>
      </div>

      <div v-else class="view-container">
        
        <div class="header-section">
          <h3 class="playlist-title">{{ selectedPlaylist.name }}</h3>
          <div class="edit-controls" v-if="isEditMode">
             <button class="mode-btn add" :class="{ active: editAction === 'add' }" @click="setEditAction('add')">
               <i class="bi bi-plus-lg"></i> Add
             </button>
             <button class="mode-btn remove" :class="{ active: editAction === 'remove' }" @click="setEditAction('remove')">
               <i class="bi bi-dash-lg"></i> Remove
             </button>
          </div>
        </div>

        <div v-if="isEditMode && editAction === 'add'" class="add-songs-container">
            <input type="text" v-model="searchQuery" placeholder="Search available tracks..." class="search-input"/>
            <div v-if="loadingAvailable" class="loading-text">Loading tracks...</div>
            <ul v-else class="songs-list select-list">
                <li 
                  v-for="song in filteredAvailableSongs" 
                  :key="song.path" 
                  class="song-item selectable"
                  :class="{ selected: songsToAdd.some(s => s.path === song.path) }"
                  @click="toggleSongToAdd(song)"
                >
                    <div class="song-info">
                        <div class="song-title">{{ song.title }}</div>
                        <div class="song-artist">{{ song.artist || 'Unknown' }}</div>
                    </div>
                    <div class="checkbox-indicator">
                        <i class="bi bi-check-lg" v-if="songsToAdd.some(s => s.path === song.path)"></i>
                    </div>
                </li>
            </ul>
            <div class="confirm-actions">
                 <button class="confirm-btn" @click="confirmAddSongs" :disabled="songsToAdd.length === 0">
                    Confirm Add ({{ songsToAdd.length }})
                 </button>
            </div>
        </div>

        <div v-else class="songs-container">
          <div v-if="songsLoading" class="loading-text">Loading songs...</div>
          <div v-else-if="playlistSongs.length === 0" class="no-songs">No songs in this playlist.</div>

          <ul v-else class="songs-list">
            <li
              v-for="(song, index) in playlistSongs"
              :key="song.queueId || index"
              class="song-item"
              :class="{ 'remove-mode': isEditMode && editAction === 'remove' }"
              @click="handleSongClick(song, index)"
              draggable="true"
              @dragstart="!isEditMode && dragStart($event, song.queueId)"
              @dragover.prevent
              @drop="!isEditMode && drop($event, song.queueId)"
            >
              <div class="queue-id" v-if="!isEditMode || editAction !== 'remove'">{{ index + 1 }}</div>
              <div class="remove-icon" v-if="isEditMode && editAction === 'remove'">
                  <i class="bi bi-dash-circle-fill"></i>
              </div>

              <div class="song-info">
                <div class="song-title">{{ song.title }}</div>
                <div class="song-artist">{{ song.artist }}</div>
              </div>
            </li>
          </ul>
        </div>
      </div>

      <button class="back-button" @click="goBack">
        <i class="bi bi-arrow-left"></i> {{ selectedPlaylist ? 'Back' : 'Return to menu' }}
      </button>
    </template>

    <div v-else class="player-wrapper">
      <MusicPlayerPlaylist
        :songs="playlistSongs"
        :initialIndex="currentPlayingIndex"
        :isShuffleModeOn="isShuffleModeOn"
        @close="closePlayer"
      />
    </div>

    <button v-if="showPlayer" class="back-button" @click="closePlayer">
      <i class="bi bi-arrow-left"></i> Return to songs list
    </button>
  </div>
</template>

<script>
import { MediaPlugin } from '@/plugins/MediaPlugin'
import { useMusicPlayerStore } from '@/stores/musicPlayerStore.js'
import MusicPlayerPlaylist from './MusicPlayerPlaylist.vue'

export default {
  name: "Playlists",
  components: { MusicPlayerPlaylist },
  data() {
    return {
      playerStore: useMusicPlayerStore(),
      playlists: [],
      loading: false,
      selectedPlaylist: null,
      playlistSongs: [],
      songsLoading: false,
      dragQueueId: null,
      showPlayer: false,
      currentPlayingIndex: 0,
      isShuffleModeOn: false,
      
      isEditMode: false,
      editAction: null, 
      availableSongs: [],
      loadingAvailable: false,
      searchQuery: '',
      songsToAdd: []
    }
  },
  computed: {
    filteredAvailableSongs() {
      if (!this.searchQuery) return this.availableSongs;
      const q = this.searchQuery.toLowerCase();
      return this.availableSongs.filter(s => 
        (s.title && s.title.toLowerCase().includes(q)) || 
        (s.artist && s.artist.toLowerCase().includes(q))
      );
    }
  },
  async mounted() {
    this.loadPlaylists();
  },
  methods: {
    async loadPlaylists() {
      this.loading = true
      try {
        const response = await MediaPlugin.listPlaylists();
        this.playlists = response.playlists || [];
      } catch (err) {
        alert("Failed to load playlists.");
      } finally {
        this.loading = false;
      }
    },

    async openPlaylist(playlist) {
      this.selectedPlaylist = playlist
      this.isEditMode = false
      this.editAction = null
      await this.loadPlaylistSongs(playlist.path)
    },

    async loadPlaylistSongs(path) {
      this.songsLoading = true
      try {
        const response = await MediaPlugin.getPlaylistSongs({ path })
        this.playlistSongs = response.songs || []
      } catch (err) {
        this.playlistSongs = []
      } finally {
        this.songsLoading = false
      }
    },

    async openEditMode(playlist) {
      this.selectedPlaylist = playlist
      this.isEditMode = true
      this.editAction = 'remove' 
      await this.loadPlaylistSongs(playlist.path)
    },

    setEditAction(action) {
        this.editAction = action;
        if (action === 'add') {
            this.loadAvailableMusic();
            this.songsToAdd = [];
        }
    },

    async loadAvailableMusic() {
        this.loadingAvailable = true;
        try {
            const response = await MediaPlugin.getSongsListNotInPlaylist({
                playlistName: this.selectedPlaylist.name
            });
            this.availableSongs = response.songs || [];
        } catch (e) {
            console.error(e);
        } finally {
            this.loadingAvailable = false;
        }
    },

    toggleSongToAdd(song) {
        const idx = this.songsToAdd.findIndex(s => s.path === song.path);
        if (idx >= 0) {
            this.songsToAdd.splice(idx, 1);
        } else {
            this.songsToAdd.push(song);
        }
    },

    async confirmAddSongs() {
        if (this.songsToAdd.length === 0) return;
        
        try {
            for (const song of this.songsToAdd) {
                await MediaPlugin.addSongToPlaylist({
                    playlistName: this.selectedPlaylist.name,
                    song: song
                });
            }

            await this.loadPlaylistSongs(this.selectedPlaylist.path);
            this.setEditAction('remove'); 
            alert(`${this.songsToAdd.length} songs added.`);
        } catch (e) {
            alert("Error adding songs: " + e.message);
        }
    },

    async handleSongClick(song, index) {
      if (this.isEditMode && this.editAction === 'remove') {
          if(!confirm(`Remove "${song.title}" from playlist?`)) return;
          
          try {
             await MediaPlugin.removeSongFromPlaylist({
                 playlistName: this.selectedPlaylist.name,
                 songPath: song.path
             });
             
             this.playlistSongs.splice(index, 1);
             
             this.playlistSongs.forEach((s, i) => {
                 s.queueId = i + 1;
             });

          } catch(e) {
              alert("Error removing song: " + e.message);
          }
      } else if (!this.isEditMode) {
          this.playSongAtIndex(index);
      }
    },

    async playSongAtIndex(index) {
      if (!this.playlistSongs || this.playlistSongs.length === 0) return;
      await this.playerStore.setPlaylist(this.playlistSongs, index, false);
      const songToPlay = this.playlistSongs[index];
      await this.playerStore.playSong(songToPlay, index);
      this.currentPlayingIndex = index;
      this.isShuffleModeOn = false;
      this.showPlayer = true;
    },

    async closePlayer() {
      this.showPlayer = false
      await this.playerStore.stopPlayer() 
    },

    async playInShuffleMode(playlist) {
      this.selectedPlaylist = playlist;
      this.isEditMode = false;
      this.songsLoading = true;
      try {
        const response = await MediaPlugin.getPlaylistSongs({ path: playlist.path });
        this.playlistSongs = response.songs || [];
        if (this.playlistSongs.length > 0) {
          const randomIndex = Math.floor(Math.random() * this.playlistSongs.length);
          await this.playerStore.setPlaylist(this.playlistSongs, randomIndex, true);
          const songToPlay = this.playlistSongs[randomIndex];
          await this.playerStore.playSong(songToPlay, randomIndex);
          this.currentPlayingIndex = randomIndex;
          this.isShuffleModeOn = true;
          this.showPlayer = true;
        } else {
             alert("Playlist is empty!");
        }
      } catch (err) {
         this.playlistSongs = [];
      } finally {
        this.songsLoading = false;
      }
    },

    async removePlaylist(name) {
      if (!confirm(`Delete playlist "${name}"?`)) return
      try {
        await MediaPlugin.removePlaylist({ name })
        this.playlists = this.playlists.filter(p => p.name !== name)
        if (this.selectedPlaylist?.name === name) {
          this.selectedPlaylist = null
          this.playlistSongs = []
        }
      } catch (err) {}
    },

    goBack() {
      if (this.selectedPlaylist) {
        this.selectedPlaylist = null
        this.playlistSongs = []
        this.isEditMode = false;
        this.editAction = null;
      } else {
        this.$emit('goBack')
      }
    },

    dragStart(event, queueId) {
      this.dragQueueId = queueId
      event.dataTransfer.effectAllowed = 'move'
    },

    drop(event, targetQueueId) {
      event.preventDefault()
      if (this.dragQueueId === null || this.dragQueueId === targetQueueId) return
      const dragIndex = this.playlistSongs.findIndex(s => s.queueId === this.dragQueueId)
      const targetIndex = this.playlistSongs.findIndex(s => s.queueId === targetQueueId)
      if (dragIndex < 0 || targetIndex < 0) return
      const draggedSong = this.playlistSongs.splice(dragIndex, 1)[0]
      this.playlistSongs.splice(targetIndex, 0, draggedSong)
      this.playlistSongs.forEach((song, index) => { song.queueId = index + 1 })
      this.dragQueueId = null
      this.updateQueueOrder()
    },

    async updateQueueOrder() {
      try {
        await MediaPlugin.updatePlaylistQueue({
          path: this.selectedPlaylist.path,
          queue: this.playlistSongs.map(song => ({ queueId: song.queueId, path: song.path })),
        })
      } catch (err) {}
    }
  }
}
</script>
<style scoped>
.content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.view-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    width: 100%;
}

.page-title {
  padding: 2rem 1rem 1rem;
  font-weight: 800;
  font-size: 1.6rem;
  text-align: center;
  margin: 0;
  text-shadow: 0 0 10px rgba(255, 255, 255, 0.2);
}

.playlist-container, .songs-container {
    flex: 1;
    overflow-y: auto;
    width: 100%;
    padding-bottom: 80px;
}

.playlist-list {
  list-style: none;
  padding: 0;
  margin: 0;
  width: 100%;
}

.playlist-item {
  padding: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  transition: background 0.2s;
}

.playlist-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.playlist-inline-title {
  flex: 1;
  font-weight: 700;
  font-size: 1.1rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.playlist-actions {
  display: flex;
  gap: 0.8rem;
}

.action-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 1.1rem;
  cursor: pointer;
  color: #fff;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  background-clip: padding-box, border-box;
  background-origin: border-box;
}

.action-btn:active {
  transform: scale(0.9);
}

.action-btn:hover {
  transform: translateY(-2px);
}

.open-button { 
  background-image: linear-gradient(#1e1e1e, #1e1e1e), linear-gradient(135deg, #4e9aff, #23f9d5);
  box-shadow: 0 0 8px rgba(35, 249, 213, 0.25);
}
.open-button:hover {
  box-shadow: 0 0 15px rgba(35, 249, 213, 0.6);
  border-color: transparent;
}

.shuffle-button { 
  background-image: linear-gradient(#1e1e1e, #1e1e1e), linear-gradient(135deg, #c131c4, #9e04a0);
  box-shadow: 0 0 8px rgba(193, 49, 196, 0.25);
}
.shuffle-button:hover {
  box-shadow: 0 0 15px rgba(193, 49, 196, 0.6);
}

.edit-button { 
  background-image: linear-gradient(#1e1e1e, #1e1e1e), linear-gradient(135deg, #ff9a44, #fc6076);
  box-shadow: 0 0 8px rgba(255, 154, 68, 0.25);
}
.edit-button:hover {
  box-shadow: 0 0 15px rgba(255, 154, 68, 0.6);
}

.remove-button { 
  background-image: linear-gradient(#1e1e1e, #1e1e1e), linear-gradient(135deg, #ff4e51, #f9d523);
  box-shadow: 0 0 8px rgba(255, 78, 81, 0.25);
}
.remove-button:hover {
  box-shadow: 0 0 15px rgba(255, 78, 81, 0.6);
}

.header-section {
    padding: 1.5rem 1rem 0.5rem;
    text-align: center;
    background: rgba(0,0,0,0.2);
}

.playlist-title {
  font-weight: 800;
  font-size: 1.4rem;
  margin: 0 0 0.5rem 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.edit-controls {
    display: flex;
    justify-content: center;
    gap: 1rem;
    margin-top: 0.5rem;
}

.mode-btn {
    padding: 0.5rem 1.2rem;
    border-radius: 20px;
    border: 1px solid rgba(255,255,255,0.2);
    background: transparent;
    color: #aaa;
    cursor: pointer;
    font-size: 0.9rem;
    font-weight: 600;
    transition: all 0.3s;
}

.mode-btn.active.add {
    background: #23f9d5;
    color: #000;
    border-color: #23f9d5;
    box-shadow: 0 0 10px rgba(35, 249, 213, 0.4);
}

.mode-btn.active.remove {
    background: #ff4e51;
    color: #fff;
    border-color: #ff4e51;
    box-shadow: 0 0 10px rgba(255, 78, 81, 0.4);
}

.add-songs-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 0 1rem;
}

.search-input {
    width: 100%;
    padding: 10px;
    margin: 10px 0;
    background: rgba(255,255,255,0.1);
    border: 1px solid rgba(255,255,255,0.2);
    border-radius: 8px;
    color: white;
    font-size: 1rem;
}

.select-list {
    flex: 1;
    overflow-y: auto;
    padding-bottom: 80px;
}

.confirm-actions {
    position: absolute;
    bottom: 80px;
    left: 0;
    width: 100%;
    display: flex;
    justify-content: center;
    padding: 1rem;
    pointer-events: none;
}

.confirm-btn {
    pointer-events: auto;
    padding: 0.8rem 2rem;
    background: #23f9d5;
    color: #000;
    border: none;
    border-radius: 30px;
    font-weight: 800;
    box-shadow: 0 4px 15px rgba(0,0,0,0.5);
    cursor: pointer;
}

.confirm-btn:disabled {
    background: #555;
    color: #888;
}

.songs-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.song-item {
  padding: 0.8rem 1rem;
  background-color: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  cursor: pointer;
}

.song-item.selectable {
    border-radius: 8px;
    margin-bottom: 5px;
    border: 1px solid transparent;
}

.song-item.selected {
    background: rgba(35, 249, 213, 0.15);
    border-color: #23f9d5;
}

.song-item.remove-mode:hover {
    background: rgba(255, 78, 81, 0.15);
}

.queue-id {
  color: #c131c4;
  font-weight: 700;
  width: 2rem;
  text-align: center;
  margin-right: 0.5rem;
}

.remove-icon {
    color: #ff4e51;
    font-size: 1.2rem;
    margin-right: 1rem;
}

.checkbox-indicator {
    width: 24px;
    height: 24px;
    border: 2px solid rgba(255,255,255,0.3);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #23f9d5;
}

.song-item.selected .checkbox-indicator {
    border-color: #23f9d5;
    background: rgba(35, 249, 213, 0.2);
}

.song-info {
  flex: 1;
  overflow: hidden;
}

.song-title {
  font-weight: 600;
  font-size: 0.95rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-artist {
  font-size: 0.8rem;
  opacity: 0.7;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.loading-text, .no-playlists, .no-songs {
  text-align: center;
  padding: 2rem;
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
  z-index: 100;
}

.back-button:hover {
  background: rgba(0, 0, 0, 0.1);
}

.player-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
}
</style>