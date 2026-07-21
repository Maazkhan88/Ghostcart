import { env } from "cloudflare:workers";
import type { Metadata } from "next";
import { GoogleAnalytics } from "./components/GoogleAnalytics";
import "./globals.css";
import "./site.css";

export const metadata: Metadata = {
  title: "Ghost Cart — Before you buy it, Ghost it.",
  description:
    "Capture an almost-buy, cool the urge, then decide with a clear head. Ghost Cart is simulation-only: no real payment and no real delivery.",
  keywords: ["Ghost Cart", "almost bought", "cooling off", "impulse shopping", "shopping simulation"],
  openGraph: {
    title: "Ghost Cart — Before you buy it, Ghost it.",
    description: "The cooling-off app for everything you almost bought.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const gaMeasurementId = (env.GA_MEASUREMENT_ID as string | undefined) ?? null;
  return (
    <html lang="en">
      <body>
        <GoogleAnalytics measurementId={gaMeasurementId} />
        {children}
      </body>
    </html>
  );
}
