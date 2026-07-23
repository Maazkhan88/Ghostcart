import { Brand } from "./Brand";

const LEGAL_LAST_UPDATED = "23 July 2026";

type LegalPageProps = {
  title: string;
  body: string;
};

// Shared layout for /privacy, /terms, /data-security - see the matching
// Android LegalDocumentScreen for the same content.
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
        <p className="gc-legal-updated">Last updated: {LEGAL_LAST_UPDATED}</p>
        {paragraphs.map((paragraph, index) => (
          <p key={index}>{paragraph}</p>
        ))}
      </div>
    </main>
  );
}
