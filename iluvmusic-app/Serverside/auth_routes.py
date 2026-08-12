from flask import Blueprint, jsonify, request
import sqlite3
from firebase_admin import auth as firebase_auth

DATABASE = 'app_data.db'
auth_api = Blueprint('firebase_auth_api', __name__)

def get_db_connection():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA foreign_keys = ON')
    return conn

def verify_token():
    header = request.headers.get('Authorization', '')
    if not header.startswith('Bearer '):
        return None, (jsonify({'success': False, 'error': 'Missing Firebase ID token'}), 401)
    try:
        return firebase_auth.verify_id_token(header[7:].strip()), None
    except Exception as exc:
        print(f'[Firebase] Token verification failed: {exc}')
        return None, (jsonify({'success': False, 'error': 'Invalid Firebase ID token'}), 401)

@auth_api.post('/api/auth/firebase')
def firebase_login():
    claims, error = verify_token()
    if error:
        return error

    data = request.get_json(silent=True) or {}
    firebase_uid = claims['uid']
    email = claims.get('email', '')
    username = str(data.get('username', '')).strip()
    handle = str(data.get('handle', '')).strip().lstrip('@').replace(' ', '')

    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        cursor.execute('SELECT id, firebase_uid, username, handle, email FROM users WHERE firebase_uid = ?', (firebase_uid,))
        user = cursor.fetchone()

        if user:
            if not username and not handle:
                return jsonify({'success': True, 'needs_profile': False, 'user_id': user['id'], 'username': user['username'], 'handle': user['handle'], 'email': user['email'], 'firebase_uid': firebase_uid})
            if not username or not handle:
                return jsonify({'success': False, 'error': 'Username and handle are both required'}), 400
            cursor.execute('SELECT id FROM users WHERE username = ? AND id != ?', (username, user['id']))
            if cursor.fetchone():
                return jsonify({'success': False, 'error': 'Username is already taken'}), 409
            cursor.execute('SELECT id FROM users WHERE handle = ? AND id != ?', (handle, user['id']))
            if cursor.fetchone():
                return jsonify({'success': False, 'error': 'Handle is already taken'}), 409
            cursor.execute('UPDATE users SET username = ?, handle = ?, email = ? WHERE id = ?', (username, handle, email, user['id']))
            conn.commit()
            return jsonify({'success': True, 'needs_profile': False, 'user_id': user['id'], 'username': username, 'handle': handle, 'email': email, 'firebase_uid': firebase_uid})

        if not username or not handle:
            return jsonify({'success': True, 'needs_profile': True, 'email': email, 'firebase_uid': firebase_uid})

        cursor.execute('SELECT id FROM users WHERE username = ?', (username,))
        if cursor.fetchone():
            return jsonify({'success': False, 'error': 'Username is already taken'}), 409
        cursor.execute('SELECT id FROM users WHERE handle = ?', (handle,))
        if cursor.fetchone():
            return jsonify({'success': False, 'error': 'Handle is already taken'}), 409

        cursor.execute('INSERT INTO users (firebase_uid, username, handle, email) VALUES (?, ?, ?, ?)', (firebase_uid, username, handle, email))
        user_id = cursor.lastrowid
        conn.commit()
        return jsonify({'success': True, 'needs_profile': False, 'user_id': user_id, 'username': username, 'handle': handle, 'email': email, 'firebase_uid': firebase_uid}), 201
    except sqlite3.IntegrityError:
        conn.rollback()
        return jsonify({'success': False, 'error': 'Username or handle is already taken'}), 409
    finally:
        conn.close()
