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

def row_to_dict(row):
    return dict(row) if row else None

# --- API Endpoints ---

@app.route('/api/albums', methods=['GET'])
def get_all_albums():
    """Get all albums"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('''
                   SELECT a.id as album_id, a.title, a.release_date, a.cover_image_url,
                          ar.id as artist_id, ar.name as artist_name, ar.genre, ar.country
                   FROM albums a
                            JOIN artists ar ON a.artist_id = ar.id
                   ORDER BY a.id
                   ''')
    albums = cursor.fetchall()
    close_db(conn)
    return jsonify([row_to_dict(row) for row in albums])

@app.route('/api/albums/<int:album_id>', methods=['GET'])
def get_album_by_id(album_id):
    """Get single album by ID"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('''
                   SELECT a.id as album_id, a.title, a.release_date, a.cover_image_url,
                          ar.id as artist_id, ar.name as artist_name, ar.genre, ar.country
                   FROM albums a
                            JOIN artists ar ON a.artist_id = ar.id
                   WHERE a.id = ?
                   ''', (album_id,))
    album = cursor.fetchone()
    close_db(conn)
    if album is None:
        return jsonify({"error": "Album not found"}), 404
    return jsonify(row_to_dict(album))

@app.route('/api/artists', methods=['GET'])
def get_all_artists():
    """Get all artists"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('SELECT id as artist_id, name, genre, country FROM artists ORDER BY id')
    artists = cursor.fetchall()
    close_db(conn)
    return jsonify([row_to_dict(row) for row in artists])

@app.route('/api/albums/artist/<artist_name>', methods=['GET'])
def get_albums_by_artist(artist_name):
    """Get all albums by a specific artist"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('''
                   SELECT a.id as album_id, a.title, a.release_date, a.cover_image_url,
                          ar.id as artist_id, ar.name as artist_name, ar.genre, ar.country
                   FROM albums a
                            JOIN artists ar ON a.artist_id = ar.id
                   WHERE ar.name LIKE ?
                   ORDER BY a.id
                   ''', (f'%{artist_name}%',))
    albums = cursor.fetchall()
    close_db(conn)
    return jsonify([row_to_dict(row) for row in albums])

@app.route('/api/albums/search', methods=['GET'])
def search_albums():
    """Search albums by title or artist"""
    query = request.args.get('q', '')
    if not query:
        return jsonify({"error": "Query parameter 'q' required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('''
                   SELECT a.id as album_id, a.title, a.release_date, a.cover_image_url,
                          ar.id as artist_id, ar.name as artist_name, ar.genre, ar.country
                   FROM albums a
                            JOIN artists ar ON a.artist_id = ar.id
                   WHERE a.title LIKE ? OR ar.name LIKE ?
                   ORDER BY a.id
                   ''', (f'%{query}%', f'%{query}%'))
    albums = cursor.fetchall()
    close_db(conn)
    return jsonify([row_to_dict(row) for row in albums])

@app.route('/api/stats', methods=['GET'])
def get_stats():
    """Get database statistics"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('SELECT COUNT(*) as total_albums FROM albums')
    total_albums = cursor.fetchone()['total_albums']

    cursor.execute('SELECT COUNT(*) as total_artists FROM artists')
    total_artists = cursor.fetchone()['total_artists']

    close_db(conn)
    return jsonify({
        "total_albums": total_albums,
        "total_artists": total_artists
    })

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "database": DATABASE
    })

@app.route('/api/albums/<int:album_id>/full', methods=['GET'])
def get_album_full(album_id):
    """Get album with artist and all reviews"""
    conn = get_db_connection()
    cursor = conn.cursor()

    # Fetch album + artist
    cursor.execute('''
                   SELECT a.id AS album_id, a.title, a.release_date, a.cover_image_url,
                          ar.id AS artist_id, ar.name AS artist_name, ar.genre, ar.country
                   FROM albums a
                            JOIN artists ar ON a.artist_id = ar.id
                   WHERE a.id = ?
                   ''', (album_id,))
    album = cursor.fetchone()
    if album is None:
        close_db(conn)
        return jsonify({"error": "Album not found"}), 404

    album_data = row_to_dict(album)

    # Fetch reviews with user info
    cursor.execute('''
                   SELECT r.id AS review_id, r.rating, r.content, r.created_at,
                          u.id AS user_id, u.username
                   FROM reviews r
                            JOIN users u ON r.user_id = u.id
                   WHERE r.album_id = ?
                   ORDER BY r.created_at DESC
                   ''', (album_id,))
    reviews = cursor.fetchall()
    album_data['reviews'] = [row_to_dict(r) for r in reviews]

    close_db(conn)
    return jsonify(album_data)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)