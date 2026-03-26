import sqlite3
import json
import os

# Configuration
DB_NAME = 'app_data.db'

def init_db():
    # Connect to database (creates file if it doesn't exist)
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    # Create users table
    cursor.execute('''
                   CREATE TABLE IF NOT EXISTS users (
                                          id SERIAL PRIMARY KEY,          -- Unique ID for the user
                                          username VARCHAR(50) UNIQUE NOT NULL,
                                          email VARCHAR(255) UNIQUE NOT NULL,
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                   )
                   ''')

    # Create artists table
    cursor.execute('''
                   CREATE TABLE IF NOT EXISTS artists (
                                            id SERIAL PRIMARY KEY,
                                            name VARCHAR(100) NOT NULL,
                                            genre VARCHAR(50),
                                            country VARCHAR(50))      
                   ''')

    cursor.execute('''
                   CREATE TABLE IF NOT EXISTS albums (
                                           id SERIAL PRIMARY KEY,
                                           title VARCHAR(150) NOT NULL,
                                           release_date DATE,
                                           cover_image_url TEXT,
                                           artist_id INT NOT NULL,         -- Foreign Key column

                       -- Define the relationship
                                           CONSTRAINT fk_album_artist
                                               FOREIGN KEY(artist_id)
                                                   REFERENCES artists(id)
                                                   ON DELETE CASCADE           -- If an artist is deleted, their albums go too (optional choice)
                   )
                   ''')

    cursor.execute('''
                   CREATE TABLE IF NOT EXISTS reviews (
                                            id SERIAL PRIMARY KEY,
                                            rating DECIMAL(3, 2) CHECK (rating >= 0 AND rating <= 5), -- e.g., 8.5
                                            content TEXT NOT NULL,
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                            user_id INT NOT NULL,           -- Foreign Key: Who wrote it?
                                            album_id INT NOT NULL,          -- Foreign Key: What did they review?

                       -- Define relationships
                                            CONSTRAINT fk_review_user
                                                FOREIGN KEY(user_id)
                                                    REFERENCES users(id)
                                                    ON DELETE CASCADE,          -- If user deletes account, their reviews vanish

                                            CONSTRAINT fk_review_album
                                                FOREIGN KEY(album_id)
                                                    REFERENCES albums(id)
                                                    ON DELETE SET NULL          -- If album is deleted, review stays but loses album link (or use CASCADE)
                   )''')

    conn.commit()
    conn.close()

if __name__ == '__main__':
    init_db()