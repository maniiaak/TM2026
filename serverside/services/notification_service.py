import logging

from firebase_admin import messaging

from database import db_connection


logger = logging.getLogger(__name__)


def register_push_token(user_id: int, token: str, platform: str = "android") -> dict:
    token = token.strip()
    if not token:
        return {"success": False, "error": "Push token is required"}

    with db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO push_tokens (user_id, token, platform)
            VALUES (?, ?, ?)
            ON CONFLICT(user_id, token) DO UPDATE SET
                platform = excluded.platform,
                updated_at = CURRENT_TIMESTAMP
        """, (user_id, token, platform))
        conn.commit()

    return {"success": True}


def notify_followers_of_review(reviewer_id: int, album_id: int) -> None:
    try:
        with db_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT username, profile_image_url FROM users WHERE id = ?", (reviewer_id,))
            reviewer = cursor.fetchone()
            cursor.execute("""SELECT
                            a.id as objectID,
                            a.title as title,
                            ar.name as artistDisplayName,
                            a.cover_image_url as coverImage
                        FROM albums a
                        JOIN artists ar ON a.artist_id = ar.id
                        WHERE a.id = ?""", (album_id,))
            album = cursor.fetchone()
            cursor.execute("""
                SELECT pt.token
                FROM follows f
                JOIN push_tokens pt ON pt.user_id = f.user_id
                WHERE f.following_id = ?
            """, (reviewer_id,))
            tokens = [row["token"] for row in cursor.fetchall()]

        if not tokens:
            return

        username = reviewer["username"] if reviewer else "Someone you follow"
        profile_picture = (reviewer["profile_image_url"] if reviewer else None) or ""
        album_title = album["title"] if album else "a new album"
        artist_name = f" by {album['artistDisplayName']}" if album else ""
        message = messaging.MulticastMessage(
            data={
                "type": "new_review",
                "album_id": str(album_id),
                "profile_url": profile_picture,
                "title": f"New review by {username}",
                "body": f"{username} reviewed {album_title}{artist_name}",
            },
            android=messaging.AndroidConfig(
                priority="high",
                ttl=0,
            ),
            tokens=list(dict.fromkeys(tokens)),
        )
        response = messaging.send_each_for_multicast(message)
        logger.info("Review push sent: success=%s failure=%s", response.success_count, response.failure_count)

        if response.failure_count:
            with db_connection() as conn:
                cursor = conn.cursor()
                for send_response, token in zip(response.responses, list(dict.fromkeys(tokens))):
                    if send_response.exception and "registration-token-not-registered" in str(send_response.exception).lower():
                        cursor.execute("DELETE FROM push_tokens WHERE token = ?", (token,))
                conn.commit()
    except Exception as exc:
        logger.warning("Could not send review push notification: %s", exc)
