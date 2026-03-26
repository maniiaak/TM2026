import requests
import sqlite3
import os
import time
import random

# --- CONFIGURATION ---
DB_NAME = 'app_data.db'
MUSICBRAINZ_URL = "https://musicbrainz.org/ws/2/release"
HEADERS = {
    # Always include an email in the User-Agent for better support
    "User-Agent": "MusicReviewApp/1.0 (your-email@example.com)",
    "Accept": "application/json"
}

def get_db_connection():
    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn

def get_or_create_artist(cursor, artist_name, artist_country=None, artist_genre=None):
    cursor.execute("SELECT id FROM artists WHERE LOWER(name) = LOWER(?)", (artist_name,))
    result = cursor.fetchone()

    if result:
        return result[0]

    try:
        cursor.execute("INSERT INTO artists (name, genre, country) VALUES (?, ?, ?)",
                       (artist_name, artist_genre, artist_country))
        artist_id = cursor.lastrowid
        print(f"   ✅ New artist '{artist_name}' inserted (ID: {artist_id}).")
        return artist_id
    except Exception as e:
        print(f"   ❌ Error inserting artist: {e}")
        return None

def get_or_create_album(cursor, album_title, release_date, artist_id, cover_url=None):
    # Verify artist exists
    cursor.execute("SELECT id FROM artists WHERE id = ?", (artist_id,))
    if not cursor.fetchone():
        print(f"   ❌ ERROR: Artist ID {artist_id} not found.")
        return None

    cursor.execute("SELECT id FROM albums WHERE LOWER(title) = LOWER(?) AND artist_id = ?",
                   (album_title, artist_id))
    result = cursor.fetchone()

    if result:
        return result[0]

    try:
        cursor.execute("INSERT INTO albums (title, release_date, cover_image_url, artist_id) VALUES (?, ?, ?, ?)",
                       (album_title, release_date, cover_url, artist_id))
        return cursor.lastrowid
    except sqlite3.IntegrityError as e:
        print(f"   ❌ Foreign Key Error: {e}")
        return None
    except Exception as e:
        print(f"   ❌ Error inserting album: {e}")
        return None

def fetch_musicbrainz_data(album_name, artist_name=None, max_retries=3):
    """Fetches data with retry logic and rate limiting."""
    query_parts = []
    if album_name:
        query_parts.append(f'release:"{album_name}"')
    if artist_name:
        query_parts.append(f'artist:"{artist_name}"')

    query = " AND ".join(query_parts)
    params = {"fmt": "json", "limit": 1, "query": query}

    for attempt in range(max_retries):
        try:
            print(f"   🌐 Fetching data (Attempt {attempt + 1}/{max_retries})...")
            response = requests.get(MUSICBRAINZ_URL, params=params, headers=HEADERS, timeout=15)

            if response.status_code == 429:
                wait_time = 5 * (attempt + 1)
                print(f"   ⚠️  Rate limited! Waiting {wait_time}s...")
                time.sleep(wait_time)
                continue

            response.raise_for_status()
            data = response.json()

            if "releases" in data and len(data["releases"]) > 0:
                return data["releases"][0]
            else:
                print("❌ No results found on MusicBrainz.")
                return None

        except requests.exceptions.SSLError as e:
            print(f"   ⚠️  SSL Error: {e}. Retrying in 2 seconds...")
            time.sleep(2)
        except requests.exceptions.RequestException as e:
            print(f"   ⚠️  Network Error: {e}. Retrying in 2 seconds...")
            time.sleep(2)
        except Exception as e:
            print(f"   ❌ Unexpected error: {e}")
            return None

    print("   ❌ Failed after multiple retries.")
    return None

def process_mb_release_to_db(mb_release_data, db_conn):
    cursor = db_conn.cursor()
    try:
        title = mb_release_data.get('title')
        date_str = mb_release_data.get('date')

        artist_credit = mb_release_data.get('artist-credit', [])
        if not artist_credit:
            print("❌ No artist data found.")
            return False

        artist_name = artist_credit[0].get('artist', {}).get('name')

        print(f"\n🔄 Processing: '{title}' by '{artist_name}'")

        artist_id = get_or_create_artist(cursor, artist_name)
        if not artist_id: return False

        album_id = get_or_create_album(cursor, title, date_str, artist_id)
        if not album_id: return False

        db_conn.commit()
        print("✅ Success!\n")
        return True
    except Exception as e:
        db_conn.rollback()
        print(f"❌ Failed: {e}")
        return False
    finally:
        cursor.close()

def main():
    print("🚀 MusicBrainz to SQLite Importer (With Rate Limiting)")

    if not os.path.exists(DB_NAME):
        print(f"⚠️  Database '{DB_NAME}' not found. Initializing...")
        try:
            from init import init_db
            init_db()
            print("✅ Database initialized.\n")
        except ImportError:
            print("❌ Could not import init.py. Please ensure it's in the same folder.")
            return

    conn = get_db_connection()

    try:
        while True:
            print("\n--- Options ---")
            print("1. Import Album")
            print("2. View Albums")
            print("3. Debug: Check Artists")
            print("4. Exit")

            choice = input("Select: ").strip()

            if choice == "2":
                cursor = conn.cursor()
                cursor.execute("SELECT a.title, ar.name, a.release_date FROM albums a JOIN artists ar ON a.artist_id = ar.id")
                albums = cursor.fetchall()
                print("\n📚 Albums:")
                for a in albums: print(f"   - {a['title']} by {a['name']} ({a['release_date']})")
                cursor.close()

            elif choice == "3":
                cursor = conn.cursor()
                cursor.execute("SELECT id, name FROM artists")
                artists = cursor.fetchall()
                print("\n👨‍🎤 Artists:")
                for a in artists: print(f"   ID {a['id']}: {a['name']}")
                cursor.close()

            elif choice == "1":
                album = input("Enter Album Name: ").strip()
                if not album: continue
                artist = input("Enter Artist Name (optional): ").strip() or None

                # Add a small random delay before starting to be polite
                time.sleep(random.uniform(0.5, 1.5))

                mb_data = fetch_musicbrainz_data(album, artist)
                if mb_data:
                    process_mb_release_to_db(mb_data, conn)

            elif choice == "4":
                break
            else:
                print("Invalid choice.")
    finally:
        conn.close()
        print("Connection closed.")

if __name__ == "__main__":
    main()