"use client";

import { FormEvent, PointerEvent, useEffect, useMemo, useRef, useState } from "react";

type DemoProduct = {
  id: string;
  name: string;
  note: string;
  category: string;
  tone: string;
};

const DEMO_PRODUCTS: DemoProduct[] = [
  { id: "sneakers", name: "The white sneakers", note: "Saved after a late-night scroll", category: "Fashion", tone: "tone-ice" },
  { id: "perfume", name: "The blind-buy perfume", note: "A review made it feel urgent", category: "Beauty", tone: "tone-smoke" },
  { id: "burger", name: "The midnight combo", note: "Built from boredom, not hunger", category: "Delivery", tone: "tone-paper" },
  { id: "headphones", name: "The extra headphones", note: "A deal you did not need yesterday", category: "Tech", tone: "tone-mint" },
];

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
      <span className="wordmark-dot" aria-hidden="true" />
      <span>Ghost Cart</span>
    </span>
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
          <a href="#demo">Try it</a>
          <a href="#why">Why Ghost Cart</a>
          <a href="#faq">FAQ</a>
        </div>
        <a className="button button-small button-light" href="#waitlist">Coming soon</a>
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
            <a className="button button-primary" href="#demo">Try the experience</a>
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
            <span className="product-shape shape-shoe" aria-hidden="true" />
            <p>Late-night find</p><strong>Still want it tomorrow?</strong>
          </div>
          <div className="floating-card floating-card-two">
            <span className="product-shape shape-bottle" aria-hidden="true" />
            <p>Impulse paused</p><strong>Cooling for 24 hours</strong>
          </div>
          <div className="phone-shell hero-phone">
            <div className="phone-top"><span>9:41</span><span className="phone-island" /><span>•••</span></div>
            <div className="phone-screen">
              <div className="phone-wordmark"><Wordmark /></div>
              <p className="phone-kicker">YOUR ALMOST-BUYS</p>
              <h2>Put the craving somewhere safe.</h2>
              <div className="mini-product-list">
                <div><span className="mini-shape" /><p><strong>White sneakers</strong><small>Added just now</small></p><b>+</b></div>
                <div><span className="mini-shape mini-shape-dark" /><p><strong>Blind-buy perfume</strong><small>Cooling mode</small></p><b>✓</b></div>
              </div>
              <button type="button" className="phone-cta" onClick={() => document.querySelector("#demo")?.scrollIntoView({ behavior: "smooth" })}>Fake Checkout</button>
              <small className="phone-disclaimer">Simulation only · no real payment</small>
            </div>
          </div>
          <div className="hero-stamp"><span>THE FEELING</span><strong>without the spending</strong></div>
        </div>
      </section>

      <section id="how" className="how section-light">
        <div className="section-heading split-heading" data-reveal>
          <div><p className="eyebrow">The ritual, redirected</p><h2>Three steps to<br /><em>outsmart impulse.</em></h2></div>
          <p>Ghost Cart keeps the familiar shopping rhythm, then changes the ending. You get closure. Your real cart stays untouched.</p>
        </div>
        <div className="how-grid">
          <article className="how-step how-step-large" data-reveal>
            <span className="step-index">01</span>
            <div className="editorial-object object-orbit" aria-hidden="true"><span /></div>
            <div><p className="eyebrow">Catch the craving</p><h3>Add the almost-buy.</h3><p>Move an item out of a real cart, or choose a generic temptation in the Ghost Cart demo.</p></div>
          </article>
          <article className="how-step how-step-tall" data-reveal>
            <span className="step-index">02</span>
            <div className="editorial-object object-button" aria-hidden="true"><span>Fake Checkout</span></div>
            <div><p className="eyebrow">Close the loop</p><h3>Checkout the feeling.</h3><p>Complete the ritual without card fields, payment processing, or an order.</p></div>
          </article>
          <article className="how-step how-step-wide" data-reveal>
            <span className="step-index">03</span>
            <div><p className="eyebrow">Keep control</p><h3>Let the urge fade.<br />Keep your money.</h3><p>Finish with a Ghost Receipt—never an invoice or proof of purchase.</p></div>
            <div className="control-meter" aria-label="Illustrative craving cooling meter"><span>Craving</span><i><b /></i><span>Control</span></div>
          </article>
        </div>
      </section>

      <section id="demo" className={`demo section-dark ${focusMode ? "demo-focused" : ""}`} aria-labelledby="demo-title">
        <div className="demo-background-word" aria-hidden="true">GHOST IT</div>
        <div className="section-heading demo-heading" data-reveal>
          <div><p className="eyebrow eyebrow-dark">A working browser preview</p><h2 id="demo-title">Try the feeling.<br /><em>Skip the spending.</em></h2></div>
          <div className="demo-heading-actions">
            <p>Click “Ghost it,” double-click a card, or press and hold to cool it down.</p>
            <button type="button" className="focus-toggle" onClick={() => setFocusMode((current) => !current)} aria-pressed={focusMode}>{focusMode ? "Exit focus" : "Explore section"}</button>
          </div>
        </div>

        <div className="demo-layout" data-reveal>
          <div className="product-grid" aria-label="Demo almost-buys">
            {DEMO_PRODUCTS.map((product) => {
              const inCart = cartIds.includes(product.id);
              const cooled = cooledIds.includes(product.id);
              const ghosted = ghostedIds.includes(product.id);
              return (
                <article className={`demo-product ${product.tone} ${inCart ? "is-selected" : ""}`} key={product.id} onDoubleClick={() => addToCart(product.id)}>
                  <div className={`demo-product-art art-${product.id}`} aria-hidden="true"><span /></div>
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

          <aside className="demo-cart" aria-label="Simulated Ghost Cart">
            <div className="cart-topline"><Wordmark inverted /><span>{cartProducts.length} item{cartProducts.length === 1 ? "" : "s"}</span></div>
            {!receiptVisible ? (
              <>
                <div className="cart-copy"><p className="eyebrow eyebrow-dark">Your Ghost Cart</p><h3>{cartProducts.length ? "The urge has somewhere to go." : "Add an almost-buy."}</h3><p>{cartProducts.length ? "Nothing here will be purchased or delivered." : "Choose any card to begin the simulation."}</p></div>
                <div className="cart-items">
                  {cartProducts.map((product) => <div key={product.id}><span className={`cart-item-shape ${product.tone}`} /><p><strong>{product.name}</strong><small>Simulated item</small></p><button type="button" onClick={() => removeFromCart(product.id)} aria-label={`Remove ${product.name}`}>×</button></div>)}
                </div>
                <div className="cart-summary"><span>Real amount charged</span><strong>Zero</strong></div>
                <button type="button" className="button button-primary button-full" disabled={!cartProducts.length} onClick={fakeCheckout}>Complete Fake Checkout</button>
                <small>Simulation only. No payment details. No delivery.</small>
              </>
            ) : (
              <div className="receipt-card" role="status">
                <div className="receipt-mark" aria-hidden="true">✓</div>
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
      </section>

      <section id="why" className="why section-light">
        <div className="why-orbit" aria-hidden="true" />
        <div className="section-heading why-heading" data-reveal>
          <p className="eyebrow">The ending changes everything</p>
          <h2>Shopping apps ask,<br /><em>“Why wait?”</em></h2>
          <p>Ghost Cart asks a better question: <strong>“Will you still want it tomorrow?”</strong></p>
        </div>
        <div className="comparison" data-reveal>
          <article className="comparison-side impulse-side"><span>Impulse</span><h3>See it.<br />Buy it.<br />Regret it.</h3><div className="comparison-path"><i /><i /><i /></div><p>The cart decides the pace.</p></article>
          <div className="comparison-hinge"><span>or</span></div>
          <article className="comparison-side control-side"><span>Control</span><h3>See it.<br />Ghost it.<br />Decide later.</h3><div className="comparison-path"><i /><i /><i /></div><p>You decide the pace.</p></article>
        </div>
        <div className="benefit-strip" data-reveal>
          <span>Closure without checkout</span><span>A home for abandoned carts</span><span>Cooling space for cravings</span><span>Progress without guilt</span>
        </div>
      </section>

      <section className="insights section-dark" aria-labelledby="insights-title">
        <div className="section-heading insights-heading" data-reveal>
          <div><p className="eyebrow eyebrow-dark">Sample insight experience</p><h2 id="insights-title">Notice the pattern.<br /><em>Change the ending.</em></h2></div>
          <p>Ghost Cart can turn scattered almost-buys into a calmer picture of what tempted you, when it happened, and what you let go.</p>
        </div>
        <div className="dashboard" data-reveal>
          <div className="dashboard-sidebar"><Wordmark inverted /><nav aria-label="Sample dashboard"><span className="active">Overview</span><span>Almost-buys</span><span>Cooling hub</span><span>Receipts</span></nav><small>DEMO SNAPSHOT</small></div>
          <div className="dashboard-main">
            <header><div><p>Good evening</p><h3>Your control dashboard</h3></div><span className="avatar-placeholder">YOU</span></header>
            <div className="dashboard-grid">
              <article className="metric-card metric-primary"><p>Almost-buys ghosted</p><strong>{Math.max(ghostedIds.length, 4)}</strong><span>This demo session</span><div className="metric-rings" /></article>
              <article className="metric-card"><p>Top trigger</p><strong>Late-night scrolling</strong><span>Example pattern</span></article>
              <article className="metric-card chart-card"><p>Craving intensity</p><div className="bar-chart" aria-label="Sample chart showing lower cravings over time"><i style={{height:"82%"}}/><i style={{height:"68%"}}/><i style={{height:"76%"}}/><i style={{height:"44%"}}/><i style={{height:"28%"}}/><i style={{height:"18%"}}/></div><span>Sample data · not a user claim</span></article>
              <article className="metric-card list-card"><p>Recently released</p><ul><li><span className="list-dot"/>Delivery craving <b>Ghosted</b></li><li><span className="list-dot"/>Fashion cart <b>Cooling</b></li><li><span className="list-dot"/>Tech deal <b>Released</b></li></ul></article>
            </div>
          </div>
        </div>
      </section>

      <section className="stories section-light" aria-labelledby="stories-title">
        <div className="section-heading stories-heading" data-reveal>
          <div><p className="eyebrow">Example almost-buy moments</p><h2 id="stories-title">The things we want<br /><em>for five minutes.</em></h2></div>
          <p>These are illustrative scenarios, not customer testimonials. The moments are familiar because the impulse usually is.</p>
        </div>
        <div className="story-collage" data-reveal>
          <article className="story-card story-large"><span className="story-time">11:47 PM</span><div className="story-art story-food"><i/><i/><i/></div><h3>“I built the whole order. Then realized I was just bored.”</h3><p>Delivery craving · Ghosted before checkout</p></article>
          <article className="story-card story-tall"><span className="story-time">PAYDAY + 2H</span><div className="story-art story-fashion"><i/><i/></div><h3>“The sale felt urgent. The item didn’t.”</h3><p>Fashion cart · Cooling mode</p></article>
          <article className="story-card story-quote"><p>YOUR REAL CARTS ARE FULL FOR A REASON.</p><h3>Give every “maybe” one quiet place to wait.</h3><span>Almost-Bought Archive</span></article>
          <article className="story-card story-wide"><span className="story-time">DAY 2</span><div><p className="eyebrow">The next-day test</p><h3>Still want it—or was it just the scroll?</h3></div><button type="button" onClick={() => document.querySelector("#demo")?.scrollIntoView({behavior:"smooth"})}>Try the demo</button></article>
        </div>
      </section>

      <section id="faq" className="faq section-light" aria-labelledby="faq-title">
        <div className="faq-intro" data-reveal><p className="eyebrow">No tricks in the fine print</p><h2 id="faq-title">Your questions,<br /><em>answered clearly.</em></h2><p>Ghost Cart feels like shopping. It never pretends to be a real store, bank, payment card, or delivery service.</p></div>
        <div className="faq-list" data-reveal>
          {FAQS.map(([question, answer], index) => <details key={question} open={index === 0}><summary>{question}<span aria-hidden="true">+</span></summary><p>{answer}</p></details>)}
        </div>
      </section>

      <section id="waitlist" className="waitlist section-dark" aria-labelledby="waitlist-title">
        <div className="waitlist-line line-one" aria-hidden="true" /><div className="waitlist-line line-two" aria-hidden="true" />
        <div className="waitlist-panel" data-reveal>
          <p className="eyebrow eyebrow-dark">Coming soon</p>
          <h2 id="waitlist-title">Ghost the craving<br /><em>before it costs you.</em></h2>
          <p>Be ready when the first Ghost Cart experience opens.</p>
          {!submitted ? (
            <form className="waitlist-form" onSubmit={submitWaitlist}>
              <label htmlFor="email">Email address</label>
              <div><input id="email" name="email" type="email" inputMode="email" autoComplete="email" placeholder="you@example.com" required /><button type="submit">Join the preview list</button></div>
              <small>For this development preview, your email is saved only on this device. Live signup is not connected yet.</small>
            </form>
          ) : (
            <div className="waitlist-success" role="status"><span aria-hidden="true">✓</span><div><strong>Preview saved.</strong><p>Live waitlist connection is the next build step.</p></div></div>
          )}
        </div>
      </section>

      <footer className="site-footer section-dark">
        <div className="footer-curve footer-curve-one" aria-hidden="true" /><div className="footer-curve footer-curve-two" aria-hidden="true" />
        <div className="footer-main">
          <div className="footer-brand"><Wordmark inverted /><h2>Fake checkout.<br /><span>Real control.</span></h2><p>Save your cravings for later.<br />Keep your money now.</p></div>
          <div className="footer-links"><div><strong>Explore</strong><a href="#how">How it works</a><a href="#demo">Try the demo</a><a href="#why">Why Ghost Cart</a></div><div><strong>Information</strong><a href="#faq">FAQ</a><span>Privacy · coming soon</span><span>Terms · coming soon</span></div><div><strong>Social</strong><span>Instagram · coming soon</span><span>TikTok · coming soon</span><span>LinkedIn · coming soon</span></div></div>
        </div>
        <div className="footer-bottom"><span>© 2026 Ghost Cart</span><p>Simulation only. No real payment. No real delivery.</p><a href="#top">Back to top ↑</a></div>
      </footer>
    </main>
  );
}
