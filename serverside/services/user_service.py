"""
User service - Business logic for user operations.
Handles user profiles, statistics, and profile image updates.
"""
from typing import Dict, Any, Optional
from database import db_connection


def get_user_stats(user_id: int, current_user_id: Optional[int] = None) -> Dict[str, Any]:
    """
    Get user profile with statistics.
    Optionally includes follow status relative to current_user_id.
    """
    with db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
            SELECT
                u.id,
                u.username,
                u.handle,
                u.profile_image_url,
                COUNT(r.id) AS review_count
            FROM users u
            LEFT JOIN reviews r ON u.id = r.user_id
            WHERE u.id = ?
            GROUP BY u.id
        """, (user_id,))

        user = cursor.fetchone()

        if not user:
            return {"success": False, "error": "User not found"}

        # Get follower count
        cursor.execute("SELECT COUNT(*) as count FROM follows WHERE following_id = ?", (user_id,))
        follower_count = cursor.fetchone()["count"]

        # Get following count
        cursor.execute("SELECT COUNT(*) as count FROM follows WHERE user_id = ?", (user_id,))
        following_count = cursor.fetchone()["count"]

        # Check if current user follows this user
        is_following = False
        if current_user_id and current_user_id != user_id:
            cursor.execute(
                'SELECT 1 FROM follows WHERE user_id = ? AND following_id = ?',
                (current_user_id, user_id)
            )
            is_following = cursor.fetchone() is not None

        return {
            "success": True,
            "id": user["id"],
            "username": user["username"],
            "handle": user["handle"],
            "profile_image_url": user["profile_image_url"],
            "review_count": user["review_count"],
            "follower_count": follower_count,
            "following_count": following_count,
            "is_following": is_following
        }


def update_profile_image(user_id: int, image_url: str) -> Dict[str, Any]:
    """
    Update user's profile image URL.
    Validates URL format and length.
    """
    if image_url and not (image_url.startswith('https://') or image_url.startswith('http://')):
        return {"success": False, "error": "Profile image must be a valid HTTP(S) URL"}
    if len(image_url) > 2048:
        return {"success": False, "error": "Profile image URL is too long"}

    with db_connection() as conn:
        cursor = conn.cursor()

        try:
            cursor.execute('SELECT id FROM users WHERE id = ?', (user_id,))
            if not cursor.fetchone():
                return {"success": False, "error": "User not found"}

            cursor.execute(
                'UPDATE users SET profile_image_url = ? WHERE id = ?',
                (image_url or None, user_id)
            )
            conn.commit()

            return {"success": True, "profile_image_url": image_url or None}

        except Exception as e:
            conn.rollback()
            return {"success": False, "error": str(e)}