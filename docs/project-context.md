# Ghost Cart project context

Ghost Cart is a UAE-first, simulation-only cooling-off product for impulse purchases and abandoned carts. It gives every almost-buy a safe place to go before money leaves the user's account. Users can capture an item from anywhere, Ghost it, choose a cooling period, resolve the decision, and learn from the outcome without making a payment inside Ghost Cart.

Primary line: **Add to cart. Checkout. Keep your money.**

Positioning: **The cooling-off app for everything you almost bought.**

Core loop: temptation -> discover or share -> capture -> Ghost -> cool -> resolve -> learn.

Product discovery is a visual entry point, not a storefront. Users can browse a curated photo-first catalogue or share any safe public HTTPS product link into Ghost Cart. Open Graph, Twitter Card and Product structured metadata are best-effort and always editable; Android also performs an isolated on-device browser pass when the cloud preview is incomplete.

The simulation is a ritual inside that loop, not the product's source of truth. A Fake Checkout records an almost-buy. It does not prove that money was saved. Only a user-confirmed "I skipped it" resolution contributes to Money Kept.

The product includes a free marketing website, interactive browser demo, Android application, and shared Cloudflare/D1 backend. It does not process payments, place orders, deliver products, store money, or operate as a bank/payment service.
