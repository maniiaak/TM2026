from flask import Flask, jsonify, request
import sqlite3
from datetime import datetime

app = Flask(__name__)
DATABASE = 'app_data.db'

# --- Database helpers ---
def get_db_connection():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn

def close_db(conn):
    if conn:
        conn.close()

def row_to_list_json_format(row):
    """
    Transforms a database row into the list.json format.
    Handles missing columns by providing default values.
    """
    if not row:
        return None

    # Extract known columns
    album_id = row['id']
    title = row['title']
    release_date = row['release_date']
    cover_image_url = row['cover_image_url']
    artist_name = row['artist_name']

    # Handle missing columns with defaults to match list.json structure
    # Defaults chosen to look realistic based on your sample data
    length = row.get('duration', '0:00') if 'duration' in row.keys() else '0:00'
    tracks = row.get('track_count', 0) if 'track_count' in row.keys() else 0
    album_type = row.get('album_type', 'Album') if 'album_type' in row.keys() else 'Album'

    # Format tracks as string to match "18" in your JSON example
    tracks_str = str(tracks)

    # Generate a URL based on the ID (matching the pattern in your JSON)
    # If you have a specific URL column, replace the f-string below
    generated_url = f"https://www.metmuseum.org/art/collection/search/{album_id}"

    return {
        "artistDisplayName": artist_name,
        "coverImage": cover_image_url,
        "length": length,
        "objectDate": str(release_date),
        "objectID": album_id,
        "objectURL": generated_url,
        "title": title,
        "tracks": tracks_str,
        "type": album_type
    }

# --- API Endpoints ---

@app.route('/api/albums', methods=['GET'])
def get_all_albums():
    """Get all albums in list.json format"""
    conn = get_db_connection()
    cursor = conn.cursor()

    # SELECT only existing columns: id, title, release_date, cover_image_url
    # We join artists to get the name
    cursor.execute('''
                   SELECT a.id, a.title, a.release_date, a.cover_image_url,
                          ar.name as artist_name
                   FROM albums a
                            JOIN artists ar ON a.artist_id = ar.id
                   ORDER BY a.id
                   ''')
    albums = cursor.fetchall()
    close_db(conn)

    formatted_albums = [row_to_list_json_format(row) for row in albums]
    return jsonify(formatted_albums)

@app.route('/api/albums/<int:album_id>', methods=['GET'])
def get_album_by_id(album_id):
    """Get single album by ID in list.json format"""
    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute('''
                   SELECT a.id, a.title, a.release_date, a.cover_image_url,
                          ar.name as artist_name
                   FROM albums a
                            JOIN artists ar ON a.artist_id = ar.id
                   WHERE a.id = ?
                   ''', (album_id,))
    album = cursor.fetchone()
    close_db(conn)

    if album is None:
        return jsonify({"error": "Album not found"}), 404

    return jsonify(row_to_list_json_format(album))

# Keep other endpoints (artists, stats, search, etc.) as they were,
# or update them similarly if you need them to match the new format.

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)