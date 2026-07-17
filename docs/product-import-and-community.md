# Product link import and User Ghosted feed

## Goal

Restore the emotional and visual richness of shopping without turning Ghost Cart into a store. A user can browse curated product ideas, choose **Ghost buy** or **Cool it**, or share a public product page from any shopping/browser app into Ghost Cart.

## Import flow

1. In any shopping app or browser, the user taps **Share** and chooses Ghost Cart, or pastes a public HTTPS link.
2. Ghost Cart preserves any Android share title or thumbnail supplied by the sending app.
3. The hosted preview reads standard Open Graph, Twitter Card and Schema.org Product/Offer metadata. Amazon and Noon retain targeted best-effort fallbacks.
4. If title, image or price remains incomplete, Android opens the link in an isolated, invisible WebView using the device's normal browser identity and reads the same page metadata after it settles.
5. The preview always shows an image area. Missing or failed images show an explicit placeholder plus an editable image URL field.
6. Title, image, category and amount remain editable before the user chooses **Ghost buy** or **Cool it**.

## Safety rules

- Accept only public HTTPS links without credentials or custom ports; reject localhost, IP literals and private-looking host suffixes.
- Strip known tracking parameters while preserving product/variant parameters.
- Limit redirect count, response size and request duration.
- Never run page scripts on the server. Android's WebView fallback is isolated: no file/content access, no JavaScript bridge, no mixed content and no pop-up windows.
- Imported content is product metadata, not endorsement, sponsorship, proof of purchase or affiliation.
- No real payment or delivery occurs.

## Community flow

A user can explicitly and anonymously opt to publish a completed imported item as **User Ghosted**. The source link and profile are not exposed. Community activity and rankings remain subject to the privacy thresholds and duplicate controls in the backend.

Remote community images are an MVP limitation: before a public launch at scale, cache/proxy approved images through a Ghost Cart-controlled image service so other users do not request arbitrary retailer image hosts directly.

## Reliability

Link preview is best-effort. Sites may block cloud crawlers, return different regional pages, require consent, or render metadata late. The two-layer server + device strategy captures the same standard preview fields used by common link unfurlers when the page exposes them, while the editable fallback prevents a dead end.