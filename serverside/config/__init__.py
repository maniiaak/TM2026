"""
Configuration module for the iLoveMusic API.
Centralizes all configuration constants and environment setup.
"""
import os
from dotenv import load_dotenv
import firebase_admin
from firebase_admin import credentials

load_dotenv()

# ===== Flask App Configuration =====
DATABASE = 'app_data.db'
CORS_ORIGINS = ["http://localhost:8080", "http://192.168.1.139:8080"]
CACHE_TTL = 3600  # 1 hour in seconds

# ===== Spotify Configuration =====
SPOTIPY_CLIENT_ID = os.getenv("SPOTIPY_CLIENT_ID")
SPOTIPY_CLIENT_SECRET = os.getenv("SPOTIPY_CLIENT_SECRET")
SPOTIFY_REDIRECT_URI = "com.maniiaak.iluvmusic://callback"

# ===== Firebase Configuration =====
FIREBASE_CREDENTIALS_PATH = os.getenv("FIREBASE_CREDENTIALS_PATH", "firebase-creds.json")

# ===== Database Connection Settings =====
DB_TIMEOUT = 30.0  # seconds
DB_BUSY_TIMEOUT = 30000  # milliseconds
DB_JOURNAL_MODE = "WAL"  # Write-Ahead Logging for better concurrency

# ===== Initialize Firebase =====
def init_firebase():
    """Initialize Firebase Admin SDK."""
    if os.path.exists(FIREBASE_CREDENTIALS_PATH):
        try:
            creds = credentials.Certificate(FIREBASE_CREDENTIALS_PATH)
            firebase_admin.initialize_app(creds)
            print("[Firebase] Firebase Admin SDK initialized successfully")
        except Exception as e:
            print(f"[Firebase] Warning: Could not initialize Firebase: {e}")
    else:
        print(f"[Firebase] Warning: Firebase credentials file not found at {FIREBASE_CREDENTIALS_PATH}")