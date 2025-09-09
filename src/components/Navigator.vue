<template>
  <div class="container">
    <h1 id="title">
      N-Media Player 🎵
    </h1>

    <button class="card text avalable-media" @click="$emit('navigate', 'media')">
      Show Available Media<br> 🎶
    </button>

    <button class="card text playlists" @click="$emit('navigate', 'playlists')">
      Show Playlists<br> 📁
    </button>

    <button class="card text create-playlist" @click="$emit('navigate', 'create')">
      Create New Playlist<br> ✨
    </button>
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
}

.text {
	color: #121212;
	font-weight: 800;
	font-size: clamp(24px, 2.5vw, 20px);
	text-align: center;
}

#title {
	font-size: clamp(24px, 5vw, 36px);
	margin-top: 4vh;
	animation: enter-from-top 700ms ease-in-out;
}

.card {
	width: 100%;
	max-width: 90vw;
	height: clamp(100px, 20vh, 180px);
	margin: 3vh 0;
	border-radius: 12px;
	border: 1px solid #00000072;
	display: flex;
	justify-content: center;
	align-items: center;
	text-align: center;
	flex-wrap: wrap;
	transition: transform 0.1s ease, box-shadow 0.2s ease;
	cursor: pointer;
}

.card:active {
	transform: scale(0.98);
	box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.card:focus {
	outline: none;
	box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.3);
}

.avalable-media {
	animation: enter-from-top 800ms ease-in-out;
	background: linear-gradient(130deg, #00cbcf 0%, #fdbb2d 100%);
}

.playlists {
	animation: enter-from-top 900ms ease-in-out;
	background: linear-gradient(130deg, #8743b4 0%, #df1f1f 50%, #eea744 100%);
}

.create-playlist {
	background: linear-gradient(130deg, #2f9ceb 0%, #944ccf 100%);
	animation: enter-from-top 1000ms ease-in-out;
}

@keyframes enter-from-top {
	0% {
		transform: translateY(-100%);
		opacity: 0;
	}
	100% {
		transform: translateY(0);
		opacity: 1;
	}
}

@media screen and (orientation: landscape) and (max-height: 500px) {
	.container {
		flex-direction: row;
		flex-wrap: wrap;
		justify-content: center;
		align-items: flex-start;
		gap: 2vw;
		padding: 2vh;
	}

	.card {
		width: 30vw;
		height: 25vh;
		margin: 1vh;
	}

	#title {
		width: 100%;
		font-size: clamp(20px, 4vw, 32px);
	}
}
</style>
