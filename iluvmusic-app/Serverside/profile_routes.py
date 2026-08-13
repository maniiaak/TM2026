from flask import Blueprint, jsonify, request
import sqlite3

profile_api = Blueprint('profile_api', __name__)
DATABASE = 'app_data.db'


def get_db_connection():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA foreign_keys = ON')
    return conn


@profile_api.get('/api/users/<int:user_id>/profile')
def get_profile(user_id):
    current_user_id = request.args.get('current_user_id', type=int)
    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT u.id, u.username, u.handle, u.profile_image_url,
                   COUNT(DISTINCT r.id) AS review_count
            FROM users u
            LEFT JOIN reviews r ON u.id = r.user_id
            WHERE u.id = ?
            GROUP BY u.id
        ''', (user_id,))
        user = cursor.fetchone()
        if not user:
            return jsonify({'success': False, 'error': 'User not found'}), 404

        cursor.execute('SELECT COUNT(*) AS count FROM follows WHERE following_id = ?', (user_id,))
        follower_count = cursor.fetchone()['count']
        cursor.execute('SELECT COUNT(*) AS count FROM follows WHERE user_id = ?', (user_id,))
        following_count = cursor.fetchone()['count']

        is_following = False
        if current_user_id and current_user_id != user_id:
            cursor.execute(
                'SELECT 1 FROM follows WHERE user_id = ? AND following_id = ?',
                (current_user_id, user_id)
            )
            is_following = cursor.fetchone() is not None

        return jsonify({
            'success': True,
            'id': user['id'],
            'username': user['username'],
            'handle': user['handle'],
            'profile_image_url': user['profile_image_url'],
            'review_count': user['review_count'],
            'follower_count': follower_count,
            'following_count': following_count,
            'is_following': is_following
        })
    finally:
        conn.close()


@profile_api.put('/api/users/<int:user_id>/profile-image')
def update_profile_image(user_id):
    data = request.get_json(silent=True) or {}
    image_url = str(data.get('profile_image_url', '')).strip()

    if image_url and not (image_url.startswith('https://') or image_url.startswith('http://')):
        return jsonify({'success': False, 'error': 'Profile image must be a valid HTTP(S) URL'}), 400
    if len(image_url) > 2048:
        return jsonify({'success': False, 'error': 'Profile image URL is too long'}), 400

    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        cursor.execute('SELECT id FROM users WHERE id = ?', (user_id,))
        if not cursor.fetchone():
            return jsonify({'success': False, 'error': 'User not found'}), 404

        cursor.execute(
            'UPDATE users SET profile_image_url = ? WHERE id = ?',
            (image_url or None, user_id)
        )
        conn.commit()
        return jsonify({'success': True, 'profile_image_url': image_url or None})
    finally:
        conn.close()
