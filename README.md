# Karoo Metrics Overlay

A Kotlin Android app for the [Hammerhead Karoo](https://www.hammerhead.io/) bike computer that streams live ride metrics over WiFi as a browser overlay — designed for live streaming with OBS Studio, Streamlabs, or any tool that supports browser sources.

The app runs a lightweight server directly on the Karoo. Point any browser or streaming tool to the overlay URL and get real-time cycling metrics composited over your video feed.

**[Download the latest APK](https://github.com/zenpeartree/karoo-metrics-overlay/releases/latest/download/app-release.apk)**

## Metrics

| Metric | Details |
|--------|---------|
| **Power** | Current watts with 7-zone color coding (based on your FTP) |
| **Heart Rate** | Current BPM with 5-zone color coding (based on your max HR) |
| **Speed** | Current speed in km/h |
| **Distance** | Ride distance in km |
| **Grade** | Current gradient % (color-coded: red uphill, cyan downhill) |
| **Avg Power** | Ride average power in watts |

### Power Zones

| Zone | Range | Color |
|------|-------|-------|
| Z1 Recovery | < 55% FTP | Gray |
| Z2 Endurance | 55–75% | Blue |
| Z3 Tempo | 75–90% | Green |
| Z4 Threshold | 90–105% | Yellow |
| Z5 VO2max | 105–120% | Orange |
| Z6 Anaerobic | 120–150% | Red |
| Z7 Sprint | > 150% | Purple |

### Heart Rate Zones

| Zone | Range | Color |
|------|-------|-------|
| Z1 Recovery | < 60% max | Gray |
| Z2 Aerobic | 60–70% | Blue |
| Z3 Tempo | 70–80% | Green |
| Z4 Threshold | 80–90% | Yellow |
| Z5 VO2max | > 90% | Red |

## Install

### Prerequisites

- Hammerhead Karoo (K2 or later) with developer mode enabled
- ADB installed on your computer ([install guide](https://developer.android.com/tools/adb))
- The Karoo and your streaming device on the **same WiFi network**

### Steps

1. Download the latest `app-release.apk` from [Releases](../../releases)
2. Connect to your Karoo via ADB:
   ```bash
   adb connect <karoo-ip>:5555
   ```
3. Install the APK:
   ```bash
   adb install app-release.apk
   ```

## Usage

### Configure

1. On the Karoo, open **Karoo Metrics Overlay** from the app drawer
2. Enter your **FTP** (watts) and **Max HR** (bpm) — these are used to calculate zones
3. Tap **Start Server**
4. The app displays the overlay URL (e.g., `http://192.168.1.42:9091/`)

### Add to OBS / Streaming Tool

1. In OBS: **Sources → Add → Browser**
2. Set the URL to the address shown in the app
3. Width: **440**, Height: **180**
4. Position the overlay in your desired corner
5. The background is transparent — the video feed shows through
6. Start a ride on the Karoo — metrics update in real time

### Mobile Streaming Setup

For fully mobile setups (e.g., streaming from a phone while riding):

1. Use your phone as a WiFi hotspot
2. Connect the Karoo to the hotspot
3. Use a streaming app that supports browser sources (e.g., Streamlabs)
4. Add the overlay URL as a web/browser layer

## License

Apache 2.0
