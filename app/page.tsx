import { Brand } from "./components/Brand";
import { GhostFlowDemo } from "./components/GhostFlowDemo";
import { RevealObserver } from "./components/RevealObserver";
import { SiteNav } from "./components/SiteNav";
import { WaitlistForm } from "./components/WaitlistForm";

const FLOW_STEPS = [
  { number: "01", verb: "Capture", title: "Bring the temptation in.", copy: "Paste a link, share from another app, upload a screenshot or type it yourself.", art: "capture" },
  { number: "02", verb: "Ghost", title: "Complete the emotional stop.", copy: "A short, satisfying ritual gives the urge somewhere to go—without placing an order.", art: "ghost" },
  { number: "03", verb: "Cool", title: "Match the pause to the purchase.", copy: "Thirty minutes for lunch. A day for fashion. Longer for tech, luxury and big decisions.", art: "cool" },
  { number: "04", verb: "Resolve", title: "Say what actually happened.", copy: "Skipped it, bought it intentionally, or need more time. Every answer is useful.", art: "resolve" },
  { number: "05", verb: "Confirm", title: "Count only what you kept.", copy: "Money Kept increases only after you confirm the purchase was skipped.", art: "confirm" },
] as const;

const FAQS = [
  ["Is Ghost Cart a real shopping app?", "No. Ghost Cart is a simulation and cooling-off tool for almost-buys. It never sells products or places orders."],
  ["What can I add?", "Anything tempting you—from food and fashion to gadgets, tickets and subscriptions. The planned app supports links, screenshots and manual entry."],
  ["Does Ghosting something mean I saved that money?", "Not yet. A Ghosted item begins a cooling period. It becomes confirmed Money Kept only when you later say you skipped the purchase."],
  ["What if I still buy the item?", "Log it as an intentional purchase. Your Money Kept total stays unchanged, while the decision remains part of your private progress history."],
  ["Is the Ghost membership card a payment card?", "No. It is an optional profile and achievement card with no payment credentials or financial functionality."],
  ["Will Ghost Cart remind me?", "The planned app can notify you when a cooling period ends. Meal and habit reminders are optional, individually controlled and easy to pause."],
  ["Is anything delivered?", "No. Any checkout or delivery-style moment is playful and imaginary. No real payment, purchase or delivery ever takes place."],
  ["When is Ghost Cart launching?", "Ghost Cart is currently being shaped and tested. Join the waitlist for launch updates without a promised date."],
  ["How do I get support or ask a question?", "Email info@theghostcart.com and we'll get back to you."],
] as const;

export default function Home() {
  return (
    <main id="top" className="gc-site">
      <RevealObserver />
      <SiteNav />

      <section className="gc-hero" aria-labelledby="hero-title">
        <div className="gc-hero-grid" />
        <div className="gc-hero-copy" data-gc-reveal>
          <p className="gc-kicker">The cooling-off app for almost-buys</p>
          <h1 id="hero-title">Before you buy it,<br /><em>Ghost it.</em></h1>
          <p className="gc-hero-lede">Capture the temptation from anywhere, complete the satisfying stop, give it time, then decide with a clear head.</p>
          <div className="gc-hero-actions">
            <a className="gc-button gc-button-green" href="#try-it">Try the decision flow</a>
            <a className="gc-arrow-link" href="#how-it-works">See how it works <span aria-hidden="true">↓</span></a>
          </div>
          <div className="gc-safety-line" aria-label="Product safety">
            <span>Simulation only</span><span>No real payment</span><span>No real delivery</span>
          </div>
        </div>

        <div className="gc-hero-stage" aria-label="Ghost Cart progress preview" data-gc-reveal>
          <div className="gc-hero-orbit gc-orbit-a" />
          <div className="gc-hero-orbit gc-orbit-b" />
          <div className="gc-hero-product gc-product-sneaker"><img src="/products/sneaker.png" alt="Generic white sneaker representing an almost-buy" /><span>Captured</span></div>
          <div className="gc-hero-product gc-product-perfume"><img src="/products/perfume.png" alt="Generic perfume bottle representing an almost-buy" /><span>72h pause</span></div>
          <div className="gc-hero-phone">
            <div className="gc-phone-top"><span>9:41</span><i /></div>
            <div className="gc-phone-brand"><img src="/brand/ghost-cart-icon.png" alt="" /><span>Ghost Cart</span></div>
            <p className="gc-kicker gc-kicker-dark">One active cooldown</p>
            <h2>Still want it<br />tomorrow?</h2>
            <div className="gc-phone-item">
              <img src="/products/sneaker.png" alt="" />
              <div><strong>White sneakers</strong><span>Cooling · 18h left</span></div>
              <b aria-hidden="true">•••</b>
            </div>
            <div className="gc-phone-progress"><span style={{ width: "62%" }} /></div>
            <div className="gc-phone-truth">
              <div><span>Almost spent</span><strong>549</strong></div>
              <div><span>Money Kept</span><strong>0</strong></div>
            </div>
            <span className="gc-phone-button">Resolve when ready</span>
            <small>Sample screen · no purchase made</small>
          </div>
          <img className="gc-hero-mascot" src="/mascot/mascot-cooldown.png" alt="Friendly Ghost Cart mascot waiting beside a timer" />
          <aside className="gc-hero-note"><span aria-hidden="true">✓</span><div><strong>Clarity over theatre.</strong><p>Nothing counts until you decide.</p></div></aside>
        </div>
      </section>

      <section id="download" className="gc-download" aria-labelledby="download-title">
        <div className="gc-download-card" data-gc-reveal>
          <img src="/brand/ghost-cart-icon.png" alt="" />
          <div>
            <p className="gc-kicker gc-kicker-dark">Try it for real, right now</p>
            <h2 id="download-title">Download the<br /><em>Android beta.</em></h2>
            <p>
              A real, working build — not a mockup. Direct-install APK, updated regularly, Android
              only for now (not on the Play Store yet). Your phone may ask you to allow installs
              from this source the first time.
            </p>
            <a className="gc-button gc-button-green" href="/download/android">Download for Android</a>
          </div>
        </div>
      </section>

      <section id="how-it-works" className="gc-flow-section gc-section-light" aria-labelledby="how-title">
        <header className="gc-section-heading gc-flow-heading-copy" data-gc-reveal>
          <div><p className="gc-kicker gc-kicker-dark">A better interruption</p><h2 id="how-title">One ritual.<br /><em>Five honest moments.</em></h2></div>
          <p>Ghost Cart does not force you to say no. It creates enough distance to make the next choice yours.</p>
        </header>
        <div className="gc-flow-list">
          {FLOW_STEPS.map((step, index) => (
            <article key={step.number} className={`gc-flow-row is-${step.art}`} data-gc-reveal>
              <span className="gc-flow-number">{step.number}</span>
              <p className="gc-flow-verb">{step.verb}</p>
              <div><h3>{step.title}</h3><p>{step.copy}</p></div>
              <div className="gc-flow-art" aria-hidden="true">
                {index === 0 && <><span className="gc-mini-card">LINK</span><span className="gc-mini-card">SHOT</span><span className="gc-mini-card">TYPE</span></>}
                {index === 1 && <img src="/mascot/mascot-cart.png" alt="" />}
                {index === 2 && <><span className="gc-ring"><i /></span><strong>24h</strong></>}
                {index === 3 && <><span>Skip</span><span>Buy</span><span>Wait</span></>}
                {index === 4 && <><b>Almost spent</b><i>→</i><strong>Money Kept</strong></>}
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="gc-truth-section" aria-labelledby="truth-title">
        <div className="gc-truth-dark" data-gc-reveal>
          <p className="gc-kicker">The old shortcut</p>
          <h2>Ghosted<br /><span>≠</span> saved.</h2>
          <p>A checkout simulation can cool the urge. It cannot know what you actually kept.</p>
        </div>
        <div className="gc-truth-light" data-gc-reveal>
          <p className="gc-kicker gc-kicker-dark">The honest model</p>
          <h2>Resolve first.<br /><em>Count second.</em></h2>
          <p>Almost spent, still cooling and confirmed Money Kept stay visibly separate.</p>
          <div className="gc-truth-equation"><span>Capture</span><i>+</i><span>Time</span><i>+</i><span>Decision</span><i>=</i><strong>Progress</strong></div>
        </div>
      </section>

      <section id="try-it" className="gc-demo-section" aria-labelledby="demo-title">
        <header className="gc-section-heading gc-section-heading-dark" data-gc-reveal>
          <div><p className="gc-kicker">Try the full loop</p><h2 id="demo-title">Put one urge<br /><em>through the pause.</em></h2></div>
          <p>This interactive sample stays in your browser. It does not contact a shop, collect payment details or create an order.</p>
        </header>
        <div data-gc-reveal><GhostFlowDemo /></div>
      </section>

      <section id="progress" className="gc-progress-section gc-section-light" aria-labelledby="progress-title">
        <header className="gc-section-heading" data-gc-reveal>
          <div><p className="gc-kicker gc-kicker-dark">Progress—not pretend finance</p><h2 id="progress-title">A clearer picture<br /><em>of every almost-buy.</em></h2></div>
          <p>No pretend financial account. No money movement. Just a private record of what tempted you and how you chose.</p>
        </header>

        <div className="gc-progress-editorial">
          <div className="gc-progress-ledger" data-gc-reveal>
            <div className="gc-progress-ledger-head"><span>Sample progress</span><strong>THIS WEEK</strong></div>
            <article><span className="gc-progress-index">01</span><div><p>Almost spent</p><strong>1,388</strong></div><small>All captured value</small></article>
            <article><span className="gc-progress-index">02</span><div><p>Cooling now</p><strong>549</strong></div><small>Not yet resolved</small></article>
            <article className="is-kept"><span className="gc-progress-index">03</span><div><p>Confirmed Money Kept</p><strong>839</strong></div><small>Explicitly skipped</small></article>
            <p>Sample data · not a user claim</p>
          </div>

          <div className="gc-progress-copy" data-gc-reveal>
            <p className="gc-display-quote">“The win isn&apos;t pretending you never wanted it. The win is knowing what happened next.”</p>
            <div className="gc-progress-notes">
              <article><span>Outcome history</span><p>See skipped, intentionally purchased and unresolved items without moral scoring.</p></article>
              <article><span>Trigger patterns</span><p>Learn when boredom, stress, hunger or FOMO tends to fill the cart.</p></article>
              <article><span>Cooldown control</span><p>Choose reminder times, pause nudges and extend any decision without pressure.</p></article>
            </div>
          </div>
        </div>

        <div className="gc-membership-story" data-gc-reveal>
          <div className="gc-membership-card">
            <div><Brand light compact /><span>MEMBER</span></div>
            <p>Your name</p>
            <strong>GHOST ID · GC 2407</strong>
            <small>Progress companion · no payment capability</small>
          </div>
          <div className="gc-membership-copy"><p className="gc-kicker gc-kicker-dark">Optional identity</p><h3>A membership card.<br /><em>Never a payment card.</em></h3><p>Personalize a shareable achievement card with your name and Ghost ID. It contains no bank-style credentials or payment-network marks.</p></div>
        </div>
      </section>

      <section className="gc-principles" aria-labelledby="principles-title">
        <div className="gc-principles-intro" data-gc-reveal>
          <p className="gc-kicker">Designed for agency</p>
          <h2 id="principles-title">No guilt.<br />No pressure.<br /><em>No fake proof.</em></h2>
        </div>
        <div className="gc-principle-lines">
          <article data-gc-reveal><span>01</span><h3>Private by default</h3><p>Your almost-buys are personal. Social activity should be anonymous, thresholded and based on real events only.</p></article>
          <article data-gc-reveal><span>02</span><h3>Reminders you control</h3><p>Cooling alerts are useful. Lunch, dinner and habit nudges remain separate, optional preferences.</p></article>
          <article data-gc-reveal><span>03</span><h3>Brands stay in their lane</h3><p>Sponsored discovery, if introduced later, never interrupts a cooldown or disguises itself as advice.</p></article>
          <article data-gc-reveal><span>04</span><h3>Choices without shame</h3><p>Skipping is progress. Buying intentionally is also a clear outcome. The app records—not judges.</p></article>
        </div>
      </section>

      <section className="gc-moments gc-section-light" aria-labelledby="moments-title">
        <header className="gc-section-heading" data-gc-reveal>
          <div><p className="gc-kicker gc-kicker-dark">Built for real moments</p><h2 id="moments-title">Not a marketplace.<br /><em>A place to pause.</em></h2></div>
          <p>The same ritual adapts to the tempo and consequence of each temptation.</p>
        </header>
        <div className="gc-moment-track">
          <article className="gc-moment-food" data-gc-reveal><span>15–30 MINUTES</span><h3>The second dinner delivery.</h3><p>Capture hunger or boredom, then decide after the immediate craving settles.</p><div aria-hidden="true">LUNCH<br />OR<br /><strong>IMPULSE?</strong></div></article>
          <article className="gc-moment-fashion" data-gc-reveal><img src="/products/sneaker.png" alt="Generic white sneaker" /><div><span>24 HOURS</span><h3>The pair that followed you home from an ad.</h3><p>Move it out of the retailer cart and give it a quieter place to wait.</p></div></article>
          <article className="gc-moment-tech" data-gc-reveal><span>72 HOURS</span><h3>The upgrade that suddenly felt urgent.</h3><p>Separate a real need from launch-day energy before making the purchase.</p><div className="gc-tech-lines" aria-hidden="true"><i /><i /><i /><i /></div></article>
        </div>
      </section>

      <section id="faq" className="gc-faq gc-section-light" aria-labelledby="faq-title">
        <div className="gc-faq-title" data-gc-reveal><p className="gc-kicker gc-kicker-dark">Straight answers</p><h2 id="faq-title">Questions,<br /><em>without the fog.</em></h2><img src="/mascot/mascot-wave-alt.png" alt="Friendly Ghost Cart mascot waving" /></div>
        <div className="gc-faq-list" data-gc-reveal>
          {FAQS.map(([question, answer], index) => (
            <details key={question} name="ghost-cart-faq">
              <summary><span>{String(index + 1).padStart(2, "0")}</span>{question}<i aria-hidden="true">+</i></summary>
              <p>{answer}</p>
            </details>
          ))}
        </div>
      </section>

      <section id="waitlist" className="gc-waitlist" aria-labelledby="waitlist-title">
        <div className="gc-waitlist-orbit" />
        <img src="/mascot/mascot-thumbsup.png" alt="Ghost Cart mascot giving a thumbs up" />
        <div className="gc-waitlist-copy" data-gc-reveal>
          <p className="gc-kicker">Coming soon</p>
          <h2 id="waitlist-title">Give the urge<br /><em>somewhere to go.</em></h2>
          <p>Join the waitlist for Ghost Cart—the cooling-off app for everything you almost bought.</p>
          <WaitlistForm />
        </div>
      </section>

      <footer className="gc-footer">
        <div className="gc-footer-curve" />
        <div className="gc-footer-top"><Brand light /><p>Capture it. Cool it. Decide.<br /><span>Count only what you kept.</span></p><a href="#top">Back to top ↑</a></div>
        <div className="gc-footer-links">
          <div><span>Explore</span><a href="#how-it-works">How it works</a><a href="#try-it">Try it</a><a href="#progress">Progress</a></div>
          <div><span>Product</span><a href="#download">Download beta</a><a href="#faq">FAQ</a><a href="#waitlist">Join waitlist</a><a href="/admin">Catalog admin</a></div>
          <div><span>Contact</span><a href="mailto:info@theghostcart.com">info@theghostcart.com</a></div>
          <div><span>Follow</span><a href="#waitlist">Instagram · coming soon</a><a href="#waitlist">TikTok · coming soon</a><a href="#waitlist">LinkedIn · coming soon</a></div>
        </div>
        <div className="gc-footer-bottom"><p>Simulation only. No real payment. No real delivery.</p><p>Ghost Cart is not a bank, wallet or payment instrument.</p><span>© Ghost Cart · <a href="mailto:info@theghostcart.com">info@theghostcart.com</a></span></div>
      </footer>
    </main>
  );
}
