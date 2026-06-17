"""
feature_graphic.py — Premium 1024x500 Play Store feature graphic for Licha
==========================================================================
A "split hero" banner:
  - Left: glowing speaker-bubble logo + LICHA wordmark + tagline
  - Center bridge: an equalizer / sound-wave motif (chat -> speech)
  - Right: a tilted phone mockup showing the real chat screen, with a
    purple rim-light and soft drop shadow
  - Background: deep charcoal with a large radial purple glow, a faint dot
    grid for texture, and an edge vignette for depth

Reuses the Licha brand palette and logo from generate_showcase.py.

Run:  python3 feature_graphic.py [en-US|es-ES|fr-FR]
Out:  output/feature_graphic.png
"""

import os, sys, math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ─── Brand palette (matches generate_showcase.py) ───────────────────────────
ACCENT       = (145, 70, 255)    # Twitch Purple  #9146FF
ACCENT_LIGHT = (169, 112, 255)   # Light Purple   #A970FF
BG_DARK      = (12, 12, 14)       # Near-black     #0C0C0E
BG_SURFACE   = (24, 24, 27)       # Surface        #18181B
WHITE        = (239, 239, 241)    # Text light     #EFEFF1
MUTED        = (173, 173, 184)    # Text muted     #ADADB8
GREEN        = (0, 245, 212)       # Accent green   #00F5D4

W, H = 1024, 500
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
# Real app logo (adaptive-icon foreground, transparent background) shipped in the app module.
LOGO_PNG = os.path.join(SCRIPT_DIR, "..", "app", "src", "main", "res",
                        "mipmap-xxxhdpi", "ic_launcher_foreground.png")

LANGS = ["en-US", "es-ES", "fr-FR"]

TAGLINE = {
    "en-US": "Your Twitch chat, read aloud",
    "es-ES": "Tu chat de Twitch, en voz alta",
    "fr-FR": "Votre chat Twitch, à voix haute",
}

# ─── Fonts ──────────────────────────────────────────────────────────────────
def _try_fonts(paths, size):
    for p in paths:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()

def font_sans(size, bold=False):
    return _try_fonts([
        f"/usr/share/fonts/truetype/dejavu/DejaVuSans{'-Bold' if bold else ''}.ttf",
        f"/usr/share/fonts/truetype/liberation/LiberationSans-{'Bold' if bold else 'Regular'}.ttf",
    ], size)

# ─── Helpers ────────────────────────────────────────────────────────────────
def radial_glow(size, center, radius, color, max_alpha=160):
    """A soft circular glow rendered by blurring a filled ellipse."""
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy = center
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
              fill=(*color, max_alpha))
    return layer.filter(ImageFilter.GaussianBlur(radius * 0.55))

def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([(0, 0), (size[0] - 1, size[1] - 1)],
                                        radius=radius, fill=255)
    return m

def drop_shadow(rgba, blur=26, alpha=150, grow=40):
    """Return an RGBA shadow image (same canvas the sprite will sit in)."""
    sw, sh = rgba.width + grow * 2, rgba.height + grow * 2
    shadow = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    stamp = Image.new("RGBA", rgba.size, (0, 0, 0, alpha))
    shadow.paste(stamp, (grow, grow), rgba.split()[3])
    return shadow.filter(ImageFilter.GaussianBlur(blur))

def draw_licha_logo(draw, cx, cy, height, foreground=WHITE, accent=ACCENT):
    """Speech bubble containing a speaker icon (ported from generate_showcase)."""
    s = height / 120.0
    bw, bh = int(110 * s), int(80 * s)
    bx1, by1 = cx - bw // 2, cy - bh // 2 - int(10 * s)
    bx2, by2 = cx + bw // 2, cy + bh // 2 - int(10 * s)
    draw.rounded_rectangle([bx1, by1, bx2, by2], radius=int(18 * s), fill=accent)
    draw.polygon([(bx1 + int(20 * s), by2), (bx1 + int(40 * s), by2),
                  (bx1 + int(12 * s), by2 + int(20 * s))], fill=accent)
    sx, sy = cx - int(25 * s), cy - int(20 * s)
    sw, sh = int(15 * s), int(20 * s)
    draw.rectangle([sx, sy + int(2 * s), sx + sw, sy + sh - int(2 * s)], fill=foreground)
    draw.polygon([(sx + sw, sy + int(5 * s)), (sx + sw + int(15 * s), sy),
                  (sx + sw + int(15 * s), sy + sh), (sx + sw, sy + sh - int(5 * s))],
                 fill=foreground)
    draw.arc([cx - int(2 * s), cy - int(12 * s), cx + int(14 * s), cy + int(12 * s)],
             start=300, end=60, fill=foreground, width=max(2, int(2.5 * s)))
    draw.arc([cx - int(10 * s), cy - int(20 * s), cx + int(22 * s), cy + int(20 * s)],
             start=310, end=50, fill=foreground, width=max(2, int(2.5 * s)))

def load_real_logo(height):
    """Load the real app logo (adaptive foreground), trim transparent padding,
    and scale it to `height` px tall. Returns an RGBA image, or None if missing."""
    if not os.path.exists(LOGO_PNG):
        return None
    logo = Image.open(LOGO_PNG).convert("RGBA")
    bbox = logo.getbbox()           # tight crop around the visible glyph
    if bbox:
        logo = logo.crop(bbox)
    w = int(logo.width * (height / logo.height))
    return logo.resize((w, height), Image.LANCZOS)

def letter_spaced(draw, text, xy, font, fill, spacing):
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=font, fill=fill)
        x += draw.textbbox((0, 0), ch, font=font)[2] + spacing
    return x

# ─── Phone mockup ───────────────────────────────────────────────────────────
def build_phone(screen_path, screen_h=470):
    """Return an RGBA phone mockup (dark bezel + purple rim) of the chat screen."""
    raw = Image.open(screen_path).convert("RGB")
    # Trim the status bar / nav gesture area so the screen reads clean.
    top = int(raw.height * 0.045)
    bot = int(raw.height * 0.02)
    screen = raw.crop((0, top, raw.width, raw.height - bot))

    sh = screen_h
    sw = int(screen.width * (sh / screen.height))
    screen = screen.resize((sw, sh), Image.LANCZOS)

    bezel, radius = 12, 44
    fw, fh = sw + bezel * 2, sh + bezel * 2
    phone = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    # Body
    body = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    ImageDraw.Draw(body).rounded_rectangle([(0, 0), (fw - 1, fh - 1)],
                                           radius=radius, fill=(10, 10, 12, 255))
    phone.alpha_composite(body)
    # Screen with rounded corners
    sm = rounded_mask((sw, sh), radius - 14)
    phone.paste(screen, (bezel, bezel), sm)
    # Purple rim-light
    ImageDraw.Draw(phone).rounded_rectangle(
        [(1, 1), (fw - 2, fh - 2)], radius=radius, outline=(*ACCENT_LIGHT, 230), width=3)
    return phone

# ─── Main composition ───────────────────────────────────────────────────────
def make_feature_graphic(out_path, lang="en-US"):
    canvas = Image.new("RGB", (W, H), BG_DARK).convert("RGBA")

    # 1) Base vertical gradient (subtle purple lift toward bottom-right)
    grad = Image.new("RGB", (W, H), BG_DARK)
    gd = ImageDraw.Draw(grad)
    for i in range(H):
        t = i / H
        r = int(BG_DARK[0] + (ACCENT[0] - BG_DARK[0]) * t * 0.18)
        g = int(BG_DARK[1] + (ACCENT[1] - BG_DARK[1]) * t * 0.06)
        b = int(BG_DARK[2] + (ACCENT[2] - BG_DARK[2]) * t * 0.16)
        gd.line([(0, i), (W, i)], fill=(r, g, b))
    canvas.alpha_composite(grad.convert("RGBA"))

    # 2) Faint dot grid for texture
    dots = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dd = ImageDraw.Draw(dots)
    for yy in range(40, H, 34):
        for xx in range(40, W, 34):
            dd.ellipse([xx, yy, xx + 2, yy + 2], fill=(*WHITE, 10))
    canvas.alpha_composite(dots)

    # 3) Big radial purple glow behind the phone, smaller one behind the logo
    canvas.alpha_composite(radial_glow((W, H), (760, 250), 340, ACCENT, 120))
    canvas.alpha_composite(radial_glow((W, H), (210, 230), 190, ACCENT, 90))

    # 4) Phone mockup (right), slightly rotated for energy
    chat = os.path.join(SCRIPT_DIR, "raw", "phone", lang, "chat.png")
    if not os.path.exists(chat):
        chat = os.path.join(SCRIPT_DIR, "raw", "phone", "en-US", "chat.png")
    if os.path.exists(chat):
        phone = build_phone(chat, screen_h=474)
        phone = phone.rotate(-7, expand=True, resample=Image.BICUBIC)
        shadow = drop_shadow(phone, blur=30, alpha=170, grow=50)
        px, py = 612, H // 2 - phone.height // 2
        canvas.alpha_composite(shadow, (px - 50, py - 50 + 16))
        canvas.alpha_composite(phone, (px, py))

    draw = ImageDraw.Draw(canvas)

    # 5) Equalizer / sound-wave motif bridging logo -> phone ("chat to speech")
    bars = [22, 46, 70, 40, 58, 30, 16]
    bx, baseline, bw, gap = 360, H // 2, 9, 16
    for i, bh in enumerate(bars):
        x = bx + i * (bw + gap)
        col = ACCENT_LIGHT if i % 2 == 0 else GREEN
        draw.rounded_rectangle([x, baseline - bh, x + bw, baseline + bh],
                               radius=bw // 2, fill=(*col, 230))

    # 6) Brand lockup (left) — real app logo with a soft glow behind it
    logo_cx, logo_cy = 158, 168
    canvas.alpha_composite(radial_glow((W, H), (logo_cx, logo_cy), 120, ACCENT, 150))
    logo = load_real_logo(height=170)
    if logo is not None:
        canvas.alpha_composite(logo, (logo_cx - logo.width // 2, logo_cy - logo.height // 2))
    else:
        draw_licha_logo(ImageDraw.Draw(canvas), cx=logo_cx, cy=logo_cy, height=150)
    draw = ImageDraw.Draw(canvas)

    f_brand = font_sans(72, bold=True)
    f_tag = font_sans(26, bold=False)
    letter_spaced(draw, "LICHA", (70, 268), f_brand, WHITE, spacing=10)
    draw.line([(74, 358), (250, 358)], fill=(*ACCENT_LIGHT, 230), width=4)
    draw.text((74, 378), TAGLINE.get(lang, TAGLINE["en-US"]), font=f_tag, fill=MUTED)

    # 7) Edge vignette for depth
    vig = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    vd = ImageDraw.Draw(vig)
    vd.rectangle([0, 0, W, H], outline=(0, 0, 0, 150), width=60)
    canvas.alpha_composite(vig.filter(ImageFilter.GaussianBlur(50)))

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    canvas.convert("RGB").save(out_path, "PNG", optimize=True)
    print(f"  🎨 Feature graphic → {out_path}  ({W}x{H}, {os.path.getsize(out_path)//1024} KB)")


if __name__ == "__main__":
    out_dir = os.path.join(SCRIPT_DIR, "output")
    langs = sys.argv[1:] or LANGS
    for lang in langs:
        make_feature_graphic(os.path.join(out_dir, f"feature_graphic_{lang}.png"), lang)
    # Default (en-US) also written to the canonical filename for convenience.
    if "en-US" in langs:
        make_feature_graphic(os.path.join(out_dir, "feature_graphic.png"), "en-US")
