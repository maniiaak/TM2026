#!/usr/bin/env python3
"""
Seed app_data.db (your Flask app's actual schema) with real listening-history
data pulled from the Musicboard.app API.

Maps Musicboard ratings -> your reviews table:
    - rating: Musicboard 0-10  ->  app 0-5 decimal (divided by 2)
    - content: for entries with a review_uid, the real review text is
      fetched from /v2/reviews/mine/ (title + description) and used as-is.
      Plain ratings (no review_uid) get a short auto-generated note instead,
      since your `content` column is NOT NULL and there's no text to import.
    - Musicboard users become app users with a synthetic firebase_uid
      (seed:<musicboard_uid>) so init_db()'s dev-mode cleanup of
      firebase_uid-less users won't delete them.

Usage:
    # seed from top 30 Musicboard users, all their ratings
    python seed_app_db.py --top 30

    # seed from specific usernames, cap 100 ratings each
    python seed_app_db.py --usernames jeunemaniak23,cjrrats --max-per-user 100

    # also synthesize a follow graph among the seeded users (for testing
    # home feed / friend-based features)
    python seed_app_db.py --top 30 --follow-density 0.1

Requires: pip install requests
Set MUSICBOARD_TOKEN env var, or edit TOKEN below.
Run this from the same directory as init.py (it imports init_db from there).
"""

import argparse
import os
import random
import sqlite3
import sys
import time

import requests

from init import init_db  # reuse your actual schema definition

DB_NAME = ("app_data_dev.db")
BASE_URL = "https://api.musicboard.app"
TOKEN = os.environ.get("MUSICBOARD_TOKEN", "9cb82aefb9a0c99d34d0ef91f44cbcb179d870c2")

HEADERS = {
    "Accept": "application/json",
    "Authorization": f"Token {TOKEN}",
    "Origin": "https://musicboard.app",
}


# ---------------------------------------------------------------- API layer

def get_top_users(limit: int) -> list:
    users, offset, page_size = [], 0, min(limit, 100)
    while len(users) < limit:
        resp = requests.get(
            f"{BASE_URL}/v2/users/top/",
            params={"limit": page_size, "offset": offset, "speedup": "true"},
            headers=HEADERS,
        )
        resp.raise_for_status()
        data = resp.json()
        page = data.get("results", [])
        if not page:
            break
        users.extend(page)
        if not data.get("next"):
            break
        offset += page_size
    return users[:limit]


def find_user(username: str) -> dict:
    resp = requests.get(
        f"{BASE_URL}/v2/users/top/",
        params={"advanced_search": username, "limit": 100, "offset": 0, "speedup": "true"},
        headers=HEADERS,
    )
    resp.raise_for_status()
    results = resp.json().get("results", [])
    for user in results:
        if user.get("username", "").lower() == username.lower():
            return user
    if results:
        return results[0]
    raise ValueError(f"No user found matching '{username}'")


def fetch_reviews(creator_uid: str, page_limit: int = 24, delay: float = 0.0) -> dict:
    """Pull all of a user's written reviews and return {review_uid: text}.

    Each review object's own `uid` is what ratings reference via their
    `review_uid` field, and `description` holds the actual written text
    (title is often blank).
    """
    review_map = {}
    offset = 0
    while True:
        params = {
            "content_type": "", "creator": creator_uid, "genres__id": "",
            "has_reports": "", "limit": page_limit, "offset": offset,
            "order_by": "-created_at", "pinned": "", "private": "",
            "record_type": "", "release_date__gte": "", "release_date__lte": "",
            "release_date__year": "", "speedup": "true", "styles__id": "", "title": "",
        }
        resp = requests.get(f"{BASE_URL}/v2/reviews/mine/", params=params, headers=HEADERS)
        resp.raise_for_status()
        data = resp.json()
        page = data.get("results", [])
        if not page:
            break

        for review in page:
            title = (review.get("title") or "").strip()
            description = (review.get("description") or "").strip()
            text = f"{title}\n\n{description}".strip() if title else description
            if text:
                review_map[review["uid"]] = text

        if not data.get("next"):
            break
        offset += page_limit
        if delay:
            time.sleep(delay)

    return review_map


def fetch_ratings(creator_uid: str, max_items: int = None, page_limit: int = 24,
                  delay: float = 0.0) -> list:
    all_results, offset = [], 0
    while True:
        params = {
            "content_type": "", "creator": creator_uid, "genres__id": "",
            "limit": page_limit, "offset": offset, "order_by": "-listened_at",
            "private": "", "record_type": "", "release_date__gte": "",
            "release_date__lte": "", "release_date__year": "",
            "speedup": "true", "styles__id": "",
        }
        resp = requests.get(f"{BASE_URL}/v2/ratings/mine/", params=params, headers=HEADERS)
        resp.raise_for_status()
        data = resp.json()
        page = data.get("results", [])
        if not page:
            break
        all_results.extend(page)
        if max_items and len(all_results) >= max_items:
            return all_results[:max_items]
        if not data.get("next"):
            break
        offset += page_limit
        if delay:
            time.sleep(delay)
    return all_results


# ------------------------------------------------------------- DB layer

class SeedContext:
    """Local caches so we dedupe artists/albums/users the same way the app does."""

    def __init__(self, conn: sqlite3.Connection):
        self.conn = conn
        self.artist_cache = {}   # name -> artist_id
        self.album_cache = {}    # (artist_id, title, release_date) -> album_id
        self.user_cache = {}     # musicboard uid -> local user id

    def get_or_create_artist(self, name: str) -> int:
        if not name:
            name = "Unknown Artist"
        if name in self.artist_cache:
            return self.artist_cache[name]
        cur = self.conn.cursor()
        cur.execute("SELECT id FROM artists WHERE name = ?", (name,))
        row = cur.fetchone()
        if row:
            artist_id = row["id"]
        else:
            cur.execute("INSERT INTO artists (name) VALUES (?)", (name,))
            artist_id = cur.lastrowid
        self.artist_cache[name] = artist_id
        return artist_id

    def get_or_create_album(
        self,
        title: str,
        artist_id: int,
        cover_url: str = None,
        release_date: str = None,
        length: str = None,
        tracks: int = None,
        spotify_id: str = None,
    ) -> int:
        """
        Get or create album with robust deduplication.
        Priority: spotify_id > (title, artist_id, release_date) > (title, artist_id)
        """
        cur = self.conn.cursor()

        # 1. Try to match by Spotify ID (most reliable)
        if spotify_id:
            cur.execute("SELECT id FROM albums WHERE spotify_id = ?", (spotify_id,))
            row = cur.fetchone()
            if row:
                album_id = row["id"]
                # Update missing fields if we have better data
                self._update_album_if_missing(album_id, cover_url, release_date, length, tracks)
                self.album_cache[(artist_id, title, release_date or "")] = album_id
                return album_id

        # 2. Try exact match with release_date (different editions)
        key = (artist_id, title, release_date or "")
        if key in self.album_cache:
            return self.album_cache[key]

        cur.execute(
            "SELECT id FROM albums WHERE title = ? AND artist_id = ? AND (release_date = ? OR release_date IS NULL)",
            (title, artist_id, release_date)
        )
        row = cur.fetchone()
        if row:
            album_id = row["id"]
            self._update_album_if_missing(album_id, cover_url, release_date, length, tracks)
            if spotify_id:
                cur.execute("UPDATE albums SET spotify_id = ? WHERE id = ?", (spotify_id, album_id))
            self.album_cache[key] = album_id
            return album_id

        # 3. Try fuzzy match without release_date (fallback)
        cur.execute(
            "SELECT id FROM albums WHERE title = ? AND artist_id = ?",
            (title, artist_id)
        )
        row = cur.fetchone()
        if row:
            album_id = row["id"]
            self._update_album_if_missing(album_id, cover_url, release_date, length, tracks)
            if spotify_id:
                cur.execute("UPDATE albums SET spotify_id = ? WHERE id = ?", (spotify_id, album_id))
            self.album_cache[key] = album_id
            return album_id

        # 4. Create new album
        cur.execute(
            """INSERT INTO albums (title, artist_id, cover_image_url, release_date, length, tracks, spotify_id)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            (title, artist_id, cover_url, release_date, length, tracks, spotify_id),
        )
        album_id = cur.lastrowid
        self.album_cache[key] = album_id
        return album_id

    def _update_album_if_missing(self, album_id: int, cover_url: str = None, release_date: str = None,
                                  length: str = None, tracks: int = None):
        """Update album fields only if they're currently NULL/empty."""
        cur = self.conn.cursor()
        updates = []
        params = []
        if cover_url:
            updates.append("cover_image_url = ?")
            params.append(cover_url)
        if release_date:
            updates.append("release_date = ?")
            params.append(release_date)
        if length:
            updates.append("length = ?")
            params.append(length)
        if tracks is not None:
            updates.append("tracks = ?")
            params.append(tracks)
        if updates:
            params.append(album_id)
            cur.execute(f"UPDATE albums SET {', '.join(updates)} WHERE id = ?", params)

    def get_or_create_user(self, mb_user: dict) -> int:
        mb_uid = mb_user["uid"]
        if mb_uid in self.user_cache:
            return self.user_cache[mb_uid]

        username = mb_user["username"]
        handle = username.lower()
        email = f"{username.lower()}@seed.musicboard.local"
        firebase_uid = f"seed:{mb_uid}"
        profile_image_url = mb_user.get("profile_picture")

        cur = self.conn.cursor()
        # Atomic upsert: try insert, on conflict update profile_image_url
        cur.execute("""
            INSERT INTO users (firebase_uid, username, handle, email, profile_image_url)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(firebase_uid) DO UPDATE SET
                profile_image_url = COALESCE(excluded.profile_image_url, users.profile_image_url),
                username = COALESCE(NULLIF(excluded.username, ''), users.username),
                handle = COALESCE(NULLIF(excluded.handle, ''), users.handle)
        """, (firebase_uid, username, handle, email, profile_image_url))

        # Get the user_id (either newly inserted or existing)
        cur.execute("SELECT id FROM users WHERE firebase_uid = ?", (firebase_uid,))
        row = cur.fetchone()
        if row:
            user_id = row["id"]
        else:
            # Fallback: shouldn't happen, but handle gracefully
            suffix = mb_uid[:8]
            cur.execute(
                """INSERT INTO users (firebase_uid, username, handle, email, profile_image_url)
                   VALUES (?, ?, ?, ?, ?)""",
                (firebase_uid, f"{username}_{suffix}", f"{handle}_{suffix}",
                 f"{username.lower()}_{suffix}@seed.musicboard.local", profile_image_url),
            )
            user_id = cur.lastrowid

        self.user_cache[mb_uid] = user_id
        return user_id


def scaled_rating(mb_rating):
    """Musicboard is 0-10, the app is 0-5 decimal."""
    if mb_rating is None:
        return None
    return round(mb_rating / 2.0, 2)


def build_review_content(entry: dict, review_map: dict) -> str:
    """content is NOT NULL in your schema, so every imported row needs text.
    Entries with a review_uid get the real written review (looked up via
    review_map); entries without one get a short auto-generated note since
    there's no text to import for a bare rating."""
    review_uid = entry.get("review_uid")
    if review_uid:
        text = review_map.get(review_uid)
        if text:
            return text
        # had a review_uid but we couldn't find/fetch the text (e.g. it was
        # deleted between calls, or belongs to a different page than fetched)
        return "(Musicboard review text unavailable at import time)"

    rating = entry.get("rating")
    first_listen = entry.get("first_listen")
    bits = []
    if rating is not None:
        bits.append(f"Rated {rating}/10 on Musicboard")
    else:
        bits.append("Logged a listen on Musicboard")
    if first_listen:
        bits.append("(first listen)")
    return " ".join(bits) + " — imported for testing."


def ensure_import_log(conn: sqlite3.Connection):
    """A tracking table private to the seed script (not part of your app's
    schema) that records which Musicboard rating IDs have already been
    imported, so re-running the same command is idempotent instead of
    duplicating every review."""
    conn.execute("""
                 CREATE TABLE IF NOT EXISTS _seed_import_log (
                                                                 mb_rating_id INTEGER PRIMARY KEY,
                                                                 review_id INTEGER NOT NULL,
                                                                 imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                 )
                 """)


def insert_review(ctx: SeedContext, entry: dict, review_map: dict):
    content_obj = entry.get("content") or {}
    creator = entry.get("creator") or {}
    if not content_obj or not creator:
        return False

    mb_rating_id = entry.get("id")
    if mb_rating_id is not None:
        already = ctx.conn.execute(
            "SELECT 1 FROM _seed_import_log WHERE mb_rating_id = ?", (mb_rating_id,)
        ).fetchone()
        if already:
            return False  # already imported on a previous run -> skip, don't duplicate

    artist_name = (content_obj.get("artist") or {}).get("name", "Unknown Artist")
    artist_id = ctx.get_or_create_artist(artist_name)

    # Extract all available album metadata from Musicboard
    album_title = content_obj.get("title", "Untitled")
    cover_url = content_obj.get("cover")
    release_date = content_obj.get("release_date")
    length = content_obj.get("length")  # Musicboard doesn't typically have this
    tracks = content_obj.get("track_count")  # Musicboard doesn't typically have this
    spotify_id = content_obj.get("spotify_id")  # If available

    album_id = ctx.get_or_create_album(
        album_title, artist_id, cover_url, release_date, length, tracks, spotify_id
    )
    user_id = ctx.get_or_create_user(creator)

    rating = scaled_rating(entry.get("rating"))
    content = build_review_content(entry, review_map)
    created_at = entry.get("listened_at") or entry.get("created_at")

    # Additional deduplication: check if same user already reviewed this album
    # with same rating and similar content (prevents re-rating duplicates)
    cur = ctx.conn.cursor()
    cur.execute("""
        SELECT id FROM reviews
        WHERE user_id = ? AND album_id = ? AND rating = ? AND created_at = ?
    """, (user_id, album_id, rating, created_at))
    existing = cur.fetchone()
    if existing:
        if mb_rating_id is not None:
            ctx.conn.execute(
                "INSERT OR IGNORE INTO _seed_import_log (mb_rating_id, review_id) VALUES (?, ?)",
                (mb_rating_id, existing["id"]),
            )
        return False

    cur = ctx.conn.execute(
        """INSERT INTO reviews (rating, content, created_at, user_id, album_id)
           VALUES (?, ?, ?, ?, ?)""",
        (rating, content, created_at, user_id, album_id),
    )
    if mb_rating_id is not None:
        ctx.conn.execute(
            "INSERT INTO _seed_import_log (mb_rating_id, review_id) VALUES (?, ?)",
            (mb_rating_id, cur.lastrowid),
        )
    return True


def synthesize_follows(conn: sqlite3.Connection, user_ids: list, density: float):
    """Randomly wire up a follow graph among seeded users, for testing
    friend-based home feed features. density is roughly the fraction of
    possible (follower, followed) pairs to create."""
    if density <= 0 or len(user_ids) < 2:
        return 0

    created = 0
    cur = conn.cursor()
    for uid in user_ids:
        others = [u for u in user_ids if u != uid]
        n_follows = max(1, int(len(others) * density))
        for target in random.sample(others, min(n_follows, len(others))):
            try:
                cur.execute(
                    "INSERT INTO follows (user_id, following_id) VALUES (?, ?)",
                    (uid, target),
                )
                created += 1
            except sqlite3.IntegrityError:
                pass  # already following, ignore
    return created


# ------------------------------------------------------------------ main

def main():
    parser = argparse.ArgumentParser(description="Seed app_data.db with real Musicboard data")
    src = parser.add_mutually_exclusive_group(required=True)
    src.add_argument("--usernames", help="Comma-separated Musicboard usernames to pull")
    src.add_argument("--top", type=int, help="Pull the top N users on Musicboard")
    parser.add_argument("--max-per-user", type=int, default=None,
                        help="Cap ratings fetched per user (default: no cap)")
    parser.add_argument("--delay", type=float, default=0.0,
                        help="Delay in seconds between paginated requests")
    parser.add_argument("--follow-density", type=float, default=0.0,
                        help="0.0-1.0, synthesize a follow graph among seeded users (default: none)")
    parser.add_argument("--no-review-text", action="store_true",
                        help="Skip fetching real review text; use auto-generated placeholders instead")
    parser.add_argument("--reset", action="store_true",
                        help="Wipe previously-seeded data (users/reviews/follows/import log) before seeding")
    args = parser.parse_args()

    print("Ensuring schema exists (init_db)...", file=sys.stderr)
    init_db()

    if args.usernames:
        names = [u.strip() for u in args.usernames.split(",") if u.strip()]
        print(f"Resolving {len(names)} username(s)...", file=sys.stderr)
        mb_users = [find_user(name) for name in names]
    else:
        print(f"Fetching top {args.top} Musicboard users...", file=sys.stderr)
        mb_users = get_top_users(args.top)

    conn = sqlite3.connect(DB_NAME, timeout=30.0)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    conn.execute("PRAGMA journal_mode = WAL")
    conn.execute("PRAGMA busy_timeout = 30000")
    ensure_import_log(conn)

    if args.reset:
        print("Resetting previously-seeded data...", file=sys.stderr)
        conn.execute("DELETE FROM users WHERE firebase_uid LIKE 'seed:%'")  # cascades reviews/follows
        conn.execute("DELETE FROM _seed_import_log")
        conn.execute("DELETE FROM albums WHERE id NOT IN (SELECT DISTINCT album_id FROM reviews WHERE album_id IS NOT NULL)")
        conn.execute("DELETE FROM artists WHERE id NOT IN (SELECT DISTINCT artist_id FROM albums)")
        conn.commit()

    ctx = SeedContext(conn)

    total_reviews = 0
    for i, mb_user in enumerate(mb_users, 1):
        print(f"[{i}/{len(mb_users)}] {mb_user['username']}...", file=sys.stderr)
        ctx.get_or_create_user(mb_user)
        conn.commit()

        review_map = {} if args.no_review_text else fetch_reviews(mb_user["uid"], delay=args.delay)
        ratings = fetch_ratings(mb_user["uid"], max_items=args.max_per_user, delay=args.delay)
        inserted = 0
        for entry in ratings:
            if insert_review(ctx, entry, review_map):
                inserted += 1
        conn.commit()

        total_reviews += inserted
        print(f"    -> {inserted} reviews (running total: {total_reviews})", file=sys.stderr)

    if args.follow_density > 0:
        print(f"Synthesizing follow graph (density={args.follow_density})...", file=sys.stderr)
        n_follows = synthesize_follows(conn, list(ctx.user_cache.values()), args.follow_density)
        conn.commit()
        print(f"  -> {n_follows} follow relationships created", file=sys.stderr)

    n_users = conn.execute("SELECT COUNT(*) FROM users").fetchone()[0]
    n_artists = conn.execute("SELECT COUNT(*) FROM artists").fetchone()[0]
    n_albums = conn.execute("SELECT COUNT(*) FROM albums").fetchone()[0]
    n_reviews = conn.execute("SELECT COUNT(*) FROM reviews").fetchone()[0]
    n_follows_total = conn.execute("SELECT COUNT(*) FROM follows").fetchone()[0]
    conn.close()

    print("\nDone.", file=sys.stderr)
    print(f"  users:    {n_users}", file=sys.stderr)
    print(f"  artists:  {n_artists}", file=sys.stderr)
    print(f"  albums:   {n_albums}", file=sys.stderr)
    print(f"  reviews:  {n_reviews}", file=sys.stderr)
    print(f"  follows:  {n_follows_total}", file=sys.stderr)
    print(f"  db file:  {DB_NAME}", file=sys.stderr)


if __name__ == "__main__":
    main()