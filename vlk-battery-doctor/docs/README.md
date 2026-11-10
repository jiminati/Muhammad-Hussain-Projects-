# Reference data recovered from the DALY app

Extracted from `com.daly.bms.app` version 4.1.0 (base APK, English locale).
These are the names and labels the official app uses, so anything this app
shows matches what a technician sees in the DALY app.

| File | Rows | Contents |
|---|---|---|
| `daly_fault_flags.csv` | 78 | Every fault and status flag the firmware can raise |
| `daly_history_events.csv` | 117 | History log event names, with Level 1/2/3 where present |
| `daly_parameters.csv` | 189 | Every settable parameter, with its unit |

## To do

Mark each row of `daly_parameters.csv` with the tier it belongs to:

- `T` — technician may change it
- `E` — engineer PIN required
- `X` — never exposed in the app

That marking drives the Settings screen.
