# VLK Battery Doctor

An Android app for Vlektra after-sales technicians. It reads a Vlektra battery
pack straight off its DALY BMS, applies Vlektra's own field rules, and gives one
clear verdict plus the steps that follow from it — in Roman Urdu for the floor
and English for the report.

Vlektra R&D · developed by Sen

---

## What it does

| Screen | What it shows |
|---|---|
| Connect | Nearby packs, with chassis and batch when the pack is in the register |
| Pack | Live voltage, current, SOC, 24-cell grid, temperatures, board state |
| Diagnosis | One verdict — OK / watch / workshop / engineer — with the evidence |
| Log | The BMS fault log, with routine power-on entries filtered out |
| Settings | Every BMS parameter, split into technician, engineer and locked tiers |
| Report | Job card, then a WhatsApp-ready message or a full report to share |

## The four verdicts

| Verdict | Meaning |
|---|---|
| `NO FAULT` | Nothing needs work. Hand it back. |
| `MINOR` | Usable now. Re-check at the next service. |
| `REPAIR NEEDED` | Do not hand it back. The listed steps fix it. |
| `ENGINEER ONLY` | Stop. An engineer sees it before it goes back on a bike. |

Every finding carries the numbers it came from, so a technician — or the
engineer reading the report — can check the reasoning rather than trust it.

## Rules in this build

Thresholds live in one place: `diagnosis/RuleEngine.kt`, object `Thresholds`.

| Trigger | Verdict | Action |
|---|---|---|
| Cell 7 or 18 sags ≥300 mV below median on Batch 6 | Engineer | Busbar repair line, warranty candidate |
| Any other cell sags ≥300 mV | Workshop | Check connection, then plan cell replacement |
| Spread ≥100 mV | Workshop | Overnight active balancing at 2900 mV |
| Spread 50–99 mV | Watch | Re-read at next service |
| Lowest cell <2800 mV | Engineer | No normal-rate charge; supervised recovery |
| Temperature spread ≥15 °C | Workshop | Check the four NTCs |
| BMS SOC off by ≥25% from the OCV estimate | Watch | Full charge to re-reference |
| Short Circuit Protect in the log | Engineer | Inspect cabling, XT90, controller |
| ≥3 voltage sampling faults | Engineer | Sense harness or BMS front end |
| Temp sampling fault or ErrDeltaT | Workshop | Check all four NTCs |
| ≥10 cell under-voltage events | Workshop | Customer charging habits |
| Capacity Learning ON | Workshop | Turn off, one full charge |
| Active Balancing OFF | Workshop | Enable at 2900 mV, balance overnight |
| Range complaint with nothing else found | OK | Explain 100→0% range measurement |

## Project layout

```
app/src/main/java/com/vlektra/batterydoctor/
  MainActivity.kt            entry point, Bluetooth permissions
  AppState.kt                navigation and all app state
  bms/
    BmsSource.kt             the interface every data source implements
    SimulatedBms.kt          three real Vlektra packs, no hardware needed
    DalyBleSource.kt         real BLE — Modbus framing done, register map pending
  diagnosis/
    Diagnosis.kt             severity, finding and result types
    RuleEngine.kt            every threshold and every rule
  model/
    Models.kt                pack snapshot, history event, parameters, job card
    FaultCatalog.kt          78 DALY fault flags, recovered from the official app
  report/ReportBuilder.kt    WhatsApp message and full report
  ui/                        the six screens, plus theme and shared widgets
```

## Branding

Every colour comes from one object: `ui/theme/Theme.kt`, object `Vlk`. Change
the four brand values there and the whole app follows. Nothing else hard-codes
a brand colour.

The launcher icon is a vector in `res/drawable/ic_launcher_foreground.xml`.

## Current limitation: live Bluetooth

`DalyBleSource` has correct Modbus-RTU framing and CRC-16, and the right GATT
service and characteristics for the JHB-R24TK board family. What it does not yet
have is the register map: which register holds which value, how the history log
is walked, and which register each parameter lives at.

Until that is confirmed from the DALY protocol document, every live read fails
with a clear message rather than guessing. A wrong register read returns
nonsense; a wrong register **write** could change a protection threshold on a
live pack. The app will not guess on battery hardware.

Turn the switch on the Connect screen to **Demo packs** to use the app today.

### What is needed to finish it

1. The DALY communication protocol document for JHB-R24TK boards, covering the
   history-record read commands and the parameter register map. Ask the DALY
   sales contact — volume customers get these.
2. Failing that, a decoded capture of the official app talking to a pack.

With either one, `DalyBleSource` is a day of work and no screen changes.

## Parameter writing

Writing is switched off in this build. When it is enabled:

| Tier | Who | What |
|---|---|---|
| Technician | Any technician | Capacity Learning, Active Balancing, SOC recalibrate, balancing start |
| Engineer | PIN required | Voltage, current and temperature protection thresholds |
| Locked | Nobody in the app | Rated capacity, cell count, SOC calibration points, firmware |

Every write is recorded with who, when, the old value and the new value, before
it is sent.

## Building

Pushing to this repository builds the APK automatically. See
`.github/workflows/build-apk.yml` at the repository root.

To build locally instead:

```
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.
