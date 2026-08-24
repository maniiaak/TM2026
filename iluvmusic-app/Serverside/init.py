import os
import sqlite3

DB_NAME = 'app_data.db'

def init_db():
    conn = sqlite3.connect(DB_NAME, timeout=30.0)
    conn.execute("PRAGMA journal_mode = WAL")
    conn.execute("PRAGMA busy_timeout = 30000")
    cursor = conn.cursor()

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            firebase_uid VARCHAR(255) UNIQUE,
            username VARCHAR(100) UNIQUE NOT NULL,
            handle VARCHAR(50) UNIQUE,
            email VARCHAR(255) UNIQUE NOT NULL,
            profile_image_url TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')

    cursor.execute('PRAGMA table_info(users)')
    columns = {row[1] for row in cursor.fetchall()}
    if 'firebase_uid' not in columns:
        cursor.execute('ALTER TABLE users ADD COLUMN firebase_uid VARCHAR(255)')
        cursor.execute('CREATE UNIQUE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(firebase_uid)')
    if 'handle' not in columns:
        cursor.execute('ALTER TABLE users ADD COLUMN handle VARCHAR(50)')
        cursor.execute('CREATE UNIQUE INDEX IF NOT EXISTS idx_users_handle ON users(handle)')
    if 'profile_image_url' not in columns:
        cursor.execute('ALTER TABLE users ADD COLUMN profile_image_url TEXT')

    if os.getenv('ENVIRONMENT', 'development').lower() == 'development':
        cursor.execute('DELETE FROM users WHERE firebase_uid IS NULL')
        print('🧹 Development database: deleted legacy users without Firebase identities.')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS artists (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name VARCHAR(100) NOT NULL,
            genre VARCHAR(50),
            country VARCHAR(50)
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS albums (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title VARCHAR(150) NOT NULL,
            release_date DATE,
            cover_image_url TEXT,
            artist_id INT NOT NULL,
            length INTEGER,
            tracks INTEGER,
            spotify_id VARCHAR(100) UNIQUE,
            CONSTRAINT fk_album_artist FOREIGN KEY(artist_id)
                REFERENCES artists(id) ON DELETE CASCADE
        )
    ''')

    # Migration: add spotify_id column if missing (for existing databases)
    cursor.execute('PRAGMA table_info(albums)')
    columns = {row[1] for row in cursor.fetchall()}
    if 'spotify_id' not in columns:
        cursor.execute('ALTER TABLE albums ADD COLUMN spotify_id VARCHAR(100)')
        cursor.execute('CREATE UNIQUE INDEX IF NOT EXISTS idx_albums_spotify_id ON albums(spotify_id) WHERE spotify_id IS NOT NULL')
        print('📦 Added spotify_id column to albums table')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS reviews (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            rating DECIMAL(3, 2) CHECK (rating >= 0 AND rating <= 5),
            content TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            user_id INT NOT NULL,
            album_id INT,
            CONSTRAINT fk_review_user FOREIGN KEY(user_id)
                REFERENCES users(id) ON DELETE CASCADE,
            CONSTRAINT fk_review_album FOREIGN KEY(album_id)
                REFERENCES albums(id) ON DELETE SET NULL
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS follows (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            following_id INTEGER NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
            UNIQUE(user_id, following_id)
        )
    ''')

    conn.commit()
    conn.close()
    print(f"✅ Database '{DB_NAME}' initialized successfully.")

if __name__ == '__main__':
    init_db()
