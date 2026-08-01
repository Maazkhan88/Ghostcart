import SwiftUI

// Mirrors Android's LegalRow/LEGAL_DOCUMENT_TITLES/LEGAL_DOCUMENT_BODIES/
// LegalDocumentScreen (GhostCartV2Screens.kt:1352-1474) verbatim - same
// three native in-app text screens (not external URLs, not a WebView).
// Bump legalDocumentsLastUpdated whenever any body changes materially.
enum LegalDocument: String, CaseIterable, Identifiable {
    case privacy, terms, dataSecurity = "data-security"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .privacy: return "Privacy Policy"
        case .terms: return "Terms & Conditions"
        case .dataSecurity: return "Data Security"
        }
    }
}

private let legalDocumentsLastUpdated = "23 July 2026"

private let legalDocumentBodies: [LegalDocument: String] = [
    .privacy: """
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
    Under the PDPL you may access, correct, or request deletion of your personal data. Delete your account and all associated data at any time from Profile → Delete Account. For any other request, contact info@theghostcart.com.

    Data retention
    We retain your data while your account is active, or as needed to meet legal obligations. Deleting your account permanently removes your profile, cooldown history, favorites, and simulated wallet data.

    Children's privacy
    Ghost Cart is not directed at, and should not be used by, anyone under 18.

    Changes to this policy
    We may update this policy from time to time; continued use of the app after a change means you accept the update.

    Contact: info@theghostcart.com
    """,
    .terms: """
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
    We may suspend or terminate accounts that violate these Terms. You may delete your own account at any time from Profile.

    8. Disclaimer and limitation of liability
    The App and Site are provided "as is," without warranties of any kind. To the fullest extent permitted by UAE law, we are not liable for indirect, incidental, or consequential damages arising from your use of the App or Site.

    9. Governing law
    These Terms are governed by the laws of the United Arab Emirates. Any dispute is subject to the exclusive jurisdiction of the competent courts of the UAE.

    10. Contact: info@theghostcart.com
    """,
    .dataSecurity: """
    We take reasonable technical and organizational measures to protect your data, consistent with UAE Federal Decree-Law No. 45 of 2021 on the Protection of Personal Data.

    • Encryption in transit: all traffic between the App/Site and our servers uses HTTPS/TLS.
    • Password security: passwords are never stored in plain text. We store a salted, one-way cryptographic hash and cannot see or recover your actual password.
    • Session security: sign-in sessions use httpOnly, secure tokens; admin access is separately gated and audited.
    • Data minimization: the anonymous community feed and leaderboard never expose your email, password, or real name to other users.
    • Infrastructure: our backend runs on Cloudflare's Workers/D1 platform; sign-in and push notifications use Google Firebase. These providers maintain their own independent security certifications.
    • Retention and deletion: deleting your account (Profile → Delete Account) permanently removes your stored profile, cooldown history, favorites, and simulated wallet data from our production database.
    • Incident response: if we become aware of a data breach affecting your personal data, we will take reasonable steps to notify affected users and the relevant UAE authority without undue delay, as required by law.

    This page is a good-faith summary of our practices, not an exhaustive security audit. Questions: info@theghostcart.com
    """,
]

struct LegalDocumentView: View {
    let document: LegalDocument

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("Last updated: \(legalDocumentsLastUpdated)")
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
                Text((legalDocumentBodies[document] ?? "Legal copy for this document has not been supplied yet.")
                    .trimmingCharacters(in: .whitespacesAndNewlines))
                    .font(.subheadline)
                    .foregroundStyle(Color.secondary)
                    .lineSpacing(4)
            }
            .padding(20)
        }
        .navigationTitle(document.title)
        .navigationBarTitleDisplayMode(.inline)
        .background(Color(.systemBackground))
    }
}

struct LegalDocumentsSection: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeading(title: "Legal")
            GhostCard {
                VStack(spacing: 0) {
                    ForEach(Array(LegalDocument.allCases.enumerated()), id: \.element.id) { index, document in
                        NavigationLink {
                            LegalDocumentView(document: document)
                        } label: {
                            HStack {
                                Text(document.title).font(.subheadline.weight(.medium)).foregroundStyle(Color.primary)
                                Spacer()
                                Image(systemName: "chevron.right").font(.caption.weight(.bold)).foregroundStyle(Color.secondary)
                            }
                            .padding(.vertical, 12)
                        }
                        .buttonStyle(.plain)
                        if index < LegalDocument.allCases.count - 1 { Divider() }
                    }
                }
            }
        }
    }
}
