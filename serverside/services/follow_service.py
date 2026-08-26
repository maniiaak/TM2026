"""
Follow service - Business logic for follow/unfollow operations.
Handles user follow relationships.
"""
from typing import Dict, Any
from database import db_connection


def follow_user(current_user_id: int, following_id: int) -> Dict[str, Any]:
    """
    Follow a user.
    Returns dict with success status.
    """
    if current_user_id == following_id:
        return {"success": False, "error": "Cannot follow yourself"}

    with db_connection() as conn:
        cursor = conn.cursor()

        try:
            # Check if already following
            cursor.execute(
                "SELECT 1 FROM follows WHERE user_id = ? AND following_id = ?",
                (current_user_id, following_id)
            )
            if cursor.fetchone():
                return {"success": False, "error": "Already following this user"}

            # Insert follow relationship
            cursor.execute("""
                INSERT INTO follows (user_id, following_id)
                VALUES (?, ?)
            """, (current_user_id, following_id))

            conn.commit()
            return {"success": True, "message": "User followed successfully"}

        except Exception as e:
            conn.rollback()
            return {"success": False, "error": str(e)}


def unfollow_user(current_user_id: int, following_id: int) -> Dict[str, Any]:
    """
    Unfollow a user.
    Returns dict with success status.
    """
    with db_connection() as conn:
        cursor = conn.cursor()

        try:
            cursor.execute("""
                DELETE FROM follows
                WHERE user_id = ? AND following_id = ?
            """, (current_user_id, following_id))

            conn.commit()

            if cursor.rowcount == 0:
                return {"success": False, "error": "Not following this user"}

            return {"success": True, "message": "User unfollowed successfully"}

        except Exception as e:
            conn.rollback()
            return {"success": False, "error": str(e)}