# QR 2 PC by Yusuf (Version 2.3)

**QR 2 PC** is a professional Android application designed to scan QR codes and Barcodes (including NID PDF417) and instantly send the data to a connected PC.

![Banner](https://github.com/bdyusuf2016/qr2pc-android/raw/main/assets/banner.png) <!-- Replace with your actual banner link later -->

## 🚀 Key Features
- ⚡ **Instant Data Transfer**: Scan on your phone, and the text is automatically typed onto your PC.
- 🎯 **NID Card Support**: Optimized for high-density PDF417 barcodes used in National ID cards.
- 🌓 **Text Capture (OCR)**: Deliberate scanning—freeze and preview detected text before sending it to the PC.
- 🌐 **Full Multilingual Support**: Toggle between **English** and **বাংলা (Bengali)** seamlessly.
- 🖥️ **Desktop GUI**: A user-friendly Windows listener app with real-time status and logs.
- 📊 **Scan History**: Full-screen log to view, favorite, and manage your previous scans.
- 🔋 **Batch Mode**: Fast, sequential scanning without any delays.

## 🛠️ How to Setup

### 1. Desktop Listener (Windows)
1.  Download the latest `scanner_listener.exe` from the [Releases](https://github.com/bdyusuf2016/qr2pc-android/releases) page.
2.  Run the application on your PC.
3.  Ensure your PC is connected to the internet (or same WiFi as phone).

### 2. Android App
1.  Install the APK on your Android device.
2.  Open the app and grant camera permissions.
3.  If on the same WiFi, the app will **Auto-Pair**. Otherwise, manually enter the **Pairing ID** from the Desktop app into the Mobile app's settings.

## 📁 Repository Structure
- `/app`: The complete Android Studio project source code.
- `scanner_listener.py`: Python source code for the Desktop GUI.
- `README.md`: Project documentation.

## ⚠️ Security Warning
> [!CAUTION]
> The source code requires a `serviceAccountKey.json` for Firebase Admin SDK and a `google-services.json` for the Android app. These files are **NOT** included in the repository for security reasons. You must provide your own Firebase configuration to build from source.

## 🤝 Connect with Me
- **GitHub**: [bdyusuf2016](https://github.com/bdyusuf2016)
- **LinkedIn**: [Md. Yusuf Ali](https://bd.linkedin.com/in/bdyusuf2016)
- **Facebook**: [Yusuf Ali](https://www.facebook.com/bdyusuf2016)

---
Developed with ❤️ by **Md. Yusuf Ali**
