from flask import Flask, jsonify, request
import sqlite3
import requests
import spotipy
from dotenv import load_dotenv
from spotipy.oauth2 import SpotifyClientCredentials
from datetime import datetime
import os

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
def get_albums():
    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute("""
                   SELECT
                       a.id as objectID,
                       a.title,
                       ar.name as artistDisplayName,
                       a.cover_image_url as coverImage,
                       a.release_date as objectDate,
                       'Album' as type,
                       '0:00' as length,
                       '0' as tracks,
                       COALESCE(COUNT(r.id), 0) as totalRatings,
                       COALESCE(ROUND(AVG(r.rating), 1), 0.0) as rating
                   FROM albums a
                            JOIN artists ar ON a.artist_id = ar.id
                            LEFT JOIN reviews r ON a.id = r.album_id
                   GROUP BY a.id
                   """)

    albums = cursor.fetchall()
    conn.close()

    result = []
    for row in albums:
        result.append({
            "objectID": row['objectID'],
            "title": row['title'],
            "artistDisplayName": row['artistDisplayName'],
            "coverImage": row['coverImage'],
            "objectDate": row['objectDate'],
            "type": row['type'],
            "length": row['length'],
            "tracks": row['tracks'],
            "totalRatings": row['totalRatings'],
            "rating": row['rating']
        })

    return jsonify(result), 200

@app.route('/api/albums/<int:album_id>/reviews', methods=['GET'])
def get_album_reviews(album_id):
    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute("""
                   SELECT r.rating, r.content, r.created_at, u.username
                   FROM reviews r
                            JOIN users u ON r.user_id = u.id
                   WHERE r.album_id = ?
                   ORDER BY r.created_at DESC
                   """, (album_id,))

    reviews = cursor.fetchall()
    conn.close()

    result = []
    for row in reviews:
        result.append({
            "rating": row['rating'],
            "content": row['content'],
            "created_at": row['created_at'],
            "username": row['username']
        })

    return jsonify(result), 200


@app.route('/api/all_albums', methods=['GET'])
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
    user_id = data.get('user_id',)
    album_id = data.get('album_id')

    if rating is None or content is None or album_id is None:
        print(f" Missing fields. Got: rating={rating}, content={content}, album_id={album_id}, user_id={user_id}")
        return jsonify({"error": "Missing required fields"}), 400

    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        # Debug: Check if album exists
        cursor.execute("SELECT id FROM albums WHERE id = ?", (album_id,))
        if not cursor.fetchone():
            print(f" Album ID {album_id} not found in DB!")
            return jsonify({"error": f"Album {album_id} not found"}), 404

        cursor.execute("SELECT username FROM users WHERE id = ?", (user_id,))
        user_row = cursor.fetchone()

        if not user_row:
            return jsonify({"error": "User not found"}), 404

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

@app.route('/api/auth/spotify', methods=['POST'])
def spotify_login():
    data = request.get_json()
    code = data.get('code')

    load_dotenv()

    client_id=os.getenv("SPOTIPY_CLIENT_ID"),
    client_secret=os.getenv("SPOTIPY_CLIENT_SECRET")

    if not code:
        return jsonify({"error": "No code"}), 400

    print(f"Attempting exchange with:")
    print(f"  Code: {code[:10]}...") # Print first 10 chars
    print(f"  Redirect URI: com.jetbrains.kmpapp://callback")
    print(f"  Client ID: {client_id}")

    # 1. Exchange code for Access Token
    token_url = "https://accounts.spotify.com/api/token"
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    body = {
        "grant_type": "authorization_code",
        "code": code,
        "redirect_uri": "com.jetbrains.kmpapp://callback",
        "client_id": client_id,
        "client_secret": client_secret
    }

    print(f"Exchanging code for token...")
    token_response = requests.post(token_url, headers=headers, data=body)

    if token_response.status_code != 200:
        print(f"Token Exchange Failed: {token_response.status_code}")
        print(f"Response: {token_response.text}")
        return jsonify({"error": "Token exchange failed", "details": token_response.text}), 400

    token_data = token_response.json()
    access_token = token_data['access_token']
    print(f"Token received: {access_token[:10]}...")

    # 2. Fetch User Profile
    profile_url = "https://api.spotify.com/v1/me"
    profile_headers = {"Authorization": f"Bearer {access_token}"}

    print(f"Fetching profile with token...")
    profile_resp = requests.get(profile_url, headers=profile_headers)

    if profile_resp.status_code != 200:
        print(f"Profile Fetch Failed: {profile_resp.status_code}")
        print(f"Response: {profile_resp.text}")
        return jsonify({"error": "Profile fetch failed", "details": profile_resp.text}), 400

    user_info = profile_resp.json()
    print(f"Profile fetched: {user_info.get('display_name')} ({user_info.get('id')})")

    # 3. Extract Data
    spotify_id = user_info.get('id')
    email = user_info.get('email')
    name = user_info.get('display_name')

    # DEBUG: Print extracted data
    print(f"Extracted: ID={spotify_id}, Email={email}, Name={name}")

    if not spotify_id:
        print("ERROR: Spotify ID is missing!")
        return jsonify({"error": "Invalid profile data: missing ID"}), 500

    # 4. Save to Database
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Check if user exists by Email
        cursor.execute("SELECT id, username FROM users WHERE email = ?", (email,))
        user = cursor.fetchone()

        if user:
            user_id = user['id']
            # Update name if changed
            cursor.execute("UPDATE users SET username = ? WHERE id = ?", (name, user_id))
            print(f"Updated existing user: {user_id}")
        else:
            # New user
            cursor.execute("INSERT INTO users (username, email) VALUES (?, ?)", (name, email))
            user_id = cursor.lastrowid
            print(f"Created new user: {user_id}")

        conn.commit()
        conn.close()

        # 5. Return Success

        return jsonify({
            "success": True,
            "user_id": user_id,
            "username": name,
            "spotify_id": spotify_id
        }), 200

    except Exception as e:
        print(f"Database Error: {str(e)}")
        import traceback
        traceback.print_exc()
        return jsonify({"error": "Database error", "details": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)