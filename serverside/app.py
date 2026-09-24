"""
Application factory - Creates and configures the Flask application.
"""
from werkzeug.middleware.proxy_fix import ProxyFix
from flask import Flask, request
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

    # Add ProxyFix to handle X-Forwarded-For headers
    app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1)

    # Configure CORS
    CORS(app, origins=CORS_ORIGINS)

    # Custom key function for Cloudflare tunnels
    def get_cloudflare_remote_address():
        # Use CF-Connecting-IP if available, otherwise fall back to request.remote_addr
        return request.headers.get("CF-Connecting-IP", request.remote_addr)

    # Initialize Flask-Limiter with Cloudflare-aware key function
    limiter = Limiter(
        get_cloudflare_remote_address,
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