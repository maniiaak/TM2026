import sqlite3
import os
import time
import random
import spotipy
from dotenv import load_dotenv
from spotipy.oauth2 import SpotifyClientCredentials

# --- CONFIGURATION ---
DB_NAME = 'app_data.db'

# --- SPOTIFY AUTH ---
load_dotenv()
sp = spotipy.Spotify(
    auth_manager=SpotifyClientCredentials(
        client_id=os.getenv("SPOTIPY_CLIENT_ID"),
        client_secret=os.getenv("SPOTIPY_CLIENT_SECRET")
    )
)

# --- DATABASE ---
def get_db_connection():
    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn

# --- ARTIST HANDLER ---
def get_or_create_artist(cursor, artist_name, artist_genre=None, artist_country=None):
    cursor.execute("SELECT id FROM artists WHERE LOWER(name) = LOWER(?)", (artist_name,))
    result = cursor.fetchone()
    if result:
        return result[0]

    try:
        cursor.execute(
            "INSERT INTO artists (name, genre, country) VALUES (?, ?, ?)",
            (artist_name, artist_genre, artist_country)
        )
        artist_id = cursor.lastrowid
        print(f"   ✅ New artist '{artist_name}' inserted (ID: {artist_id}).")
        return artist_id
    except Exception as e:
        print(f"   ❌ Error inserting artist: {e}")
        return None

# --- ALBUM HANDLER ---
def get_or_create_album(cursor, album_title, release_date, artist_id, cover_url=None):
    cursor.execute(
        "SELECT id FROM albums WHERE LOWER(title) = LOWER(?) AND artist_id = ?",
        (album_title, artist_id)
    )
    result = cursor.fetchone()
    if result:
        return result[0]

    try:
        cursor.execute(
            "INSERT INTO albums (title, release_date, cover_image_url, artist_id) VALUES (?, ?, ?, ?)",
            (album_title, release_date, cover_url, artist_id)
        )
        return cursor.lastrowid
    except Exception as e:
        print(f"   ❌ Error inserting album: {e}")
        return None

# --- SPOTIFY FETCH ---
def fetch_spotify_data(album_name, artist_name=None):
    try:
        print("   🌐 Fetching from Spotify...")

        query = f"album:{album_name}"
        if artist_name:
            query += f" artist:{artist_name}"

        results = sp.search(q=query, type="album", limit=1)
        if not results["albums"]["items"]:
            print("❌ No results found on Spotify.")
            return None

        album = results["albums"]["items"][0]

        # Get full album and artist details
        full_album = sp.album(album["id"])
        artist_id_spotify = album["artists"][0]["id"]
        artist = sp.artist(artist_id_spotify)

        genres = ", ".join(artist.get("genres") or [])
        album_cover = full_album["images"][0]["url"] if full_album.get("images") else None

        return {
            "title": full_album.get("name"),
            "release_date": full_album.get("release_date"),
            "cover_url": album_cover,
            "artist_name": artist.get("name"),
            "artist_genres": genres,
            "artist_country": None  # Spotify API does not provide country reliably
        }

    except Exception as e:
        print(f"   ❌ Spotify error: {e}")
        return None

# --- PROCESS TO DB ---
def process_spotify_to_db(data, db_conn):
    cursor = db_conn.cursor()
    try:
        print(f"\n🔄 Processing: '{data['title']}' by '{data['artist_name']}'")

        artist_id = get_or_create_artist(
            cursor,
            data["artist_name"],
            data["artist_genres"],
            data["artist_country"]
        )
        if not artist_id:
            return False

        album_id = get_or_create_album(
            cursor,
            data["title"],
            data["release_date"],
            artist_id,
            data["cover_url"]
        )
        if not album_id:
            return False

        db_conn.commit()
        print("✅ Success!\n")
        return True
    except Exception as e:
        db_conn.rollback()
        print(f"❌ Failed: {e}")
        return False
    finally:
        cursor.close()

# --- MAIN LOOP ---
def main():
    print("🚀 Spotify to SQLite Importer")

    if not os.path.exists(DB_NAME):
        print(f"⚠️  Database '{DB_NAME}' not found. Please run init_db() first.")
        return

    conn = get_db_connection()

    try:
        while True:
            print("\n--- Options ---")
            print("1. Import Album")
            print("2. View Albums")
            print("3. View Artists")
            print("4. Exit")

            choice = input("Select: ").strip()

            if choice == "2":
                cursor = conn.cursor()
                cursor.execute("""
                               SELECT a.title, ar.name, a.release_date
                               FROM albums a
                                        JOIN artists ar ON a.artist_id = ar.id
                               """)
                albums = cursor.fetchall()
                print("\n📚 Albums:")
                for a in albums:
                    print(f"   - {a['title']} by {a['name']} ({a['release_date']})")
                cursor.close()

            elif choice == "3":
                cursor = conn.cursor()
                cursor.execute("SELECT id, name, genre, country FROM artists")
                artists = cursor.fetchall()
                print("\n👨‍🎤 Artists:")
                for a in artists:
                    print(f"   ID {a['id']}: {a['name']} | Genres: {a['genre']} | Country: {a['country']}")
                cursor.close()

            elif choice == "1":
                album = input("Enter Album Name: ").strip()
                if not album: continue
                artist = input("Enter Artist Name (optional): ").strip() or None

                time.sleep(random.uniform(0.5, 1.0))  # Politeness delay

                data = fetch_spotify_data(album, artist)
                if data:
                    process_spotify_to_db(data, conn)

            elif choice == "4":
                break
            else:
                print("Invalid choice.")

    finally:
        conn.close()
        print("Connection closed.")

if __name__ == "__main__":
    main()