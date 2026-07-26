import { and, eq } from "drizzle-orm";
import { getDb } from "../db";
import { contentBlocks } from "../db/schema";
import { Brand } from "./components/Brand";
import { DirhamAmount } from "./components/DirhamAmount";
import { RevealObserver } from "./components/RevealObserver";
import { SiteNav } from "./components/SiteNav";
import { WaitlistForm } from "./components/WaitlistForm";

async function getGhostCartStories() {
  try {
    const db = getDb();
    const rows = await db
      .select({ id: contentBlocks.id, imageKey: contentBlocks.imageKey })
      .from(contentBlocks)
      .where(and(eq(contentBlocks.type, "story"), eq(contentBlocks.isActive, true)))
      .orderBy(contentBlocks.sortOrder);
    return rows;
  } catch {
    // Same admin-curated story feed the Android app already uses - if it's
    // ever unavailable, the homepage should still render everything else.
    return [];
  }
}

const HOW_STEPS = [
  {
    number: "01",
    title: "Ghost it",
    copy: "Share a shopping link, choose an item, or add it yourself.",
    label: "Captured safely",
  },
  {
    number: "02",
    title: "Let it cool",
    copy: "Every Ghost starts a 24-hour pause. We remind you when it is ready.",
    label: "24-hour cooldown",
  },
  {
    number: "03",
    title: "Decide",
    copy: "Skip it, buy from the source, mark it already bought, or give it more time.",
    label: "Your choice",
  },
] as const;

const WHY_CARDS = [
  {
    number: "01",
    title: "Break the instant-buy loop",
    copy: "Move the item out of the retailer cart before urgency makes the decision.",
  },
  {
    number: "02",
    title: "Make an honest decision",
    copy: "Wanting something is allowed. Ghost Cart simply gives the feeling time to settle.",
  },
  {
    number: "03",
    title: "Count only real skips",
    copy: "Money Kept increases only when you confirm that you did not buy the item.",
  },
] as const;

const FAQS = [
  [
    "Does Ghost Cart buy anything?",
    "No. Ghost Cart is a simulation-only cooling-off app. It does not process a payment, place an order, or deliver a product.",
  ],
  [
    "What happens after 24 hours?",
    "You receive a reminder and choose what actually happened: skip it, buy from the source, mark it already bought, or restart the cooldown.",
  ],
  [
    "Can I return to the original product?",
    "Yes. When an item has a valid source link, you can choose to reveal it after the cooldown and continue with an intentional decision.",
  ],
  [
    "Is Ghost Cart available on iPhone?",
    "Not yet. Our Ghost devs are working hard to bring Ghost Cart to Apple devices. Join the waitlist for iOS updates.",
  ],
] as const;

export default async function Home() {
  const ghostCartStories = await getGhostCartStories();
  return (
    <main id="top" className="gc-site gc-v3-site">
      <RevealObserver />
      <SiteNav />

      <section id="what" className="gc-v3-hero" aria-labelledby="hero-title">
        <div className="gc-v3-hero-glow" aria-hidden="true" />
        <div className="gc-v3-hero-copy" data-gc-reveal>
          <p className="gc-v3-eyebrow">A pause for everything you almost buy</p>
          <h1 id="hero-title">
            Want it?
            <em>Ghost it first.</em>
          </h1>
          <p className="gc-v3-lede">
            Share any product to Ghost Cart. It cools for 24 hours, then you decide with a clearer head.
          </p>
          <div className="gc-v3-hero-actions">
            <a className="gc-button gc-button-green" href="#when">Join the waitlist</a>
            <a className="gc-v3-text-link" href="#how">See how it works <span aria-hidden="true">↓</span></a>
          </div>
          <p className="gc-v3-safety">Simulation only <span>·</span> No real payment <span>·</span> No real delivery</p>
        </div>

        <div className="gc-v3-hero-visual" aria-label="A product cooling for 24 hours in Ghost Cart" data-gc-reveal>
          <div className="gc-v3-float-tag gc-v3-float-share" aria-hidden="true">Shared from anywhere</div>
          <div className="gc-v3-phone">
            <div className="gc-v3-phone-status"><span>9:41</span><i /></div>
            <div className="gc-v3-phone-brand"><Brand compact /><span>Cooling</span></div>
            <div className="gc-v3-phone-product">
              <button type="button" aria-label="Saved item">♡</button>
              <img src="/products/sneaker.png" alt="Generic white sneakers" />
            </div>
            <p>White sneakers</p>
            <h2>Give it a day.</h2>
            <div className="gc-v3-countdown">
              <strong>23:41:08</strong>
              <span>until your decision</span>
            </div>
            <div className="gc-v3-progress" aria-hidden="true"><i /></div>
            <div className="gc-v3-reminder"><span>✓</span><p><strong>We will remind you</strong><small>Push and email when the cooldown ends</small></p></div>
          </div>
          <img className="gc-v3-hero-mascot" src="/mascot/mascot-cooldown.png" alt="Ghost Cart mascot waiting beside a timer" />
          <div className="gc-v3-float-tag gc-v3-float-time" aria-hidden="true"><strong>24h</strong><span>before you decide</span></div>
        </div>
      </section>

      <section id="how" className="gc-v3-how" aria-labelledby="how-title">
        <header className="gc-v3-section-heading" data-gc-reveal>
          <p className="gc-v3-eyebrow gc-v3-eyebrow-dark">How it works</p>
          <h2 id="how-title">Ghost it. Let it cool. <em>Decide.</em></h2>
          <p>One clear action starts the pause. The decision stays yours.</p>
        </header>

        <div className="gc-v3-how-layout">
          <div className="gc-v3-step-list">
            {HOW_STEPS.map((step) => (
              <article key={step.number} data-gc-reveal>
                <span>{step.number}</span>
                <div><h3>{step.title}</h3><p>{step.copy}</p></div>
                <small>{step.label}</small>
              </article>
            ))}
            <p className="gc-v3-batch-note" data-gc-reveal>
              Ghosted several items together? You decide each one individually when the cooldown ends.
            </p>
          </div>

          <div className="gc-v3-how-phone" data-gc-reveal aria-label="Ghost Cart decision screen example">
            <div className="gc-v3-how-phone-head"><Brand compact /><span>Ready to decide</span></div>
            <div className="gc-v3-decision-product"><img src="/products/sneaker.png" alt="" /><div><strong>White sneakers</strong><small>24 hours complete</small></div></div>
            <h3>What happened?</h3>
            <button type="button"><i className="is-green">✓</i><span><strong>I skipped it</strong><small>Count it as Money Kept</small></span></button>
            <button type="button"><i>↗</i><span><strong>I want to buy it</strong><small>Reveal the original source</small></span></button>
            <button type="button"><i>↻</i><span><strong>Give me more time</strong><small>Choose another cooldown</small></span></button>
            <small className="gc-v3-demo-label">Example screen · no purchase made</small>
          </div>
        </div>
      </section>

      <section id="why" className="gc-v3-why" aria-labelledby="why-title">
        <header className="gc-v3-section-heading gc-v3-section-heading-light" data-gc-reveal>
          <p className="gc-v3-eyebrow">Why Ghost Cart</p>
          <h2 id="why-title">Because wanting something does not mean <em>buying it now.</em></h2>
        </header>

        <div className="gc-v3-why-cards">
          {WHY_CARDS.map((card) => (
            <article key={card.number} data-gc-reveal>
              <span>{card.number}</span>
              <h3>{card.title}</h3>
              <p>{card.copy}</p>
            </article>
          ))}
        </div>

        <div className="gc-v3-progress-card" data-gc-reveal>
          <div>
            <p className="gc-v3-eyebrow gc-v3-eyebrow-dark">Example progress</p>
            <h3>A truthful view of your almost-buys.</h3>
            <p>Cooling is not counted as saving. Only a confirmed skip becomes Money Kept.</p>
          </div>
          <dl>
            <div><dt>Almost spent</dt><dd><DirhamAmount amount="1,388" /></dd></div>
            <div><dt>Cooling</dt><dd><DirhamAmount amount="549" /></dd></div>
            <div className="is-kept"><dt>Money Kept</dt><dd><DirhamAmount amount="839" /></dd></div>
          </dl>
          <small>Demo data only · not a public claim</small>
        </div>

        <p className="gc-v3-trust-line" data-gc-reveal>
          <span aria-hidden="true">✓</span> No purchase, payment, or delivery happens inside Ghost Cart.
        </p>
      </section>

      {ghostCartStories.length > 0 ? (
        <section id="stories" className="gc-v3-stories" aria-labelledby="stories-title">
          <header className="gc-v3-section-heading gc-v3-section-heading-light" data-gc-reveal>
            <p className="gc-v3-eyebrow">Ghost Cart Stories</p>
            <h2 id="stories-title">See it in the wild.</h2>
            <p>A few moments from people who ghosted something instead of buying it.</p>
          </header>

          <div className="gc-v3-stories-row" data-gc-reveal>
            {ghostCartStories.map((story) => (
              <div className="gc-v3-story-card" key={story.id}>
                <img
                  src={`/api/content-blocks/image/${story.imageKey}`}
                  alt="A Ghost Cart story"
                  loading="lazy"
                />
              </div>
            ))}
          </div>
        </section>
      ) : null}

      <section id="faq" className="gc-v3-faq" aria-labelledby="faq-title">
        <header className="gc-v3-section-heading" data-gc-reveal>
          <p className="gc-v3-eyebrow gc-v3-eyebrow-dark">Good to know</p>
          <h2 id="faq-title">Four quick <em>answers.</em></h2>
        </header>
        <div className="gc-v3-faq-list" data-gc-reveal>
          {FAQS.map(([question, answer], index) => (
            <details key={question} name="ghost-cart-faq">
              <summary><span>{String(index + 1).padStart(2, "0")}</span>{question}<i aria-hidden="true">+</i></summary>
              <p>{answer}</p>
            </details>
          ))}
        </div>
      </section>

      <section id="when" className="gc-v3-when" aria-labelledby="when-title">
        <div className="gc-v3-when-copy" data-gc-reveal>
          <p className="gc-v3-eyebrow">When can I use it?</p>
          <h2 id="when-title">Android is in <em>closed testing.</em></h2>
          <p>Ghost Cart is being tested on Android now. Join the waitlist for the next testing opportunity and public-release updates.</p>
          <WaitlistForm />
        </div>
        <div className="gc-v3-when-art" data-gc-reveal>
          <img src="/mascot/mascot-thumbsup.png" alt="Ghost Cart mascot giving a thumbs up" />
          <div><span>Android</span><strong>Closed testing</strong><small>Join for the next opportunity</small></div>
        </div>
      </section>

      <footer className="gc-v3-footer">
        <div className="gc-v3-footer-main">
          <Brand light />
          <nav aria-label="Footer navigation"><a href="#what">What</a><a href="#how">How</a><a href="#why">Why</a><a href="#when">When</a></nav>
          <a href="#top">Back to top ↑</a>
        </div>
        <div className="gc-v3-footer-meta">
          <p>Simulation only. No real payment. No real delivery.</p>
          <div><a href="/privacy">Privacy</a><a href="/terms">Terms</a><a href="/data-security">Data security</a><a href="mailto:info@theghostcart.com">Contact</a></div>
          <span>© Ghost Cart</span>
        </div>
      </footer>
    </main>
  );
}
