import type { Metadata } from "next";
import { LegalPage } from "../components/LegalPage";

export const metadata: Metadata = { title: "Data Security — Ghost Cart" };

const BODY = `
We take reasonable technical and organizational measures to protect your data, consistent with UAE Federal Decree-Law No. 45 of 2021 on the Protection of Personal Data.

• Encryption in transit: all traffic between the App/Site and our servers uses HTTPS/TLS.
• Password security: passwords are never stored in plain text. We store a salted, one-way cryptographic hash and cannot see or recover your actual password.
• Session security: sign-in sessions use httpOnly, secure tokens; admin access is separately gated and audited.
• Data minimization: the anonymous community feed and leaderboard never expose your email, password, or real name to other users.
• Infrastructure: our backend runs on Cloudflare's Workers/D1 platform; sign-in and push notifications use Google Firebase. These providers maintain their own independent security certifications.
• Retention and deletion: deleting your account (Profile → Delete Account in the app) permanently removes your stored profile, cooldown history, favorites, and simulated wallet data from our production database.
• Incident response: if we become aware of a data breach affecting your personal data, we will take reasonable steps to notify affected users and the relevant UAE authority without undue delay, as required by law.

This page is a good-faith summary of our practices, not an exhaustive security audit. Questions: info@theghostcart.com
`;

export default function DataSecurityPage() {
  return <LegalPage title="Data Security" body={BODY} />;
}
