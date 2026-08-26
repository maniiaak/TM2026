"""
Home routes - API endpoints for home feed.
"""
from flask import Blueprint, jsonify, request
from services import (
    get_home_feed,
    get_popular_this_week_paginated,
    get_newly_reviewed_by_friends_paginated,
    get_popular_with_friends_paginated
)

home_bp = Blueprint('home', __name__, url_prefix='/api')


@home_bp.route('/home', methods=['GET'])
def get_home():
    """
    Get home page categories:
    - popular_this_week: Always included (global, cached)
    - newly_reviewed_by_friends: If current_user_id provided
    - popular_with_friends: If current_user_id provided
    Each category limited to first 5 items.
    """
    current_user_id = request.args.get('current_user_id', type=int)
    response = get_home_feed(current_user_id)
    return jsonify(response), 200


@home_bp.route('/home/popular-this-week', methods=['GET'])
def get_home_popular_this_week():
    """Get full list of popular albums this week (for 'See more' screen)."""
    page = request.args.get('page', 1, type=int)
    limit = request.args.get('limit', 20, type=int)

    result = get_popular_this_week_paginated(page, limit)
    return jsonify(result), 200


@home_bp.route('/home/newly-reviewed-by-friends', methods=['GET'])
def get_home_newly_reviewed_by_friends():
    """Get full list of newly reviewed albums by friends (for 'See more' screen)."""
    current_user_id = request.args.get('current_user_id', type=int)

    if not current_user_id:
        return jsonify({"error": "current_user_id is required"}), 400

    page = request.args.get('page', 1, type=int)
    limit = request.args.get('limit', 20, type=int)

    result = get_newly_reviewed_by_friends_paginated(current_user_id, page, limit)
    return jsonify(result), 200


@home_bp.route('/home/popular-with-friends', methods=['GET'])
def get_home_popular_with_friends():
    """Get full list of popular albums with friends (for 'See more' screen)."""
    current_user_id = request.args.get('current_user_id', type=int)

    if not current_user_id:
        return jsonify({"error": "current_user_id is required"}), 400

    page = request.args.get('page', 1, type=int)
    limit = request.args.get('limit', 20, type=int)

    result = get_popular_with_friends_paginated(current_user_id, page, limit)
    return jsonify(result), 200