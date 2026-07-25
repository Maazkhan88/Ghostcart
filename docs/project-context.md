# Ghost Cart project context

Ghost Cart is a UAE-first, simulation-only cooling-off product for impulse purchases and abandoned carts. It gives every almost-buy a safe place to go before money leaves the user's account. Users can capture an item from anywhere, Ghost it, choose a cooling period, resolve the decision, and learn from the outcome without making a payment inside Ghost Cart.

Primary line: **Add to cart. Checkout. Keep your money.**

Positioning: **The cooling-off app for everything you almost bought.**

Core loop: temptation -> discover or share -> Ghost -> automatic 24-hour cooldown -> reminder -> decide -> learn.

One Ghosted item always equals one item whose cooldown was started. This definition is shared by item counts, personal progress, public aggregate activity, and the opt-in leaderboard; later outcomes do not retroactively add or remove a Ghost.

“Ghost it” is the single primary action across catalogue, link import, manual capture, product detail, and bulk review. It always starts a 24-hour cooldown by default. Duration selection is a secondary action used only when a customer restarts a completed cooldown.

Product discovery is a visual entry point, not a storefront. Users can browse a curated photo-first catalogue or share any safe public HTTPS product link into Ghost Cart. Open Graph, Twitter Card and Product structured metadata are best-effort and always editable; Android also performs an isolated on-device browser pass when the cloud preview is incomplete.

Food and delivery cravings have a dedicated Home lane. Links shared from public Noon Food, Keeta, Talabat, Deliveroo, Uber Eats, Careem Food, and other food pages enter the same editable 24-hour cooling flow.

The simulation is a ritual inside that loop, not the product's source of truth. A Fake Checkout records an almost-buy. It does not prove that money was saved. Only a user-confirmed "I skipped it" resolution contributes to Money Kept.

The product includes a free marketing website, interactive browser demo, Android application, and shared Cloudflare/D1 backend. It does not process payments, place orders, deliver products, store money, or operate as a bank/payment service.
