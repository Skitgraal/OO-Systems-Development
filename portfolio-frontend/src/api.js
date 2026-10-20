// Thin wrapper around fetch: throws an Error carrying the server's JSON message.
async function request(method, path, body) {
  const response = await fetch(`/api${path}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined
  })
  if (response.status === 204) return null
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    let message = data?.message || `HTTP ${response.status}`
    if (data?.fieldErrors) {
      message += ': ' + Object.entries(data.fieldErrors).map(([f, m]) => `${f} ${m}`).join(', ')
    }
    throw new Error(message)
  }
  return data
}

export const api = {
  instruments: () => request('GET', '/instruments'),
  quotes: (symbols) => request('GET', `/quotes?symbols=${encodeURIComponent(symbols.join(','))}`),
  portfolios: () => request('GET', '/portfolios'),
  portfolio: (id) => request('GET', `/portfolios/${id}`),
  createPortfolio: (payload) => request('POST', '/portfolios', payload),
  deletePortfolio: (id) => request('DELETE', `/portfolios/${id}`),
  deposit: (id, amount) => request('POST', `/portfolios/${id}/deposit`, { amount }),
  withdraw: (id, amount) => request('POST', `/portfolios/${id}/withdraw`, { amount }),
  buy: (id, symbol, quantity) => request('POST', `/portfolios/${id}/buy`, { symbol, quantity }),
  sell: (id, symbol, quantity) => request('POST', `/portfolios/${id}/sell`, { symbol, quantity }),
  transactions: (id) => request('GET', `/portfolios/${id}/transactions`)
}

const eur = new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'EUR' })
export const money = (v) => (v == null ? '—' : eur.format(v))
export const pct = (v) => (v == null ? '—' : `${v > 0 ? '+' : ''}${Number(v).toFixed(2)}%`)
export const signClass = (v) => (v > 0 ? 'up' : v < 0 ? 'down' : '')
