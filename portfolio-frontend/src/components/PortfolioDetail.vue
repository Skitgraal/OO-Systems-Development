<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { api, money, pct, signClass } from '../api'

const props = defineProps({ portfolioId: Number, pickedSymbol: String })
const emit = defineEmits(['changed', 'deleted'])

const portfolio = ref(null)
const transactions = ref([])
const error = ref('')
const notice = ref('')
const busy = ref(false)

const symbol = ref(props.pickedSymbol || 'BNP.PA')
const quantity = ref(10)
const cashAmount = ref(1000)
let timer = null

watch(() => props.pickedSymbol, (s) => { if (s) symbol.value = s })

async function load() {
  try {
    const [p, t] = await Promise.all([api.portfolio(props.portfolioId), api.transactions(props.portfolioId)])
    portfolio.value = p
    transactions.value = t
    error.value = ''
  } catch (e) {
    error.value = e.message
  }
}

async function run(action, successMessage) {
  busy.value = true
  notice.value = ''
  try {
    await action()
    error.value = ''
    notice.value = successMessage
    await load()
    emit('changed')
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

const buy = () => run(() => api.buy(props.portfolioId, symbol.value, Number(quantity.value)),
  `Bought ${quantity.value} ${symbol.value.toUpperCase()}`)
const sell = () => run(() => api.sell(props.portfolioId, symbol.value, Number(quantity.value)),
  `Sold ${quantity.value} ${symbol.value.toUpperCase()}`)
const deposit = () => run(() => api.deposit(props.portfolioId, Number(cashAmount.value)), 'Deposit done')
const withdraw = () => run(() => api.withdraw(props.portfolioId, Number(cashAmount.value)), 'Withdrawal done')

async function remove() {
  if (!confirm('Delete this portfolio and its history?')) return
  try {
    await api.deletePortfolio(props.portfolioId)
    emit('deleted')
  } catch (e) {
    error.value = e.message
  }
}

function sellAll(p) {
  symbol.value = p.symbol
  quantity.value = p.quantity
}

const time = (iso) => new Date(iso).toLocaleString('en-GB')

onMounted(() => {
  load()
  timer = setInterval(load, 5000)
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div v-if="error" class="error">{{ error }}</div>
  <div v-if="notice" class="success">{{ notice }}</div>

  <template v-if="portfolio">
    <div class="panel head">
      <div>
        <div class="title">{{ portfolio.name }}</div>
        <div class="muted">Owner: {{ portfolio.owner }} · #{{ portfolio.id }}</div>
      </div>
      <button class="ghost" @click="remove">Delete</button>
    </div>

    <div class="kpis">
      <div class="panel kpi"><h2>Total value</h2><div class="big num">{{ money(portfolio.totalValue) }}</div></div>
      <div class="panel kpi"><h2>Cash</h2><div class="big num">{{ money(portfolio.cashBalance) }}</div></div>
      <div class="panel kpi"><h2>Invested</h2><div class="big num">{{ money(portfolio.investedValue) }}</div></div>
      <div class="panel kpi">
        <h2>Unrealized P&amp;L</h2>
        <div class="big num" :class="signClass(portfolio.unrealizedPnl)">{{ money(portfolio.unrealizedPnl) }}</div>
        <div class="num small" :class="signClass(portfolio.unrealizedPnlPercent)">{{ pct(portfolio.unrealizedPnlPercent) }}</div>
      </div>
    </div>

    <div class="grid">
      <div class="panel">
        <h2>Positions</h2>
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Symbol</th><th class="num">Qty</th><th class="num">Avg cost</th><th class="num">Last</th>
                <th class="num">Value</th><th class="num">P&amp;L</th><th class="num">Weight</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in portfolio.positions" :key="p.symbol" class="clickable" @click="sellAll(p)"
                  title="Click to prefill the trade ticket">
                <td><span class="tag">{{ p.symbol }}</span> <span class="muted small">{{ p.name }}</span></td>
                <td class="num">{{ p.quantity }}</td>
                <td class="num">{{ money(p.averageCost) }}</td>
                <td class="num">{{ money(p.marketPrice) }}</td>
                <td class="num">{{ money(p.marketValue) }}</td>
                <td class="num" :class="signClass(p.unrealizedPnl)">
                  {{ money(p.unrealizedPnl) }}<br /><span class="small">{{ pct(p.unrealizedPnlPercent) }}</span>
                </td>
                <td class="num">{{ Number(p.weightPercent).toFixed(1) }}%</td>
              </tr>
              <tr v-if="!portfolio.positions.length">
                <td colspan="7" class="muted">No position. Buy a stock with the trade ticket.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="stack">
        <form class="panel stack" @submit.prevent>
          <h2>Trade ticket · market order</h2>
          <input v-model="symbol" placeholder="Symbol (e.g. BNP.PA)" />
          <input v-model="quantity" type="number" min="1" step="1" aria-label="Quantity" />
          <div class="row">
            <button class="buy grow" :disabled="busy" @click="buy">Buy</button>
            <button class="sell grow" :disabled="busy" @click="sell">Sell</button>
          </div>
        </form>

        <form class="panel stack" @submit.prevent>
          <h2>Cash</h2>
          <input v-model="cashAmount" type="number" min="1" step="100" aria-label="Amount" />
          <div class="row">
            <button class="grow" :disabled="busy" @click="deposit">Deposit</button>
            <button class="grow" :disabled="busy" @click="withdraw">Withdraw</button>
          </div>
        </form>
      </div>
    </div>

    <div class="panel">
      <h2>Transactions</h2>
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Date</th><th>Type</th><th>Symbol</th><th class="num">Qty</th>
              <th class="num">Price</th><th class="num">Cash</th><th class="num">Realized P&amp;L</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in transactions" :key="t.id">
              <td class="muted small">{{ time(t.executedAt) }}</td>
              <td><span class="tag">{{ t.type }}</span></td>
              <td>{{ t.symbol || '—' }}</td>
              <td class="num">{{ t.quantity ?? '—' }}</td>
              <td class="num">{{ money(t.price) }}</td>
              <td class="num" :class="signClass(t.amount)">{{ money(t.amount) }}</td>
              <td class="num" :class="signClass(t.realizedPnl)">{{ money(t.realizedPnl) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </template>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 20px; font-weight: 600; }
.kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.kpi h2 { margin-bottom: 6px; }
.big { font-size: 20px; text-align: left; }
.kpi .small { text-align: left; }
.small { font-size: 12px; }
.grid { display: grid; grid-template-columns: 1fr 280px; gap: 16px; align-items: start; }
.grow { flex: 1; }
@media (max-width: 1100px) {
  .grid { grid-template-columns: 1fr; }
  .kpis { grid-template-columns: repeat(2, 1fr); }
}
</style>
