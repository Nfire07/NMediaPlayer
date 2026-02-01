<template>
  <div class="content">
    <template v-if="!showPlayer">
      <h2 class="page-title" v-if="!selectedPlaylist">Your Playlists  <i class="bi bi-view-list"></i></h2>

      <div v-if="!selectedPlaylist">
        <div v-if="loading" class="loading-text">Loading playlists...</div>
        <div v-else-if="playlists.length === 0" class="no-playlists">No playlists found.</div>
        <ul v-else class="playlist-list">
          <li v-for="playlist in playlists" :key="playlist.path" class="playlist-item">
            <span class="playlist-inline-title">{{ playlist.name }}</span>
            <button class="open-button" @click="openPlaylist(playlist)"><i class="bi bi-arrow-right"></i></button>
            <button class="remove-button" @click="removePlaylist(playlist.name)"><i class="bi bi-trash-fill"></i></button>
            <button class="shuffle-button" @click="playInShuffleMode(playlist)"><i class="bi bi-shuffle"></i></button>
          </li>
        </ul>
      </div>

      <div v-else>
        <h3 class="playlist-title">{{ selectedPlaylist.name }}</h3>

        <div v-if="songsLoading" class="loading-text">Loading songs...</div>
        <div v-else-if="playlistSongs.length === 0" class="no-songs">No songs in this playlist.</div>

        <ul v-else class="songs-list">
          <li
            v-for="(song, index) in playlistSongs"
            :key="song.queueId !== undefined ? song.queueId : (song.path || song.title)"
            class="song-item"
            @click="playSongAtIndex(index)"
            draggable="true"
            @dragstart="dragStart($event, song.queueId)"
            @dragover.prevent
            @drop="drop($event, song.queueId)"
          >
            <div class="queue-id">{{ song.queueId !== undefined ? song.queueId : '–' }}</div>
            <div class="song-info">
              <div class="song-title">{{ song.title }}</div>
              <div class="song-artist">{{ song.artist }}</div>
            </div>
          </li>
        </ul>
      </div>

      <button class="back-button" @click="goBack">
        <i class="bi bi-arrow-left"></i> {{ selectedPlaylist ? 'Back to playlists' : 'Return to menu' }}
      </button>
    </template>

    <div v-else class="player-wrapper">
      <MusicPlayerPlaylist
        :songs="playlistSongs"
        :initialIndex="currentPlayingIndex"
        :isShuffleModeOn="isShuffleModeOn"
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
    }
  },
  async mounted() {
    this.loading = true
    try {
      const response = await MediaPlugin.listPlaylists();
      this.playlists = response.playlists || [];
    } catch (err) {
      alert("Failed to load playlists: " + err.message);
    } finally {
      this.loading = false;
    }
  },
  methods: {
    async playSongAtIndex(index) {
      // Non è più strettamente necessario chiedere la location per l'audio, 
      // ma se serve per altre feature la lasciamo
      this.currentPlayingIndex = index
      this.isShuffleModeOn = false
      this.showPlayer = true
    },
    async closePlayer() {
      this.showPlayer = false
      await this.playerStore.stop() // Ferma tutto correttamente tramite store
    },
    async openPlaylist(playlist) {
      this.selectedPlaylist = playlist
      this.songsLoading = true
      try {
        const response = await MediaPlugin.getPlaylistSongs({ path: playlist.path })
        this.playlistSongs = response.songs || []
      } catch (err) {
        this.playlistSongs = []
        alert(err.message)
      } finally {
        this.songsLoading = false
      }
    },
    async playInShuffleMode(playlist) {
      await this.openPlaylist(playlist)
      if (this.playlistSongs.length > 0) {
        const randomIndex = Math.floor(Math.random() * this.playlistSongs.length)
        this.currentPlayingIndex = randomIndex
        this.isShuffleModeOn = true
        this.showPlayer = true
      } else {
        alert("Playlist empty.")
      }
    },
    async removePlaylist(name) {
      if (!confirm(`Delete "${name}"?`)) return
      try {
        await MediaPlugin.removePlaylist({ name })
        this.playlists = this.playlists.filter(p => p.name !== name)
        if (this.selectedPlaylist?.name === name) {
          this.selectedPlaylist = null
          this.playlistSongs = []
        }
      } catch (err) {
        alert(err.message)
      }
    },
    goBack() {
      if (this.selectedPlaylist) {
        this.selectedPlaylist = null
        this.playlistSongs = []
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
      } catch (err) {
        console.error(err)
      }
    }
  }
}
</script>

<style scoped>
.playlist-inline-title{
  max-width: 100px;
  text-wrap: nowrap;
  font-weight: 800;
  overflow: hidden;
}

.content {
  width: 100%;
  height: 90vh;
  display: flex;
  flex-direction: column;
}

.page-title {
  padding: 2.5rem 1rem 1rem;
  font-weight: 700;
  font-size: 1.5rem;
  text-align: center;
  margin-bottom: 1rem;
}

.playlist-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 50vh;
  overflow-y: scroll;
  width: 100%;
}

.playlist-item {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #2c2c2c;
  background: rgba(255, 255, 255, 0.15);
  font-weight: 600;
  display: flex;
  justify-content: space-evenly;
  align-items: center;
}

.open-button,
.remove-button,
.shuffle-button {
  margin-left: 0.5rem;
  padding: 0.25rem 0.5rem;
  border: none;
  cursor: pointer;
  font-weight: 600;
}

.open-button {
  background: linear-gradient(135deg, #4e9affa1, #23f9d597);
  color: #2c2c2c;
}

.remove-button {
  background: linear-gradient(135deg, #ff4e51a1, #f9d52397);
  color: #2c2c2c;
}

.shuffle-button{
  background:linear-gradient(30deg,#c131c4,#9e04a0);
  color:#2c2c2c;
}

.songs-list {
  list-style: none;
  padding: 0 1.5rem;
  margin: 0;
  max-height: 80vh;
  overflow-y: scroll;
  width: 100%;
  box-sizing: border-box;
}

.song-item {
  padding: 1rem 1.5rem;
  font-size: 0.95rem;
  background-color: rgba(255, 255, 255, 0.1);
  border-bottom: 1px solid #aaa;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.song-item:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.queue-id {
  color: #a110d6a1;
  text-shadow: #666 1px 1px 2px;
  font-size: 17px;
  width: 3rem;
  text-align: left;
  font-weight: 700;
}

.song-info {
  margin-left: 0.5rem;
  display: flex;
  flex-direction: column;
}

.song-title {
  font-weight: 600;
  font-size: 1rem;
}

.song-artist {
  font-style: italic;
  color: #999;
  font-size: 0.85rem;
  opacity: 0.75;
}

.loading-text,
.no-playlists,
.no-songs {
  text-align: center;
  padding: 2rem;
  font-style: italic;
  color: #666;
  opacity: 0.6;
}

.playlist-title {
  font-weight: 800;
  font-size: 1.5rem;
  text-align: center;
  margin-top: 2rem;
  margin-bottom: 1rem;
  max-width: 300px;
  overflow: hidden;
}

.back-button {
  padding: 2rem 1rem;
  font-size: 1rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: background 0.3s ease;
  width: 100%;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.3);
  color: inherit;
  position:fixed;
  bottom:0;
}

.back-button:hover {
  background: rgba(0, 0, 0, 0.1);
}

.player-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: calc(100vh - 80px); 
  width: 100%;
  padding: 1rem;
  box-sizing: border-box;
}
.button-icon{
  width:30px;
  height:30px;
}
</style>
