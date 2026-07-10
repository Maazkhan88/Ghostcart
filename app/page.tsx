"use client";

import { DragEvent, FormEvent, PointerEvent, useEffect, useMemo, useRef, useState } from "react";

type DemoProduct = {
  id: string;
  name: string;
  note: string;
  category: string;
  tone: string;
  image?: string;
};

const DEMO_PRODUCTS: DemoProduct[] = [
  { id: "sneakers", name: "The white sneakers", note: "Saved after a late-night scroll", category: "Fashion", tone: "tone-ice", image: "/products/sneaker.png" },
  { id: "perfume", name: "The blind-buy perfume", note: "A review made it feel urgent", category: "Beauty", tone: "tone-smoke", image: "/products/perfume.png" },
  { id: "burger", name: "The midnight combo", note: "Built from boredom, not hunger", category: "Delivery", tone: "tone-paper" },
  { id: "headphones", name: "The extra headphones", note: "A deal you did not need yesterday", category: "Tech", tone: "tone-mint" },
];

const DASHBOARD_DEMO_DATA = {
  minimumGhostedCount: 4,
  topPattern: "Late-night scrolling",
  coolingStreakDays: 3,
  protectedBars: [18, 28, 44, 76, 68, 82],
  categories: [
    { label: "Food & drinks", share: 35, dotClass: "dot-1" },
    { label: "Fashion", share: 20, dotClass: "dot-2" },
    { label: "Tech", share: 15, dotClass: "dot-3" },
    { label: "Entertainment", share: 15, dotClass: "dot-4" },
    { label: "Other", share: 15, dotClass: "dot-5" },
  ],
  mood: {
    points: "10,55 52,68 94,40 136,58 178,30 220,42 270,15",
    finalPoint: { x: 270, y: 15 },
    days: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
  },
} as const;

const FAQS = [
  ["Is Ghost Cart a real shopping app?", "No. Ghost Cart is a simulation designed for almost-buys and impulse cooling. Products shown in the demo are generic references, not items for sale."],
  ["Do I pay anything during Fake Checkout?", "No. Fake Checkout never asks for card details and never processes a payment."],
  ["Does anything get delivered?", "No. Delivery-style updates are playful, clearly imaginary parts of the simulation."],
  ["What can I put in Ghost Cart?", "Anything you are tempted to buy—from a delivery craving to a fashion cart or gadget—provided it is for personal reflection and not a real order."],
  ["Is Ghost Wallet real money?", "No. Ghost Wallet is a simulated progress view. It does not store, transfer, or receive money and is not a bank account."],
  ["When is Ghost Cart launching?", "The product is in active development. Join the launch list to be ready for early access once the live signup is connected."],
];

function Wordmark({ inverted = false }: { inverted?: boolean }) {
  return (
    <span className={`wordmark ${inverted ? "wordmark-inverted" : ""}`} aria-label="Ghost Cart">
      <img className="wordmark-icon" src="/brand/ghost-cart-icon.png" alt="" aria-hidden="true" width={32} height={32} />
      <span>Ghost Cart</span>
    </span>
  );
}

const MASCOT_POSES = {
  wave: "/mascot/mascot-wave.png",
  waveAlt: "/mascot/mascot-wave-alt.png",
  cart: "/mascot/mascot-cart.png",
  wallet: "/mascot/mascot-wallet.png",
  cooldown: "/mascot/mascot-cooldown.png",
  thumbsup: "/mascot/mascot-thumbsup.png",
  trio: "/mascot/mascot-trio.png",
  phoneList: "/mascot/mascot-phone-list.png",
  checkoutPhone: "/mascot/mascot-checkout-phone.png",
  combo: "/mascot/mascot-combo.png",
} as const;

function GhostMascot({ pose = "wave", className = "" }: { pose?: keyof typeof MASCOT_POSES; className?: string }) {
  return <img className={`ghost-mascot ${className}`} src={MASCOT_POSES[pose]} alt="" aria-hidden="true" />;
}

const ICON_PATHS: Record<string, string> = {
  shield: "M12 3l7 3v6c0 5-3.5 8-7 9-3.5-1-7-4-7-9V6l7-3z",
  lock: "M6 11V8a6 6 0 0 1 12 0v3M5 11h14v9H5z",
  leaf: "M5 19c8 0 14-6 14-14-8 0-14 6-14 14zM5 19c0-4 2-7 5-9",
  cartHeart: "M3 4h2l2.4 12.4A2 2 0 0 0 9.36 18H18a2 2 0 0 0 1.94-1.5L21 8H6M9 21a1 1 0 1 0 0-2 1 1 0 0 0 0 2zm9 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2z",
  cardX: "M3 6h18v12H3zM3 10h18M15 14l3 3m0-3l-3 3",
  boxX: "M3 8l9-4 9 4-9 4-9-4zm0 0v9l9 4 9-4V8M9 15l3-3 3 3",
  chart: "M4 20V10M11 20V4M18 20v-7",
  walletCheck: "M3 7h15a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h11M15 12l1.5 1.5L19 11",
  brain: "M9 3a3 3 0 0 0-3 3 3 3 0 0 0-2 5 3 3 0 0 0 2 5h.5A3.5 3.5 0 0 0 10 12.5V6a3 3 0 0 0-1-3zM15 3a3 3 0 0 1 3 3 3 3 0 0 1 2 5 3 3 0 0 1-2 5h-.5A3.5 3.5 0 0 1 14 12.5V6a3 3 0 0 1 1-3zM12 6v10",
  wallet: "M3 7a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7zM16 12h.01",
  drag: "M8 11V5a2 2 0 0 1 4 0v6M12 8V5a2 2 0 0 1 4 0v7M16 9a2 2 0 0 1 4 0v5c0 4-3 7-7 7h-1c-3 0-5-1-7-4l-2-3a2 2 0 0 1 3-2l2 2",
  cool: "M12 2v20M4.2 6.5l15.6 11M4.2 17.5l15.6-11M9 4l3 3 3-3M9 20l3-3 3 3M4 10l4 1-1 4M20 14l-4-1 1-4",
  sparkle: "M12 3l1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5L12 3zM18 15l.8 2.2L21 18l-2.2.8L18 21l-.8-2.2L15 18l2.2-.8L18 15z",
};

function Icon({ name }: { name: keyof typeof ICON_PATHS }) {
  return (
    <svg viewBox="0 0 24 24" className="icon-glyph" aria-hidden="true" focusable="false">
      <path d={ICON_PATHS[name]} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function PhoneMockup({
  variant = "list",
  className = "",
  onCheckout,
}: {
  variant?: "list" | "welcome";
  className?: string;
  onCheckout?: () => void;
}) {
  return (
    <div className={`phone-shell ${className}`}>
      <div className="phone-top">
        <span>9:41</span>
        <span className="phone-island" />
        <span>•••</span>
      </div>
      <div className="phone-screen">
        <div className="phone-wordmark"><Wordmark /></div>
        {variant === "list" ? (
          <>
            <p className="phone-kicker">YOUR ALMOST-BUYS</p>
            <h2>Put the craving somewhere safe.</h2>
            <div className="mini-product-list">
              <div>
                <span className="mini-shape" />
                <p><strong>White sneakers</strong><small>Added just now</small></p>
                <b>+</b>
              </div>
              <div>
                <span className="mini-shape mini-shape-dark" />
                <p><strong>Blind-buy perfume</strong><small>Cooling mode</small></p>
                <b>✓</b>
              </div>
            </div>
            <button type="button" className="phone-cta" onClick={onCheckout}>Fake Checkout</button>
            <small className="phone-disclaimer">Simulation only · no real payment</small>
          </>
        ) : (
          <>
            <p className="phone-kicker">GOOD EVENING, GHOSTER</p>
            <h2>Today&rsquo;s focus: stay present.</h2>
            <div className="phone-wallet-chip">
              <GhostMascot pose="wallet" className="phone-wallet-mascot" />
              <span>Ghost Wallet</span>
              <strong>Protected amount this week</strong>
            </div>
            <small className="phone-disclaimer">Simulated wallet · not a bank account</small>
          </>
        )}
      </div>
    </div>
  );
}

export default function Home() {
  const [cartIds, setCartIds] = useState<string[]>([]);
  const [ghostedIds, setGhostedIds] = useState<string[]>([]);
  const [cooledIds, setCooledIds] = useState<string[]>([]);
  const [receiptVisible, setReceiptVisible] = useState(false);
  const [focusMode, setFocusMode] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const holdTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const cartProducts = useMemo(
    () => DEMO_PRODUCTS.filter((product) => cartIds.includes(product.id)),
    [cartIds],
  );

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) entry.target.classList.add("is-visible");
        });
      },
      { threshold: 0.14 },
    );

    document.querySelectorAll<HTMLElement>("[data-reveal]").forEach((element) => observer.observe(element));
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setFocusMode(false);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  const addToCart = (id: string) => {
    setReceiptVisible(false);
    setCartIds((current) => (current.includes(id) ? current : [...current, id]));
  };

  const removeFromCart = (id: string) => {
    setCartIds((current) => current.filter((item) => item !== id));
  };

  const beginCooling = (event: PointerEvent<HTMLButtonElement>, id: string) => {
    event.currentTarget.setPointerCapture(event.pointerId);
    holdTimer.current = setTimeout(() => {
      setCooledIds((current) => (current.includes(id) ? current : [...current, id]));
      setCartIds((current) => current.filter((item) => item !== id));
    }, 900);
  };

  const cancelCooling = () => {
    if (holdTimer.current) clearTimeout(holdTimer.current);
    holdTimer.current = null;
  };

  const beginDrag = (event: DragEvent<HTMLElement>, id: string) => {
    event.dataTransfer.setData("text/plain", id);
    event.dataTransfer.effectAllowed = "copy";
  };

  const dropIntoGhostCart = (event: DragEvent<HTMLElement>) => {
    event.preventDefault();
    const id = event.dataTransfer.getData("text/plain");
    if (DEMO_PRODUCTS.some((product) => product.id === id)) addToCart(id);
  };

  const fakeCheckout = () => {
    if (!cartIds.length) return;
    setGhostedIds((current) => Array.from(new Set([...current, ...cartIds])));
    setCartIds([]);
    setReceiptVisible(true);
  };

  const submitWaitlist = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const email = String(formData.get("email") ?? "");
    window.localStorage.setItem("ghost-cart-preview-email", email);
    setSubmitted(true);
  };

  return (
    <main id="top" className={focusMode ? "focus-mode" : ""}>
      <nav className="site-nav" aria-label="Primary navigation">
        <a href="#top" className="nav-brand"><Wordmark inverted /></a>
        <div className="nav-links">
          <a href="#how">How it works</a>
          <a href="#why">Features</a>
          <a href="#waitlist">Coming soon</a>
          <a href="#faq">FAQ</a>
        </div>
        <a className="button button-small button-light" href="#waitlist">Join waitlist</a>
      </nav>

      <section className="hero section-dark" aria-labelledby="hero-title">
        <div className="hero-grain" aria-hidden="true" />
        <div className="hero-copy" data-reveal>
          <p className="eyebrow eyebrow-dark">For everything you almost bought.</p>
          <h1 id="hero-title">
            <span>Add to cart.</span>
            <span>Checkout.</span>
            <span className="accent-text">Keep your money.</span>
          </h1>
          <p className="hero-lede">The fake checkout app for everything you almost bought—giving cravings somewhere to go without turning them into real payments.</p>
          <div className="hero-actions">
            <a className="button button-primary" href="#demo">Try the demo</a>
            <a className="text-link" href="#how">See how it works <span aria-hidden="true">↓</span></a>
          </div>
          <div className="safety-row" aria-label="Safety information">
            <span>Simulation only</span><span>No real payment</span><span>No real delivery</span>
          </div>
        </div>

        <div className="hero-stage" aria-label="Ghost Cart product preview" data-reveal>
          <div className="orbit orbit-one" aria-hidden="true" />
          <div className="orbit orbit-two" aria-hidden="true" />
          <div className="floating-card floating-card-one">
            <img className="product-shape shape-shoe" src="/products/sneaker.png" alt="" aria-hidden="true" />
            <p>Late-night find</p><strong>Still want it tomorrow?</strong>
          </div>
          <div className="floating-card floating-card-two">
            <img className="product-shape shape-bottle" src="/products/perfume.png" alt="" aria-hidden="true" />
            <p>Impulse paused</p><strong>Cooling for 24 hours</strong>
          </div>
          <PhoneMockup
            variant="list"
            className="hero-phone"
            onCheckout={() => document.querySelector("#demo")?.scrollIntoView({ behavior: "smooth" })}
          />
          <GhostMascot pose="cart" className="hero-mascot" />
          <div className="hero-stamp"><span>THE FEELING</span><strong>without the spending</strong></div>
          <div className="hero-toast" role="status">
            <span className="hero-toast-check" aria-hidden="true">✓</span>
            <div><strong>Checkout complete</strong><p>Your money stays right where it should.</p></div>
          </div>
        </div>
      </section>

      <section id="how" className="how section-light">
        <div className="how-intro" data-reveal>
          <div className="how-copy">
            <p className="eyebrow">How it works</p>
            <h2>Three steps to<br /><em>outsmart impulse.</em></h2>
            <p>Simulate the purchase, satisfy the urge, and avoid the spend.</p>
          </div>
          <div className="how-phone-wrap">
            <PhoneMockup variant="list" className="how-phone" />
            <GhostMascot pose="wave" className="how-mascot" />
          </div>
        </div>

        <div className="how-steps" data-reveal>
          <article className="how-step">
            <span className="step-badge">1</span>
            <h3>Add to cart.</h3>
            <p>Add anything you want.<br />No rules.</p>
            <div className="step-art art-sneakers" aria-hidden="true"><img src="/products/sneaker.png" alt="" /></div>
          </article>
          <span className="step-arrow" aria-hidden="true">→</span>
          <article className="how-step how-step-dark">
            <span className="step-badge">2</span>
            <h3>Checkout.</h3>
            <p>Run a fake checkout.<br />Feel the satisfaction.</p>
            <div className="step-art art-checklist" aria-hidden="true">
              <span>Shipping<i>✓</i></span><span>Payment<i>✓</i></span><span>Review<i>✓</i></span>
            </div>
          </article>
          <span className="step-arrow" aria-hidden="true">→</span>
          <article className="how-step how-step-mint">
            <span className="step-badge">3</span>
            <h3>Keep your money.</h3>
            <p>The urge is satisfied.<br />Your money stays yours.</p>
            <div className="step-art art-smile" aria-hidden="true">☺</div>
          </article>
        </div>

        <div className="how-feature-strip" data-reveal>
          <div><Icon name="shield" /><div><strong>Simulation only</strong><p>Everything is simulated. No real transactions.</p></div></div>
          <div><Icon name="lock" /><div><strong>Private &amp; safe</strong><p>Your data stays private. We never charge.</p></div></div>
          <div><Icon name="leaf" /><div><strong>Better habits</strong><p>Make mindful choices. Build financial freedom.</p></div></div>
        </div>
      </section>

      <section id="demo" className={`demo section-dark ${focusMode ? "demo-focused" : ""}`} aria-labelledby="demo-title">
        <div className="demo-background-word" aria-hidden="true">GHOST IT</div>
        <div className="demo-experience" data-reveal>
          <aside className="demo-instruction-rail" aria-label="Demo instructions">
            <p className="eyebrow eyebrow-dark">Try the demo</p>
            <h2 id="demo-title">Ghost it.<br /><em>Don&rsquo;t</em> buy it.</h2>
            <p className="demo-intro">Move a temptation into Ghost Cart, close the loop with Fake Checkout, and leave the real purchase behind.</p>

            <div className="demo-legend">
              <div><span><Icon name="drag" /></span><p><strong>Drag into Ghost Cart</strong><small>Drop an item into the portal</small></p></div>
              <div><span><Icon name="cool" /></span><p><strong>Hold to cool down</strong><small>Press for a calmer decision</small></p></div>
              <div><span><Icon name="sparkle" /></span><p><strong>Double-click to ghost it</strong><small>Or use the visible Ghost it button</small></p></div>
            </div>

            <div className="cooling-visual" aria-hidden="true">
              <GhostMascot pose="cooldown" className="demo-mascot" />
              <div className="cooling-ring"><span><Icon name="cool" /></span></div>
              <p>Cooling down…</p>
            </div>

            <button type="button" className="focus-toggle" onClick={() => setFocusMode((current) => !current)} aria-pressed={focusMode}>{focusMode ? "Exit focus" : "Explore in focus"}</button>
            <div className="demo-safety-note"><Icon name="shield" /><p><strong>This is a simulation.</strong><small>No real payment. No real delivery.</small></p></div>
          </aside>

          <div className="demo-browser" aria-label="Interactive Ghost Cart browser demo">
            <div className="demo-browser-bar" aria-hidden="true"><span /><span /><span /><b>Ghost Cart · interactive preview</b></div>
            <div className="demo-browser-body">
              <section className="demo-catalog" aria-labelledby="browse-temptation-title">
                <header><div><p className="eyebrow eyebrow-dark">Browse temptation</p><h3 id="browse-temptation-title">Choose an almost-buy.</h3></div><span>Drag, hold, double-click, or tap</span></header>
                <div className="product-grid" aria-label="Demo almost-buys">
                  {DEMO_PRODUCTS.map((product) => {
                    const inCart = cartIds.includes(product.id);
                    const cooled = cooledIds.includes(product.id);
                    const ghosted = ghostedIds.includes(product.id);
                    return (
                      <article
                        className={`demo-product ${product.tone} ${inCart ? "is-selected" : ""}`}
                        key={product.id}
                        draggable
                        onDragStart={(event) => beginDrag(event, product.id)}
                        onDoubleClick={() => addToCart(product.id)}
                      >
                        <div className={`demo-product-art art-${product.id}`} aria-hidden="true">
                          {product.image ? <img src={product.image} alt="" /> : <span />}
                        </div>
                        <p className="demo-category">{product.category}</p>
                        <h3>{product.name}</h3>
                        <p>{product.note}</p>
                        <div className="product-status" aria-live="polite">
                          {cooled ? "Cooling mode active" : ghosted ? "Ghosted successfully" : inCart ? "In your Ghost Cart" : "Ready to ghost"}
                        </div>
                        <div className="product-actions">
                          {inCart ? <button type="button" className="product-button secondary" onClick={() => removeFromCart(product.id)}>Undo</button> : <button type="button" className="product-button" onClick={() => addToCart(product.id)}>Ghost it</button>}
                          <button type="button" className="hold-button" onPointerDown={(event) => beginCooling(event, product.id)} onPointerUp={cancelCooling} onPointerCancel={cancelCooling} onPointerLeave={cancelCooling}>Hold to cool</button>
                        </div>
                      </article>
                    );
                  })}
                </div>
              </section>

              <aside
                className={`demo-cart ${cartProducts.length ? "has-items" : ""}`}
                aria-label="Simulated Ghost Cart drop area"
                onDragOver={(event) => event.preventDefault()}
                onDrop={dropIntoGhostCart}
              >
                <div className="cart-topline"><Wordmark inverted /><span>{cartProducts.length} item{cartProducts.length === 1 ? "" : "s"}</span></div>
                {!receiptVisible ? (
                  <>
                    <div className="ghost-portal" aria-hidden="true"><span className="portal-ring portal-ring-one" /><span className="portal-ring portal-ring-two" /><GhostMascot pose="waveAlt" className="portal-mascot" /></div>
                    <div className="cart-copy"><p className="eyebrow eyebrow-dark">Your Ghost Cart</p><h3>{cartProducts.length ? "The urge has somewhere to go." : "Drag items here."}</h3><p>{cartProducts.length ? "Nothing here will be purchased or delivered." : "Or use any visible Ghost it button."}</p></div>
                    <div className="cart-items">
                      {cartProducts.map((product) => <div key={product.id}><span className={`cart-item-shape ${product.tone}`} /><p><strong>{product.name}</strong><small>Simulated item</small></p><button type="button" onClick={() => removeFromCart(product.id)} aria-label={`Remove ${product.name}`}>×</button></div>)}
                    </div>
                    <div className="cart-summary"><span>Real amount charged</span><strong>Zero</strong></div>
                    <button type="button" className="button button-primary button-full" disabled={!cartProducts.length} onClick={fakeCheckout}>Complete Fake Checkout</button>
                    <small>Simulation only. No payment details. No delivery.</small>
                  </>
                ) : (
                  <div className="receipt-card" role="status">
                    <GhostMascot pose="thumbsup" className="receipt-mascot" />
                    <p className="eyebrow eyebrow-dark">Ghost Receipt</p>
                    <h3>Nothing purchased.<br />Craving completed.</h3>
                    <div className="receipt-rule" />
                    <p>{ghostedIds.length} almost-buy{ghostedIds.length === 1 ? "" : "s"} ghosted in this demo.</p>
                    <strong>Real amount charged: Zero</strong>
                    <button type="button" className="button button-outline" onClick={() => setReceiptVisible(false)}>Ghost another cart</button>
                    <small>Not an invoice or proof of purchase.</small>
                  </div>
                )}
              </aside>
            </div>
          </div>

          <aside className="almost-bought-rail" aria-label="Example almost-bought list">
            <header><div><p className="eyebrow eyebrow-dark">Almost bought</p><h3>Your demo list</h3></div><span>{DEMO_PRODUCTS.length}</span></header>
            <p>Everything you wanted. Zero commitment.</p>
            <div className="almost-bought-list">
              {DEMO_PRODUCTS.map((product) => {
                const status = ghostedIds.includes(product.id) ? "Ghosted" : cooledIds.includes(product.id) ? "Cooling" : cartIds.includes(product.id) ? "In Ghost Cart" : "Ready";
                return <div key={product.id}><span className={`almost-thumb art-${product.id}`}>{product.image ? <img src={product.image} alt="" /> : <span />}</span><p><strong>{product.name}</strong><small>{status}</small></p></div>;
              })}
            </div>
            <a href="#stories">Explore example moments <span aria-hidden="true">↗</span></a>
          </aside>
        </div>
        <p className="demo-pro-tip"><span aria-hidden="true">✦</span> Pro tip: use the cooldown before an impulse becomes a purchase.</p>
      </section>

      <section id="why" className="why section-light">
        <div className="why-orbit" aria-hidden="true" />
        <div className="section-heading why-heading" data-reveal>
          <p className="eyebrow">Control the craving. Keep the cash.</p>
          <h2>Why <em>Ghost Cart</em><br />works.</h2>
          <p>Ghost Cart gives you the satisfaction of checkout without the consequences. It&rsquo;s self-control, made simple.</p>
        </div>

        <div className="benefit-grid" data-reveal>
          <article className="benefit-card"><Icon name="cartHeart" /><h3>Satisfy the urge</h3><p>Get the rush without the regret. Add, checkout, feel better.</p></article>
          <article className="benefit-card"><Icon name="cardX" /><h3>No real payment</h3><p>No charges. No bills. Just the feeling of getting it.</p></article>
          <article className="benefit-card"><Icon name="boxX" /><h3>No real delivery</h3><p>Nothing shows up at your door. Just peace of mind.</p></article>
          <article className="benefit-card"><Icon name="chart" /><h3>Track almost-buys</h3><p>See your patterns. Understand your habits. Stay in control.</p></article>
          <article className="benefit-card"><Icon name="walletCheck" /><h3>Protect your salary</h3><p>Keep your money for what matters. Future you will thank you.</p></article>
          <article className="benefit-card"><Icon name="brain" /><h3>Cool off impulse</h3><p>Give yourself space. Most urges fade in 24 hours.</p></article>
        </div>

        <div className="comparison-panel" data-reveal>
          <div className="comparison-row">
            <div className="comparison-label impulse-label">Impulse buy <span aria-hidden="true">✗</span></div>
            <ol className="comparison-steps">
              <li><Icon name="cartHeart" /><span>See something you want</span></li>
              <li><Icon name="cardX" /><span>Pay without thinking</span></li>
              <li><Icon name="boxX" /><span>Wait for it to arrive</span></li>
              <li><span className="comparison-glyph" aria-hidden="true">☹</span><span>Feel guilty afterwards</span></li>
              <li><Icon name="wallet" /><span>Money gone. Regret stays.</span></li>
            </ol>
          </div>
          <div className="comparison-vs" aria-hidden="true">VS</div>
          <div className="comparison-row comparison-row-ghost">
            <div className="comparison-label ghost-label">Ghost it <span aria-hidden="true">✓</span></div>
            <ol className="comparison-steps">
              <li><Icon name="cartHeart" /><span>See something you want</span></li>
              <li><GhostMascot pose="cart" className="comparison-ghost-icon" /><span>Ghost Cart checkout</span></li>
              <li><GhostMascot pose="thumbsup" className="comparison-ghost-icon" /><span>Feel the rush. Skip the cost.</span></li>
              <li><span className="comparison-glyph" aria-hidden="true">☺</span><span>Feel in control. Move on.</span></li>
              <li><Icon name="walletCheck" /><span>Money saved. You win.</span></li>
            </ol>
          </div>
        </div>

        <div className="benefit-strip" data-reveal>
          <span>Closure without checkout</span><span>A home for abandoned carts</span><span>Cooling space for cravings</span><span>Progress without guilt</span>
        </div>
      </section>

      <section id="community" className="insights section-dark" aria-labelledby="insights-title">
        <div className="section-heading insights-heading" data-reveal>
          <div><p className="eyebrow eyebrow-dark">Community &amp; dashboard</p><h2 id="insights-title">You&rsquo;re not<br /><em>alone</em> in this.</h2></div>
          <p>Ghost Cart can turn scattered almost-buys into a calmer picture of what tempted you, when it happened, and what you let go.</p>
        </div>
        <div className="community-layout" data-reveal>
          <div className="dashboard-wrap">
            <div className="dashboard">
              <div className="dashboard-sidebar"><Wordmark inverted /><nav aria-label="Sample dashboard"><span className="active">Overview</span><span>Almost-buys</span><span>Ghost Wallet</span><span>Receipts</span></nav><small>DEMO SNAPSHOT</small></div>
              <div className="dashboard-main">
                <header><div><p>Good evening</p><h3>Your control dashboard</h3></div><span className="avatar-placeholder">YOU</span></header>
                <div className="dashboard-grid">
                  <article className="metric-card metric-primary"><p>Weekly ghost receipt</p><strong>{Math.max(ghostedIds.length, DASHBOARD_DEMO_DATA.minimumGhostedCount)}</strong><span>Almost-buys ghosted this session</span><div className="metric-rings" /></article>
                  <article className="metric-card"><p>Top almost-buys</p><strong>{DASHBOARD_DEMO_DATA.topPattern}</strong><span>Example pattern</span></article>
                  <article className="metric-card chart-card"><p>Protected this week</p><div className="bar-chart" aria-label="Sample chart showing a rising protected amount">{DASHBOARD_DEMO_DATA.protectedBars.map((height, index) => <i key={`${height}-${index}`} style={{height:`${height}%`}} />)}</div><span>Sample data · not a user claim</span></article>
                  <article className="metric-card streak-card"><p>Cooling mode streak</p><strong>{DASHBOARD_DEMO_DATA.coolingStreakDays} days</strong><span>Keep the streak alive</span></article>
                  <article id="dashboard-charts" className="metric-card cravings-card">
                    <p>Cravings by category</p>
                    <div className="donut-wrap">
                      <div className="donut-chart" aria-label="Sample chart showing craving categories" role="img" />
                      <ul className="donut-legend">
                        {DASHBOARD_DEMO_DATA.categories.map((category) => <li key={category.label}><i className={`legend-dot ${category.dotClass}`} aria-hidden="true" />{category.label}<b>{category.share}%</b></li>)}
                      </ul>
                    </div>
                    <span>Sample data · not a user claim</span>
                  </article>
                  <article className="metric-card mood-card">
                    <p>Your weekly mood &amp; mindset</p>
                    <svg className="mood-chart" viewBox="0 0 280 90" aria-label="Sample chart showing mood trending upward across the week" role="img">
                      <polyline points={DASHBOARD_DEMO_DATA.mood.points} />
                      <circle cx={DASHBOARD_DEMO_DATA.mood.finalPoint.x} cy={DASHBOARD_DEMO_DATA.mood.finalPoint.y} r="4" />
                    </svg>
                    <div className="mood-axis">{DASHBOARD_DEMO_DATA.mood.days.map((day) => <span key={day}>{day}</span>)}</div>
                    <span>Sample data · not a user claim</span>
                  </article>
                </div>
              </div>
            </div>
            <PhoneMockup variant="welcome" className="community-phone" />
          </div>
          <aside className="community-feed" aria-label="Sample community feed">
            <div className="community-feed-head"><span>Community feed</span></div>
            <p className="community-feed-caption">Illustrative posts, not real user testimonials.</p>
            <ul>
              <li><span className="feed-avatar">A</span><div><strong>Alex</strong><small>Sample post</small><p>Ghosted the whole cart. Feels like a win.</p></div></li>
              <li><span className="feed-avatar">M</span><div><strong>Maya</strong><small>Sample post</small><p>The late-night reset challenge is a game changer.</p></div></li>
              <li><span className="feed-avatar">J</span><div><strong>Jordan</strong><small>Sample post</small><p>3 days in a row. Let&rsquo;s go!</p></div></li>
            </ul>
          </aside>
        </div>
      </section>

      <section id="stories" className="stories section-light" aria-labelledby="stories-title">
        <div className="section-heading stories-heading" data-reveal>
          <div><p className="eyebrow">Stories / Almost-buy moments</p><h2 id="stories-title">The things we want<br /><em>for five minutes.</em></h2></div>
          <p>These are illustrative scenarios, not customer testimonials. The moments are familiar because the impulse usually is.</p>
        </div>
        <div className="story-collage" data-reveal>
          <article className="story-card story-large"><span className="story-time">11:47 PM</span><div className="story-art story-food"><img src="/mascot/mascot-combo.png" alt="" /></div><h3>“I built the whole order. Then realized I was just bored.”</h3><p>Delivery craving · Ghosted before checkout</p><span className="story-persona">Midnight Browser · sample persona</span></article>
          <article className="story-card story-tall"><span className="story-time">PAYDAY + 2H</span><div className="story-art story-fashion"><img src="/products/sneaker.png" alt="" /></div><h3>“The sale felt urgent. The item didn’t.”</h3><p>Fashion cart · Cooling mode</p><span className="story-persona">Mindful Minimalist · sample persona</span></article>
          <article className="story-card story-quote"><p>YOUR REAL CARTS ARE FULL FOR A REASON.</p><h3>Give every “maybe” one quiet place to wait.</h3><span>Almost-Bought Archive</span></article>
          <article className="story-card story-wide"><span className="story-time">DAY 2</span><div><p className="eyebrow">The next-day test</p><h3>Still want it—or was it just the scroll?</h3></div><button type="button" onClick={() => document.querySelector("#demo")?.scrollIntoView({behavior:"smooth"})}>Try the demo</button></article>
        </div>
        <div className="story-callout-strip" data-reveal>
          <span className="story-callout-lead">Join our early community</span>
          <span><Icon name="brain" />Pause impulsive purchases</span>
          <span><Icon name="lock" />Protect your money &amp; peace of mind</span>
          <span><Icon name="leaf" />Build smarter shopping habits</span>
          <a className="button button-primary" href="#waitlist">Join waitlist</a>
        </div>
      </section>

      <section id="faq" className="faq section-light" aria-labelledby="faq-title">
        <div className="faq-intro" data-reveal>
          <p className="eyebrow">No tricks in the fine print</p>
          <h2 id="faq-title">Your questions,<br /><em>answered clearly.</em></h2>
          <p>Ghost Cart feels like shopping. It never pretends to be a real store, bank, payment card, or delivery service.</p>
          <img className="faq-decor faq-decor-sneaker" src="/products/sneaker.png" alt="" aria-hidden="true" />
          <img className="faq-decor faq-decor-perfume" src="/products/perfume.png" alt="" aria-hidden="true" />
          <GhostMascot pose="waveAlt" className="faq-decor faq-decor-mascot" />
        </div>
        <div className="faq-column" data-reveal>
          <div className="faq-list">
            {FAQS.map(([question, answer], index) => <details key={question} open={index === 0}><summary>{question}<span aria-hidden="true">+</span></summary><p>{answer}</p></details>)}
          </div>
          <p className="faq-contact">Still have questions? We&rsquo;re ghosts, not mind readers. <a href="#waitlist">Drop us a message</a></p>
        </div>
      </section>

      <div className="cta-footer-fusion">
        <section id="waitlist" className="waitlist section-dark" aria-labelledby="waitlist-title">
          <div className="waitlist-line line-one" aria-hidden="true" /><div className="waitlist-line line-two" aria-hidden="true" />
          <div className="waitlist-panel" data-reveal>
            <p className="eyebrow eyebrow-dark">Coming soon</p>
            <h2 id="waitlist-title">Ghost your cravings<br /><em>before they cost you.</em></h2>
            <p>Ghost Cart is the fake checkout app for everything you almost bought. Coming soon.</p>
            {!submitted ? (
              <form className="waitlist-form" onSubmit={submitWaitlist}>
                <label htmlFor="email">Email address</label>
                <div><input id="email" name="email" type="email" inputMode="email" autoComplete="email" placeholder="you@example.com" required /><button type="submit">Join waitlist</button></div>
                <small>For this development preview, your email is saved only on this device. Live signup is not connected yet.</small>
              </form>
            ) : (
              <div className="waitlist-success" role="status"><span aria-hidden="true">✓</span><div><strong>Preview saved.</strong><p>Live waitlist connection is the next build step.</p></div></div>
            )}
            <div className="safety-row safety-row-center" aria-label="Safety information">
              <span>Simulation only</span><span>No real payment</span><span>No real delivery</span>
            </div>
            <div className="platform-row" aria-label="Planned platforms">
              <span className="platform-lead">Coming soon on</span>
              <span className="platform-chip">App Store</span>
              <span className="platform-chip">Google Play</span>
              <span className="platform-chip">Web App</span>
            </div>
          </div>
        </section>

        <footer className="site-footer section-dark">
          <div className="footer-curve footer-curve-one" aria-hidden="true" /><div className="footer-curve footer-curve-two" aria-hidden="true" />
          <img className="footer-badge" src="/brand/fake-checkout-real-control-badge.png" alt="" aria-hidden="true" />
          <div className="footer-main">
            <div className="footer-brand"><Wordmark inverted /><h2>Fake checkout.<br /><span>Real control.</span></h2><p>Save your cravings for later.<br />Keep your money now.</p></div>
            <div className="footer-links"><div><strong>Explore</strong><a href="#how">How it works</a><a href="#demo">Try the demo</a><a href="#why">Why Ghost Cart</a></div><div><strong>Information</strong><a href="#faq">FAQ</a><span>Privacy · coming soon</span><span>Terms · coming soon</span></div><div><strong>Social</strong><span>Instagram · coming soon</span><span>TikTok · coming soon</span><span>LinkedIn · coming soon</span></div></div>
          </div>
          <div className="footer-bottom"><span>© 2026 Ghost Cart</span><p>Simulation only. No real payment. No real delivery.</p><a href="#top">Back to top ↑</a></div>
        </footer>
      </div>
    </main>
  );
}
