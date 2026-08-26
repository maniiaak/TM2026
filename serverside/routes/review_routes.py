"""
Review routes - API endpoints for review operations.
"""
from flask import Blueprint, jsonify, request, g
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from services import get_album_reviews, submit_review
from auth import require_auth

review_bp = Blueprint('reviews', __name__, url_prefix='/api')

# Create a limiter for write operations (stricter limits)
write_limiter = Limiter(
    get_remote_address,
    default_limits=["30 per minute", "5 per second"],
    storage_uri="memory://",
    strategy="fixed-window",
)


@review_bp.route('/reviews', methods=['POST'])
@require_auth
@write_limiter.limit("10 per minute")
def submit_review_endpoint():
    """Submit a new review for an album. Uses authenticated user's ID."""
    data = request.get_json()

    if not data:
        return jsonify({"error": "No data provided"}), 400

    rating = data.get('rating')
    content = data.get('content')
    album_id = data.get('album_id')

    if rating is None or content is None or album_id is None:
        return jsonify({"error": "Missing required fields"}), 400

    # Use authenticated user's ID (from Firebase token)
    user_id = g.user_id

    result = submit_review(rating, content, user_id, album_id)

    if not result.get("success"):
        return jsonify(result), 400 if "Missing" in result.get("error", "") else 500

    return jsonify(result), 201