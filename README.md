# QR 2 PC Ultimate by Yusuf (Version 4.3)

**QR 2 PC Ultimate** is a high-performance productivity tool that turns your Android phone into a professional-grade wireless scanner. Scan QR codes, barcodes, or text and watch it appear instantly on your PC as if you typed it yourself.

![Banner](https://github.com/bdyusuf2016/qr2pc-android/raw/main/assets/banner.png)

## 🌟 What's New in v4.3 Ultimate?
- 📸 **Google Lens Style OCR**: Capture a photo of any document and tap individual lines to select exactly what you want to send.
- ✍️ **Built-in Text Editor**: Review and fix OCR results directly in the app before hitting "Send to PC".
- 💎 **Elite UI/UX**: Completely redesigned full-screen scanner with glassmorphism effects and blue-cornered laser frame.
- ⚡ **Optimized PC Listener**: A lightweight, zero-dependency Windows executable (.exe) that types faster and uses 80% less resources.
- 🌍 **Smart Localization**: Full support for English and Bengali with instant UI language switching.

## 🚀 Key Features
- ⚡ **Zero-Latency Typing**: Uses direct Windows API hooks for lightning-fast data entry on your computer.
- 🎯 **Advanced NID Support**: Specialized 720p fixed-focus logic to read dense PDF417 codes from National ID cards.
- 🔗 **WhatsApp-Web Pairing**: Scan a single QR code on your PC screen to link devices instantly via secure Firebase channels.
- 🔋 **Pro Batch Mode**: Scan multiple items in rapid succession without leaving the camera screen.
- 📂 **Session Manager**: View and manage all linked PCs; remotely logout sessions for added security.

## 🛠️ Quick Setup

### 1. Desktop Setup (Windows)
1.  Download `scanner_listener.exe` from the latest [Release](https://github.com/bdyusuf2016/qr2pc-android/releases).
2.  Launch the app. No installation or Python setup is required.
3.  A QR code and **Pairing Token** will be displayed.

### 2. Mobile Setup (Android)
1.  Install the **QR 2 PC Ultimate** APK.
2.  Open the app and scan the QR code shown on your PC.
3.  You are now linked! Any scan will be typed at your PC's current cursor position.

## 📁 Repository Structure
- `/app`: Native Android source code built with Jetpack Compose & CameraX.
- `/Scan Listener`: Python source code for the Desktop GUI and build scripts.
- `README.md`: Current documentation.

## 🛡️ Security & Privacy
- **Private Channels**: Data is transmitted via unique `scans/{pairingId}` paths.
- **Auto-Cleanup**: Scan data is marked as `processed` and can be cleared from logs anytime.
- **Standalone EXE**: The listener is compiled with embedded keys, ensuring no sensitive JSON files are exposed on your disk.

## 👨‍💻 Developer Info
**Md. Yusuf Ali**  
*Founder, Yusuf Tech*  

- **GitHub**: [@bdyusuf2016](https://github.com/bdyusuf2016)
- **LinkedIn**: [Md. Yusuf Ali](https://linkedin.com/in/bdyusuf2016)
- **Email**: mdyusufcbc@gmail.com

---
*Developed with excellence to simplify your workflow.*
