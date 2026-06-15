#!/usr/bin/env python3
"""
generate_showcase.py — Play Store Screenshot & Feature Graphic Generator for Licha (TwitchChatTTS)
=============================================================================================
Creates beautiful 1080×1920 showcase cards (for Phone, 7-inch Tablet, and 10-inch Tablet)
in English, Spanish, and French, plus a 1024×500 Feature Graphic banner.

If raw screenshots are not found under `raw/`, this script automatically generates
highly polished, branded mock screenshots for Login, Chat, and Settings screens.

Run:
  python3 generate_showcase.py
"""

import os
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ─── Constants ───
OUT_W = 1080
OUT_H = 1920

# Mock status and navigation bar heights for raw screen cropping/masking
STATUS_BAR = 96
NAV_BAR = 120

LANGS = ["en-US", "es-ES", "fr-FR"]
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RAW_DIR = os.path.join(SCRIPT_DIR, "raw")
OUT_DIR = os.path.join(SCRIPT_DIR, "output")

# Brand colors (Twitch-inspired palette)
ACCENT = (145, 70, 255)       # Twitch Purple (#9146FF)
ACCENT_LIGHT = (169, 112, 255) # Light Twitch Purple (#A970FF)
BG_DARK = (14, 14, 16)         # Dark background (#0E0E10)
BG_SURFACE = (24, 24, 27)      # Dark surface (#18181B)
BG_CARD = (31, 31, 35)         # Dark card/bubble (#1F1F23)
WHITE = (239, 239, 241)        # Text light (#EFEFF1)
MUTED = (173, 173, 184)        # Text muted (#ADADB8)
GREEN = (0, 245, 212)          # Alert/Success Green (#00F5D4)
RED = (255, 85, 85)            # Alert/Error Red (#FF5555)

# ─── Localized Copy Config ───
LANG_COPY = {
    "en-US": {
        "brand": "L I C H A",
        "login": {
            "title": "SECURE LOGIN",
            "headline": "SECURE AUTHENTICATION",
            "subtext": "Connect with Twitch OAuth using secure client credentials."
        },
        "chat": {
            "title": "TWITCH TTS",
            "headline": "REAL-TIME SPEECH",
            "subtext": "Listen to your chat messages aloud while you focus on streaming."
        },
        "settings": {
            "title": "SETTINGS",
            "headline": "TAILORED SPEECH SETTINGS",
            "subtext": "Adjust speech voice, rate, pitch, and chat filters to your liking."
        },
        "feature_graphic": {
            "headline": "L I C H A",
            "subtext": "Twitch Chat Text-to-Speech Reader"
        }
    },
    "es-ES": {
        "brand": "L I C H A",
        "login": {
            "title": "ACCESO SEGURO",
            "headline": "CONEXIÓN SEGURA",
            "subtext": "Conéctate con Twitch OAuth utilizando credenciales seguras."
        },
        "chat": {
            "title": "TWITCH TTS",
            "headline": "CHAT A VOZ EN TIEMPO REAL",
            "subtext": "Escucha los mensajes del chat en tiempo real mientras transmites."
        },
        "settings": {
            "title": "AJUSTES",
            "headline": "VOZ Y FILTROS A TU MEDIDA",
            "subtext": "Ajusta voces del sintetizador, velocidad, tono y filtros a tu gusto."
        },
        "feature_graphic": {
            "headline": "L I C H A",
            "subtext": "Lector de Chat de Twitch por Voz (TTS)"
        }
    },
    "fr-FR": {
        "brand": "L I C H A",
        "login": {
            "title": "CONNEXION",
            "headline": "CONNEXION SÉCURISÉE",
            "subtext": "Connectez-vous via Twitch OAuth avec des identifiants sécurisés."
        },
        "chat": {
            "title": "TWITCH TTS",
            "headline": "LECTURE DU CHAT EN DIRECT",
            "subtext": "Écoutez les messages de votre chat à haute voix tout en diffusant."
        },
        "settings": {
            "title": "OPTIONS",
            "headline": "PARAMÈTRES SUR MESURE",
            "subtext": "Ajustez la vitesse, le volume, le pitch et les filtres de chat."
        },
        "feature_graphic": {
            "headline": "L I C H A",
            "subtext": "Synthèse Vocale pour le Chat Twitch"
        }
    }
}

# ─── Font Helpers ───
def _try_fonts(paths, size):
    for p in paths:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                continue
    return ImageFont.load_default()

def font_sans(size, bold=False):
    return _try_fonts([
        f"/usr/share/fonts/truetype/dejavu/DejaVuSans{'-Bold' if bold else ''}.ttf",
        f"/usr/share/fonts/truetype/liberation/LiberationSans-{'Bold' if bold else 'Regular'}.ttf",
        "/usr/share/fonts/truetype/freefont/FreeSansBold.ttf" if bold else "/usr/share/fonts/truetype/freefont/FreeSans.ttf"
    ], size)

# ─── Drawing Helpers ───
def center_text(draw, text, y, font, fill, width=OUT_W):
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    x = (width - tw) // 2
    draw.text((x, y), text, font=font, fill=fill)

def wrap_text_centered(draw, text, y, font, fill, max_w, line_gap=10, width=OUT_W):
    words = text.split()
    lines, cur = [], ""
    for w in words:
        test = f"{cur} {w}".strip()
        if draw.textbbox((0, 0), test, font=font)[2] <= max_w:
            cur = test
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)

    lh = draw.textbbox((0, 0), "Ag", font=font)[3] - draw.textbbox((0,0), "Ag", font=font)[1] + line_gap
    for i, ln in enumerate(lines):
        center_text(draw, ln, y + i * lh, font, fill, width)
    return len(lines) * lh

def get_wrap_height(draw, text, font, max_w, line_gap=10):
    words = text.split()
    lines, cur = [], ""
    for w in words:
        test = f"{cur} {w}".strip()
        if draw.textbbox((0, 0), test, font=font)[2] <= max_w:
            cur = test
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    lh = draw.textbbox((0, 0), "Ag", font=font)[3] - draw.textbbox((0,0), "Ag", font=font)[1] + line_gap
    return len(lines) * lh

def accent_rule(draw, y, length=140, color=None, width=OUT_W):
    color = color or (*ACCENT, 200)
    cx = width // 2
    draw.line([(cx - length // 2, y), (cx + length // 2, y)], fill=color, width=3)

def dark_gradient(w, h, tint_ratio=0.35):
    img = Image.new("RGB", (w, h), BG_DARK)
    d = ImageDraw.Draw(img)
    for i in range(h):
        t = i / h
        r = int(BG_DARK[0] + (ACCENT[0] - BG_DARK[0]) * t * tint_ratio)
        g = int(BG_DARK[1] + (ACCENT[1] - BG_DARK[1]) * t * tint_ratio * 0.4)
        b = int(BG_DARK[2] + (ACCENT[2] - BG_DARK[2]) * t * tint_ratio * 0.8)
        d.line([(0, i), (w, i)], fill=(r, g, b))
    return img

def rounded_rect_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([(0, 0), (size[0] - 1, size[1] - 1)], radius=radius, fill=255)
    return mask

def apply_shadow(img, blur=18, offset=(6, 10), color=(0, 0, 0, 160)):
    pad = blur * 2
    sw, sh = img.width + pad + abs(offset[0]), img.height + pad + abs(offset[1])
    shadow = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    stamp = Image.new("RGBA", img.size, color)
    mask = img.split()[3]
    ox, oy = pad // 2 + max(0, offset[0]), pad // 2 + max(0, offset[1])
    shadow.paste(stamp, (ox, oy), mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    ix, iy = pad // 2 + max(0, -offset[0]), pad // 2 + max(0, -offset[1])
    shadow.paste(img, (ix, iy), img)
    return shadow, (ix - ox, iy - oy)

def draw_licha_logo(draw, cx, cy, height, foreground_color=WHITE, accent_color=ACCENT):
    """
    Draws the Licha logo: a speech bubble containing a speaker icon.
    """
    scale = height / 120.0
    bw, bh = int(110 * scale), int(80 * scale)
    bx1, by1 = cx - bw // 2, cy - bh // 2 - int(10 * scale)
    bx2, by2 = cx + bw // 2, cy + bh // 2 - int(10 * scale)
    
    # Draw speech bubble body
    draw.rounded_rectangle([bx1, by1, bx2, by2], radius=int(18 * scale), fill=accent_color)
    
    # Tail (triangle at bottom-left)
    tx1 = bx1 + int(20 * scale)
    ty1 = by2
    tx2 = bx1 + int(40 * scale)
    ty2 = by2
    tx3 = bx1 + int(12 * scale)
    ty3 = by2 + int(20 * scale)
    draw.polygon([(tx1, ty1), (tx2, ty1), (tx3, ty3)], fill=accent_color)
    
    # Speaker icon inside the bubble
    sx = cx - int(25 * scale)
    sy = cy - int(20 * scale)
    sw, sh = int(15 * scale), int(20 * scale)
    draw.rectangle([sx, sy + int(2 * scale), sx + sw, sy + sh - int(2 * scale)], fill=foreground_color)
    
    cx1 = sx + sw
    cy1 = sy + int(5 * scale)
    cx2 = cx1 + int(15 * scale)
    cy2 = sy
    cx3 = cx2
    cy3 = sy + sh
    cx4 = cx1
    cy4 = sy + sh - int(5 * scale)
    draw.polygon([(cx1, cy1), (cx2, cy2), (cx3, cy3), (cx4, cy4)], fill=foreground_color)
    
    # Arcs
    draw.arc([cx - int(2 * scale), cy - int(12 * scale), cx + int(14 * scale), cy + int(12 * scale)], start=300, end=60, fill=foreground_color, width=int(2.5 * scale))
    draw.arc([cx - int(10 * scale), cy - int(20 * scale), cx + int(22 * scale), cy + int(20 * scale)], start=310, end=50, fill=foreground_color, width=int(2.5 * scale))


# ─── Mock Screenshot Generator ───
def generate_mock_screenshot(screen_type, lang, width, height, channel_name="twitch_streamer"):
    """
    Generates a highly aesthetic mock Android screenshot for Licha.
    Used automatically when raw/ screenshots are missing.
    """
    img = Image.new("RGB", (width, height), BG_DARK)
    draw = ImageDraw.Draw(img)
    
    # Constants scaled relative to width
    pad = int(width * 0.05)
    f_title = font_sans(int(width * 0.065), bold=True)
    f_body = font_sans(int(width * 0.04))
    f_sub = font_sans(int(width * 0.035), bold=True)
    f_meta = font_sans(int(width * 0.03))
    
    # ── 1. Status Bar
    draw.text((pad, int(height * 0.015)), "12:00", font=f_meta, fill=WHITE)
    # Draw simple battery & wifi symbols
    bx = width - pad - int(width * 0.08)
    by = int(height * 0.018)
    draw.rectangle([bx, by, bx + int(width * 0.05), by + int(height * 0.012)], outline=WHITE, width=1)
    draw.rectangle([bx, by, bx + int(width * 0.035), by + int(height * 0.012)], fill=WHITE)
    draw.rectangle([bx + int(width * 0.05), by + int(height * 0.003), bx + int(width * 0.055), by + int(height * 0.009)], fill=WHITE)
    
    # ── 2. App Header
    # Draw speaker logo beside app name
    logo_h = int(height * 0.04)
    draw_licha_logo(draw, pad + int(width * 0.05), int(height * 0.075), logo_h, WHITE, ACCENT)
    
    brand_text = "Licha"
    draw.text((pad + int(width * 0.14), int(height * 0.055)), brand_text, font=f_title, fill=WHITE)
    draw.line([(pad, int(height * 0.115)), (width - pad, int(height * 0.115))], fill=BG_CARD, width=2)
    
    content_y = int(height * 0.14)
    
    # ── 3. Screen Contents
    if screen_type == "login":
        # Form Container Card
        card_y1 = content_y
        card_y2 = height - int(height * 0.12)
        draw.rounded_rectangle([pad, card_y1, width - pad, card_y2], radius=int(width * 0.04), fill=BG_SURFACE, outline=BG_CARD, width=1)
        
        y = card_y1 + int(height * 0.04)
        
        # Heading inside card
        lbl_login = LANG_COPY[lang]["login"]["title"]
        draw.text((pad + int(width * 0.05), y), lbl_login, font=f_body, fill=WHITE)
        y += int(height * 0.06)
        
        # Client ID Input field
        draw.text((pad + int(width * 0.05), y), "Twitch Client ID:", font=f_meta, fill=MUTED)
        y += int(height * 0.03)
        input_w = width - pad * 4
        input_h = int(height * 0.06)
        draw.rounded_rectangle([pad * 2, y, pad * 2 + input_w, y + input_h], radius=int(width * 0.02), fill=BG_DARK, outline=ACCENT, width=1)
        draw.text((pad * 2 + int(width * 0.04), y + int(input_h * 0.25)), "gp5v6m207xndyqj1b43u9z...", font=f_meta, fill=WHITE)
        y += input_h + int(height * 0.04)
        
        # OAuth Token Input field
        draw.text((pad + int(width * 0.05), y), "Twitch OAuth Token:", font=f_meta, fill=MUTED)
        y += int(height * 0.03)
        draw.rounded_rectangle([pad * 2, y, pad * 2 + input_w, y + input_h], radius=int(width * 0.02), fill=BG_DARK, outline=BG_CARD, width=1)
        draw.text((pad * 2 + int(width * 0.04), y + int(input_h * 0.25)), "oauth:••••••••••••••••••••••••", font=f_meta, fill=MUTED)
        y += input_h + int(height * 0.06)
        
        # Button: CONNECT TO TWITCH
        btn_w = input_w
        btn_h = int(height * 0.07)
        btn_y1 = y
        draw.rounded_rectangle([pad * 2, btn_y1, pad * 2 + btn_w, btn_y1 + btn_h], radius=int(width * 0.03), fill=ACCENT)
        
        lbl_conn = "CONNECT TO TWITCH" if lang == "en-US" else "CONECTAR A TWITCH" if lang == "es-ES" else "CONNEXION TWITCH"
        # Center button text
        bbox = draw.textbbox((0, 0), lbl_conn, font=f_sub)
        bw = bbox[2] - bbox[0]
        bx = pad * 2 + (btn_w - bw) // 2
        draw.text((bx, btn_y1 + int(btn_h * 0.28)), lbl_conn, font=f_sub, fill=WHITE)
        
    elif screen_type == "chat":
        # Connected Status Banner
        banner_h = int(height * 0.06)
        draw.rounded_rectangle([pad, content_y, width - pad, content_y + banner_h], radius=int(width * 0.02), fill=BG_SURFACE, outline=BG_CARD, width=1)
        draw.ellipse([pad + int(width * 0.04), content_y + int(banner_h * 0.35), pad + int(width * 0.04) + int(width * 0.03), content_y + int(banner_h * 0.35) + int(width * 0.03)], fill=GREEN)
        
        lbl_status = f"Connected to #{channel_name}" if lang == "en-US" else f"Conectado a #{channel_name}" if lang == "es-ES" else f"Connecté à #{channel_name}"
        draw.text((pad + int(width * 0.10), content_y + int(banner_h * 0.25)), lbl_status, font=f_meta, fill=WHITE)
        
        # Messages List
        y = content_y + banner_h + int(height * 0.03)
        
        messages = {
            "en-US": [
                ("System", "Speech synthesis initialized successfully.", GREEN),
                ("Nightbot", "Remember to subscribe to the channel for benefits!", ACCENT_LIGHT),
                ("Gamer_X", "Hello streamer! Love the gameplay today.", WHITE),
                ("Speedy_07", "Can you hear my text to speech message?", WHITE),
                ("TechGeek", "This Licha reader makes VR streams so much easier!", WHITE)
            ],
            "es-ES": [
                ("System", "Síntesis de voz inicializada correctamente.", GREEN),
                ("Nightbot", "¡Recuerda suscribirte al canal para obtener beneficios!", ACCENT_LIGHT),
                ("Gamer_X", "¡Hola! Me encanta el stream de hoy.", WHITE),
                ("Speedy_07", "¿Puedes oír mi mensaje de voz a texto?", WHITE),
                ("TechGeek", "¡Licha hace que los streams de VR sean mucho más fáciles!", WHITE)
            ],
            "fr-FR": [
                ("System", "Synthèse vocale initialisée avec succès.", GREEN),
                ("Nightbot", "Pensez à vous abonner pour soutenir la chaîne !", ACCENT_LIGHT),
                ("Gamer_X", "Salut ! Super stream aujourd'hui.", WHITE),
                ("Speedy_07", "Est-ce que tu entends mon message vocal ?", WHITE),
                ("TechGeek", "Licha rend les streams VR tellement plus simples !", WHITE)
            ]
        }[lang]
        
        for name, text, text_color in messages:
            box_h = int(height * 0.11)
            draw.rounded_rectangle([pad, y, width - pad, y + box_h], radius=int(width * 0.03), fill=BG_SURFACE)
            
            # Speaker icon for voice indicator on TTS message
            draw.arc([pad + int(width * 0.03), y + int(box_h * 0.35), pad + int(width * 0.07), y + int(box_h * 0.35) + int(width * 0.04)], start=300, end=60, fill=ACCENT_LIGHT, width=2)
            draw.polygon([(pad + int(width * 0.02), y + int(box_h * 0.42)), (pad + int(width * 0.04), y + int(box_h * 0.35)), (pad + int(width * 0.04), y + int(box_h * 0.65)), (pad + int(width * 0.02), y + int(box_h * 0.58))], fill=ACCENT_LIGHT)
            
            # Name
            draw.text((pad + int(width * 0.10), y + int(box_h * 0.15)), name, font=f_sub, fill=ACCENT_LIGHT if name != "System" else GREEN)
            # Message Text
            draw.text((pad + int(width * 0.10), y + int(box_h * 0.55)), text, font=f_meta, fill=text_color)
            
            y += box_h + int(height * 0.02)
            
    elif screen_type == "settings":
        y = content_y
        
        settings_items = {
            "en-US": [
                ("TTS Engine Status", "Active (Google TTS Engine)", "status"),
                ("Voice Locale", "English (United States)", "selector"),
                ("Speech Speed (1.2x)", "Rate slider", "slider", 0.6),
                ("Speech Pitch (1.0x)", "Pitch slider", "slider", 0.5),
                ("Read Chat Usernames", "Reads the sender's username", "toggle", True),
                ("Ignore System Bot Commands", "Filters out chat command messages", "toggle", False)
            ],
            "es-ES": [
                ("Estado del Motor TTS", "Activo (Motor de Voz de Google)", "status"),
                ("Idioma de la Voz", "Español (España)", "selector"),
                ("Velocidad de Voz (1.2x)", "Velocidad", "slider", 0.6),
                ("Tono de Voz (1.0x)", "Tono", "slider", 0.5),
                ("Leer Nombres de Usuario", "Lee el nombre de quien envía", "toggle", True),
                ("Ignorar Comandos de Bots", "Filtra mensajes con comandos de chat", "toggle", False)
            ],
            "fr-FR": [
                ("État de la Synthèse", "Actif (Moteur de Synthèse Google)", "status"),
                ("Langue de la Voix", "Français (France)", "selector"),
                ("Vitesse de parole (1.2x)", "Vitesse", "slider", 0.6),
                ("Pitch de parole (1.0x)", "Pitch", "slider", 0.5),
                ("Lire les noms d'utilisateurs", "Lit le nom de l'expéditeur", "toggle", True),
                ("Ignorer commandes de robots", "Filtre les commandes de chat", "toggle", False)
            ]
        }[lang]
        
        for title, desc, control_type, *args in settings_items:
            item_h = int(height * 0.09)
            draw.rounded_rectangle([pad, y, width - pad, y + item_h], radius=int(width * 0.02), fill=BG_SURFACE)
            
            # Title & Desc
            draw.text((pad + int(width * 0.04), y + int(item_h * 0.2)), title, font=f_sub, fill=WHITE)
            draw.text((pad + int(width * 0.04), y + int(item_h * 0.58)), desc, font=f_meta, fill=MUTED)
            
            # Draw visual control interfaces
            cx = width - pad - int(width * 0.12)
            cy = y + int(item_h * 0.35)
            
            if control_type == "toggle":
                val = args[0]
                # Toggle box
                t_w, t_h = int(width * 0.10), int(item_h * 0.35)
                rx = width - pad - t_w - int(width * 0.03)
                ry = y + (item_h - t_h)//2
                draw.rounded_rectangle([rx, ry, rx + t_w, ry + t_h], radius=t_h//2, fill=ACCENT if val else BG_DARK, outline=ACCENT, width=1)
                circle_d = t_h - int(width * 0.01)
                cx_c = rx + (t_w - circle_d - int(width * 0.005)) if val else rx + int(width * 0.005)
                cy_c = ry + int(width * 0.005)
                draw.ellipse([cx_c, cy_c, cx_c + circle_d, cy_c + circle_d], fill=WHITE)
                
            elif control_type == "slider":
                val = args[0]
                s_w = int(width * 0.22)
                rx = width - pad - s_w - int(width * 0.03)
                ry = y + int(item_h * 0.48)
                # Slider track
                draw.line([(rx, ry), (rx + s_w, ry)], fill=BG_DARK, width=6)
                # Active track
                draw.line([(rx, ry), (rx + int(s_w * val), ry)], fill=ACCENT, width=6)
                # Slider knob
                kx = rx + int(s_w * val)
                draw.ellipse([kx - 8, ry - 8, kx + 8, ry + 8], fill=WHITE)
                
            elif control_type == "selector" or control_type == "status":
                # Arrow indicator
                rx = width - pad - int(width * 0.06)
                ry = y + int(item_h * 0.35)
                draw.polygon([(rx, ry), (rx + int(width*0.02), ry + int(width*0.015)), (rx, ry + int(width*0.03))], fill=MUTED)
                
            y += item_h + int(height * 0.015)
            
    # ── 4. Navigation Bar
    ny = height - int(height * 0.04)
    # Draw simple triangle back button
    draw.polygon([(pad * 2, ny), (pad * 2 + int(width * 0.03), ny - int(height * 0.012)), (pad * 2 + int(width * 0.03), ny + int(height * 0.012))], fill=MUTED)
    # Circle home button
    cx_n = width // 2
    draw.ellipse([cx_n - 12, ny - 12, cx_n + 12, ny + 12], fill=MUTED)
    # Square recents button
    rx_n = width - pad * 2 - 20
    draw.rectangle([rx_n, ny - 10, rx_n + 20, ny + 10], fill=MUTED)
    
    return img

def get_raw_screenshot(screen_type, lang, width, height, form_factor, channel_name="twitch_streamer", regenerate=False):
    """
    Looks for the screenshot in the source directory.
    If not found, generates a beautiful mock screenshot.
    """
    os.makedirs(os.path.join(RAW_DIR, form_factor, lang), exist_ok=True)
    filename = f"{screen_type}.png"
    filepath = os.path.join(RAW_DIR, form_factor, lang, filename)
    
    if regenerate and os.path.exists(filepath):
        try:
            os.remove(filepath)
        except Exception:
            pass
            
    if os.path.exists(filepath):
        try:
            return Image.open(filepath)
        except Exception as e:
            print(f"  ⚠ Failed to load {filepath}: {e}. Generating mockup instead.")
            
    # Generate mock screenshot dynamically
    mock_img = generate_mock_screenshot(screen_type, lang, width, height, channel_name)
    mock_img.save(filepath, "PNG")
    print(f"  🎨 Autogenerated mock screenshot: {filepath} ({width}x{height})")
    return mock_img


# ─── Showcase Card Generator (Universal) ───
def make_showcase_card(screen_type, lang, form_factor, out_path, channel_name="twitch_streamer", regenerate=False):
    """
    Builds a unified 1080×1920 Google Play Store Showcase Card.
    - Phone layout: Mockup frame at the top (y=100 to y=1300), copy centered at the bottom.
    - Tablet layout: Copy at the top, tablet mockup frame at the bottom (y=460 to y=1920).
    """
    W, H = OUT_W, OUT_H
    
    # 1. Determine screenshot source size
    if form_factor == "phone":
        raw_w, raw_h = 1080, 2400
        phone_h_render = 1200
        crop_top, crop_bottom = STATUS_BAR, NAV_BAR
    elif form_factor == "tablet_7":
        raw_w, raw_h = 1200, 1920
        phone_h_render = 1200
        crop_top, crop_bottom = STATUS_BAR, NAV_BAR
    else: # tablet_10
        raw_w, raw_h = 1600, 2560
        phone_h_render = 1150
        crop_top, crop_bottom = STATUS_BAR, NAV_BAR
        
    # Get the raw screenshot
    raw_img = get_raw_screenshot(screen_type, lang, raw_w, raw_h, form_factor, channel_name, regenerate).convert("RGBA")
    
    # Crop status & nav bars
    cropped = raw_img.crop((0, crop_top, raw_img.width, raw_img.height - crop_bottom))
    
    # Scale to target render height
    scale = phone_h_render / cropped.height
    pw = int(cropped.width * scale)
    ph = phone_h_render
    scaled = cropped.resize((pw, ph), Image.LANCZOS)
    
    # Create mask for rounded corners
    corner_radius = 42 if form_factor == "phone" else 24
    mask = rounded_rect_mask((pw, ph), radius=corner_radius)
    scaled.putalpha(mask)
    
    # Draw premium accent border around screenshot
    border_draw = ImageDraw.Draw(scaled)
    border_draw.rounded_rectangle([(0, 0), (pw - 1, ph - 1)], radius=corner_radius, outline=(*ACCENT, 180), width=4)
    
    # Apply soft drop shadow
    shadowed, (six, siy) = apply_shadow(scaled, blur=22, offset=(0, 14))
    
    # Paste onto canvas
    canvas = dark_gradient(W, H).convert("RGBA")
    draw = ImageDraw.Draw(canvas)
    
    # Text styles
    f_label = font_sans(24, bold=True)
    f_head = font_sans(56, bold=True)
    f_sub = font_sans(32, bold=False)
    
    brand_lbl = LANG_COPY[lang]["brand"]
    headline = LANG_COPY[lang][screen_type]["headline"]
    subtext = LANG_COPY[lang][screen_type]["subtext"]
    
    if form_factor == "phone":
        # ── Phone: Screenshot Top, Text Bottom
        # Paste Mockup centered horizontally
        sx = (W - shadowed.width) // 2
        sy = 100 - siy
        canvas.paste(shadowed, (sx, sy), shadowed)
        
        # Soft vertical fade-out transition
        fade_h = 100
        fade_y = 1250
        fade = Image.new("RGBA", (W, fade_h))
        fd = ImageDraw.Draw(fade)
        for i in range(fade_h):
            a = int(255 * ((i/fade_h) ** 1.6))
            fd.line([(0, i), (W, i)], fill=(*BG_DARK, a))
        canvas.alpha_composite(fade, (0, fade_y))
        
        # Calculate dynamic text height to center it perfectly
        label_h = 24
        gap1, gap2 = 45, 30
        headline_h = get_wrap_height(draw, headline, f_head, W - 100, 16)
        gap3 = 24
        subtext_h = get_wrap_height(draw, subtext, f_sub, W - 120, 12)
        
        total_text_h = label_h + gap1 + gap2 + headline_h + gap3 + subtext_h
        panel_y = 1300
        panel_h = H - panel_y
        
        text_y = panel_y + (panel_h - total_text_h) // 2
        
        # Draw text elements
        center_text(draw, brand_lbl, text_y, f_label, (*ACCENT_LIGHT, 210))
        text_y += label_h + gap1
        accent_rule(draw, text_y, length=100, color=(*ACCENT_LIGHT, 140))
        text_y += gap2
        
        y_used = wrap_text_centered(draw, headline, text_y, f_head, WHITE, max_w=W - 100, line_gap=16)
        text_y += y_used + gap3
        accent_rule(draw, text_y, length=60, color=(*ACCENT_LIGHT, 160))
        text_y += gap3
        
        wrap_text_centered(draw, subtext, text_y, f_sub, MUTED, max_w=W - 120, line_gap=12)
        
    else:
        # ── Tablet (7" and 10"): Text Top, Screenshot Bottom
        # Draw text elements at the top
        text_y = 54
        center_text(draw, brand_lbl, text_y, f_label, (*ACCENT_LIGHT, 210))
        accent_rule(draw, 102, length=100, color=(*ACCENT_LIGHT, 140))
        
        y_used = wrap_text_centered(draw, headline, 134, f_head, WHITE, max_w=W - 100, line_gap=16)
        accent_rule(draw, 134 + y_used + 16, length=60, color=(*ACCENT_LIGHT, 160))
        
        wrap_text_centered(draw, subtext, 134 + y_used + 46, f_sub, MUTED, max_w=W - 120, line_gap=12)
        
        # Paste tablet mockup centered at the bottom
        sx = (W - shadowed.width) // 2
        sy = 480 - siy
        canvas.paste(shadowed, (sx, sy), shadowed)
        
    # Save the card
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    canvas.convert("RGB").save(out_path, "PNG", optimize=True)
    size_kb = os.path.getsize(out_path) // 1024
    print(f"  ✅ Generated card: {out_path} ({size_kb} KB)")


# ─── Feature Graphic Generator ───
def make_feature_graphic(out_path, lang="en-US"):
    """
    Generates a 1024×500 Store Banner (Feature Graphic) following Play Store best practices:
      - Deep rich dark background gradient
      - Split layout: Large speaker bubble logo on the left, copy on the right
      - Styled typography using brand colors
    """
    W, H = 1024, 500
    
    # Background gradient
    canvas = Image.new("RGB", (W, H), BG_DARK)
    draw = ImageDraw.Draw(canvas)
    for i in range(H):
        ratio = i / H
        r = int(BG_DARK[0] + (ACCENT[0] - BG_DARK[0]) * ratio * 0.25)
        g = int(BG_DARK[1] + (ACCENT[1] - BG_DARK[1]) * ratio * 0.10)
        b = int(BG_DARK[2] + (ACCENT[2] - BG_DARK[2]) * ratio * 0.20)
        draw.line([(0, i), (W, i)], fill=(r, g, b))
        
    # Draw Licha Logo on the left
    logo_h = 240
    draw_licha_logo(draw, cx=280, cy=H//2, height=logo_h, foreground_color=WHITE, accent_color=ACCENT)
    
    # Draw brand copy on the right
    f_title = font_sans(58, bold=True)
    f_sub = font_sans(24, bold=False)
    
    headline = LANG_COPY[lang]["feature_graphic"]["headline"]
    subtitle = LANG_COPY[lang]["feature_graphic"]["subtext"]
    
    y = H//2 - 60
    draw.text((480, y), headline, font=f_title, fill=WHITE)
    
    y += 84
    draw.line([(480, y), (600, y)], fill=(*ACCENT_LIGHT, 220), width=4)
    
    y += 24
    draw.text((480, y), subtitle, font=f_sub, fill=MUTED)
    
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    canvas.save(out_path, "PNG", optimize=True)
    size_kb = os.path.getsize(out_path) // 1024
    print(f"  🎨 Generated Feature Graphic: {out_path} ({size_kb} KB)")


# ─── Main ───
def main():
    import argparse
    parser = argparse.ArgumentParser(description="Play Store Showcase generator for Licha")
    parser.add_argument("--channel", default="twitch_streamer", help="Twitch channel name to display (default: 'twitch_streamer')")
    parser.add_argument("--regenerate-mockups", action="store_true", help="Force deletion and regeneration of mock screenshots in raw/")
    args = parser.parse_args()
    
    channel_name = args.channel
    regenerate = args.regenerate_mockups
    
    print("=" * 70)
    print("      LICHA (TwitchChatTTS) — Play Store Asset Creation Suite")
    print(f"      Target Channel: #{channel_name}")
    print("=" * 70)
    
    # 1. Process showcases for each locale & form factor
    for form_factor in ["phone", "tablet_7", "tablet_10"]:
        print(f"\n🚀 Processing Form Factor: {form_factor.upper()}")
        print("-" * 50)
        
        for lang in LANGS:
            print(f"  Locale: {lang}")
            for screen in ["login", "chat", "settings"]:
                out_file = f"showcase_{screen}.png"
                out_path = os.path.join(OUT_DIR, form_factor, lang, out_file)
                make_showcase_card(screen, lang, form_factor, out_path, channel_name, regenerate)
                
    # 2. Process Feature Graphic (English as default banner)
    print(f"\n🚀 Generating Feature Graphic Banner")
    print("-" * 50)
    make_feature_graphic(os.path.join(OUT_DIR, "feature_graphic.png"), lang="en-US")
    
    print(f"\n{'='*70}")
    print("  COMPLETED SUCCESSFULLY!")
    print(f"{'='*70}")
    print(f"  All Play Store assets have been written to:")
    print(f"  {OUT_DIR}/")
    print(f"  └─ phone/      ← Upload to Phone Screenshots section")
    print(f"  └─ tablet_7/   ← Upload to 7-inch Tablet Screenshots section")
    print(f"  └─ tablet_10/  ← Upload to 10-inch Tablet Screenshots section")
    print(f"  └─ feature_graphic.png  ← Upload to Feature Graphic section")
    print("-" * 70)

if __name__ == "__main__":
    main()
