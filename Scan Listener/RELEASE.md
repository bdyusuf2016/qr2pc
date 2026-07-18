# QR 2 PC Desktop Ultimate - Release Notes

## Version

- Version: v4.3
- Release Date: 2026-07-19
- Platform: Windows

## Overview

QR 2 PC Desktop Ultimate is a Windows desktop application that enables seamless mobile-to-PC QR scanning. Scanned content is delivered to the computer and typed into the active window, making it easy to transfer text quickly and efficiently.

## What’s New in This Release

- Refined dark-mode desktop UI with a more premium look
- Improved pairing experience between mobile and PC
- Better reliability for receiving and processing scanned content
- Smoother text typing into the active application
- Enhanced overall stability during continuous use

## Key Features

- Mobile-to-PC QR pairing
- Automatic delivery of scanned content to the desktop
- Real-time typing into the active window
- Link and unlink support for sessions
- Lightweight Windows-based input handling

## System Requirements

- Windows 10 or later
- Internet connection required for backend communication
- Python runtime required for development builds
- PyInstaller recommended for building the executable

## Installation

1. Download the release executable from the release package.
2. Run the application on Windows.
3. Use the displayed QR code and token to pair your mobile device.
4. Start scanning QR codes from the mobile app.

## Usage Notes

- The app listens for a paired device and sends incoming scanned text to the cursor location.
- The pairing token is shown on the desktop app for quick setup.
- Use the logout/unlink option to reset the connection whenever needed.

## Build Information

This release is packaged as a Windows executable using PyInstaller.

## Notes

- This release depends on the Firebase backend for communication.
- Some applications may require specific handling for certain characters or input formats.

## Developer

- Developer: Md. Yusuf Ali
- Brand: Yusuf Tech
- GitHub: https://github.com/bdyusuf2016
