// stores/musicPlayerStore.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { MediaPlugin } from '@/plugins/MediaPlugin'

export const useMusicPlayerStore = defineStore('musicPlayer', () => {
  // --- STATE ---
  const playlist = ref([])
  const originalPlaylist = ref([]) // Serve per ripristinare l'ordine se togliamo lo shuffle
  const currentSong = ref(null)
  const currentIndex = ref(-1)
  const isPlaying = ref(false)
  const isShuffleMode = ref(false)
  const isRepeatMode = ref(false) // false = no repeat, true = repeat all (semplificato)

  // --- GETTERS ---
  const hasNext = computed(() => playlist.value.length > 0)
  const hasPrev = computed(() => playlist.value.length > 0)

  // --- HELPER INTERNI ---
  
  /**
   * Calcola chi sarà la PROSSIMA canzone in base alla modalità (Shuffle/Normal/Repeat).
   * Questo serve per dire a Java: "Se l'app muore, tu suona questa dopo".
   */
  function calculateNextSongInfo(currentIdx) {
    if (playlist.value.length === 0) return null

    let nextIdx = -1

    if (isShuffleMode.value) {
      // Logica Shuffle Semplice: prende un indice a caso diverso dall'attuale
      // (Per una logica perfetta servirebbe un array "shuffled" separato, ma questo basta per ora)
      if (playlist.value.length > 1) {
        do {
          nextIdx = Math.floor(Math.random() * playlist.value.length)
        } while (nextIdx === currentIdx)
      } else {
        nextIdx = 0
      }
    } else {
      // Logica Sequenziale
      if (currentIdx < playlist.value.length - 1) {
        nextIdx = currentIdx + 1
      } else if (isRepeatMode.value) {
        nextIdx = 0 // Ricomincia da capo
      } else {
        return null // Fine playlist
      }
    }

    return {
      song: playlist.value[nextIdx],
      index: nextIdx
    }
  }

  // --- ACTIONS ---

  /**
   * Imposta la playlist e prepara il primo brano (senza suonarlo automaticamente se non richiesto)
   */
  function setPlaylist(songs, startIndex = 0, shuffle = false) {
    playlist.value = [...songs]
    originalPlaylist.value = [...songs]
    isShuffleMode.value = shuffle
    
    // Se shuffle è attivo fin dall'inizio, potremmo voler mescolare l'array qui.
    // Per ora manteniamo l'indice richiesto.
    if (songs[startIndex]) {
      currentSong.value = songs[startIndex]
      currentIndex.value = startIndex
    }
  }

  /**
   * Azione principale: Suona una canzone e PRECARICA la successiva in Java.
   */
  async function playSong(song, index) {
    if (!song) return

    // 1. Aggiorna lo stato locale
    currentSong.value = song
    if (index !== undefined && index !== null) {
      currentIndex.value = index
    }
    isPlaying.value = true

    // 2. Calcola il prossimo brano per il "gapless" a schermo spento
    const nextInfo = calculateNextSongInfo(currentIndex.value)

    try {
      // 3. Chiama il plugin
      await MediaPlugin.play({
        path: song.path,
        title: song.title || 'Unknown Title',
        artist: song.artist || 'Unknown Artist',
        album: song.album || '',
        // Dati PRELOAD per il Service Java
        nextPath: nextInfo ? nextInfo.song.path : null,
        nextTitle: nextInfo ? nextInfo.song.title : null,
        nextArtist: nextInfo ? nextInfo.song.artist : null
      })
    } catch (error) {
      console.error("Errore Play Plugin:", error)
      isPlaying.value = false
    }
  }

  async function resume() {
    try {
      await MediaPlugin.resume()
      isPlaying.value = true
    } catch (e) { console.error(e) }
  }

  async function pause() {
    try {
      await MediaPlugin.pause()
      isPlaying.value = false
    } catch (e) { console.error(e) }
  }

  async function next() {
    const nextInfo = calculateNextSongInfo(currentIndex.value)
    if (nextInfo) {
      await playSong(nextInfo.song, nextInfo.index)
    } else {
      // Fine playlist o caso limite
      await pause()
    }
  }

  async function previous() {
    let prevIdx = currentIndex.value - 1
    if (prevIdx < 0) prevIdx = playlist.value.length - 1 // Loop o stop
    
    const prevSong = playlist.value[prevIdx]
    if (prevSong) {
      await playSong(prevSong, prevIdx)
    }
  }

  /**
   * SINCRONIZZAZIONE AUTOMATICA
   * Chiamata quando Java ha cambiato canzone DA SOLO (schermo spento).
   * Dobbiamo aggiornare la UI per riflettere la realtà, ma NON chiamare .play()!
   */
  async function syncNextSong() {
    console.log("🔄 Syncing state with Native Auto-Next...")
    
    // Ricalcoliamo chi doveva essere il prossimo, perché è quello che sta suonando ora
    const nextInfo = calculateNextSongInfo(currentIndex.value)
    
    if (nextInfo) {
      // Aggiorniamo SOLO le variabili di stato
      currentIndex.value = nextInfo.index
      currentSong.value = nextInfo.song
      isPlaying.value = true
      
      // Opzionale: Ora che siamo su Song B, calcoliamo Song C e proviamo a mandarla a Java
      // (Questo funziona solo se l'app è in background ma non "killata" del tutto)
      const futureInfo = calculateNextSongInfo(nextInfo.index)
      if (futureInfo) {
         // Potremmo avere un metodo plugin 'updateNextTrack' dedicato, 
         // ma per ora lasciamo così per evitare complessità.
         // Al prossimo play manuale si risincronizza tutto.
      }
    }
  }

  /**
   * Gestore centrale degli eventi che arrivano da Java (Notifica, Cuffie, Auto-Next)
   */
  async function handleNotificationAction(action) {
    console.log("🔔 Store Action:", action)
    switch (action) {
      case 'com.nmediaplayer.ACTION_PLAY':
      case 'com.nmediaplayer.ACTION_RESUME':
        isPlaying.value = true
        break
      case 'com.nmediaplayer.ACTION_PAUSE':
        isPlaying.value = false
        break
      case 'com.nmediaplayer.ACTION_NEXT':
        await next()
        break
      case 'com.nmediaplayer.ACTION_PREV':
        await previous()
        break
      case 'com.nmediaplayer.ACTION_AUTO_NEXT_STARTED':
        await syncNextSong()
        break
    }
  }

  return {
    // State
    playlist,
    currentSong,
    currentIndex,
    isPlaying,
    isShuffleMode,
    isRepeatMode,
    // Getters
    hasNext,
    hasPrev,
    // Actions
    setPlaylist,
    playSong,
    resume,
    pause,
    next,
    previous,
    handleNotificationAction,
    syncNextSong
  }
})