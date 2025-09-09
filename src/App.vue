<template>
  <div class="layout">
    <div class="main">
      <Navigator v-if="currentView === 'navigator'" @navigate="setView" />
      <AvailableMedia v-if="currentView === 'media'" @goBack="setView('navigator')" />
      <Playlists v-if="currentView === 'playlists'" @goBack="setView('navigator')" />
      <CreatePlaylist v-if="currentView === 'create'" @goBack="setView('navigator')" />
    </div>
    <footer class="footer" v-if="currentView === 'navigator'">
      <p>Developed by <img src="./assets/ICON_NO_ALPHA.png" alt="N" /></p>
    </footer>
  </div>
</template>


<script>
import Navigator from './components/Navigator.vue'
import AvailableMedia from './components/AvailableMedia.vue'
import Playlists from './components/Playlists.vue'
import CreatePlaylist from './components/CreatePlaylist.vue'

export default {
  components: {
    Navigator,
    AvailableMedia,
    Playlists,
    CreatePlaylist
  },
  data() {
    return {
      currentView: 'navigator'
    }
  },
  mounted() {
    this.applyTheme()
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', this.applyTheme)
  },
  beforeUnmount() {
    window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', this.applyTheme)
  },
  methods: {
    setView(view) {
      this.currentView = view
    },
	applyTheme() {
      const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      document.body.classList.toggle('dark', isDark)
    }
  }
}
</script>



<style>
	html, body {
		margin: 0;
		padding: 0;
		width: 100vw;
		height: 100vh;
		overflow-x: hidden;
		background: linear-gradient(30deg, #EEAECA 0%, #94BBE9 100%);
		font-family: sans-serif;
		flex-direction: column;
		overflow-x:hidden;
	}	
	body.dark {
		background: linear-gradient(30deg, #121211 0%, #363636 100%);
		color: #fff;
	}
	.layout {
		display: flex;
		flex-direction: column;
		min-height: 100vh;
	}
	.main {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: flex-start;
	}

	.footer {
		margin-top: auto;
		width: 100%;
		padding: 1.5rem;
		text-align: center;
		background: rgba(255, 255, 255, 0.2);
		backdrop-filter: blur(10px);
		-webkit-backdrop-filter: blur(10px);
		border-top: 1px solid rgba(255, 255, 255, 0.3);
		color: #000;
		font-weight: 600;
		display: flex;
		justify-content: center;
		align-items: center;
		gap: 0.5rem;
	}

	body.dark .footer {
		background: rgba(0, 0, 0, 0.3);
		border-top: 1px solid rgba(255, 255, 255, 0.2);
		color: #fff;
	}

	.footer img {
		height: 24px;
		vertical-align: middle;
	}
</style>
