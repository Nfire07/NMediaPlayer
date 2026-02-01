import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { MediaPlugin } from '@/plugins/MediaPlugin'

export const useMusicPlayerStore = defineStore('musicPlayer', () => {
  // State
  const currentSong = ref(null)
  const playlist = ref([])
  const currentIndex = ref(0)
  const isPlaying = ref(false)
  const isShuffleMode = ref(false)
  const isNotificationActive = ref(false)

  // Computed
  const hasNext = computed(() => {
    if (isShuffleMode.value) return playlist.value.length > 1
    return playlist.value.length > 0
  })

  const hasPrev = computed(() => {
    return !isShuffleMode.value && currentIndex.value > 0
  })

  // Actions
  async function playSong(song, index = null) {
    try {
      console.log('🎵 playSong:', song.title)
      
      currentSong.value = song
      if (index !== null) currentIndex.value = index

      // 1. Avvia riproduzione nativa
      await MediaPlugin.play({
        path: song.path,
        title: song.title || 'Unknown',
        artist: song.artist || 'Unknown Artist'
      })

      isPlaying.value = true
      
      // 2. Forza notifica immediatamente
      await startMusicNotification(song)
      
    } catch (error) {
      console.error('❌ Error in playSong:', error)
    }
  }

  async function pause() {
    try {
      await MediaPlugin.pause()
      isPlaying.value = false
      await updateMusicNotification()
    } catch (e) { console.error(e) }
  }

  async function resume() {
    try {
      await MediaPlugin.resume()
      isPlaying.value = true
      await updateMusicNotification()
    } catch (e) { console.error(e) }
  }

  async function stop() {
    try {
      await MediaPlugin.stop()
      isPlaying.value = false
      currentSong.value = null
      await stopMusicNotification()
    } catch (e) { console.error(e) }
  }

  async function next() {
    console.log('⏭️ Next song requested')
    if (playlist.value.length === 0) return

    let nextIndex = currentIndex.value

    if (isShuffleMode.value) {
      if (playlist.value.length > 1) {
         do {
          nextIndex = Math.floor(Math.random() * playlist.value.length)
        } while (nextIndex === currentIndex.value)
      }
    } else {
      if (currentIndex.value < playlist.value.length - 1) {
        nextIndex++
      } else {
        nextIndex = 0 // Loop
      }
    }
    
    await playSong(playlist.value[nextIndex], nextIndex)
  }

  async function previous() {
    if (!hasPrev.value || playlist.value.length === 0) return
    let prevIndex = currentIndex.value > 0 ? currentIndex.value - 1 : 0
    await playSong(playlist.value[prevIndex], prevIndex)
  }

  function setPlaylist(songs, startIndex = 0, shuffle = false) {
    playlist.value = songs
    currentIndex.value = startIndex
    isShuffleMode.value = shuffle
  }

  async function startMusicNotification(song = null) {
    const s = song || currentSong.value
    if(!s) return
    try {
      await MediaPlugin.startMusicNotification({
        title: s.title,
        artist: s.artist,
        isPlaying: isPlaying.value
      })
      isNotificationActive.value = true
    } catch (e) { console.error(e) }
  }

  async function updateMusicNotification(song = null) {
    const s = song || currentSong.value
    if(!s) return
    try {
      await MediaPlugin.updateMusicNotification({
        title: s.title,
        artist: s.artist,
        isPlaying: isPlaying.value
      })
    } catch (e) { console.error(e) }
  }

  async function stopMusicNotification() {
    try {
      await MediaPlugin.stopMusicNotification()
      isNotificationActive.value = false
    } catch (e) { console.error(e) }
  }

  async function handleNotificationAction(action) {
    console.log('Action handler:', action)
    switch (action) {
      case 'com.nmediaplayer.ACTION_PLAY': await resume(); break
      case 'com.nmediaplayer.ACTION_PAUSE': await pause(); break
      case 'com.nmediaplayer.ACTION_NEXT': await next(); break
      case 'com.nmediaplayer.ACTION_PREV': await previous(); break
      case 'com.nmediaplayer.ACTION_STOP': await stop(); break
    }
  }

  return {
    currentSong, playlist, currentIndex, isPlaying, isShuffleMode, hasNext, hasPrev,
    playSong, pause, resume, stop, next, previous, setPlaylist, handleNotificationAction
  }
})