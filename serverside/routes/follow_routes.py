"""
Follow routes - API endpoints for follow/unfollow operations.
"""
from flask import Blueprint, jsonify, request, g
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from services import follow_user, unfollow_user
from auth import require_auth

follow_bp = Blueprint('follows', __name__, url_prefix='/api')

# Create a limiter for write operations (stricter limits)
write_limiter = Limiter(
    get_remote_address,
    default_limits=["30 per minute", "5 per second"],
    storage_uri="memory://",
    strategy="fixed-window",
)


@follow_bp.route('/users/<int:following_id>/follow', methods=['POST'])
@require_auth
@write_limiter.limit("10 per minute")
def follow_user_endpoint(following_id):
    """Follow a user. Uses authenticated user's ID from Firebase token."""
    # Use authenticated user's ID (from Firebase token)
    current_user_id = g.user_id

    result = follow_user(current_user_id, following_id)

    if not result.get("success"):
        return jsonify(result), 400

    return jsonify(result), 201


@follow_bp.route('/users/<int:following_id>/unfollow', methods=['POST'])
@require_auth
@write_limiter.limit("10 per minute")
def unfollow_user_endpoint(following_id):
    """Unfollow a user. Uses authenticated user's ID from Firebase token."""
    # Use authenticated user's ID (from Firebase token)
    current_user_id = g.user_id

    result = unfollow_user(current_user_id, following_id)

    if not result.get("success"):
        return jsonify(result), 400

    return jsonify(result), 200