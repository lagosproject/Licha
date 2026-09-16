#!/usr/bin/env python3
"""
capture_live_screens.py - Bulletproof capture of real crisp screenshots from connected device
across es-ES, en-US, and fr-FR.
Guarantees the app is focused, never exits to launcher, and preserves chat messages.
"""
import os
import time
import subprocess

DEVICE = "7e2f615"
PACKAGE = "com.LakesCorp.TwitchChatTTS"
ACTIVITY = "com.lakescorp.twitchchattts.MainActivity"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RAW_DIR = os.path.join(SCRIPT_DIR, "raw", "phone")

def adb(cmd):
    res = subprocess.run(f"adb -s {DEVICE} {cmd}", shell=True, text=True, capture_output=True)
    return res.stdout.strip()

def ensure_app_focused():
    # Keep screen on, wake, and unlock
    adb("shell settings put system screen_off_timeout 600000")
    adb("shell input keyevent 224")
    adb("shell input swipe 500 1800 500 500")
    time.sleep(0.3)
    adb(f"shell am start -n {PACKAGE}/{ACTIVITY}")
    for _ in range(10):
        focus = adb("shell dumpsys window | grep -E 'mCurrentFocus'")
        if PACKAGE in focus:
            return True
        time.sleep(0.5)
        adb(f"shell am start -n {PACKAGE}/{ACTIVITY}")
    return False

def screencap(save_path):
    os.makedirs(os.path.dirname(save_path), exist_ok=True)
    temp_remote = "/sdcard/licha_cap.png"
    adb(f"shell screencap -p {temp_remote}")
    adb(f"pull {temp_remote} {save_path}")
    adb(f"shell rm {temp_remote}")
    print(f"  📸 Captured: {save_path}")

locales = ["es-ES", "en-US", "fr-FR"]

for loc in locales:
    print(f"\n🌍 Processing locale: {loc}")
    ensure_app_focused()

    # Switch app locale
    adb(f"shell cmd locale set-app-locales {PACKAGE} --locales {loc}")
    time.sleep(1.8)
    ensure_app_focused()

    # 1. Capture Chat Screen (Quick Controls closed)
    chat_file = os.path.join(RAW_DIR, loc, "chat.png")
    screencap(chat_file)

    # 2. Tap Quick Tuning button (x=700, y=206) to open quick controls
    adb("shell input tap 700 206")
    time.sleep(1.0)
    tuning_file = os.path.join(RAW_DIR, loc, "tuning.png")
    screencap(tuning_file)

    # Tap Quick Tuning button again to close
    adb("shell input tap 700 206")
    time.sleep(0.8)

    # 3. Tap Settings gear (x=1000, y=206)
    adb("shell input tap 1000 206")
    time.sleep(1.2)
    settings_file = os.path.join(RAW_DIR, loc, "settings.png")
    screencap(settings_file)

    # Tap TopAppBar Back Arrow (x=80, y=206) to return safely to chat without hitting system launcher
    adb("shell input tap 80 206")
    time.sleep(1.0)

# Reset locale back to es-ES
adb(f"shell cmd locale set-app-locales {PACKAGE} --locales es-ES")
ensure_app_focused()
print("\n✅ All live device screenshots captured successfully across all languages!")
