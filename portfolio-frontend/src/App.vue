<script setup>
import { onMounted, ref } from 'vue'
import { api } from './api'
import MarketWatch from './components/MarketWatch.vue'
import PortfolioList from './components/PortfolioList.vue'
import PortfolioDetail from './components/PortfolioDetail.vue'

const portfolios = ref([])
const selectedId = ref(null)
const selectedSymbol = ref('')
const error = ref('')

async function loadPortfolios() {
  try {
    portfolios.value = await api.portfolios()
    error.value = ''
    if (!selectedId.value && portfolios.value.length) selectedId.value = portfolios.value[0].id
  } catch (e) {
    error.value = e.message
  }
}

function onDeleted() {
  selectedId.value = null
  loadPortfolios()
}

onMounted(loadPortfolios)
</script>

<template>
  <header class="topbar">
    <div class="brand">
      <span class="logo">◆</span>
      <strong>Portfolio Desk</strong>
      <span class="muted">Paris equities · simulated prices</span>
    </div>
  </header>

  <main class="layout">
    <aside class="side">
      <PortfolioList
        :portfolios="portfolios"
        :selected-id="selectedId"
        @select="selectedId = $event"
        @created="(p) => { selectedId = p.id; loadPortfolios() }"
      />
      <MarketWatch @pick="selectedSymbol = $event" />
    </aside>

    <section class="content">
      <div v-if="error" class="error">{{ error }}</div>
      <PortfolioDetail
        v-if="selectedId"
        :key="selectedId"
        :portfolio-id="selectedId"
        :picked-symbol="selectedSymbol"
        @changed="loadPortfolios"
        @deleted="onDeleted"
      />
      <div v-else class="panel empty">
        <h2>No portfolio selected</h2>
        <p class="muted">Create a portfolio on the left to start trading.</p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid var(--line);
  background: var(--panel);
}
.brand { display: flex; gap: 12px; align-items: baseline; }
.logo { color: var(--accent); }
.layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 16px;
  padding: 16px 24px;
  align-items: start;
}
.side { display: flex; flex-direction: column; gap: 16px; }
.content { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
.empty { padding: 40px; text-align: center; }
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; padding: 16px; }
  .brand .muted { display: none; }
}
</style>
