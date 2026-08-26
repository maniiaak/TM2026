"""
Application factory - Creates and configures the Flask application.
"""
from flask import Flask
from flask_cors import CORS
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from config import CORS_ORIGINS, init_firebase
from routes import (
    album_bp,
    review_bp,
    home_bp,
    user_bp,
    follow_bp,
    spotify_auth_bp
)
from services import schedule_cache_refresh, refresh_global_home_cache
from database import check_tables


def create_app():
    """Create and configure the Flask application."""
    app = Flask(__name__)

    # Configure CORS
    CORS(app, origins=CORS_ORIGINS)

    # Initialize Flask-Limiter
    limiter = Limiter(
        get_remote_address,
        app=app,
        default_limits=["200 per minute", "50 per second"],
        storage_uri="memory://",
        strategy="fixed-window",
    )

    # Initialize Firebase
    init_firebase()

    # Register blueprints
    app.register_blueprint(album_bp)
    app.register_blueprint(review_bp)
    app.register_blueprint(home_bp)
    app.register_blueprint(user_bp)
    app.register_blueprint(follow_bp)
    app.register_blueprint(spotify_auth_bp)

    # Initialize database tables
    check_tables()

    # Start background cache refresh thread
    schedule_cache_refresh()

    # Pre-populate global cache on startup
    refresh_global_home_cache()

    return app