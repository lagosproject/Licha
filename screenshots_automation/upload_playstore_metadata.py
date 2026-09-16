#!/usr/bin/env python3
"""
upload_playstore_metadata.py — Programmatic Google Play Store Publisher for Licha
================================================================================
Publishes or updates localized store listings, phone screenshots,
tablet screenshots, feature graphics, and optionally AAB bundles using the Google Play Developer API.
Follows the architecture from Easy Navigator.
"""

import os
import sys
import glob
import time
import socket
import argparse
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

# Set default socket timeout to 120 seconds to prevent premature drops during large asset transfers
socket.setdefaulttimeout(120)

PACKAGE_NAME = "com.LakesCorp.TwitchChatTTS"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "output")

DEFAULT_AAB_PATH = os.path.abspath(os.path.join(SCRIPT_DIR, "..", "app", "build", "outputs", "bundle", "release", "app-release.aab"))

KNOWN_KEY_DIR = "/home/vant/SynologyDrive/Proyectos/Aplicaciones/PlayStore"

PREFERRED_IMAGE_ORDER = [
    "showcase_chat.png",
    "showcase_tuning.png",
    "showcase_settings.png"
]


def execute_with_retry(request_factory, max_retries=5, initial_delay=3):
    """Executes a Google API request with exponential backoff on network errors/timeouts."""
    delay = initial_delay
    for attempt in range(1, max_retries + 1):
        try:
            req = request_factory()
            return req.execute(num_retries=2)
        except Exception as e:
            if attempt == max_retries:
                raise
            print(f"    ⚠️ Request failed ({type(e).__name__}: {e}), retrying in {delay}s (attempt {attempt}/{max_retries})...")
            time.sleep(delay)
            delay *= 2


def find_default_key_file():
    env_key = os.environ.get("PLAY_STORE_JSON_KEY")
    if env_key and os.path.exists(env_key):
        return env_key
    local_key = os.path.join(SCRIPT_DIR, "playstore-key.json")
    if os.path.exists(local_key):
        return local_key
    if os.path.isdir(KNOWN_KEY_DIR):
        candidates = glob.glob(os.path.join(KNOWN_KEY_DIR, "*.json"))
        if candidates:
            return sorted(candidates)[0]
    return None


METADATA = {
    "es-ES": {
        "title": "Licha - Twitch Chat TTS",
        "shortDescription": "Escucha tu chat de Twitch en voz alta mientras juegas o emites.",
        "fullDescription": (
            "No te pierdas ni un mensaje de tu chat de Twitch sin apartar la vista de la acción.\n\n"
            "Licha lee en voz alta los mensajes de tu chat de Twitch en tiempo real, para que sigas conectado con tu comunidad mientras emites, juegas, creas o simplemente te relajas. Se acabó parar a leer: solo escucha.\n\n"
            "Por qué te encantará Licha:\n"
            "• 🎙️ Lectura por voz en tiempo real de cada mensaje\n"
            "• ⚡ Inicio de sesión con Twitch rápido y sencillo: conectado en segundos\n"
            "• 🔊 Voz totalmente personalizable: ajusta velocidad, tono y volumen\n"
            "• 🗣️ Elige entre las voces instaladas en tu dispositivo\n"
            "• 🚫 Filtros inteligentes: silencia bots, usuarios concretos o grupos enteros (espectadores, suscriptores, moderadores)\n"
            "• 🔁 Conexión estable que se reconecta automáticamente si se cae la red\n"
            "• 🌍 Disponible en español, inglés y francés\n\n"
            "Perfecta para streamers que quieren interactuar con el chat sin usar las manos, espectadores que hacen varias cosas a la vez y cualquiera que prefiera escuchar antes que leer.\n\n"
            "Descarga Licha y dale voz a tu chat de Twitch. 🎧"
        ),
        "releaseNotes": "• Soporte completo de pantalla completa (Edge-to-Edge) con gestión de recortes de pantalla en Android 15+.\n• Optimización avanzada de memoria y rendimiento con R8 y reducción de recursos.\n• Nuevas capturas y presentación visual en Google Play."
    },
    "en-US": {
        "title": "Licha - Twitch Chat TTS",
        "shortDescription": "Listen to your Twitch chat read aloud — hands-free, while you stream or play.",
        "fullDescription": (
            "Keep up with your Twitch chat without ever taking your eyes off the action.\n\n"
            "Licha reads your Twitch chat messages out loud in real time, so you stay connected with your community while you stream, game, create, or simply relax. No more pausing to scroll — just listen.\n\n"
            "Why you'll love Licha:\n"
            "• 🎙️ Real-time text-to-speech for every chat message\n"
            "• ⚡ Quick, easy Twitch login — connected in seconds\n"
            "• 🔊 Fully customizable voice: adjust speed, pitch and volume\n"
            "• 🗣️ Choose from the voices installed on your device\n"
            "• 🚫 Smart filters: mute bots, specific users, or whole groups (regular viewers, subscribers, moderators)\n"
            "• 🔁 Rock-solid connection that reconnects automatically if your network drops\n"
            "• 🌍 Available in English, Spanish and French\n\n"
            "Perfect for streamers who want to engage with chat hands-free, viewers who multitask, and anyone who'd rather listen than read.\n\n"
            "Download Licha and give your Twitch chat a voice. 🎧"
        ),
        "releaseNotes": "• Full Edge-to-Edge display support with cutout insets handling on Android 15+.\n• Advanced memory and performance optimization with R8 and resource shrinking.\n• Updated showcase screenshots and store visuals."
    },
    "fr-FR": {
        "title": "Licha - Twitch Chat TTS",
        "shortDescription": "Écoutez votre chat Twitch à voix haute, sans lever les yeux.",
        "fullDescription": (
            "Ne manquez plus un seul message de votre chat Twitch sans quitter l'action des yeux.\n\n"
            "Licha lit à voix haute les messages de votre chat Twitch en temps réel, pour rester connecté à votre communauté pendant que vous streamez, jouez, créez ou vous détendez. Fini les pauses pour lire : écoutez, tout simplement.\n\n"
            "Pourquoi vous allez adorer Licha :\n"
            "• 🎙️ Synthèse vocale en temps réel de chaque message\n"
            "• ⚡ Connexion à Twitch rapide et simple : prêt en quelques secondes\n"
            "• 🔊 Voix entièrement personnalisable : réglez la vitesse, la hauteur et le volume\n"
            "• 🗣️ Choisissez parmi les voix installées sur votre appareil\n"
            "• 🚫 Filtres intelligents : coupez les bots, certains utilisateurs ou des groupes entiers (spectateurs, abonnés, modérateurs)\n"
            "• 🔁 Connexion stable qui se reconnecte automatiquement en cas de coupure réseau\n"
            "• 🌍 Disponible en français, anglais et espagnol\n\n"
            "Idéale pour les streamers qui veulent interagir avec le chat sans les mains, les spectateurs multitâches et tous ceux qui préfèrent écouter plutôt que lire.\n\n"
            "Téléchargez Licha et donnez une voix à votre chat Twitch. 🎧"
        ),
        "releaseNotes": "• Prise en charge complète de l'affichage bord à bord (Edge-to-Edge) et des encoches sur Android 15+.\n• Optimisation avancée de la mémoire et des performances avec R8 et réduction des ressources.\n• Mises à jour des captures d'écran et visuels de présentation."
    }
}


def get_ordered_images(directory):
    """Returns image paths ordered according to PREFERRED_IMAGE_ORDER, then remainder alphabetically."""
    if not os.path.isdir(directory):
        return []
    all_files = [f for f in os.listdir(directory) if f.endswith(".png")]
    ordered = []
    for pref in PREFERRED_IMAGE_ORDER:
        if pref in all_files:
            ordered.append(os.path.join(directory, pref))
            all_files.remove(pref)
    for remaining in sorted(all_files):
        ordered.append(os.path.join(directory, remaining))
    return ordered


def upload_listing_and_images_for_locale(service, edit_id, lang):
    if lang not in METADATA:
        raise ValueError(f"No metadata found for locale: {lang}")

    print(f"\n=======================================================")
    print(f" Processing Locale: {lang}")
    print(f"=======================================================")

    # 1. Update text listing
    print(f"Updating store listing text for '{lang}'...")
    listing_info = METADATA[lang]
    listing_res = execute_with_retry(lambda: service.edits().listings().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        language=lang,
        body={
            "title": listing_info["title"],
            "shortDescription": listing_info["shortDescription"],
            "fullDescription": listing_info["fullDescription"]
        }
    ))
    print(f"  ✓ Updated listing: title='{listing_res.get('title')}'")
    print(f"  ✓ Short description ({len(listing_info['shortDescription'])} chars)")
    print(f"  ✓ Full description ({len(listing_info['fullDescription'])} chars)")

    # Helper to upload images for an imageType
    def upload_images_for_type(image_type, image_paths):
        print(f"\nUploading {image_type} for '{lang}' ({len(image_paths)} images)...")
        try:
            execute_with_retry(lambda: service.edits().images().deleteall(
                packageName=PACKAGE_NAME,
                editId=edit_id,
                language=lang,
                imageType=image_type
            ))
        except Exception as e:
            print(f"  (Notice on deleteall: {e})")

        for idx, img_path in enumerate(image_paths, start=1):
            if not os.path.exists(img_path):
                print(f"  ✗ File missing: {img_path}")
                continue
            def do_upload(path=img_path):
                media = MediaFileUpload(path, mimetype="image/png")
                return service.edits().images().upload(
                    packageName=PACKAGE_NAME,
                    editId=edit_id,
                    language=lang,
                    imageType=image_type,
                    media_body=media
                )
            res = execute_with_retry(do_upload)
            img_id = res.get("image", {}).get("id")
            size_kb = os.path.getsize(img_path) // 1024
            print(f"  ✓ [{idx}/{len(image_paths)}] Uploaded {os.path.basename(img_path)} ({size_kb} KB) -> ID: {img_id}")

    # 2. Phone Screenshots (3 cards)
    phone_dir = os.path.join(OUTPUT_DIR, "phone", lang)
    phone_images = get_ordered_images(phone_dir)
    if phone_images:
        upload_images_for_type("phoneScreenshots", phone_images)
    else:
        print(f"  ✗ Phone screenshots dir missing or empty: {phone_dir}")

    # 3. 7-inch Tablet Screenshots (3 cards)
    tablet7_dir = os.path.join(OUTPUT_DIR, "tablet_7", lang)
    tablet7_images = get_ordered_images(tablet7_dir)
    if tablet7_images:
        upload_images_for_type("sevenInchScreenshots", tablet7_images)
    else:
        print(f"  ✗ 7-inch tablet screenshots dir missing or empty: {tablet7_dir}")

    # 4. 10-inch Tablet Screenshots (3 cards)
    tablet10_dir = os.path.join(OUTPUT_DIR, "tablet_10", lang)
    tablet10_images = get_ordered_images(tablet10_dir)
    if tablet10_images:
        upload_images_for_type("tenInchScreenshots", tablet10_images)
    else:
        print(f"  ✗ 10-inch tablet screenshots dir missing or empty: {tablet10_dir}")

    # 5. Feature Graphic (1024x500)
    fg_candidates = [
        os.path.join(OUTPUT_DIR, f"feature_graphic_{lang}.png"),
        os.path.join(OUTPUT_DIR, "feature_graphic.png")
    ]
    fg_path = next((p for p in fg_candidates if os.path.exists(p)), None)
    if fg_path:
        upload_images_for_type("featureGraphic", [fg_path])
    else:
        print(f"  ✗ Feature graphic missing for {lang}")


def upload_bundle_to_track(service, edit_id, bundle_path, track_name="production"):
    if not os.path.exists(bundle_path):
        print(f"  ✗ AAB bundle file not found at: {bundle_path}")
        return None
    print(f"\n[AAB Upload] Uploading App Bundle: {bundle_path}...")
    def do_upload_bundle():
        media = MediaFileUpload(bundle_path, mimetype="application/octet-stream")
        return service.edits().bundles().upload(
            packageName=PACKAGE_NAME,
            editId=edit_id,
            media_body=media
        )
    bundle_res = execute_with_retry(do_upload_bundle)
    version_code = bundle_res.get("versionCode")
    print(f"  ✓ Uploaded AAB bundle! Version code: {version_code}")

    print(f"[Track Update] Assigning bundle {version_code} to track '{track_name}'...")
    release_notes_list = []
    for loc, info in METADATA.items():
        if "releaseNotes" in info:
            release_notes_list.append({
                "language": loc,
                "text": info["releaseNotes"]
            })

    track_body = {
        "track": track_name,
        "releases": [
            {
                "name": f"{version_code} (1.0.2)",
                "versionCodes": [str(version_code)],
                "status": "completed",
                "releaseNotes": release_notes_list
            }
        ]
    }
    execute_with_retry(lambda: service.edits().tracks().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        track=track_name,
        body=track_body
    ))
    print(f"  ✓ Successfully updated track '{track_name}' with version {version_code}")
    return version_code


def main():
    parser = argparse.ArgumentParser(description="Upload Play Store metadata, screenshots and bundles for Licha")
    parser.add_argument("--lang", default="all", choices=["all", "es-ES", "en-US", "fr-FR"], help="Locale to upload (default: all)")
    parser.add_argument("--key", default=find_default_key_file(), help="Path to service account JSON key (default: auto-detected or PLAY_STORE_JSON_KEY)")
    parser.add_argument("--dry-run", action="store_true", help="Perform upload without committing changes")
    parser.add_argument("--upload-bundle", action="store_true", help="Upload the release AAB bundle to Play Store")
    parser.add_argument("--bundle-path", default=DEFAULT_AAB_PATH, help=f"Path to release AAB bundle (default: {DEFAULT_AAB_PATH})")
    parser.add_argument("--track", default="production", choices=["production", "beta", "alpha", "internal"], help="Track to assign the bundle to (default: production)")
    parser.add_argument("--skip-metadata", action="store_true", help="Skip updating store listings and screenshots")
    args = parser.parse_args()

    if not args.key or not os.path.exists(args.key):
        sys.exit(f"Error: Service account JSON key not found. Looked at '{args.key}'. Specify with --key <path> or set PLAY_STORE_JSON_KEY environment variable.")

    print(f"Key file: {args.key}")
    print(f"Connecting to Google Play Developer API for package '{PACKAGE_NAME}'...")
    credentials = service_account.Credentials.from_service_account_file(
        args.key,
        scopes=["https://www.googleapis.com/auth/androidpublisher"]
    )
    service = build("androidpublisher", "v3", credentials=credentials)

    edit = execute_with_retry(lambda: service.edits().insert(body={}, packageName=PACKAGE_NAME))
    edit_id = edit["id"]
    print(f"Created edit session: {edit_id}")

    try:
        if not args.skip_metadata:
            locales = list(METADATA.keys()) if args.lang == "all" else [args.lang]
            for loc in locales:
                upload_listing_and_images_for_locale(service, edit_id, loc)
        else:
            print("Skipping store listings and screenshots update (--skip-metadata)")

        if args.upload_bundle:
            upload_bundle_to_track(service, edit_id, args.bundle_path, track_name=args.track)

        if args.dry_run:
            print("\n=======================================================")
            print(" [DRY RUN] Aborting edit session without committing changes.")
            print("=======================================================")
            service.edits().delete(packageName=PACKAGE_NAME, editId=edit_id).execute()
            print("Edit session successfully aborted.")
        else:
            print(f"\nCommitting edit {edit_id} to Google Play...")
            try:
                commit_res = execute_with_retry(lambda: service.edits().commit(
                    packageName=PACKAGE_NAME,
                    editId=edit_id
                ))
            except Exception as e:
                if "changesNotSentForReview" in str(e):
                    commit_res = execute_with_retry(lambda: service.edits().commit(
                        packageName=PACKAGE_NAME,
                        editId=edit_id,
                        changesNotSentForReview=True
                    ))
                else:
                    raise
            print(f"🎉 Successfully committed edit! Result ID: {commit_res.get('id')}")
            print("👉 In Google Play Console UI, review changes under the track and click 'Send for review' (Enviar a revisión) if required.")

    except Exception as e:
        print(f"\n❌ Error encountered during Play Store update: {e}")
        try:
            service.edits().delete(packageName=PACKAGE_NAME, editId=edit_id).execute()
            print("Cleaned up edit session.")
        except Exception:
            pass
        raise


if __name__ == "__main__":
    main()
