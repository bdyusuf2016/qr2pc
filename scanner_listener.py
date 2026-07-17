import pyautogui
import firebase_admin
from firebase_admin import credentials, db
import time
import socket
import threading
import json
import random
import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox
import sys
import os

# ----------------- Configuration & Path Helper -----------------
FIREBASE_DB_URL = "https://qr2pc-72742-default-rtdb.asia-southeast1.firebasedatabase.app/"

def resource_path(relative_path):
    """ Get absolute path to resource, works for dev and for PyInstaller """
    try:
        # PyInstaller creates a temp folder and stores path in _MEIPASS
        base_path = sys._MEIPASS
    except Exception:
        base_path = os.path.abspath(".")
    return os.path.join(base_path, relative_path)

SERVICE_ACCOUNT_PATH = resource_path("serviceAccountKey.json")

class ScannerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("QR to PC Listener by Yusuf v2.2")
        self.root.geometry("500x450")

        random_id = f"{random.randint(1000, 9999)}"
        self.pairing_id = tk.StringVar(value=random_id)
        self.status_msg = tk.StringVar(value="Initializing...")
        self.is_connected = False
        self.listener_handle = None

        self.setup_ui()
        self.setup_firebase()

        # Start UDP Discovery
        threading.Thread(target=self.broadcast_presence, daemon=True).start()

    def setup_ui(self):
        main_frame = ttk.Frame(self.root, padding="20")
        main_frame.pack(fill=tk.BOTH, expand=True)

        # Header
        ttk.Label(main_frame, text="QR to PC Desktop by Yusuf", font=("Helvetica", 16, "bold")).pack(pady=10)

        # Status Light
        status_frame = ttk.Frame(main_frame)
        status_frame.pack(pady=5)
        self.canvas = tk.Canvas(status_frame, width=15, height=15)
        self.canvas.pack(side=tk.LEFT, padx=5)
        self.status_circle = self.canvas.create_oval(2, 2, 13, 13, fill="red")
        ttk.Label(status_frame, textvariable=self.status_msg).pack(side=tk.LEFT)

        # Pairing ID Entry
        id_frame = ttk.LabelFrame(main_frame, text=" Connection Settings ", padding="10")
        id_frame.pack(fill=tk.X, pady=15)

        ttk.Label(id_frame, text="Pairing ID:").pack(side=tk.LEFT, padx=5)
        ttk.Entry(id_frame, textvariable=self.pairing_id, width=10).pack(side=tk.LEFT, padx=5)
        ttk.Button(id_frame, text="Update ID", command=self.update_listener).pack(side=tk.LEFT, padx=5)

        # Log Area
        ttk.Label(main_frame, text="Scan Log:").pack(anchor=tk.W)
        self.log_area = scrolledtext.ScrolledText(main_frame, height=10, font=("Consolas", 9))
        self.log_area.pack(fill=tk.BOTH, expand=True, pady=5)

        dev_label = ttk.Label(main_frame, text="App Version: 2.2 | Created by Md. Yusuf Ali", foreground="#3366CC", cursor="hand2")
        dev_label.pack(pady=5)
        dev_label.bind("<Button-1>", lambda e: self.show_developer_info())

    def log(self, message):
        timestamp = time.strftime("%H:%M:%S")
        self.log_area.insert(tk.END, f"[{timestamp}] {message}\n")
        self.log_area.see(tk.END)

    def show_developer_info(self):
        info = (
            "Developer: Md. Yusuf Ali\n"
            "Email: mdyusufcbc@gmail.com\n"
            "Mob: +8801933814200"
        )
        messagebox.showinfo("Developer Info", info)

    def setup_firebase(self):
        try:
            if not firebase_admin._apps:
                cred = credentials.Certificate(SERVICE_ACCOUNT_PATH)
                firebase_admin.initialize_app(cred, {'databaseURL': FIREBASE_DB_URL})
            self.status_msg.set("Connected to Firebase ✅")
            self.canvas.itemconfig(self.status_circle, fill="green")
            self.is_connected = True
            self.update_listener()
        except Exception as e:
            self.status_msg.set(f"Connection Failed ❌")
            self.log(f"Firebase Error: {e}")

    def update_listener(self):
        if not self.is_connected: return

        # Remove old listener if exists
        if self.listener_handle:
            self.listener_handle.close()

        pid = self.pairing_id.get()
        self.log(f"Listening for Pairing ID: {pid}")

        # Start new listener for private path
        scan_ref = db.reference(f'scans/{pid}')
        self.listener_handle = scan_ref.listen(self.handle_new_scan)

    def handle_new_scan(self, event):
        if event.data:
            try:
                path = event.path
                scan_data = event.data

                if path == "/":
                    if isinstance(scan_data, dict):
                        for sid, data in scan_data.items():
                            self.process_item(sid, data)
                    return

                scan_id = path.split('/')[-1]
                self.process_item(scan_id, scan_data)

            except Exception as e:
                self.log(f"Error handling scan: {e}")

    def process_item(self, scan_id, data):
        if not isinstance(data, dict): return

        text = data.get('text')
        status = data.get('status', 'pending')

        if text and status == 'pending':
            self.log(f"Received: {text[:20]}...")

            # Type into PC
            try:
                time.sleep(0.1)
                pyautogui.write(text, interval=0.01)
                if not text.endswith('\n'):
                    pyautogui.press('enter')

                # Mark as processed
                pid = self.pairing_id.get()
                db.reference(f'scans/{pid}/{scan_id}').update({'status': 'processed'})
            except Exception as e:
                self.log(f"PyAutoGUI Error: {e}")

    def broadcast_presence(self):
        udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        udp_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

        while True:
            try:
                pid = self.pairing_id.get()
                # Get local IP
                s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                try:
                    s.connect(('8.8.8.8', 1))
                    local_ip = s.getsockname()[0]
                except:
                    local_ip = '127.0.0.1'
                finally:
                    s.close()

                discovery_data = json.dumps({
                    "type": "QR2PC_SERVER",
                    "pairingId": pid,
                    "ip": local_ip
                }).encode('utf-8')

                udp_socket.sendto(discovery_data, ('<broadcast>', 8888))

                # Cloud Heartbeat
                db.reference(f'servers/{pid}').update({
                    'lastSeen': time.time() * 1000,
                    'status': 'online'
                })
            except: pass
            time.sleep(5)

if __name__ == "__main__":
    pyautogui.FAILSAFE = True
    root = tk.Tk()
    app = ScannerApp(root)
    root.mainloop()
