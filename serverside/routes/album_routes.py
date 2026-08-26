"""
Album routes - API endpoints for album operations.
"""
from flask import Blueprint, jsonify, request
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from services import (
    get_all_albums,
    get_all_albums_with_stats,
    get_album_by_id,
    get_album_reviews,
    search_albums,
    create_album_from_spotify
)

album_bp = Blueprint('albums', __name__, url_prefix='/api')

# Create a limiter for Spotify-related operations (external API calls)
spotify_limiter = Limiter(
    get_remote_address,
    default_limits=["30 per minute", "5 per second"],
    storage_uri="memory://",
    strategy="fixed-window",
)


@album_bp.route('/albums', methods=['GET'])
def get_albums():
    """Get all albums with basic info."""
    albums = get_all_albums()
    return jsonify(albums), 200


@album_bp.route('/all_albums', methods=['GET'])
def get_all_albums_endpoint():
    """Get all albums with review statistics in list.json format."""
    albums = get_all_albums_with_stats()
    return jsonify(albums)


@album_bp.route('/albums/<int:album_id>', methods=['GET'])
def get_album(album_id):
    """Get single album by ID with review statistics."""
    album = get_album_by_id(album_id)

    if album is None:
        return jsonify({"error": "Album not found"}), 404

    return jsonify(album)


@album_bp.route('/albums/<int:album_id>/reviews', methods=['GET'])
def get_album_reviews_endpoint(album_id):
    """Get reviews for an album with statistics."""
    result = get_album_reviews(album_id)
    return jsonify(result), 200


@album_bp.route('/spotify/search', methods=['POST'])
@spotify_limiter.limit("20 per minute")
def spotify_search():
    """Search for albums in local DB and Spotify."""
    data = request.get_json()
    query = data.get("query", "").strip()

    if not query:
        return jsonify({"error": "Missing query"}), 400

    results = search_albums(query)

    if not results:
        return jsonify({
            "success": False,
            "error": "Album not found"
        }), 404

    return jsonify({
        "success": True,
        "results": results
    })


@album_bp.route('/spotify/import', methods=['POST'])
@spotify_limiter.limit("10 per minute")
def spotify_import():
    """Import an album from Spotify into local database."""
    data = request.get_json()
    spotify_id = data.get("spotify_id")

    if not spotify_id:
        return jsonify({
            "success": False,
            "error": "Missing spotify_id"
        }), 400

    result = create_album_from_spotify(spotify_id)

    if not result.get("success"):
        return jsonify(result), 500

    return jsonify(result)