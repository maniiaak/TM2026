"""
Review service - Business logic for review operations.
Handles review creation, retrieval, and statistics.
"""
from typing import List, Dict, Optional, Any
from database import db_connection


def get_album_reviews(album_id: int) -> Dict[str, Any]:
    """
    Get reviews for an album with statistics.
    Returns dict with reviews list, total count, and average rating.
    """
    with db_connection() as conn:
        cursor = conn.cursor()

        # Get reviews with usernames
        cursor.execute("""
            SELECT r.rating, r.content, r.created_at, u.username, r.user_id
            FROM reviews r
            LEFT JOIN users u ON r.user_id = u.id
            WHERE r.album_id = ?
            ORDER BY r.created_at DESC
        """, (album_id,))

        reviews = cursor.fetchall()

        # Calculate stats
        cursor.execute("""
            SELECT COUNT(*), COALESCE(ROUND(AVG(rating), 1), 0.0)
            FROM reviews
            WHERE album_id = ?
        """, (album_id,))

        stats = cursor.fetchone()
        total_count = stats[0]
        avg_rating = stats[1]

        # Format reviews
        reviews_list = []
        for row in reviews:
            reviews_list.append({
                "rating": row['rating'],
                "content": row['content'],
                "created_at": row['created_at'],
                "username": row['username'],
                "user_id": row['user_id']
            })

        return {
            "reviews": reviews_list,
            "totalRatings": total_count,
            "rating": avg_rating
        }


def submit_review(rating: float, content: str, user_id: int, album_id: int) -> Dict[str, Any]:
    """
    Submit a new review for an album.
    Returns dict with success status and review info.
    """
    # Validate inputs
    if rating is None or content is None or album_id is None:
        return {"success": False, "error": "Missing required fields"}

    with db_connection() as conn:
        cursor = conn.cursor()

        try:
            # Verify album exists
            cursor.execute("SELECT id FROM albums WHERE id = ?", (album_id,))
            if not cursor.fetchone():
                return {"success": False, "error": f"Album {album_id} not found"}

            # Verify user exists
            cursor.execute("SELECT username FROM users WHERE id = ?", (user_id,))
            if not cursor.fetchone():
                return {"success": False, "error": "User not found"}

            # Insert review
            cursor.execute('''
                INSERT INTO reviews (rating, content, user_id, album_id)
                VALUES (?, ?, ?, ?)
            ''', (rating, content, user_id, album_id))

            conn.commit()
            review_id = cursor.lastrowid

            return {
                "success": True,
                "review_id": review_id,
                "message": "Review submitted successfully"
            }

        except Exception as e:
            conn.rollback()
            return {"success": False, "error": str(e)}


def get_user_reviews(user_id: int, page: int = 1, limit: int = 10) -> List[Dict[str, Any]]:
    """
    Get paginated reviews for a user.
    """
    offset = (page - 1) * limit

    with db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT r.id,
                   r.content,
                   r.rating,
                   r.created_at,
                   a.id as album_id,
                   a.title,
                   ar.name as artist_name,
                   a.cover_image_url
            FROM reviews r
            JOIN albums a ON r.album_id = a.id
            JOIN artists ar ON a.artist_id = ar.id
            WHERE r.user_id = ?
            ORDER BY r.created_at DESC
            LIMIT ? OFFSET ?
        """, (user_id, limit, offset))

        return [dict(row) for row in cursor.fetchall()]