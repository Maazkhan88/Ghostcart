# Ghost Cart v2 analytics contract

Analytics exists to measure whether Ghost Cart helps users reach intentional decisions. It must not reward compulsive fake shopping.

## North star

`resolved_almost_buys_per_weekly_active_user`

A resolution is a user-confirmed skipped or intentionally bought outcome. Unresolved Fake Checkouts do not count.

## Funnel events

| Event | Required properties |
|---|---|
| `almost_buy_captured` | category, source, has_amount, has_url, trigger |
| `cooling_started` | almost_buy_id, duration_minutes, recommended_duration |
| `simulation_started` | almost_buy_id, mode |
| `simulation_completed` | almost_buy_id, mode |
| `cooling_ready` | almost_buy_id |
| `almost_buy_resolved` | almost_buy_id, outcome, cooling_minutes |
| `almost_buy_snoozed` | almost_buy_id, additional_minutes |
| `reminder_preference_changed` | reminder_type, enabled |
| `notification_opened` | notification_type, destination |

## Derived metrics

- Capture-to-cooling rate.
- Cooling-to-resolution rate.
- Confirmed-skip rate.
- Intentional-purchase rate.
- Median time to resolution.
- Seven-day returning-user rate.
- Unresolved-item rate.
- Notification open and disable rates by reminder type.

## Guardrails

- Never treat Almost Spent as savings.
- Never rank users by money or number of Ghost actions.
- Never expose an individual's triggers or outcomes in community trends.
- Do not collect product URLs, free-text notes, email, or names in analytics payloads.
- Public trends require aggregation and the privacy threshold defined by the backend.
- Analytics consent and deletion behavior must be documented before production collection is enabled.
