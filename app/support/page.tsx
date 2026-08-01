import type { Metadata } from "next";
import { LegalPage } from "../components/LegalPage";

export const metadata: Metadata = { title: "Support — Ghost Cart" };

const BODY = `
Need help with Ghost Cart? We're here.

• Email: info@theghostcart.com
• We typically reply within 1-2 business days.

Common questions:

Is my money real? No. Ghost Cart is simulation-only. It never processes payment, places a real order, stores money, or delivers a product. The "amount charged" is always zero.

How do I delete my account? Profile → Delete Account in the app. This permanently removes your device profile, favorites, cooldowns, simulated wallet, and activity history.

How does the Community Leaderboard work? Opting in is optional and separate from your account. You choose a username shown publicly; your email is never shown. You can opt out at any time from Profile.

Found a bug or have feedback? Email us at info@theghostcart.com with details and, if possible, a screenshot.
`;

export default function SupportPage() {
  return <LegalPage title="Support" body={BODY} />;
}
