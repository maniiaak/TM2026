"""
Routes package - API endpoint definitions.
"""
from routes.album_routes import album_bp
from routes.review_routes import review_bp
from routes.home_routes import home_bp
from routes.user_routes import user_bp
from routes.follow_routes import follow_bp
from routes.spotify_auth_routes import spotify_auth_bp

__all__ = [
    "album_bp",
    "review_bp",
    "home_bp",
    "user_bp",
    "follow_bp",
    "spotify_auth_bp",
]