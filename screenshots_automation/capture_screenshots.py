#!/usr/bin/env python3
"""
capture_screenshots.py — Interactive ADB Screenshot Capture for Licha (TwitchChatTTS)
===================================================================================
Automates set-app-locales, starts Licha, and grabs screenshots via ADB, saving
them directly into the raw/ directories for phone, tablet_7, or tablet_10.

Requirements:
  1. A connected Android device or emulator with USB debugging enabled.
  2. The Licha app installed on that device.
  3. Python 3.

Run:
  python3 capture_screenshots.py
"""

import subprocess
import time
import os
import sys

LANGS = ["en-US", "es-ES", "fr-FR"]
PACKAGE = "com.lakescorp.twitchchattts"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RAW_DIR = os.path.join(SCRIPT_DIR, "raw")


def run_cmd(cmd):
    print(f"  → {cmd}")
    r = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE, text=True)
    if r.returncode != 0:
        print(f"    ⚠ stderr: {r.stderr.strip()}")
    return r.stdout.strip()


def check_adb():
    devices = run_cmd("adb devices").splitlines()
    # Filter out header and empty lines
    active_devices = [line for line in devices[1:] if line.strip() and "device" in line]
    if not active_devices:
        print("\n❌ ERROR: No Android devices or emulators connected via ADB.")
        print("   Please connect a device, enable USB debugging, and ensure 'adb devices' displays it.\n")
        return False
    return True


def screencap(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    # Capture screen directly to local path via ADB pipe
    run_cmd(f"adb shell screencap -p > {path}")
    print(f"    📸 Saved raw screenshot: {path}")
    time.sleep(0.5)


def capture_flow(form_factor):
    print(f"\n========================================================")
    print("🚀 Running FULLY AUTOMATED screenshot capture flow via ADB")
    print("========================================================")
    
    emulated = False
    try:
        # Override screen size and density to emulate tablets on a phone device
        if form_factor == "tablet_7":
            print("  [DPI/DENSITY OVERRIDE] Emulating 7-inch Tablet (1200x1920, 320 DPI)...")
            run_cmd("adb shell wm size 1200x1920")
            run_cmd("adb shell wm density 320")
            emulated = True
            time.sleep(2.0)
        elif form_factor == "tablet_10":
            print("  [DPI/DENSITY OVERRIDE] Emulating 10-inch Tablet (1600x2560, 320 DPI)...")
            run_cmd("adb shell wm size 1600x2560")
            run_cmd("adb shell wm density 320")
            emulated = True
            time.sleep(2.0)
            
        screens = ["Login", "Chat", "Settings"]
        
        # Process each language locale
        for lang in LANGS:
            print(f"\n🌍 Locale Track: {lang.upper()}")
            print("-" * 50)
            
            # 1. Update app locale
            print(f"  Setting Licha locale to {lang}...")
            run_cmd(f"adb shell cmd locale set-app-locales {PACKAGE} --locales {lang}")
            time.sleep(0.5)
            
            for screen in screens:
                # 2. Reset app stack
                run_cmd(f"adb shell am force-stop {PACKAGE}")
                time.sleep(0.5)
                
                # 3. Launch Licha in screenshot mode for target screen
                print(f"  Launching {screen} screen...")
                run_cmd(f"adb shell am start -n {PACKAGE}/.MainActivity --ez screenshot_mode true --es target_screen {screen} --es lang {lang}")
                time.sleep(2.8)  # Wait for layout & mock data injection to settle
                
                # 4. Capture screenshot automatically
                out_file = f"{screen.lower()}.png"
                screencap(os.path.join(RAW_DIR, form_factor, lang, out_file))
                
            # Force stop after locale is done
            run_cmd(f"adb shell am force-stop {PACKAGE}")
            print(f"\n✅ Finished locale: {lang}")
            
    finally:
        if emulated:
            print("\n  [RESTORE] Resetting device screen size & density back to native settings...")
            run_cmd("adb shell wm size reset")
            run_cmd("adb shell wm density reset")
            time.sleep(1.5)
            
    print("\n🎉 Screenshot capture session completed successfully!")
    print("Compiling showcase images...")
    
    # Run the generate_showcase.py script
    showcase_script = os.path.join(SCRIPT_DIR, "generate_showcase.py")
    subprocess.run(["python3", showcase_script])


def main():
    import argparse
    parser = argparse.ArgumentParser(description="ADB Screenshot Capturer for Licha")
    parser.add_argument("--form-factor", choices=["phone", "tablet_7", "tablet_10"], help="Device form factor to capture")
    args = parser.parse_args()

    print("=" * 60)
    print("   Licha (TwitchChatTTS) — ADB Screenshot Capturer")
    print("=" * 60)
    
    # Wake up screen and unlock
    run_cmd("adb shell input keyevent KEYCODE_WAKEUP")
    run_cmd("adb shell input keyevent 82")
    
    if not check_adb():
        sys.exit(1)
        
    if args.form_factor:
        form_factor = args.form_factor
    else:
        # Ask form factor
        print("Which form factor are you capturing screenshots for?")
        print("  1. Phone (default)")
        print("  2. 7-inch Tablet (tablet_7)")
        print("  3. 10-inch Tablet (tablet_10)")
        
        choice = input("\nEnter choice (1, 2, or 3): ").strip()
        if choice == "2":
            form_factor = "tablet_7"
        elif choice == "3":
            form_factor = "tablet_10"
        else:
            form_factor = "phone"
        
    print(f"\n🚀 Starting capture flow for form factor: {form_factor.upper()}")
    capture_flow(form_factor)


if __name__ == "__main__":
    main()
