import type { Metadata } from "next";
import { LegalPage } from "../components/LegalPage";

export const metadata: Metadata = { title: "Terms & Conditions — Ghost Cart" };

const BODY = `
By using the Ghost Cart app (the "App") or theghostcart.com (the "Site"), you agree to these Terms & Conditions, governed by the laws of the United Arab Emirates.

1. What Ghost Cart is
Ghost Cart is a simulation-only cooling-off tool that helps you pause before an impulse purchase. Every "purchase," "checkout," "delivery," and "payment" shown in the App is simulated for behavioral/educational purposes only. No real money moves through the App, no real goods are ordered or delivered, and Ghost Cart is not a bank, e-wallet, payment service provider, or licensed financial institution. The "Ghost Card" is a non-financial achievement/membership card only - it is not a payment card and carries no CVV, expiry date, or real card number.

2. Eligibility
You must be at least 18 years old to create an account.

3. Your account
You are responsible for keeping your login credentials secure and for all activity under your account. Notify us immediately at info@theghostcart.com if you suspect unauthorized access.

4. Community features
The Community Leaderboard and community product feed let opted-in users share a chosen username, avatar, and activity - never your email or real name. Do not impersonate others or post unlawful, defamatory, or offensive content. We may remove content or suspend accounts that violate this, consistent with UAE Federal Decree-Law No. 34 of 2021 on Combating Rumours and Cybercrimes.

5. No real transactions
Nothing in the App constitutes an offer to sell, a real order, a real payment instrument, or a real delivery service. "Money Kept" and similar figures are personal tracking metrics only - not a financial product, investment, or guarantee of savings.

6. Intellectual property
The Ghost Cart name, logo, mascot, and app/site design belong to us. Do not copy, reproduce, or reuse them without permission.

7. Termination
We may suspend or terminate accounts that violate these Terms. You may delete your own account at any time from Profile in the app.

8. Disclaimer and limitation of liability
The App and Site are provided "as is," without warranties of any kind. To the fullest extent permitted by UAE law, we are not liable for indirect, incidental, or consequential damages arising from your use of the App or Site.

9. Governing law
These Terms are governed by the laws of the United Arab Emirates. Any dispute is subject to the exclusive jurisdiction of the competent courts of the UAE.

10. Contact: info@theghostcart.com
`;

export default function TermsPage() {
  return <LegalPage title="Terms & Conditions" body={BODY} />;
}
