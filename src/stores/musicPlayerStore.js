import { defineStore } from 'pinia'
import { MediaPlugin } from '@/plugins/MediaPlugin'

export const useMusicPlayerStore = defineStore('musicPlayer', {
  state: () => ({
    playlist: [],
    originalPlaylist: [],
    currentSong: null,
    currentIndex: -1,
    isPlaying: false,
    isShuffleModeOn: false,
    repeatMode: 'off'
  }),

  getters: {
    hasNext: (state) => state.playlist.length > 0 && state.currentIndex < state.playlist.length - 1,
    hasPrev: (state) => state.playlist.length > 0 && state.currentIndex > 0,
    getCurrentTitle: (state) => state.currentSong?.title || 'No song playing',
    getCurrentArtist: (state) => state.currentSong?.artist || ' '
  },

  actions: {
    async setPlaylist(songs, startIndex = 0, isShuffle = false) {
      if (!songs || songs.length === 0) return;

      this.playlist = songs;
      this.isShuffleModeOn = isShuffle;

      const nativePlaylist = songs.map(s => ({
        path: s.path || '',
        title: s.title || 'Unknown Title',
        artist: s.artist || 'Unknown Artist'
      }));

      try {
        await MediaPlugin.setPlaylist({ songs: nativePlaylist });
      } catch (e) {}
    },

    async initListener() {
        await MediaPlugin.addListener('playerStateChange', (data) => {
            if (data.action === 'songChanged') {
                const newIndex = data.index;
                if (newIndex >= 0 && newIndex < this.playlist.length) {
                    this.currentIndex = newIndex;
                    this.currentSong = this.playlist[newIndex];
                    this.isPlaying = data.isPlaying;
                }
            }

            if (data.action === 'pause') this.isPlaying = false;
            if (data.action === 'play' || data.action === 'resume') this.isPlaying = true;
        });
    },

    async playSong(song, index) {
      if (!song) return;

      const songPath = song.path || song.url;
      if (!songPath) return;

      this.currentSong = song;
      this.currentIndex = index;
      this.isPlaying = true;

      try {
        await MediaPlugin.play({
          path: songPath,
          title: song.title || "Unknown",
          artist: song.artist || "Unknown",
          index: index
        });
      } catch (e) {}
    },

    async pause() {
      this.isPlaying = false;
      try {
        await MediaPlugin.pause();
      } catch (e) {}
    },

    async resume() {
      this.isPlaying = true;
      try {
        await MediaPlugin.resume();
      } catch (e) {}
    },

    async next() {
      if (this.hasNext) {
        const nextIndex = this.currentIndex + 1;
        await this.playSong(this.playlist[nextIndex], nextIndex);
      }
    },

    async previous() {
      if (this.hasPrev) {
        const prevIndex = this.currentIndex - 1;
        await this.playSong(this.playlist[prevIndex], prevIndex);
      }
    },

    handleNotificationAction(data) {
      switch (data.action) {
        case 'com.nmediaplayer.ACTION_AUTO_NEXT_STARTED':
        case 'com.nmediaplayer.ACTION_NEXT':
        case 'com.nmediaplayer.ACTION_PREV':
          if (typeof data.index === 'number' && data.index > -1) {
            if (this.playlist[data.index]) {
              this.currentIndex = data.index;
              this.currentSong = this.playlist[data.index];
              this.isPlaying = true;
            }
          }
          break;

        case 'com.nmediaplayer.ACTION_PAUSE':
          this.isPlaying = false;
          break;

        case 'com.nmediaplayer.ACTION_RESUME':
        case 'com.nmediaplayer.ACTION_PLAY':
          this.isPlaying = true;
          break;
          
        case 'com.nmediaplayer.ACTION_STOP':
          this.isPlaying = false;
          break;
      }
    }
  }
})