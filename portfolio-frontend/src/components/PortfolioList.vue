<script setup>
import { ref } from 'vue'
import { api, money } from '../api'

defineProps({ portfolios: Array, selectedId: Number })
const emit = defineEmits(['select', 'created'])

const name = ref('')
const owner = ref('')
const initialCash = ref(10000)
const error = ref('')
const busy = ref(false)

async function create() {
  busy.value = true
  try {
    const created = await api.createPortfolio({
      name: name.value,
      owner: owner.value,
      initialCash: Number(initialCash.value)
    })
    name.value = ''
    error.value = ''
    emit('created', created)
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="panel">
    <h2>Portfolios</h2>
    <ul class="list">
      <li v-for="p in portfolios" :key="p.id" :class="{ active: p.id === selectedId }" @click="emit('select', p.id)">
        <div>
          <div class="name">{{ p.name }}</div>
          <div class="muted small">{{ p.owner }} · {{ p.positionCount }} positions</div>
        </div>
        <div class="num small">{{ money(p.cashBalance) }}</div>
      </li>
      <li v-if="!portfolios.length" class="muted small">No portfolio yet.</li>
    </ul>

    <form class="stack form" @submit.prevent="create">
      <input v-model="name" placeholder="Portfolio name" />
      <input v-model="owner" placeholder="Owner" />
      <div class="row">
        <input v-model="initialCash" type="number" min="0" step="100" class="grow" aria-label="Initial cash" />
        <button class="primary" :disabled="busy">Create</button>
      </div>
      <div v-if="error" class="error small">{{ error }}</div>
    </form>
  </div>
</template>

<style scoped>
.list { list-style: none; margin: 0 0 12px; padding: 0; }
.list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 4px;
  cursor: pointer;
  border-left: 2px solid transparent;
}
.list li:hover { background: var(--panel-2); }
.list li.active { background: var(--panel-2); border-left-color: var(--accent); }
.name { font-weight: 500; }
.small { font-size: 12px; }
.form { border-top: 1px solid var(--line); padding-top: 12px; }
.grow { flex: 1; min-width: 0; }
</style>
