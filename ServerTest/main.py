from flask import Flask, jsonify, request
import sqlite3
from datetime import datetime

app = Flask(__name__)
DATABASE = 'music.db'

def get_db_connection():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    return conn

def close_db(conn):
    if conn:
        conn.close()

# Helper to convert Row to dict
def row_to_dict(row):
    return dict(row) if row else None

@app.route('/api/music', methods=['GET'])
def get_all_music():
    """Get all music records as a raw list"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM music_collection ORDER BY objectID')
    rows = cursor.fetchall()
    close_db(conn)

    # Return raw list directly
    return jsonify([row_to_dict(row) for row in rows])

@app.route('/api/music/<int:objectID>', methods=['GET'])
def get_music_by_id(objectID):
    """Get single music record by ID (returns 404 if not found)"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM music_collection WHERE objectID = ?', (objectID,))
    row = cursor.fetchone()
    close_db(conn)

    if row is None:
        return jsonify({"error": "Record not found"}), 404

    # Return raw dictionary
    return jsonify(row_to_dict(row))

@app.route('/api/music/artist/<artist_name>', methods=['GET'])
def get_music_by_artist(artist_name):
    """Get all music by a specific artist as a raw list"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
        'SELECT * FROM music_collection WHERE artist_display_name LIKE ?',
        (f'%{artist_name}%',)
    )
    rows = cursor.fetchall()
    close_db(conn)

    return jsonify([row_to_dict(row) for row in rows])

@app.route('/api/music/type/<release_type>', methods=['GET'])
def get_music_by_type(release_type):
    """Get all music by type as a raw list"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
        'SELECT * FROM music_collection WHERE type = ?',
        (release_type,)
    )
    rows = cursor.fetchall()
    close_db(conn)

    return jsonify([row_to_dict(row) for row in rows])

@app.route('/api/music/search', methods=['GET'])
def search_music():
    """Search music by title or artist (raw list)"""
    query = request.args.get('q', '')
    if not query:
        return jsonify({"error": "Query parameter 'q' required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
        '''SELECT * FROM music_collection
           WHERE title LIKE ? OR artist_display_name LIKE ?
           ORDER BY object_id''',
        (f'%{query}%', f'%{query}%')
    )
    rows = cursor.fetchall()
    close_db(conn)

    return jsonify([row_to_dict(row) for row in rows])

@app.route('/api/stats', methods=['GET'])
def get_stats():
    """Get collection statistics (raw object)"""
    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute('SELECT COUNT(*) as total FROM music_collection')
    total = cursor.fetchone()['total']

    cursor.execute('SELECT type, COUNT(*) as count FROM music_collection GROUP BY type')
    by_type = {row['type']: row['count'] for row in cursor.fetchall()}

    cursor.execute('SELECT object_date, COUNT(*) as count FROM music_collection GROUP BY object_date ORDER BY object_date')
    by_year = {str(row['object_date']): row['count'] for row in cursor.fetchall()}

    close_db(conn)

    # Return raw stats object
    return jsonify({
        "total_records": total,
        "by_type": by_type,
        "by_year": by_year
    })

@app.route('/health', methods=['GET'])
def health_check():
    """Health check (raw status)"""
    return jsonify({
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "database": DATABASE
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)