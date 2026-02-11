<script>
const STRINGS = {
  it: {
    title: 'Crediti',
    developedBy: 'Sviluppato da',
    visitPortfolio: 'Visita il Portfolio',
    return: 'Torna al menu',
    version: 'Versione',
    date: 'Data',
    license: 'Licenza'
  },
  en: {
    title: 'Credits',
    developedBy: 'Developed by',
    visitPortfolio: 'Visit Portfolio',
    return: 'Return to menu',
    version: 'Version',
    date: 'Date',
    license: 'License'
  }
}

export default {
  name: 'Credits',
  props: {
    currentLang: {
      type: String,
      default: 'it'
    }
  },
  computed: {
    strings() {
      return this.currentLang === 'en' ? STRINGS.en : STRINGS.it;
    }
  }
};
</script>

<template>
    <div class="credits-view">
        <div class="header-section">
            <h2 class="page-title">{{ strings.title }} <i class="bi bi-info-circle"></i></h2>
        </div>

        <div class="content-wrapper">
            
            <div class="info-card app-card">
                <div class="app-icon-container">
                    <img src="../assets/ic_launcher.png" alt="App Icon" class="app-icon" />
                </div>
                <h1 class="app-name">NMediaPlayer</h1>
                
                <div class="meta-info">
                    <div class="meta-row">
                        <span class="meta-label">{{ strings.version }}:</span>
                        <span class="meta-value">1.0.0</span>
                    </div>
                    <div class="meta-row">
                        <span class="meta-label">{{ strings.date }}:</span>
                        <span class="meta-value">Feb 2026</span>
                    </div>
                    <div class="meta-row">
                        <span class="meta-label">{{ strings.license }}:</span>
                        <span class="meta-value">MIT</span>
                    </div>
                </div>
            </div>

            <div class="info-card dev-card">
                <div class="img-container">
                    <img src="../assets/ICON_NO_ALPHA.png" alt="N-Dev Logo" class="dev-logo" />
                </div>
                
                <div class="text-content">
                    <span class="label">{{ strings.developedBy }}</span>
                    <h2 class="dev-name">N-Dev</h2>
                </div>

                <a href="https://nfire07.github.io/E-Portfolio/#contents" target="_blank" class="portfolio-btn">
                    {{ strings.visitPortfolio }} <i class="bi bi-box-arrow-up-right"></i>
                </a>
            </div>

        </div>

        <button class="back-button" @click="$emit('goBack')">
            <i class="bi bi-arrow-left"></i> {{ strings.return }}
        </button>
    </div>
</template>

<style scoped>
.credits-view {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  color: #fff;
}

.header-section {
  padding: 4vh 1rem 2vh;
  text-align: center;
  flex-shrink: 0;
  animation: fade-in-down 0.6s ease-out;
}

.page-title {
  font-weight: 800;
  font-size: clamp(24px, 5vw, 32px);
  margin: 0;
  text-shadow: 0 0 15px rgba(255, 255, 255, 0.4);
}

.content-wrapper {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 2rem;
  padding: 1rem 1rem 100px 1rem;
  width: 100%;
  box-sizing: border-box;
}

.info-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 2rem;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.3);
  max-width: 85vw;
  width: 320px;
  animation: scale-in 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.app-card {
    animation-delay: 0s;
}

.dev-card {
    animation-delay: 0.2s;
    animation-fill-mode: backwards;
}

.app-icon-container {
    width: 80px;
    height: 80px;
    margin-bottom: 0.5rem;
    filter: drop-shadow(0 0 10px rgba(255, 255, 255, 0.2));
}

.app-icon {
    width: 100%;
    height: 100%;
    object-fit: contain;
}

.app-name {
    font-size: 1.8rem;
    font-weight: 800;
    margin: 0;
    background: linear-gradient(130deg, #00cbcf 0%, #fdbb2d 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.meta-info {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    margin-top: 0.5rem;
    background: rgba(0,0,0,0.2);
    padding: 1rem;
    border-radius: 12px;
}

.meta-row {
    display: flex;
    justify-content: space-between;
    font-size: 0.95rem;
    border-bottom: 1px solid rgba(255,255,255,0.05);
    padding-bottom: 4px;
}

.meta-row:last-child {
    border-bottom: none;
    padding-bottom: 0;
}

.meta-label {
    opacity: 0.7;
    font-weight: 500;
}

.meta-value {
    font-weight: 700;
    color: #fff;
}

.img-container {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, #00cbcf, #944ccf);
  box-shadow: 0 0 20px rgba(148, 76, 207, 0.5);
}

.dev-logo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  background-color: #000;
}

.text-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.label {
  font-size: 0.8rem;
  opacity: 0.7;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.dev-name {
  margin: 0;
  font-size: 2rem;
  font-weight: 900;
  background: linear-gradient(90deg, #fff, #a382ba);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.portfolio-btn {
  margin-top: 0.5rem;
  text-decoration: none;
  padding: 0.8rem 1.5rem;
  border-radius: 50px;
  background: linear-gradient(135deg, #2f9ceb 0%, #944ccf 100%);
  color: white;
  font-weight: 700;
  font-size: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 4px 15px rgba(47, 156, 235, 0.4);
}

.portfolio-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(148, 76, 207, 0.6);
}

.portfolio-btn:active {
  transform: scale(0.95);
}

.back-button {
  width: 100%;
  padding: 1.5rem 1rem;
  font-size: 1rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: background 0.3s ease;
  background: rgba(20, 20, 20, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  position: fixed;
  bottom: 0;
  left: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  z-index: 100;
}

.back-button:hover {
  background: rgba(40, 40, 40, 0.9);
}

@keyframes fade-in-down {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes scale-in {
  from { opacity: 0; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1); }
}
</style>