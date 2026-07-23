import type { Metadata } from "next";
import { LegalPage } from "../components/LegalPage";

export const metadata: Metadata = { title: "Privacy Policy — Ghost Cart" };

const BODY = `
Ghost Cart ("we", "us", "our") respects your privacy. This policy explains what information we collect, why, and how it is used, consistent with UAE Federal Decree-Law No. 45 of 2021 on the Protection of Personal Data ("PDPL").

What we collect
• Account information: your email address and a securely hashed password - we never store your actual password.
• Profile information you choose to provide: display name, leaderboard username, and profile photo.
• Simulated activity: items you capture, cool off, or resolve in the app (Ghost Cart items, cooldowns, "Money Kept" records). None of this reflects a real purchase, payment, or bank transaction.
• Device and usage information: push-notification device tokens, and aggregate analytics via Firebase Analytics (Android) and Google Analytics (website) - feature usage and crash diagnostics only.
• Anonymous community content: if you opt in, your leaderboard username, avatar, and activity counts (never your email or real name) become visible to other users. You can withdraw this consent at any time from Profile.

Why we use it
To operate and secure the app and website, personalize your experience, respond to support requests, and improve the product. We do not sell your personal data.

Who we share it with
We use trusted service providers to run Ghost Cart: Cloudflare (hosting, database, and email delivery) and Google Firebase / Google Analytics (sign-in, push notifications, analytics). These providers process data on our behalf and may store it outside the UAE; we take reasonable steps to ensure they provide an adequate level of protection.

Your rights
Under the PDPL you may access, correct, or request deletion of your personal data. Delete your account and all associated data at any time from Profile → Delete Account in the app. For any other request, contact info@theghostcart.com.

Data retention
We retain your data while your account is active, or as needed to meet legal obligations. Deleting your account permanently removes your profile, cooldown history, favorites, and simulated wallet data.

Children's privacy
Ghost Cart is not directed at, and should not be used by, anyone under 18.

Changes to this policy
We may update this policy from time to time; continued use of the app or site after a change means you accept the update.

Contact: info@theghostcart.com
`;

export default function PrivacyPage() {
  return <LegalPage title="Privacy Policy" body={BODY} />;
}
