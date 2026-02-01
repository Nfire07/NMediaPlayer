<template>
  <div class="content">
    <h2>Create Playlist <i class="bi bi-pencil"></i></h2>
    <input
      class="name-input"
      type="text"
      v-model="playlistName"
      placeholder="Name your playlist..."
    />
    <p style="text-align: justify;">
      Select here your music before creating your playlist:
    </p>

    <SongLoader v-slot="{ songs }">
      <div v-if="songs.length === 0" class="no-songs">No Music Found</div>
      <ul
        v-else
        class="songs-list"
        style="overflow-y: auto; max-height: 50vh; width: 100%;"
      >
        <li
          v-for="song in songs"
          :key="song.path"
          class="song-item"
          :class="{ selected: isSelected(song) }"
          @click="selectItem(song)"
        >
          <div class="song-title">{{ song.title }}</div>
        </li>
      </ul>
    </SongLoader>

    <button
      class="confirm-button"
      @click="createPlaylist"
      :disabled="!playlistName || selectedSongs.length === 0"
    >
      Create Playlist
    </button>
  </div>

  <button class="back-button" @click="$emit('goBack')">Return to menu</button>
</template>

<script>
import SongLoader from './SongLoader.vue'
import { MediaPlugin } from '@/plugins/MediaPlugin'

export default {
  name: 'CreatePlaylist',
  components: { SongLoader },
  data() {
    return {
      selectedSongs: [],
      playlistName: '',
    }
  },
  methods: {
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
        console.error('Error fetching playlists:', error)
        return false
      }
    },

    async createPlaylist() {
      if (!this.playlistName) {
        alert('Please enter a playlist name before confirming.❌')
        return
      }
      if (await this.playlistExists(this.playlistName)) {
        alert(
          'A playlist with this name already exists. Please choose a different name.❌'
        )
        return
      }

      try {
        const response = await MediaPlugin.createPlaylist({
          name: this.playlistName,
          songs: this.selectedSongs.map((song) => ({
            title: song.title,
            path: song.path,
            artist: song.artist,
          })),
        })

        console.log('✅ Playlist saved into:', response.path)
        alert('Playlist created successfully! ✅')

        // Reset form
        this.playlistName = ''
        this.selectedSongs = []
      } catch (err) {
        console.error('❌Error during creation', err)
        alert('Error:❌ ' + err.message)
      }
    },
  },
}
</script>

<style scoped>

.content {
  padding: 2rem;
  width: 80%;
  height: 85vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: right;
}

.name-input {
  width: 100%;
  padding: 0.75rem 1rem;
  margin: 1rem 0;
  border: 1px solid #ccc;
  border-radius: 8px;
  font-size: 1rem;
  box-sizing: border-box;
  background: #2c2c2c10;
}

body.dark .name-input{
  color:#fefefe;
  background: #2c2c2c33;
}

.name-input:focus {
  border-color: #007BFF;
  outline: none;
  background-color:#2c2c2c4a;
}

.name-input:active {
  border-color: #007BFF;
  outline: none;
  background-color:#2c2c2c73;
}

.confirm-button {
  width: 100%;
  padding: 0.75rem 1rem;
  background-color: #002fbb;
  color:#fefefe;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.confirm-button:active{
  background-color: #001f7b;
}

.back-button {
  padding-bottom: 2rem;
  border: none;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.3s ease;
  padding-top:2rem;
  position:fixed;
  bottom: 0;
  width: 100%;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.3);
  color: #000;
  font-weight: 600;
  gap: 0.5rem;
}

body.dark .back-button {
  background: rgba(0, 0, 0, 0.3);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
}

.back-button:hover {
  background: #2c2c2c;
}

.no-songs {
  text-align: center;
  padding: 2rem;
}

.songs-list {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  margin: 0;
  list-style: none;
}

.song-item {
  padding: 1rem 1.5rem;
  padding-right: 2rem;
  border-bottom: 1px solid #2c2c2c;
  background: rgba(255, 255, 255, 0.15);
  transition: background 0.2s ease;
}

.song-title {
  font-weight: 600;
  font-size: 1rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.song-item.selected {
  background: rgba(255, 255, 255, 0.05);
}

.confirm-button:disabled{
  background-color: #555;
  cursor: not-allowed;
}

</style>

