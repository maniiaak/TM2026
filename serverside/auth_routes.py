from flask import Blueprint, jsonify, request, g
import sqlite3
from database import db_connection
from auth import verify_firebase_token, extract_token_from_header, get_current_user_id

auth_api = Blueprint('firebase_auth_api', __name__)

@auth_api.post('/api/auth/firebase')
def firebase_login():
    try:
        token = extract_token_from_header()
        claims = verify_firebase_token(token)
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 401

    data = request.get_json(silent=True) or {}
    firebase_uid = claims['uid']
    email = claims.get('email', '')
    username = str(data.get('username', '')).strip()
    handle = str(data.get('handle', '')).strip().lstrip('@').replace(' ', '')

    with db_connection() as conn:
        cursor = conn.cursor()
        try:
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
