<template>
  <div
    :class="{'player-container': currentSong, 'list-container': !currentSong}"
    style="width: 100%; height: 100%;"
  >
    <MusicPlayer v-if="currentSong" :currentSong="currentSong" @returnToList="stopPlayback"/>
    
    <div v-else>
      <h2 class="page-title">🎶 Available Media</h2>
      <SongLoader v-slot="{ songs }">
        <div v-if="songs.length === 0" class="no-songs">No Music Found</div>
        <ul v-else class="songs-list">
          <li
            v-for="song in songs"
            :key="song.title"
            class="song-item"
            @click="askAndPlay(song)"
          >
            <div class="song-title">{{ song.title }}</div>
            <div class="song-artist">{{ song.artist }}</div>
          </li>
        </ul>
      </SongLoader>
    </div>

    <button class="back-button" @click="$emit('goBack');stopPlayback()">Return to menu</button>
  </div>
</template>


<script>
import SongLoader from './SongLoader.vue'
import { MediaPlugin } from '@/plugins/MediaPlugin'
import { ForegroundService, ServiceType } from '@capawesome-team/capacitor-android-foreground-service'
import { Geolocation } from '@capacitor/geolocation'
import MusicPlayer from './MusicPlayer.vue'

export default {
  name: "AvailableMedia",
  components: {
    SongLoader,
    MusicPlayer,
  },
  data() {
    return {
      permissionsGaranted: false,
      currentPlayingPath: null,
      currentSong: null,
    }
  },
  methods: {
    async requestLocation() {
      const permission = await Geolocation.requestPermissions()
      if (permission.location === 'granted') {
        return true
      } else {
        throw new Error('Location permission denied')
      }
    },
    async playSong(song) {
      await ForegroundService.startForegroundService({
        id: 1,
        title: 'Currently playing',
        body: song.title,
        smallIcon: 'ic_launcher',
        serviceType: ServiceType.MediaPlayback
      })
      await MediaPlugin.play({ path: song.path })
      this.currentPlayingPath = song.path
      this.currentSong = song
    },
    async stopPlayback() {
      await MediaPlugin.stop?.()
      await ForegroundService.stopForegroundService()
      this.currentPlayingPath = null
      this.currentSong = null
    },
    async askAndPlay(song) {
      try {
        await this.requestLocation()

        if (this.currentPlayingPath === song.path) {
          await this.stopPlayback()
        } else {
          if (this.currentPlayingPath) {
            await this.stopPlayback()
          }
          await this.playSong(song)
        }
      } catch (err) {
        console.error('Permission error:', err)
        alert('Location permission is required to play media in the foreground.')
      }
    }
  }
}
</script>

<style scoped>
.page-title {
  padding: 2.5rem 1rem 1rem;
  text-align: center;
  font-weight: 700;
  font-size: 1.5rem;
}

.player-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
  flex-direction: column;
}

.list-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
}

.songs-list {
  flex-grow: 1;
  overflow-y: auto;
  padding: 0;
  margin: 0;
  list-style: none;
  width: 100%;
  max-height: 70vh; 
  box-sizing: border-box;
}

.song-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #aaa; 
  background-color: rgba(255, 255, 255, 0.1); 
  cursor: pointer;
  transition: background 0.2s ease;
}

.song-item:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.song-title {
  font-weight: 600;
  font-size: 1rem;
  text-align: left;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-artist {
  font-size: 0.85rem;
  text-align: left;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  opacity: 0.75;
}

.no-songs {
  text-align: center;
  padding: 2rem;
  font-style: italic;
  opacity: 0.6;
}

.back-button {
  width: 100%;
  padding: 2rem 1rem;
  font-size: 1rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: background 0.3s ease;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.3);
  color: inherit;
}

.back-button:hover {
  background: rgba(0, 0, 0, 0.1);
}

body.dark .back-button {
  background: rgba(0, 0, 0, 0.3);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
}

</style>
