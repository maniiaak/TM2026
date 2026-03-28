import sqlite3
import os

# Configuration
DB_NAME = 'app_data.db'

def init_db():
    # Connect to database (creates file if it doesn't exist)
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    # --- Users table ---
    cursor.execute('''
                   CREATE TABLE IF NOT EXISTS users (
                                                        id INTEGER PRIMARY KEY AUTOINCREMENT,          -- Unique ID for the user
                                                        username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       )
                   ''')

    # --- Artists table ---
    cursor.execute('''
                   CREATE TABLE IF NOT EXISTS artists (
                                                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                          name VARCHAR(100) NOT NULL,
                       genre VARCHAR(50),
                       country VARCHAR(50)
                       )
                   ''')

    # --- Albums table ---
    cursor.execute('''
                   CREATE TABLE IF NOT EXISTS albums (
                                                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                         title VARCHAR(150) NOT NULL,
                       release_date DATE,
                       cover_image_url TEXT,
                       artist_id INT NOT NULL,
                       CONSTRAINT fk_album_artist
                       FOREIGN KEY(artist_id)
                       REFERENCES artists(id)
                       ON DELETE CASCADE
                       )
                   ''')

    # --- Reviews table ---
    cursor.execute('''
                   CREATE TABLE IF NOT EXISTS reviews (
                                                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                          rating DECIMAL(3, 2) CHECK (rating >= 0 AND rating <= 5),
                       content TEXT NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       user_id INT NOT NULL,
                       album_id INT NOT NULL,
                       CONSTRAINT fk_review_user
                       FOREIGN KEY(user_id)
                       REFERENCES users(id)
                       ON DELETE CASCADE,
                       CONSTRAINT fk_review_album
                       FOREIGN KEY(album_id)
                       REFERENCES albums(id)
                       ON DELETE SET NULL
                       )
                   ''')

    conn.commit()
    conn.close()
    print(f"✅ Database '{DB_NAME}' initialized successfully.")

if __name__ == '__main__':
    init_db()