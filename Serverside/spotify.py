import os
from dotenv import load_dotenv
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

load_dotenv()

sp = spotipy.Spotify(
    auth_manager=SpotifyClientCredentials(
        client_id=os.getenv("SPOTIPY_CLIENT_ID"),
        client_secret=os.getenv("SPOTIPY_CLIENT_SECRET")
    )
)

album_name = "Random Access Memories"

# 🔍 Search album
results = sp.search(q=f"album:{album_name}", type="album", limit=1)

if not results["albums"]["items"]:
    print("No album found.")
    exit()

album = results["albums"]["items"][0]

# 📀 Album info
print("=== ALBUM INFO ===")
print("Name:", album["name"])
print("Artists:", ", ".join(a["name"] for a in album["artists"]))
print("Release date:", album["release_date"])

# 🖼️ Album cover (largest image)
if album["images"]:
    print("Album cover:", album["images"][0]["url"])

# 🎤 Artist info (take first artist)
artist_id = album["artists"][0]["id"]
artist = sp.artist(artist_id)

print("\n=== ARTIST INFO ===")
print("Name:", artist["name"])
print("Genres:", ", ".join(artist["genres"]))
print("Followers:", artist["followers"]["total"])

# 🖼️ Artist image
if artist["images"]:
    print("Artist image:", artist["images"][0]["url"])