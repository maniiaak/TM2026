"""
Home service - Business logic for home feed computation and caching.
Handles popular albums, friend-based recommendations, and caching.
"""
import sqlite3
from datetime import datetime, timedelta
from typing import List, Dict, Any, Optional
from database import db_connection
from config import CACHE_TTL


# Cache structures
home_cache = {
    'data': None,
    'updated_at': None
}

per_user_home_cache = {}  # { user_id: { 'data': {...}, 'updated_at': ... } }


def is_cache_expired(updated_at: Optional[datetime]) -> bool:
    """Check if cache entry has expired."""
    if updated_at is None:
        return True
    return (datetime.now() - updated_at).total_seconds() > CACHE_TTL


def compute_popular_this_week() -> List[Dict[str, Any]]:
    """
    Get albums most reviewed in the last 7 days.
    Returns list of album dicts (up to 50).
    """
    with db_connection() as conn:
        cursor = conn.cursor()

        seven_days_ago = datetime.now() - timedelta(days=7)

        cursor.execute("""
            SELECT
                a.id, a.title, a.release_date, a.cover_image_url, a.length, a.tracks,
                ar.name as artist_name,
                COUNT(r.id) as review_count,
                COALESCE(AVG(r.rating), 0) as avg_rating
            FROM albums a
            JOIN artists ar ON a.artist_id = ar.id
            LEFT JOIN reviews r ON a.id = r.album_id AND r.created_at >= ?
            GROUP BY a.id
            HAVING COUNT(r.id) > 0
            ORDER BY review_count DESC
            LIMIT 50
        """, (seven_days_ago,))

        albums = cursor.fetchall()

        return [format_album_for_home(row) for row in albums]


def compute_user_friend_lists(user_id: int) -> Dict[str, List[Dict[str, Any]]]:
    """
    Get friend-based recommendations for a user.
    Returns dict with 'newly_reviewed_by_friends' and 'popular_with_friends'.
    """
    with db_connection() as conn:
        cursor = conn.cursor()

        # Get list of users that this user follows
        cursor.execute("SELECT following_id FROM follows WHERE user_id = ?", (user_id,))
        following_rows = cursor.fetchall()
        following_ids = [row['following_id'] for row in following_rows]

        result = {
            'newly_reviewed_by_friends': [],
            'popular_with_friends': []
        }

        if not following_ids:
            return result

        # Format for SQL IN clause
        placeholders = ','.join('?' * len(following_ids))

        # 1. Newly reviewed by friends (most recent reviews from people you follow)
        cursor.execute(f"""
            SELECT
                a.id, a.title, a.release_date, a.cover_image_url, a.length, a.tracks,
                ar.name as artist_name,
                COUNT(r.id) as review_count,
                COALESCE(AVG(r.rating), 0) as avg_rating,
                MAX(r.created_at) as latest_review
            FROM albums a
            JOIN artists ar ON a.artist_id = ar.id
            LEFT JOIN reviews r ON a.id = r.album_id AND r.user_id IN ({placeholders})
            GROUP BY a.id
            HAVING COUNT(r.id) > 0
            ORDER BY latest_review DESC
            LIMIT 50
        """, following_ids)

        newly_reviewed = cursor.fetchall()
        result['newly_reviewed_by_friends'] = [format_album_for_home(row) for row in newly_reviewed]

        # 2. Popular with friends (most reviewed albums by people you follow)
        cursor.execute(f"""
            SELECT
                a.id, a.title, a.release_date, a.cover_image_url, a.length, a.tracks,
                ar.name as artist_name,
                COUNT(r.id) as review_count,
                COALESCE(AVG(r.rating), 0) as avg_rating
            FROM albums a
            JOIN artists ar ON a.artist_id = ar.id
            LEFT JOIN reviews r ON a.id = r.album_id AND r.user_id IN ({placeholders})
            GROUP BY a.id
            HAVING COUNT(r.id) > 0
            ORDER BY review_count DESC
            LIMIT 50
        """, following_ids)

        popular_with_friends = cursor.fetchall()
        result['popular_with_friends'] = [format_album_for_home(row) for row in popular_with_friends]

        return result


def format_album_for_home(row: sqlite3.Row) -> Dict[str, Any]:
    """Format a database album row for home endpoint response."""
    return {
        "objectID": row['id'],
        "title": row['title'],
        "artistDisplayName": row['artist_name'],
        "coverImage": row['cover_image_url'],
        "objectDate": str(row['release_date']),
        "type": "Album",
        "length": row['length'],
        "tracks": row['tracks'],
        "totalRatings": row['review_count'],
        "rating": round(row['avg_rating'], 1)
    }


def refresh_global_home_cache():
    """Recompute and refresh global cache (popular_this_week)."""
    global home_cache
    try:
        popular = compute_popular_this_week()
        home_cache['data'] = popular
        home_cache['updated_at'] = datetime.now()
        print(f"[Cache] Global cache refreshed. Popular this week: {len(popular)} albums")
    except Exception as e:
        print(f"[Cache Error] Failed to refresh global cache: {e}")


def get_home_feed(current_user_id: Optional[int] = None) -> Dict[str, Any]:
    """
    Get home page categories:
    - popular_this_week: Always included (global, cached)
    - newly_reviewed_by_friends: If current_user_id provided
    - popular_with_friends: If current_user_id provided
    Each category limited to first 5 items.
    """
    global home_cache, per_user_home_cache

    response = {}

    # 1. Popular this week (global cache)
    if is_cache_expired(home_cache['updated_at']):
        print("[Home] Popular this week cache expired, recomputing...")
        refresh_global_home_cache()

    response['popular_this_week'] = home_cache['data'][:5] if home_cache['data'] else []

    # 2. Friend-based categories (per-user cache, only if user provided)
    if current_user_id:
        if current_user_id not in per_user_home_cache or is_cache_expired(per_user_home_cache[current_user_id].get('updated_at')):
            print(f"[Home] Friend lists cache expired for user {current_user_id}, recomputing...")
            friend_lists = compute_user_friend_lists(current_user_id)
            per_user_home_cache[current_user_id] = {
                'data': friend_lists,
                'updated_at': datetime.now()
            }

        friend_data = per_user_home_cache[current_user_id]['data']
        response['newly_reviewed_by_friends'] = friend_data['newly_reviewed_by_friends'][:5]
        response['popular_with_friends'] = friend_data['popular_with_friends'][:5]
    else:
        response['newly_reviewed_by_friends'] = []
        response['popular_with_friends'] = []

    return response


def get_popular_this_week_paginated(page: int = 1, limit: int = 20) -> Dict[str, Any]:
    """Get full list of popular albums this week with pagination."""
    global home_cache

    if is_cache_expired(home_cache['updated_at']):
        refresh_global_home_cache()

    offset = (page - 1) * limit
    all_albums = home_cache['data'] if home_cache['data'] else []
    paginated = all_albums[offset:offset + limit]

    return {
        "category": "popular_this_week",
        "albums": paginated,
        "page": page,
        "limit": limit,
        "total": len(all_albums)
    }


def get_newly_reviewed_by_friends_paginated(current_user_id: int, page: int = 1, limit: int = 20) -> Dict[str, Any]:
    """Get full list of newly reviewed albums by friends with pagination."""
    global per_user_home_cache

    if current_user_id not in per_user_home_cache or is_cache_expired(per_user_home_cache[current_user_id].get('updated_at')):
        friend_lists = compute_user_friend_lists(current_user_id)
        per_user_home_cache[current_user_id] = {
            'data': friend_lists,
            'updated_at': datetime.now()
        }

    offset = (page - 1) * limit
    all_albums = per_user_home_cache[current_user_id]['data']['newly_reviewed_by_friends']
    paginated = all_albums[offset:offset + limit]

    return {
        "category": "newly_reviewed_by_friends",
        "albums": paginated,
        "page": page,
        "limit": limit,
        "total": len(all_albums)
    }


def get_popular_with_friends_paginated(current_user_id: int, page: int = 1, limit: int = 20) -> Dict[str, Any]:
    """Get full list of popular albums with friends with pagination."""
    global per_user_home_cache

    if current_user_id not in per_user_home_cache or is_cache_expired(per_user_home_cache[current_user_id].get('updated_at')):
        friend_lists = compute_user_friend_lists(current_user_id)
        per_user_home_cache[current_user_id] = {
            'data': friend_lists,
            'updated_at': datetime.now()
        }

    offset = (page - 1) * limit
    all_albums = per_user_home_cache[current_user_id]['data']['popular_with_friends']
    paginated = all_albums[offset:offset + limit]

    return {
        "category": "popular_with_friends",
        "albums": paginated,
        "page": page,
        "limit": limit,
        "total": len(all_albums)
    }


def schedule_cache_refresh():
    """Start a background thread that refreshes global cache every hour."""
    import threading
    import time

    def refresh_loop():
        while True:
            try:
                time.sleep(CACHE_TTL)  # Wait 1 hour
                refresh_global_home_cache()
            except Exception as e:
                print(f"[Cache Error] In refresh loop: {e}")

    thread = threading.Thread(target=refresh_loop, daemon=True)
    thread.start()
    print("[Cache] Background cache refresh thread started")