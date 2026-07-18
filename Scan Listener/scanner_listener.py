import requests
import json
import time
import socket
import threading
import uuid
import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox
import ctypes  # Zero-dependency typing
import os
import sys
import qrcode
from PIL import Image, ImageTk

# ----------------- Configuration & Helper -----------------
DB_URL = "https://qr2pc-72742-default-rtdb.asia-southeast1.firebasedatabase.app/"
CONFIG_FILE = "config_v4.json"

def resource_path(relative_path):
    """ Get absolute path to resource, works for dev and for PyInstaller """
    try:
        # PyInstaller creates a temp folder and stores path in _MEIPASS
        base_path = sys._MEIPASS
    except Exception:
        base_path = os.path.abspath(".")
    return os.path.join(base_path, relative_path)

# Included for user request, though current REST logic doesn't use it
SERVICE_ACCOUNT_FILE = resource_path("serviceAccountKey.json")

class YusufScannerPC:
    def __init__(self, root):
        self.root = root
        self.root.title("QR 2 PC Desktop Ultimate - Yusuf Tech v4.3")
        self.root.geometry("500x600")
        self.root.configure(bg="#121212")

        self.pairing_id = tk.StringVar()
        self.token = str(uuid.uuid4())[:6].upper()
        self.is_linked = False
        self.running = True

        self.load_config()
        self.setup_styles()
        self.setup_ui()

        # Start background threads
        threading.Thread(target=self.pairing_listener, daemon=True).start()
        if self.is_linked:
            threading.Thread(target=self.data_listener, daemon=True).start()
            threading.Thread(target=self.heartbeat, daemon=True).start()

    def setup_styles(self):
        style = ttk.Style()
        style.theme_use('clam')
        style.configure("TFrame", background="#121212")
        style.configure("TLabel", background="#121212", foreground="white", font=("Segoe UI", 10))
        style.configure("Header.TLabel", font=("Segoe UI", 16, "bold"), foreground="#4285F4")
        style.configure("TButton", font=("Segoe UI", 10, "bold"))

    def setup_ui(self):
        for widget in self.root.winfo_children(): widget.destroy()

        main_container = ttk.Frame(self.root, padding=20)
        main_container.pack(fill=tk.BOTH, expand=True)

        ttk.Label(main_container, text="QR 2 PC ULTIMATE", style="Header.TLabel").pack(pady=10)

        if not self.is_linked:
            ttk.Label(main_container, text="Scan to Pair with Mobile", font=("Segoe UI", 11)).pack(pady=5)

            # QR Code
            qr_data = f"QR2PC:PAIR:{self.token}:{socket.gethostname()}"
            img = qrcode.make(qr_data).resize((250, 250))
            self.qr_img = ImageTk.PhotoImage(img)
            ttk.Label(main_container, image=self.qr_img).pack(pady=10)

            ttk.Label(main_container, text=f"TOKEN: {self.token}", font=("Consolas", 14, "bold"), foreground="#FBBC05").pack()
            ttk.Label(main_container, text="Waiting for mobile app...", foreground="#999").pack(pady=10)
        else:
            ttk.Label(main_container, text=f"Device: {socket.gethostname()}", foreground="#34A853").pack()
            ttk.Label(main_container, text=f"Linked ID: {self.pairing_id.get()}", font=("Consolas", 10)).pack(pady=5)

            self.log_box = scrolledtext.ScrolledText(main_container, height=15, bg="#1E1E1E", fg="#CCC", font=("Consolas", 9), borderwidth=0)
            self.log_box.pack(fill=tk.BOTH, expand=True, pady=10)
            self.log("System Online. Listening for scans...")

            ttk.Button(main_container, text="LOGOUT / UNLINK", command=self.logout).pack(pady=5)

        # Developer Info Footer
        footer = ttk.Frame(main_container)
        footer.pack(side=tk.BOTTOM, fill=tk.X, pady=10)
        ttk.Label(footer, text="Developer: Md. Yusuf Ali | Yusuf Tech", font=("Segoe UI", 8), foreground="#666").pack()
        link = tk.Label(footer, text="github.com/bdyusuf2016", font=("Segoe UI", 8), fg="#4285F4", bg="#121212", cursor="hand2")
        link.pack()
        link.bind("<Button-1>", lambda e: os.startfile("https://github.com/bdyusuf2016"))

    def log(self, text):
        if hasattr(self, 'log_box'):
            self.log_box.insert(tk.END, f"[{time.strftime('%H:%M:%S')}] {text}\n")
            self.log_box.see(tk.END)

    def load_config(self):
        if os.path.exists(CONFIG_FILE):
            try:
                with open(CONFIG_FILE, 'r') as f:
                    data = json.load(f)
                    self.pairing_id.set(data.get("pid", ""))
                    self.is_linked = data.get("linked", False)
            except: pass

    def save_config(self):
        with open(CONFIG_FILE, 'w') as f:
            json.dump({"pid": self.pairing_id.get(), "linked": self.is_linked}, f)

    def pairing_listener(self):
        while not self.is_linked and self.running:
            try:
                r = requests.get(f"{DB_URL}/pairing/{self.token}.json")
                if r.status_code == 200 and r.json():
                    data = r.json()
                    self.pairing_id.set(data['pairingId'])
                    self.is_linked = True
                    self.save_config()
                    requests.delete(f"{DB_URL}/pairing/{self.token}.json")
                    self.root.after(0, self.setup_ui)
                    threading.Thread(target=self.data_listener, daemon=True).start()
                    threading.Thread(target=self.heartbeat, daemon=True).start()
                    break
            except: pass
            time.sleep(2)

    def data_listener(self):
        pid = self.pairing_id.get()
        processed_ids = set()

        while self.is_linked and self.running:
            try:
                # Fetch scans for this ID
                r = requests.get(f"{DB_URL}/scans/{pid}.json")
                if r.status_code == 200 and r.json():
                    items = r.json()
                    for sid, val in items.items():
                        if isinstance(val, dict) and val.get('status') == 'pending':
                            if sid not in processed_ids:
                                text = val.get('text', '')
                                self.log(f"Received: {text[:30]}...")
                                self.type_text(text)
                                # Mark as processed in Firebase
                                requests.patch(f"{DB_URL}/scans/{pid}/{sid}.json", data=json.dumps({"status": "processed"}))
                                processed_ids.add(sid)

                # Check for remote logout
                rs = requests.get(f"{DB_URL}/servers/{pid}/status.json")
                if rs.text.strip('"') == "unlinked":
                    self.logout(remote=True)
                    break
            except Exception as e:
                print(f"Polling Error: {e}")

            time.sleep(1)

    def type_text(self, text):
        """Ultra-lightweight typing using Windows API (Zero dependencies)"""
        for char in text:
            if char == '\n':
                self.send_key(0x0D) # Enter
            elif char == '\t':
                self.send_key(0x09) # Tab
            else:
                # Basic typing logic for characters
                try:
                    vk = ctypes.windll.user32.VkKeyScanW(ord(char))
                    if vk != -1:
                        self.send_key(vk & 0xFF)
                except: pass

    def send_key(self, code):
        ctypes.windll.user32.keybd_event(code, 0, 0, 0)
        ctypes.windll.user32.keybd_event(code, 0, 2, 0)

    def heartbeat(self):
        pid = self.pairing_id.get()
        while self.is_linked and self.running:
            try:
                requests.patch(f"{DB_URL}/servers/{pid}.json", data=json.dumps({
                    "lastSeen": int(time.time() * 1000),
                    "status": "online",
                    "name": socket.gethostname()
                }))
            except: pass
            time.sleep(10)

    def logout(self, remote=False):
        if not remote:
            try: requests.patch(f"{DB_URL}/servers/{self.pairing_id.get()}.json", data=json.dumps({"status": "unlinked"}))
            except: pass

        self.is_linked = False
        self.pairing_id.set("")
        self.save_config()
        self.root.after(0, self.setup_ui)
        threading.Thread(target=self.pairing_listener, daemon=True).start()

if __name__ == "__main__":
    root = tk.Tk()
    app = YusufScannerPC(root)
    root.protocol("WM_DELETE_WINDOW", lambda: (setattr(app, 'running', False), root.destroy()))
    root.mainloop()
