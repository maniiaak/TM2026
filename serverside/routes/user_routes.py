"""
User routes - API endpoints for user operations.
"""
from flask import Blueprint, jsonify, request, g
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from services import get_user_stats, get_user_reviews, update_profile_image
from auth import require_auth, optional_auth

user_bp = Blueprint('users', __name__, url_prefix='/api')

# Create a limiter for write operations (stricter limits)
write_limiter = Limiter(
    get_remote_address,
    default_limits=["30 per minute", "5 per second"],
    storage_uri="memory://",
    strategy="fixed-window",
)


@user_bp.route('/users/<int:user_id>/stats', methods=['GET'])
@optional_auth
def get_user(user_id):
    """Get user profile with statistics. Optionally includes follow status."""
    # Use authenticated user's ID if available
    current_user_id = getattr(g, 'user_id', None)

    result = get_user_stats(user_id, current_user_id)

    if not result.get("success"):
        return jsonify(result), 404

    return jsonify(result)


@user_bp.route('/users/<int:user_id>/reviews', methods=['GET'])
def get_user_reviews_endpoint(user_id):
    """Get paginated reviews for a user."""
    page = int(request.args.get('page', 1))
    limit = int(request.args.get('limit', 10))

    reviews = get_user_reviews(user_id, page, limit)
    return jsonify(reviews)


@user_bp.route('/users/<int:user_id>/profile-image', methods=['PUT'])
@require_auth
@write_limiter.limit("10 per minute")
def update_profile_image_endpoint(user_id):
    """Update user's profile image. Only allows updating own profile."""
    # Verify user can only update their own profile
    if g.user_id != user_id:
        return jsonify({"success": False, "error": "Cannot update another user's profile"}), 403

    data = request.get_json(silent=True) or {}
    image_url = str(data.get('profile_image_url', '')).strip()

    result = update_profile_image(user_id, image_url)

    if not result.get("success"):
        return jsonify(result), 400

    return jsonify(result)