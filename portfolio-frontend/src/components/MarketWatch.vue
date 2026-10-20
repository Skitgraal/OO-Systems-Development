<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { api, money, pct, signClass } from '../api'

const emit = defineEmits(['pick'])
const instruments = ref([])
const quotes = ref({})
const error = ref('')
let timer = null

async function refresh() {
  try {
    if (!instruments.value.length) instruments.value = await api.instruments()
    const list = await api.quotes(instruments.value.map((i) => i.symbol))
    quotes.value = Object.fromEntries(list.map((q) => [q.symbol, q]))
    error.value = ''
  } catch (e) {
    error.value = e.message
  }
}

onMounted(() => {
  refresh()
  timer = setInterval(refresh, 3000)
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="panel">
    <h2>Market watch <span class="live">● live</span></h2>
    <div v-if="error" class="error">{{ error }}</div>
    <table v-else>
      <thead>
        <tr><th>Symbol</th><th class="num">Last</th><th class="num">Day</th></tr>
      </thead>
      <tbody>
        <tr v-for="i in instruments" :key="i.symbol" class="clickable" title="Click to trade"
            @click="emit('pick', i.symbol)">
          <td>
            <div class="sym">{{ i.symbol }}</div>
            <div class="muted small">{{ i.name }}</div>
          </td>
          <td class="num">{{ money(quotes[i.symbol]?.price) }}</td>
          <td class="num" :class="signClass(quotes[i.symbol]?.changePercent)">
            {{ pct(quotes[i.symbol]?.changePercent) }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.live { color: var(--up); font-size: 10px; margin-left: 6px; letter-spacing: 0; }
.sym { font-family: var(--mono); font-weight: 600; }
.small { font-size: 12px; }
</style>
