#!/usr/bin/env python3
"""
generate_showcase.py — Play Store Showcase Graphic & Feature Banner Generator
for Licha (TwitchChatTTS)
=============================================================================
Creates high-contrast, polished showcase cards for:
- Phone: 1080×1920 px
- 7-inch Tablet: 1080×1920 px
- 10-inch Tablet: 1200×1920 px
- Feature Graphic: 1024×500 px
Across 3 languages: es-ES, en-US, fr-FR.

Uses 100% authentic captures from raw/phone/:
  1. chat.png   - Real-time chat reader with Twitch badges and hero status banner
  2. tuning.png - Quick Audio Tuning drawer (speed, pitch, volume sliders)
  3. settings.png - Voice configuration, filters, and background playback settings
"""

import os
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# Target Dimensions
PHONE_W, PHONE_H = 1080, 1920
TAB7_W, TAB7_H = 1080, 1920
TAB10_W, TAB10_H = 1200, 1920
FEATURE_W, FEATURE_H = 1024, 500

STATUS_BAR = 96
NAV_BAR = 120

LANGS = ["es-ES", "en-US", "fr-FR"]

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RAW_DIR = os.path.join(SCRIPT_DIR, "raw", "phone")
OUT_DIR = os.path.join(SCRIPT_DIR, "output")
ICON_PATH = os.path.join(SCRIPT_DIR, "..", "app", "src", "main", "res", "mipmap-xxxhdpi", "ic_launcher_foreground.png")

# Brand Palette (Twitch Deep Dark / Royal Purple / Accent Cyan)
BG_TOP = (14, 14, 18)         # #0E0E12
BG_BOTTOM = (26, 16, 48)      # #1A1030
PRIMARY = (145, 70, 255)      # Twitch Purple #9146FF
ACCENT_LIGHT = (185, 130, 255) # Light Purple #B982FF
ACCENT_CYAN = (0, 245, 212)   # Success / Audio accent #00F5D4
WHITE = (255, 255, 255)
MUTED = (175, 175, 195)
FRAME_BORDER = (145, 70, 255, 160)
FRAME_BG = (20, 20, 24, 255)

# Localized Copy Configuration
COPY_DATA = {
    "es-ES": {
        "brand": "LICHA",
        "screens": [
            {
                "file": "chat.png",
                "out_name": "showcase_chat.png",
                "tag": "TWITCH TTS",
                "headline": "CHAT A VOZ EN TIEMPO REAL",
                "subtext": "Escucha los mensajes del chat mientras transmites o juegas sin perder detalle."
            },
            {
                "file": "tuning.png",
                "out_name": "showcase_tuning.png",
                "tag": "CONTROL RÁPIDO",
                "headline": "AJUSTES RÁPIDOS DE VOZ",
                "subtext": "Ajusta velocidad, tono y volumen al instante sobre la marcha sin salir del chat."
            },
            {
                "file": "settings.png",
                "out_name": "showcase_settings.png",
                "tag": "PERSONALIZACIÓN",
                "headline": "VOZ Y FILTROS A TU MEDIDA",
                "subtext": "Elige motores de voz, silencia por rol o lista de ignorados y reproduce en segundo plano."
            }
        ],
        "feature": {
            "title": "Licha - Twitch Chat TTS",
            "tagline": "Tu chat de Twitch, leído en voz alta",
            "chips": ["Chat en Tiempo Real", "Ajustes de Voz", "Modo 2º Plano", "Filtros por Rol"]
        }
    },
    "en-US": {
        "brand": "LICHA",
        "screens": [
            {
                "file": "chat.png",
                "out_name": "showcase_chat.png",
                "tag": "TWITCH TTS",
                "headline": "REAL-TIME SPEECH FOR TWITCH",
                "subtext": "Listen to incoming chat messages read aloud while staying focused on your gameplay."
            },
            {
                "file": "tuning.png",
                "out_name": "showcase_tuning.png",
                "tag": "AUDIO TUNING",
                "headline": "INSTANT VOICE CONTROLS",
                "subtext": "Fine-tune speech rate, pitch, and volume on the fly without leaving the chat."
            },
            {
                "file": "settings.png",
                "out_name": "showcase_settings.png",
                "tag": "CUSTOMIZATION",
                "headline": "TAILORED SPEECH & FILTERS",
                "subtext": "Pick voice engines, filter noisy roles or ignored users, and keep reading in background."
            }
        ],
        "feature": {
            "title": "Licha - Twitch Chat TTS",
            "tagline": "Your Twitch chat, read aloud",
            "chips": ["Real-Time Chat", "Instant Voice Tuning", "Background Playback", "Role Filters"]
        }
    },
    "fr-FR": {
        "brand": "LICHA",
        "screens": [
            {
                "file": "chat.png",
                "out_name": "showcase_chat.png",
                "tag": "TWITCH TTS",
                "headline": "LECTURE DU CHAT EN DIRECT",
                "subtext": "Écoutez les messages de votre stream à haute voix tout en vous concentrant sur le jeu."
            },
            {
                "file": "tuning.png",
                "out_name": "showcase_tuning.png",
                "tag": "CONTRÔLE VOCAL",
                "headline": "RÉGLAGES AUDIO EN DIRECT",
                "subtext": "Ajustez le débit, le pitch et le volume sonore instantanément sans fermer le chat."
            },
            {
                "file": "settings.png",
                "out_name": "showcase_settings.png",
                "tag": "PERSONNALISATION",
                "headline": "VOIX ET FILTRES SUR MESURE",
                "subtext": "Sélectionnez vos voix, appliquez des filtres de rôle et continuez en arrière-plan."
            }
        ],
        "feature": {
            "title": "Licha - Twitch Chat TTS",
            "tagline": "Votre chat Twitch, à voix haute",
            "chips": ["Chat en Direct", "Réglages Audio", "Lecture en Arrière-Plan", "Filtres par Rôle"]
        }
    }
}

def get_font(size, bold=True):
    font_candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
        "/usr/share/fonts/truetype/freefont/FreeSansBold.ttf" if bold else "/usr/share/fonts/truetype/freefont/FreeSans.ttf"
    ]
    for p in font_candidates:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                pass
    return ImageFont.load_default()

def draw_vertical_gradient(width, height, top_color, bottom_color):
    base = Image.new("RGB", (width, height), top_color)
    draw = ImageDraw.Draw(base)
    for y in range(height):
        ratio = y / float(height)
        r = int(top_color[0] * (1 - ratio) + bottom_color[0] * ratio)
        g = int(top_color[1] * (1 - ratio) + bottom_color[1] * ratio)
        b = int(top_color[2] * (1 - ratio) + bottom_color[2] * ratio)
        draw.line([(0, y), (width, y)], fill=(r, g, b))
    return base

def wrap_text(text, font, max_width):
    words = text.split()
    lines = []
    current_line = []
    for word in words:
        test_line = " ".join(current_line + [word])
        bbox = font.getbbox(test_line)
        w = bbox[2] - bbox[0]
        if w <= max_width or not current_line:
            current_line.append(word)
        else:
            lines.append(" ".join(current_line))
            current_line = [word]
    if current_line:
        lines.append(" ".join(current_line))
    return lines

def create_device_frame(inner_img, target_w, target_h, corner_radius=32):
    # Crop status and nav bars if raw screenshot
    crop_t = min(STATUS_BAR, int(inner_img.height * 0.05))
    crop_b = min(NAV_BAR, int(inner_img.height * 0.06))
    cropped = inner_img.crop((0, crop_t, inner_img.width, inner_img.height - crop_b))

    border_px = 8
    avail_w = target_w - border_px * 2
    avail_h = target_h - border_px * 2

    scale_w = avail_w / float(cropped.width)
    scale_h = avail_h / float(cropped.height)
    scale = min(scale_w, scale_h)

    scaled_w = int(cropped.width * scale)
    scaled_h = int(cropped.height * scale)
    resized_screen = cropped.resize((scaled_w, scaled_h), Image.Resampling.LANCZOS)

    # Frame dimensions
    frame_w = scaled_w + border_px * 2
    frame_h = scaled_h + border_px * 2

    # Mask for rounded screen corners
    mask = Image.new("L", (scaled_w, scaled_h), 0)
    draw_mask = ImageDraw.Draw(mask)
    draw_mask.rounded_rectangle([0, 0, scaled_w, scaled_h], radius=corner_radius, fill=255)

    # Frame with elegant Twitch-purple border
    frame = Image.new("RGBA", (frame_w, frame_h), (0, 0, 0, 0))
    draw_frame = ImageDraw.Draw(frame)
    draw_frame.rounded_rectangle(
        [0, 0, frame_w - 1, frame_h - 1],
        radius=corner_radius + 6,
        fill=FRAME_BG,
        outline=FRAME_BORDER,
        width=3
    )
    frame.paste(resized_screen, (border_px, border_px), mask)

    # Soft ambient drop shadow
    shadow_pad = 32
    shadow_img = Image.new("RGBA", (frame_w + shadow_pad * 2, frame_h + shadow_pad * 2), (0, 0, 0, 0))
    draw_shadow = ImageDraw.Draw(shadow_img)
    draw_shadow.rounded_rectangle(
        [shadow_pad, shadow_pad + 8, shadow_pad + frame_w, shadow_pad + frame_h + 8],
        radius=corner_radius + 8,
        fill=(0, 0, 0, 160)
    )
    shadow_img = shadow_img.filter(ImageFilter.GaussianBlur(14))
    shadow_img.paste(frame, (shadow_pad, shadow_pad), frame)

    return shadow_img, shadow_pad

def render_showcase_card(screen_info, lang, width, height, is_tablet=False):
    card = draw_vertical_gradient(width, height, BG_TOP, BG_BOTTOM)
    draw = ImageDraw.Draw(card)

    font_tag = get_font(24 if not is_tablet else 22, bold=True)
    font_hl = get_font(46 if not is_tablet else 42, bold=True)
    font_sub = get_font(26 if not is_tablet else 23, bold=False)

    raw_path = os.path.join(RAW_DIR, lang, screen_info["file"])
    if not os.path.exists(raw_path):
        print(f"  ⚠ Missing raw capture: {raw_path}")
        return None

    screen_img = Image.open(raw_path).convert("RGBA")

    # Typography section
    pad_x = 70 if not is_tablet else (90 if width > 1100 else 75)
    start_y = 80 if not is_tablet else 70
    max_w = width - 2 * pad_x

    # Pill Tag
    tag_text = screen_info["tag"].upper()
    bbox = font_tag.getbbox(tag_text)
    tag_w = bbox[2] - bbox[0] + 32
    tag_h = bbox[3] - bbox[1] + 18
    draw.rounded_rectangle([pad_x, start_y, pad_x + tag_w, start_y + tag_h], radius=12, fill=PRIMARY)
    draw.text((pad_x + 16, start_y + 8), tag_text, font=font_tag, fill=WHITE)

    # Headline
    hl_y = start_y + tag_h + 20
    hl_lines = wrap_text(screen_info["headline"], font_hl, max_w)
    curr_y = hl_y
    for line in hl_lines:
        draw.text((pad_x, curr_y), line, font=font_hl, fill=WHITE)
        bbox_l = font_hl.getbbox(line)
        curr_y += (bbox_l[3] - bbox_l[1]) + 10

    # Subtext
    curr_y += 6
    sub_lines = wrap_text(screen_info["subtext"], font_sub, max_w)
    for line in sub_lines:
        draw.text((pad_x, curr_y), line, font=font_sub, fill=MUTED)
        bbox_s = font_sub.getbbox(line)
        curr_y += (bbox_s[3] - bbox_s[1]) + 8

    # Framed device placed cleanly below typography
    device_top_y = curr_y + 35
    bottom_margin = 50
    available_h = height - device_top_y - bottom_margin
    available_w = int(width * 0.86)

    framed, shadow_pad = create_device_frame(screen_img, available_w, available_h)
    pos_x = (width - framed.width) // 2
    pos_y = device_top_y - shadow_pad
    card.paste(framed, (pos_x, pos_y), framed)

    return card

def render_feature_graphic(lang_code, lang_info):
    banner = draw_vertical_gradient(FEATURE_W, FEATURE_H, (12, 12, 16), (28, 18, 54))
    draw = ImageDraw.Draw(banner)

    # Subtle ambient glow
    glow = Image.new("RGBA", (FEATURE_W, FEATURE_H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    g_draw.ellipse([60, -80, 600, 480], fill=(145, 70, 255, 65))
    g_draw.ellipse([600, 120, 1050, 580], fill=(0, 245, 212, 40))
    glow = glow.filter(ImageFilter.GaussianBlur(60))
    banner.paste(glow, (0, 0), glow)

    # App Icon with card framing
    if os.path.exists(ICON_PATH):
        try:
            icon_img = Image.open(ICON_PATH).convert("RGBA").resize((200, 200), Image.Resampling.LANCZOS)
            icon_card = Image.new("RGBA", (230, 230), (0, 0, 0, 0))
            ic_draw = ImageDraw.Draw(icon_card)
            ic_draw.rounded_rectangle([0, 0, 229, 229], radius=44, fill=(30, 24, 48, 240), outline=PRIMARY, width=2)
            icon_card.paste(icon_img, (15, 15), icon_img)
            banner.paste(icon_card, (70, 135), icon_card)
        except Exception:
            pass

    # Typography
    text_x = 340
    f_title = get_font(52, bold=True)
    f_tagline = get_font(26, bold=False)
    f_chips = get_font(18, bold=True)

    draw.text((text_x, 140), lang_info["feature"]["title"], font=f_title, fill=WHITE)
    draw.text((text_x, 215), lang_info["feature"]["tagline"], font=f_tagline, fill=ACCENT_LIGHT)

    # Feature Chips (2x2 grid with vibrant accent dots)
    row1_y = 270
    row2_y = 325
    chips = lang_info["feature"]["chips"]
    dot_colors = [
        (0, 245, 212),   # Cyan / Live
        (169, 112, 255), # Purple / Tuning
        (255, 215, 0),   # Gold / Background
        (255, 107, 107)  # Coral / Filters
    ]

    # Row 1
    curr_x = text_x
    for i in range(min(2, len(chips))):
        chip = chips[i]
        color = dot_colors[i]
        bbox = f_chips.getbbox(chip)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        cw = tw + 50
        ch = th + 18
        draw.rounded_rectangle([curr_x, row1_y, curr_x + cw, row1_y + ch], radius=12, fill=(36, 26, 62, 230), outline=(145, 70, 255, 150), width=1)
        dot_y = row1_y + ch // 2
        draw.ellipse([curr_x + 15, dot_y - 5, curr_x + 25, dot_y + 5], fill=color)
        draw.text((curr_x + 32, row1_y + 8), chip, font=f_chips, fill=WHITE)
        curr_x += cw + 14

    # Row 2
    curr_x = text_x
    for i in range(2, min(4, len(chips))):
        chip = chips[i]
        color = dot_colors[i]
        bbox = f_chips.getbbox(chip)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        cw = tw + 50
        ch = th + 18
        draw.rounded_rectangle([curr_x, row2_y, curr_x + cw, row2_y + ch], radius=12, fill=(36, 26, 62, 230), outline=(145, 70, 255, 150), width=1)
        dot_y = row2_y + ch // 2
        draw.ellipse([curr_x + 15, dot_y - 5, curr_x + 25, dot_y + 5], fill=color)
        draw.text((curr_x + 32, row2_y + 8), chip, font=f_chips, fill=WHITE)
        curr_x += cw + 14

    return banner

def clean_old_fake_showcases():
    """Removes obsolete fake mock files from output/."""
    for root, dirs, files in os.walk(OUT_DIR):
        for f in files:
            if "login" in f.lower():
                try:
                    p = os.path.join(root, f)
                    os.remove(p)
                    print(f"  🗑 Removed obsolete fake showcase: {p}")
                except Exception:
                    pass

def main():
    print("=" * 70)
    print("      LICHA (TwitchChatTTS) — Play Store Showcase Generator")
    print("      Using 100% Authentic Device Captures from raw/phone/")
    print("=" * 70)

    clean_old_fake_showcases()

    for lang in LANGS:
        print(f"\n🚀 Generating showcase assets for '{lang}'...")
        lang_data = COPY_DATA[lang]

        phone_dir = os.path.join(OUT_DIR, "phone", lang)
        tab7_dir = os.path.join(OUT_DIR, "tablet_7", lang)
        tab10_dir = os.path.join(OUT_DIR, "tablet_10", lang)

        os.makedirs(phone_dir, exist_ok=True)
        os.makedirs(tab7_dir, exist_ok=True)
        os.makedirs(tab10_dir, exist_ok=True)

        for screen in lang_data["screens"]:
            # 1. Phone card (1080×1920)
            phone_card = render_showcase_card(screen, lang, PHONE_W, PHONE_H, is_tablet=False)
            if phone_card:
                out_p = os.path.join(phone_dir, screen["out_name"])
                phone_card.save(out_p, "PNG", optimize=True)
                print(f"  ✓ Phone card: {screen['out_name']}")

            # 2. Tablet 7" card (1080×1920)
            tab7_card = render_showcase_card(screen, lang, TAB7_W, TAB7_H, is_tablet=True)
            if tab7_card:
                out_t7 = os.path.join(tab7_dir, screen["out_name"])
                tab7_card.save(out_t7, "PNG", optimize=True)
                print(f"  ✓ Tablet 7\" card: {screen['out_name']}")

            # 3. Tablet 10" card (1200×1920)
            tab10_card = render_showcase_card(screen, lang, TAB10_W, TAB10_H, is_tablet=True)
            if tab10_card:
                out_t10 = os.path.join(tab10_dir, screen["out_name"])
                tab10_card.save(out_t10, "PNG", optimize=True)
                print(f"  ✓ Tablet 10\" card: {screen['out_name']}")

        # 4. Feature Graphics (1024×500)
        fg_card = render_feature_graphic(lang, lang_data)
        fg_path = os.path.join(OUT_DIR, f"feature_graphic_{lang}.png")
        fg_card.save(fg_path, "PNG", optimize=True)
        print(f"  ✓ Feature Graphic: {os.path.basename(fg_path)}")

    # Default banner (English)
    en_fg = render_feature_graphic("en-US", COPY_DATA["en-US"])
    en_fg.save(os.path.join(OUT_DIR, "feature_graphic.png"), "PNG", optimize=True)
    print("  ✓ Feature Graphic default: feature_graphic.png")

    print("\n" + "=" * 70)
    print("  COMPLETED SUCCESSFULLY! All Play Store showcases generated:")
    print(f"  Directory: {OUT_DIR}/")
    print("  └─ phone/      (chat, tuning, settings)")
    print("  └─ tablet_7/   (chat, tuning, settings)")
    print("  └─ tablet_10/  (chat, tuning, settings)")
    print("  └─ feature_graphic*.png")
    print("=" * 70)

if __name__ == "__main__":
    main()
