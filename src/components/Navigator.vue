<template>
  <div class="container">
    <h1 id="title">
      NMediaPlayer <i class="bi bi-music-player"></i>
    </h1>

    <div class="buttons-wrapper">
      <button class="card avalable-media" @click="$emit('navigate', 'media')">
        <span class="btn-content">
            Show Available Media <br> <i class="bi bi-music-note-list"></i>
        </span>
      </button>

      <button class="card playlists" @click="$emit('navigate', 'playlists')">
        <span class="btn-content">
            Show Playlists <br> <i class="bi bi-view-list"></i>
        </span>
      </button>

      <button class="card create-playlist" @click="$emit('navigate', 'create')">
        <span class="btn-content">
            Create New Playlist <br> <i class="bi bi-pencil"></i>
        </span>
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: "Navigator",
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
  padding: 2vh 5vw;
  box-sizing: border-box;
  color: white;
}

#title {
  font-size: clamp(24px, 5vw, 36px);
  margin-top: 4vh;
  margin-bottom: 2vh;
  animation: enter-from-top 700ms ease-in-out;
  text-shadow: 0 0 10px rgba(255, 255, 255, 0.3);
}

.buttons-wrapper {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3vh;
}

.card {
  width: 100%;
  max-width: 90vw;
  height: clamp(80px, 15vh, 120px);
  border-radius: 12px;
  
  border: 3px solid transparent;
  background-clip: padding-box, border-box;
  background-origin: padding-box, border-box;
  
  color: #fff;
  font-weight: 800;
  font-size: clamp(18px, 2.5vw, 22px);
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s ease, filter 0.2s ease;
  
  animation: enter-from-top 0.8s ease-in-out, neon-pulse 3s infinite alternate;
  
  display: flex;
  justify-content: center;
  align-items: center;
}

.btn-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  z-index: 2;
}

.avalable-media {
  --gradient: linear-gradient(130deg, #00cbcf 0%, #fdbb2d 100%);
  --shadow-color: rgba(0, 203, 207, 0.7);

  background-image: linear-gradient(#1a1a1a, #1a1a1a), var(--gradient);
}

.playlists {
  --gradient: linear-gradient(130deg, #8743b4 0%, #df1f1f 50%, #eea744 100%);
  --shadow-color: rgba(223, 31, 31, 0.7);

  background-image: linear-gradient(#1a1a1a, #1a1a1a), var(--gradient);
  animation-delay: 100ms, 0s;
}

.create-playlist {
  --gradient: linear-gradient(130deg, #2f9ceb 0%, #944ccf 100%);
  --shadow-color: rgba(47, 156, 235, 0.7);

  background-image: linear-gradient(#1a1a1a, #1a1a1a), var(--gradient);
  animation-delay: 200ms, 0s;
}

.card:hover {
  transform: scale(1.02);
  filter: brightness(1.2);
  box-shadow: 0 0 25px var(--shadow-color);
}

.card:active {
  transform: scale(0.98);
  filter: brightness(0.9);
}

@keyframes neon-pulse {
  0% {
    box-shadow: 0 0 5px var(--shadow-color), 
                inset 0 0 2px var(--shadow-color);
  }
  50% {
    box-shadow: 0 0 15px var(--shadow-color), 
                inset 0 0 5px rgba(255, 255, 255, 0.1);
  }
  100% {
    box-shadow: 0 0 5px var(--shadow-color), 
                inset 0 0 2px var(--shadow-color);
  }
}

@keyframes enter-from-top {
  0% {
    transform: translateY(-50px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}

@media screen and (orientation: landscape) and (max-height: 500px) {
  .buttons-wrapper {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    gap: 2vw;
  }

  .card {
    width: 30vw;
    height: 25vh;
    font-size: clamp(14px, 2vw, 18px);
  }
}
</style>