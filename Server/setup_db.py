import sqlite3
import json
import os

# Configuration
DB_NAME = 'music.db'
JSON_FILE = 'Server/data.json'

def init_db():
    # Connect to database (creates file if it doesn't exist)
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    # Create table
    cursor.execute('''
                   CREATE TABLE IF NOT EXISTS music_collection (
                                                                   objectID INTEGER PRIMARY KEY,
                                                                   title TEXT NOT NULL,
                                                                   artistDisplayName TEXT NOT NULL,
                                                                   type TEXT,
                                                                   objectURL TEXT,
                                                                   objectDate TEXT,
                                                                   coverImage TEXT,
                                                                   length TEXT,
                                                                   tracks TEXT
                   )
                   ''')

    # Check if data already exists to avoid duplicates
    cursor.execute('SELECT COUNT(*) FROM music_collection')
    count = cursor.fetchone()[0]

    if count > 0:
        print(f"Database already initialized with {count} records.")
        conn.close()
        return

    # Load JSON data
    if not os.path.exists(JSON_FILE):
        print(f"Error: Could not find {JSON_FILE}")
        conn.close()
        return

    with open(JSON_FILE, 'r') as f:
        try:
            data = json.load(f)
        except json.JSONDecodeError:
            print("Error: Invalid JSON format in file.")
            conn.close()
            return

    # Insert records
    for item in data:
        cursor.execute('''
        INSERT OR REPLACE INTO music_collection 
        (objectID, title, artistDisplayName, type, objectURL, objectDate, 
         coverImage, length, tracks)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            item['objectID'],
            item['title'],
            item['artistDisplayName'],
            item['type'],
            item['objectURL'],
            int(item['objectDate']),
            item['coverImage'],
            item['length'],
            int(item['tracks'])
        ))

    conn.commit()
    print(f"Successfully inserted {len(data)} records into {DB_NAME}.")
    conn.close()

if __name__ == '__main__':
    init_db()