"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";

type DemoStage = "captured" | "choosing" | "cooling" | "decision" | "resolved";
type DemoResolution = "skipped" | "bought" | null;

type DemoItem = {
  id: string;
  name: string;
  value: number;
  category: string;
  source: string;
  trigger: string;
  image: string | null;
  stage: DemoStage;
  cooldown: string | null;
  resolution: DemoResolution;
};

const DEMO_DATA: DemoItem[] = [
  {
    id: "sample-sneakers",
    name: "White sneakers",
    value: 549,
    category: "Fashion",
    source: "Screenshot",
    trigger: "FOMO",
    image: "/products/sneaker.png",
    stage: "captured",
    cooldown: null,
    resolution: null,
  },
];

const COOLING_OPTIONS = [
  { value: "30 minutes", short: "30m", note: "Food or small cravings" },
  { value: "24 hours", short: "24h", note: "Everyday almost-buys" },
  { value: "72 hours", short: "72h", note: "Tech and considered buys" },
  { value: "7 days", short: "7d", note: "Luxury and high-value items" },
] as const;

const STAGE_LABELS: Record<DemoStage, string> = {
  captured: "Captured",
  choosing: "Ghosted",
  cooling: "Cooling",
  decision: "Ready to resolve",
  resolved: "Resolved",
};

function productImage(category: string) {
  if (category === "Fashion") return "/products/sneaker.png";
  if (category === "Beauty") return "/products/perfume.png";
  return null;
}

function displayValue(value: number) {
  return new Intl.NumberFormat("en", { maximumFractionDigits: 0 }).format(value);
}

export function GhostFlowDemo() {
  const [items, setItems] = useState<DemoItem[]>(DEMO_DATA);
  const [selectedId, setSelectedId] = useState(DEMO_DATA[0].id);
  const [cooldownChoice, setCooldownChoice] = useState("24 hours");
  const [receiptId, setReceiptId] = useState<string | null>(null);
  const [announcement, setAnnouncement] = useState("Sample item ready to Ghost.");
  const storageReady = useRef(false);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const saved = window.localStorage.getItem("ghost-cart-demo-v2");
      if (saved) {
        try {
          const parsed = JSON.parse(saved) as DemoItem[];
          if (Array.isArray(parsed) && parsed.length) {
            setItems(parsed);
            setSelectedId(parsed[0].id);
          }
        } catch {
          window.localStorage.removeItem("ghost-cart-demo-v2");
        }
      }
      storageReady.current = true;
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (!storageReady.current) return;
    window.localStorage.setItem("ghost-cart-demo-v2", JSON.stringify(items));
  }, [items]);

  const selected = items.find((item) => item.id === selectedId) ?? items[0];
  const almostSpent = useMemo(() => items.reduce((sum, item) => sum + item.value, 0), [items]);
  const coolingNow = useMemo(() => items.filter((item) => item.stage === "cooling" || item.stage === "decision").reduce((sum, item) => sum + item.value, 0), [items]);
  const confirmedKept = useMemo(() => items.filter((item) => item.resolution === "skipped").reduce((sum, item) => sum + item.value, 0), [items]);
  const receipt = items.find((item) => item.id === receiptId) ?? null;

  function updateItem(id: string, update: Partial<DemoItem>) {
    setItems((current) => current.map((item) => item.id === id ? { ...item, ...update } : item));
  }

  function capture(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const name = String(data.get("item") ?? "").trim();
    const value = Number(data.get("value"));
    const category = String(data.get("category") ?? "Other");
    const trigger = String(data.get("trigger") ?? "Not sure yet");
    if (!name || !Number.isFinite(value) || value <= 0) return;

    const item: DemoItem = {
      id: `local-${Date.now()}`,
      name,
      value: Math.round(value),
      category,
      source: "Added manually",
      trigger,
      image: productImage(category),
      stage: "captured",
      cooldown: null,
      resolution: null,
    };

    setItems((current) => [item, ...current]);
    setSelectedId(item.id);
    setReceiptId(null);
    setAnnouncement(`${name} captured. No purchase was made.`);
    form.reset();
  }

  function ghostSelected() {
    if (!selected) return;
    updateItem(selected.id, { stage: "choosing", resolution: null });
    setReceiptId(null);
    setAnnouncement(`${selected.name} moved into Ghost Cart. Choose a cooling period.`);
  }

  function startCooling() {
    if (!selected) return;
    updateItem(selected.id, { stage: "cooling", cooldown: cooldownChoice, resolution: null });
    setAnnouncement(`${selected.name} is cooling for ${cooldownChoice}.`);
  }

  function readyToResolve() {
    if (!selected) return;
    updateItem(selected.id, { stage: "decision" });
    setAnnouncement(`Cooling paused for ${selected.name}. Choose an honest outcome.`);
  }

  function resolve(resolution: Exclude<DemoResolution, null>) {
    if (!selected) return;
    updateItem(selected.id, { stage: "resolved", resolution });
    setReceiptId(selected.id);
    setAnnouncement(
      resolution === "skipped"
        ? `${selected.name} resolved as skipped. ${displayValue(selected.value)} is now confirmed Money Kept.`
        : `${selected.name} recorded as an intentional purchase. Money Kept remains unchanged.`,
    );
  }

  function extendCooling() {
    if (!selected) return;
    updateItem(selected.id, { stage: "cooling", cooldown: "More time" });
    setAnnouncement(`${selected.name} returned to cooling. No outcome has been counted yet.`);
  }

  function resetDemo() {
    setItems(DEMO_DATA);
    setSelectedId(DEMO_DATA[0].id);
    setReceiptId(null);
    setAnnouncement("Demo reset to the sample item.");
    window.localStorage.removeItem("ghost-cart-demo-v2");
  }

  return (
    <div className="gc-demo-shell">
      <div className="gc-demo-toolbar">
        <div>
          <span className="gc-live-dot" aria-hidden="true" />
          <strong>Interactive sample</strong>
          <small>Device-local demo data</small>
        </div>
        <button type="button" className="gc-text-button" onClick={resetDemo}>Reset demo</button>
      </div>

      <div className="gc-demo-layout">
        <aside className="gc-capture-panel" aria-labelledby="capture-title">
          <p className="gc-kicker gc-kicker-dark">01 / Capture</p>
          <h3 id="capture-title">Put the temptation somewhere safe.</h3>
          <p>Paste, share or screenshot support is planned for the app. Try the same decision flow here by adding an item manually.</p>
          <form className="gc-capture-form" onSubmit={capture}>
            <label>What almost got you?<input name="item" placeholder="e.g. concert tickets" required /></label>
            <div className="gc-form-row">
              <label>Value<input name="value" type="number" inputMode="decimal" min="1" max="1000000" placeholder="450" required /></label>
              <label>Category<select name="category" defaultValue="Other"><option>Other</option><option>Fashion</option><option>Beauty</option><option>Food</option><option>Tech</option><option>Entertainment</option></select></label>
            </div>
            <label>What triggered it?<select name="trigger" defaultValue="Not sure yet"><option>Not sure yet</option><option>Boredom</option><option>Stress</option><option>FOMO</option><option>Hunger</option><option>Reward</option><option>Late-night scrolling</option></select></label>
            <button className="gc-button gc-button-green gc-button-full" type="submit">Capture temptation <span aria-hidden="true">→</span></button>
          </form>
          <p className="gc-form-note">Simulation only. This form never places an order.</p>
        </aside>

        <section className="gc-flow-panel" aria-labelledby="flow-title">
          <header className="gc-flow-heading">
            <div><p className="gc-kicker gc-kicker-dark">02–05 / Decide</p><h3 id="flow-title">One honest decision at a time.</h3></div>
            <span className="gc-sample-label">Sample session</span>
          </header>

          <div className="gc-item-switcher" aria-label="Captured items">
            {items.map((item) => (
              <button key={item.id} type="button" className={item.id === selected?.id ? "is-selected" : ""} onClick={() => { setSelectedId(item.id); setReceiptId(null); }}>
                <span className="gc-item-thumb">
                  {item.image ? <img src={item.image} alt="" /> : <span aria-hidden="true">{item.name.charAt(0).toUpperCase()}</span>}
                </span>
                <span><strong>{item.name}</strong><small>{STAGE_LABELS[item.stage]} · {displayValue(item.value)}</small></span>
              </button>
            ))}
          </div>

          {selected ? (
            <div className="gc-decision-stage">
              <div className="gc-selected-product">
                <div className="gc-selected-art">
                  {selected.image ? <img src={selected.image} alt={`${selected.name}, generic demo reference`} /> : <span aria-hidden="true">{selected.name.charAt(0).toUpperCase()}</span>}
                  <span>{STAGE_LABELS[selected.stage]}</span>
                </div>
                <div className="gc-selected-copy">
                  <p className="gc-kicker gc-kicker-dark">{selected.category} · {selected.source}</p>
                  <h4>{selected.name}</h4>
                  <dl><div><dt>Almost spent</dt><dd>{displayValue(selected.value)}</dd></div><div><dt>Trigger</dt><dd>{selected.trigger}</dd></div></dl>
                </div>
              </div>

              {selected.stage === "captured" && (
                <div className="gc-action-state">
                  <span className="gc-state-number">02</span><div><h4>Ghost the impulse.</h4><p>Give yourself the satisfying stop without pretending a purchase happened.</p></div>
                  <button type="button" className="gc-button gc-button-ink" onClick={ghostSelected}>Ghost this item</button>
                </div>
              )}

              {selected.stage === "choosing" && (
                <div className="gc-action-state gc-action-stack">
                  <div><span className="gc-state-number">03</span><div><h4>Choose the pause.</h4><p>Match the cooling time to the size and type of temptation.</p></div></div>
                  <div className="gc-cooling-options" role="radiogroup" aria-label="Cooling period">
                    {COOLING_OPTIONS.map((option) => (
                      <button key={option.value} type="button" role="radio" aria-checked={cooldownChoice === option.value} className={cooldownChoice === option.value ? "is-selected" : ""} onClick={() => setCooldownChoice(option.value)}>
                        <strong>{option.short}</strong><span>{option.note}</span>
                      </button>
                    ))}
                  </div>
                  <button type="button" className="gc-button gc-button-green gc-button-full" onClick={startCooling}>Start cooling</button>
                </div>
              )}

              {selected.stage === "cooling" && (
                <div className="gc-action-state gc-cooling-state">
                  <div className="gc-cooling-orbit" aria-hidden="true"><span>03</span><i /></div>
                  <div><h4>The urge is cooling.</h4><p>{selected.cooldown} selected. Nothing is counted as kept until you resolve it.</p><button type="button" className="gc-button gc-button-paper" onClick={readyToResolve}>I&apos;m ready to decide</button></div>
                </div>
              )}

              {selected.stage === "decision" && (
                <div className="gc-resolution-state">
                  <div><span className="gc-state-number">04</span><div><h4>What actually happened?</h4><p>Honesty makes the progress useful. There is no wrong answer.</p></div></div>
                  <div className="gc-resolution-buttons">
                    <button type="button" className="gc-resolution-positive" onClick={() => resolve("skipped")}><strong>I skipped it</strong><span>Count as Money Kept</span></button>
                    <button type="button" onClick={() => resolve("bought")}><strong>I bought it intentionally</strong><span>Log the decision, keep total unchanged</span></button>
                    <button type="button" onClick={extendCooling}><strong>I need more time</strong><span>Return to cooling</span></button>
                  </div>
                </div>
              )}

              {selected.stage === "resolved" && (
                <div className={`gc-outcome-state${selected.resolution === "skipped" ? " is-kept" : ""}`}>
                  <span aria-hidden="true">{selected.resolution === "skipped" ? "✓" : "→"}</span>
                  <div><p className="gc-kicker gc-kicker-dark">05 / Resolved</p><h4>{selected.resolution === "skipped" ? "Confirmed Money Kept." : "Intentional purchase logged."}</h4><p>{selected.resolution === "skipped" ? `${displayValue(selected.value)} moved from almost-spent to confirmed kept.` : "No money was added to the kept total. The clear decision still counts as progress."}</p></div>
                  <button type="button" className="gc-text-button" onClick={() => setReceiptId(selected.id)}>View Ghost Receipt</button>
                </div>
              )}
            </div>
          ) : null}
        </section>
      </div>

      <div className="gc-session-ledger" aria-label="Sample session progress">
        <article><span>Almost spent</span><strong>{displayValue(almostSpent)}</strong><p>Everything captured in this sample session.</p></article>
        <article><span>Cooling now</span><strong>{displayValue(coolingNow)}</strong><p>Still waiting for an honest outcome.</p></article>
        <article className="is-kept"><span>Confirmed Money Kept</span><strong>{displayValue(confirmedKept)}</strong><p>Only items explicitly marked as skipped.</p></article>
      </div>

      {receipt && (
        <section className="gc-receipt" aria-labelledby="receipt-title">
          <div><p className="gc-kicker">Ghost Receipt</p><h3 id="receipt-title">A decision record—not proof of purchase.</h3></div>
          <dl>
            <div><dt>Almost-buy</dt><dd>{receipt.name}</dd></div>
            <div><dt>Outcome</dt><dd>{receipt.resolution === "skipped" ? "Skipped" : "Bought intentionally"}</dd></div>
            <div><dt>Confirmed kept</dt><dd>{receipt.resolution === "skipped" ? displayValue(receipt.value) : "0"}</dd></div>
            <div><dt>Cooling choice</dt><dd>{receipt.cooldown ?? "None"}</dd></div>
          </dl>
          <p>Simulation only. This Ghost Receipt is not an invoice, payment record or proof of purchase.</p>
        </section>
      )}

      <p className="gc-sr-only" aria-live="polite">{announcement}</p>
    </div>
  );
}
