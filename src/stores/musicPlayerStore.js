import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { MediaPlugin } from '@/plugins/MediaPlugin'

export const useMusicPlayerStore = defineStore('musicPlayer', () => {
  const currentSong = ref(null)
  const playlist = ref([])
  const currentIndex = ref(0)
  const isPlaying = ref(false)
  const isShuffleMode = ref(false)
  const isForegroundServiceActive = ref(false)

  const hasNext = computed(() => playlist.value.length > 0)
  const hasPrev = computed(() => playlist.value.length > 0)

  async function playSong(song, index = null) {
    try {
      currentSong.value = song
      if (index !== null) currentIndex.value = index
      await MediaPlugin.play({ path: song.path, title: song.title, artist: song.artist })
      isPlaying.value = true
      if (!isForegroundServiceActive.value) {
        await MediaPlugin.startMusicNotification({ title: song.title, artist: song.artist, isPlaying: true })
        isForegroundServiceActive.value = true
      } else {
        await MediaPlugin.updateMusicNotification({ title: song.title, artist: song.artist, isPlaying: true })
      }
    } catch (e) { console.error(e) }
  }

  async function pause() {
    await MediaPlugin.pause()
    isPlaying.value = false
    await MediaPlugin.updateMusicNotification({ isPlaying: false })
  }

  async function resume() {
    await MediaPlugin.resume()
    isPlaying.value = true
    await MediaPlugin.updateMusicNotification({ isPlaying: true })
  }

  async function next() {
    if (playlist.value.length === 0) return
    let idx = isShuffleMode.value ? Math.floor(Math.random() * playlist.value.length) : (currentIndex.value + 1) % playlist.value.length
    await playSong(playlist.value[idx], idx)
  }

  async function previous() {
    if (playlist.value.length === 0) return
    let idx = (currentIndex.value - 1 + playlist.value.length) % playlist.value.length
    await playSong(playlist.value[idx], idx)
  }

  function handleNotificationAction(action) {
    if (action === 'com.nmediaplayer.ACTION_PAUSE') isPlaying.value = false
    else if (action === 'com.nmediaplayer.ACTION_PLAY') isPlaying.value = true
    else if (action === 'com.nmediaplayer.ACTION_NEXT') next()
    else if (action === 'com.nmediaplayer.ACTION_PREV') previous()
  }

  return {
    currentSong, playlist, currentIndex, isPlaying, isShuffleMode, hasNext, hasPrev,
    playSong, pause, resume, next, previous, handleNotificationAction,
    setPlaylist: (s, i, sh) => { playlist.value = s; currentIndex.value = i; isShuffleMode.value = sh; }
  }
})