# QR2PC

QR2PC is a desktop listener application that receives QR scan payloads from Firebase Realtime Database and types the results directly into the connected PC.

## Key Features

- Real-time Firebase RTDB listener for scan data
- Automatic input typing using `PyAutoGUI`
- Desktop GUI for pairing ID management and logs
- UDP broadcast discovery for local network presence
- Packaged with PyInstaller via `scanner_listener.spec`

## How It Works

The app listens on a Firebase path for new QR scan entries keyed by a configurable pairing ID. When a new scan arrives, it types the scanned text into the active window and marks the scan as processed.

## Requirements

- Python 3.11+ (recommended)
- `tkinter` (standard with Python on Windows)
- `pyautogui`
- `firebase-admin`
- Internet access for Firebase connectivity

## Setup

1. Install dependencies:

```powershell
pip install pyautogui firebase-admin
```

2. Add your Firebase service account key as `serviceAccountKey.json`.

3. Open `scanner_listener.py` and verify the `FIREBASE_DB_URL` is set to your Firebase Realtime Database URL.

4. Run the application:

```powershell
python scanner_listener.py
```

## Usage

1. Launch the app.
2. Copy or enter a pairing ID.
3. Use your mobile or scanner client to send scanned QR text to the Firebase path `scans/<pairing_id>`.
4. The app will type text into the active window and update the scan status to `processed`.

## Packaging

The repository includes `scanner_listener.spec` for PyInstaller packaging.

To build an executable:

```powershell
pyinstaller --onefile scanner_listener.spec
```

## Notes

- `serviceAccountKey.json` is excluded from Git and must be provided separately.
- `build/` and `dist/` directories are ignored in version control.
- Ensure the active window is ready to accept typed input before scans arrive.

## Contact

Created by Md. Yusuf Ali

Email: `mdyusufcbc@gmail.com`

Mobile: `+8801933814200`
