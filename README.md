# Cryptocurrency trading bot (WebSocket API)
This is crypto-trading bot with support for multiple crypto-exchanges using their websocket API for book retreival,
orders in most cases are placed using REST api (with rate limiting). 

It that has following strategies:
1. cross-exchange arbitrage strategy 
2. neural-network strategy to estimate future price when trading on single exchange.

**Supported exchanges**
- Binance
- Bitfinex
- Gdax
- Hitbtc
- Huobi
- Okex
- TheRockTrading
- Wex


**Main components**
- Provider (inbound adapter). Ingests websocket book data from exchange and transforms it into internal structure.
- Xo-Opportunity-Trader. Statemachine-based bot that creates orders based on selected strategy.
- Shared. Common repository
- Gateway (outbound adapter). Submits orders and provides common order management interface.
- Persistor. Stupid-simple component that allows to persist historical order book data. 
