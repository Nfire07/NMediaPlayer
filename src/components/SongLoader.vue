<template>
  <div class="song-loader">
    <div v-if="loading" class="loading">Loading songs...</div>
    <div v-else-if="error" class="error">Error: {{ error }}</div>
    <div v-else>
      <slot v-if="Array.isArray(songs)" :songs="songs"></slot>
      <div v-else class="error">No Data Found.</div>
    </div>
  </div>
</template>


<script setup>
import { ref, onMounted } from 'vue'
import { MediaPlugin } from '@/plugins/MediaPlugin'

const songs = ref([])
const loading = ref(true)
const error = ref(null)

async function loadSongs() {
  loading.value = true
  error.value = null
  try {
    const result = await MediaPlugin.getSongs()
    console.log('Songs result:', result)
    songs.value = result.songs || []
  } catch (e) {
    console.error('Error loading songs:', e)
    error.value = e.message || 'Unknown error'
  } finally {
    loading.value = false
  }
}

onMounted(loadSongs)
</script>


<style scoped>
.song-loader {
  text-align: center;
  padding: 2rem;
}
.loading {
  font-size: 1.2rem;
}
.error {
  color: red;
}
</style>
