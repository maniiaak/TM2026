"""
Spotify authentication routes - API endpoints for Spotify login.
"""
from flask import Blueprint, jsonify, request
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from services import exchange_spotify_code, save_spotify_user

spotify_auth_bp = Blueprint('spotify_auth', __name__, url_prefix='/api')

# Create a limiter specifically for auth endpoints (stricter limits)
auth_limiter = Limiter(
    get_remote_address,
    default_limits=["5 per minute", "1 per second"],
    storage_uri="memory://",
    strategy="fixed-window",
)


@spotify_auth_bp.route('/auth/spotify', methods=['POST'])
@auth_limiter.limit("5 per minute")
def spotify_login():
    """Exchange Spotify authorization code for token and create/update user."""
    data = request.get_json()
    code = data.get('code')

    if not code:
        return jsonify({"error": "No code"}), 400

    # Exchange code for token and get user profile
    auth_result = exchange_spotify_code(code)

    if not auth_result.get("success"):
        return jsonify(auth_result), 400

    # Save user to database
    user_result = save_spotify_user(
        auth_result["spotify_id"],
        auth_result["email"],
        auth_result["name"]
    )

    if not user_result.get("success"):
        return jsonify(user_result), 500

    return jsonify(user_result), 200