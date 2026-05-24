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

def get_reviews_for_album(album_id):
    """Fetch all reviews for a specific album including the user's username"""
    conn = get_db_connection()
    cursor = conn.cursor()

    # Join with 'users' table to get the username
    cursor.execute('''
                   SELECT r.user_id, u.username, r.content, r.created_at, r.rating
                   FROM reviews r
                            JOIN users u ON r.user_id = u.id
                   WHERE r.album_id = ?
                   ORDER BY r.created_at DESC
                   ''', (album_id,))

    reviews = cursor.fetchall()
    close_db(conn)

    return [{
        "userID": review["user_id"],
        "username": review["username"],  # Added username
        "content": review["content"],
        "createdAt": str(review["created_at"]),
        "rating": review["rating"]
    } for review in reviews]

def check_tables():
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = cursor.fetchall()
    print(f"🔍 Tables found in {DATABASE}: {[t[0] for t in tables]}")

    # Check reviews table specifically
    cursor.execute("PRAGMA table_info(reviews);")
    cols = cursor.fetchall()
    if cols:
        print(f" 'reviews' table exists with columns: {[c[1] for c in cols]}")
    else:
        print(" ERROR: 'reviews' table does NOT exist!")

    close_db(conn)

# Run check at startup
check_tables()

# --- API Endpoints ---

@app.route('/api/albums', methods=['GET'])
def get_all_albums():
    """Get all albums with review statistics in list.json format"""
    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute('''
                   SELECT a.id, a.title, a.release_date, a.cover_image_url,
                          ar.name as artist_name,
                          COALESCE(AVG(r.rating), 0) as avg_rating,
                          COUNT(r.id) as num_of_ratings
                   FROM albums a
                            JOIN artists ar ON a.artist_id = ar.id
                            LEFT JOIN reviews r ON a.id = r.album_id
                   GROUP BY a.id
                   ORDER BY a.id
                   ''')
    albums = cursor.fetchall()
    close_db(conn)

    formatted_albums = []
    for row in albums:
        album_data = row_to_list_json_format(row)
        album_data["rating"] = round(row["avg_rating"], 2) if row["avg_rating"] else 0
        album_data["totalRatings"] = row["num_of_ratings"]
        album_data["reviews"] = get_reviews_for_album(row["id"])
        formatted_albums.append(album_data)

    return jsonify(formatted_albums)

@app.route('/api/albums/<int:album_id>', methods=['GET'])
def get_album_by_id(album_id):
    """Get single album by ID with review statistics in list.json format"""
    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute('''
                   SELECT a.id, a.title, a.release_date, a.cover_image_url,
                          ar.name as artist_name,
                          COALESCE(AVG(r.rating), 0) as avg_rating,
                          COUNT(r.id) as num_of_ratings
                   FROM albums a
                            JOIN artists ar ON a.artist_id = ar.id
                            LEFT JOIN reviews r ON a.id = r.album_id
                   WHERE a.id = ?
                   GROUP BY a.id
                   ''', (album_id,))
    album = cursor.fetchone()
    close_db(conn)

    if album is None:
        return jsonify({"error": "Album not found"}), 404

    album_data = row_to_list_json_format(album)
    album_data["ratings"] = round(album["avg_rating"], 2) if album["avg_rating"] else 0
    album_data["num_of_ratings"] = album["num_of_ratings"]
    album_data["reviews"] = get_reviews_for_album(album_id)

    return jsonify(album_data)

@app.route('/api/reviews', methods=['POST'])
def submit_review():
    data = request.get_json()
    print(f"Received data: {data}") # Debug log

    if not data:
        return jsonify({"error": "No data provided"}), 400

    rating = data.get('rating')
    content = data.get('content')
    user_id = data.get('user_id', 0)
    album_id = data.get('album_id')

    if rating is None or content is None or album_id is None:
        print(f" Missing fields. Got: rating={rating}, content={content}, album_id={album_id}")
        return jsonify({"error": "Missing required fields"}), 400

    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        # Debug: Check if album exists
        cursor.execute("SELECT id FROM albums WHERE id = ?", (album_id,))
        if not cursor.fetchone():
            print(f" Album ID {album_id} not found in DB!")
            return jsonify({"error": f"Album {album_id} not found"}), 404

        cursor.execute('''
                       INSERT INTO reviews (rating, content, user_id, album_id)
                       VALUES (?, ?, ?, ?)
                       ''', (rating, content, user_id, album_id))

        conn.commit()
        review_id = cursor.lastrowid
        print(f" Review inserted! ID: {review_id}")

        return jsonify({
            "success": True,
            "review_id": review_id,
            "message": "Review submitted successfully"
        }), 201
    except sqlite3.Error as e:
        print(f" Database Error: {e}")
        return jsonify({"error": str(e)}), 500
    finally:
        close_db(conn)

# Keep other endpoints (artists, stats, search, etc.) as they were,
# or update them similarly if you need them to match the new format.

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)