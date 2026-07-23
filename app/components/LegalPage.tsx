import { Brand } from "./Brand";

const LEGAL_LAST_UPDATED = "23 July 2026";

type LegalPageProps = {
  title: string;
  body: string;
};

// Shared layout for /privacy, /terms, /data-security. The DRAFT banner must
// stay visible until a licensed UAE lawyer has actually reviewed the body
// text below - see the matching Android LegalDocumentScreen for the same
// content and the same rule.
export function LegalPage({ title, body }: LegalPageProps) {
  const paragraphs = body
    .trim()
    .split(/\n\s*\n/)
    .map((p) => p.trim())
    .filter(Boolean);

  return (
    <main className="gc-legal">
      <div className="gc-legal-header">
        <a href="/" className="gc-legal-brand"><Brand /></a>
        <a href="/" className="gc-legal-back">← Back to theghostcart.com</a>
      </div>
      <div className="gc-legal-body">
        <h1>{title}</h1>
        <div className="gc-legal-draft-banner">
          <strong>DRAFT — pending legal review.</strong>
          <span> Written for this app, not yet reviewed by a licensed UAE lawyer. Do not treat as final.</span>
        </div>
        <p className="gc-legal-updated">Last updated: {LEGAL_LAST_UPDATED}</p>
        {paragraphs.map((paragraph, index) => (
          <p key={index}>{paragraph}</p>
        ))}
      </div>
    </main>
  );
}
