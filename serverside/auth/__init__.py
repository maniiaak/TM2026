"""
Authentication module - Firebase token verification and user context.
"""
from functools import wraps
from flask import request, jsonify, g
from firebase_admin import auth as firebase_auth


class AuthError(Exception):
    """Authentication error with HTTP status code."""
    def __init__(self, message: str, status_code: int = 401):
        super().__init__(message)
        self.message = message
        self.status_code = status_code


def verify_firebase_token(id_token: str) -> dict:
    """
    Verify a Firebase ID token and return the decoded claims.

    Args:
        id_token: The Firebase ID token string

    Returns:
        dict: Decoded token claims including 'uid', 'email', etc.

    Raises:
        AuthError: If token is invalid or expired
    """
    try:
        return firebase_auth.verify_id_token(id_token)
    except firebase_auth.ExpiredIdTokenError:
        raise AuthError("Token has expired", 401)
    except firebase_auth.RevokedIdTokenError:
        raise AuthError("Token has been revoked", 401)
    except firebase_auth.InvalidIdTokenError:
        raise AuthError("Invalid token", 401)
    except Exception as exc:
        raise AuthError(f"Token verification failed: {exc}", 401)


def extract_token_from_header() -> str:
    """
    Extract Bearer token from Authorization header.

    Returns:
        str: The token string

    Raises:
        AuthError: If header is missing or malformed
    """
    header = request.headers.get('Authorization', '')
    if not header.startswith('Bearer '):
        raise AuthError('Missing or invalid Authorization header', 401)
    return header[7:].strip()


def get_current_user_id() -> int:
    """
    Get the authenticated user's local database ID.
    Requires verify_token() to have been called first (sets g.firebase_claims).

    Returns:
        int: Local user ID

    Raises:
        AuthError: If user not found in database
    """
    from database import db_connection

    if not hasattr(g, 'firebase_claims'):
        raise AuthError('Authentication required', 401)

    firebase_uid = g.firebase_claims['uid']

    with db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT id FROM users WHERE firebase_uid = ?', (firebase_uid,))
        user = cursor.fetchone()

        if not user:
            raise AuthError('User not found. Complete profile setup first.', 404)

        return user['id']


def require_auth(f):
    """
    Decorator that verifies Firebase token and sets g.firebase_claims and g.user_id.
    Use on routes that require authentication.

    Usage:
        @app.route('/api/protected')
        @require_auth
        def protected_route():
            user_id = g.user_id  # Verified local user ID
            ...
    """
    @wraps(f)
    def decorated_function(*args, **kwargs):
        try:
            token = extract_token_from_header()
            claims = verify_firebase_token(token)
            g.firebase_claims = claims
            g.user_id = get_current_user_id()
        except AuthError as e:
            return jsonify({'success': False, 'error': e.message}), e.status_code
        return f(*args, **kwargs)
    return decorated_function


def optional_auth(f):
    """
    Decorator that attempts to verify token but doesn't require it.
    Sets g.firebase_claims and g.user_id if valid token provided.

    Usage:
        @app.route('/api/public')
        @optional_auth
        def public_route():
            if hasattr(g, 'user_id'):
                # Authenticated user
            else:
                # Anonymous user
            ...
    """
    @wraps(f)
    def decorated_function(*args, **kwargs):
        try:
            token = extract_token_from_header()
            claims = verify_firebase_token(token)
            g.firebase_claims = claims
            g.user_id = get_current_user_id()
        except AuthError:
            # Ignore auth errors for optional auth
            pass
        return f(*args, **kwargs)
    return decorated_function