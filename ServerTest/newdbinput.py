import sqlite3
import sys

# Database configuration
DB_NAME = 'music.db'

def add_album_entry():
    # Define the new album data
    # You can modify these values directly here or make them dynamic later
    new_album = {
        "objectID": 999999,          # Must be unique
        "title": "24",
        "artistDisplayName": "La Fève",
        "type": "Album",             # Or "Mixtape"
        "objectURL": "https://example.com/album",
        "objectDate": 2023,
        "coverImage": "https://pbs.twimg.com/media/GB9UyUsXkAAhScv.jpg",
        "length": "58:39",
        "tracks": 20
    }

    try:
        # Connect to the database
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()

        # Check if the ID already exists to prevent errors
        cursor.execute('SELECT objectID FROM music_collection WHERE objectID = ?', (new_album["objectID"],))
        if cursor.fetchone():
            print(f"Error: An album with ID {new_album['objectID']} already exists.")
            conn.close()
            return

        # Insert the new record
        cursor.execute('''
            INSERT INTO music_collection (
                objectID, title, artistDisplayName, type, objectURL, 
                objectDate, coverImage, length, tracks
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            new_album["objectID"],
            new_album["title"],
            new_album["artistDisplayName"],
            new_album["type"],
            new_album["objectURL"],
            new_album["objectDate"],
            new_album["coverImage"],
            new_album["length"],
            new_album["tracks"]
        ))

        conn.commit()
        print(f"Success! Added '{new_album['title']}' by {new_album['artistDisplayName']} to the database.")
        
        # Optional: Verify the insertion
        cursor.execute('SELECT * FROM music_collection WHERE object_id = ?', (new_album["objectID"],))
        row = cursor.fetchone()
        print(f"Verified entry: {dict(row)}")

    except sqlite3.Error as e:
        print(f"Database error: {e}")
    except Exception as e:
        print(f"Unexpected error: {e}")
    finally:
        if conn:
            conn.close()

if __name__ == '__main__':
    add_album_entry()