"""
Album service - Business logic for album operations.
Handles album retrieval, formatting, and Spotify integration.
"""
import sqlite3
from typing import List, Dict, Optional, Any
from database import db_connection
from config import SPOTIPY_CLIENT_ID, SPOTIPY_CLIENT_SECRET, SPOTIFY_REDIRECT_URI
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials
import requests
import os


def format_album_for_list(row: sqlite3.Row) -> Dict[str, Any]:
    """
    Format a database album row for list.json format response.
    Handles missing columns by providing default values.
    """
    if not row:
        return None

    # Extract known columns
    album_id = row['id']
    title = row['title']
    release_date = row['release_date']
    cover_image_url = row['cover_image_url']
    artist_name = row['artist_name']

    # Handle missing columns with defaults
    length = row.get('duration', '0:00') if 'duration' in row.keys() else '0:00'
    tracks = row.get('track_count', 0) if 'track_count' in row.keys() else 0
    album_type = row.get('album_type', 'Album') if 'album_type' in row.keys() else 'Album'

    # Format tracks as string
    tracks_str = str(tracks)

    # Generate a URL based on the ID
    generated_url = f"https://www.metmuseum.org/art/collection/search/{album_id}"

    return {
        "artistDisplayName": artist_name,
        "coverImage": cover_image_url,
        "length": length,
        "objectDate": str(release_date),
        "objectID": album_id,
        "objectURL": generated_url,
        "title": title,
        "tracks": tracks_str,
        "type": album_type
    }


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


def get_all_albums() -> List[Dict[str, Any]]:
    """Get all albums with basic info (no review stats)."""
    with db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT
                a.id as objectID,
                a.title,
                ar.name as artistDisplayName,
                a.cover_image_url as coverImage,
                a.release_date as objectDate,
                'Album' as type,
                a.length as length,
                a.tracks as tracks,
                0 as totalRatings,
                0.0 as rating
            FROM albums a
            JOIN artists ar ON a.artist_id = ar.id
        """)
        albums = cursor.fetchall()

        result = []
        for row in albums:
            result.append({
                "objectID": row['objectID'],
                "title": row['title'],
                "artistDisplayName": row['artistDisplayName'],
                "coverImage": row['coverImage'],
                "objectDate": row['objectDate'],
                "type": row['type'],
                "length": row['length'],
                "tracks": row['tracks'],
                "totalRatings": row['totalRatings'],
                "rating": row['rating']
            })
        return result


def get_all_albums_with_stats() -> List[Dict[str, Any]]:
    """Get all albums with review statistics in list.json format."""
    with db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT a.id, a.title, a.release_date, a.cover_image_url,
                   ar.name as artist_name,
                   COALESCE(AVG(r.rating), 0) as avg_rating,
                   COUNT(r.id) as num_of_ratings
            FROM albums a
            JOIN artists ar ON a.artist_id = ar.id
            LEFT JOIN reviews r ON a.id = r.album_id
            GROUP BY a.id
            ORDER BY a.id
        ''')
        albums = cursor.fetchall()

        formatted_albums = []
        for row in albums:
            album_data = format_album_for_list(row)
            album_data["rating"] = round(row["avg_rating"], 2) if row["avg_rating"] else 0
            album_data["totalRatings"] = row["num_of_ratings"]
            album_data["reviews"] = get_reviews_for_album(row["id"])
            formatted_albums.append(album_data)

        return formatted_albums


def get_album_by_id(album_id: int) -> Optional[Dict[str, Any]]:
    """Get single album by ID with review statistics."""
    with db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT a.id,
                   a.title,
                   a.release_date,
                   a.cover_image_url,
                   a.length,
                   a.tracks,
                   ar.name as artist_name,
                   COALESCE(AVG(r.rating), 0) as avg_rating,
                   COUNT(r.id) as num_of_ratings
            FROM albums a
            JOIN artists ar ON a.artist_id = ar.id
            LEFT JOIN reviews r ON a.id = r.album_id
            WHERE a.id = ?
            GROUP BY a.id
        ''', (album_id,))
        album = cursor.fetchone()

        if album is None:
            return None

        album_data = format_album_for_list(album)
        album_data["length"] = album["length"]
        album_data["tracks"] = album["tracks"]
        album_data["ratings"] = round(album["avg_rating"], 2) if album["avg_rating"] else 0
        album_data["num_of_ratings"] = album["num_of_ratings"]
        album_data["reviews"] = get_reviews_for_album(album_id)

        return album_data


def get_reviews_for_album(album_id: int) -> List[Dict[str, Any]]:
    """Fetch all reviews for a specific album including the user's username."""
    with db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT r.user_id, u.username, r.content, r.created_at, r.rating
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            WHERE r.album_id = ?
            ORDER BY r.created_at DESC
        ''', (album_id,))
        reviews = cursor.fetchall()

        return [{
            "userID": review["user_id"],
            "username": review["username"],
            "content": review["content"],
            "createdAt": str(review["created_at"]),
            "rating": review["rating"]
        } for review in reviews]


def create_album_from_spotify(spotify_id: str) -> Dict[str, Any]:
    """
    Import an album from Spotify into the local database.
    Returns dict with success status and album info.
    """
    # Initialize Spotify client
    spotify = spotipy.Spotify(
        auth_manager=SpotifyClientCredentials(
            client_id=SPOTIPY_CLIENT_ID,
            client_secret=SPOTIPY_CLIENT_SECRET
        )
    )

    with db_connection() as conn:
        cursor = conn.cursor()

        try:
            album = spotify.album(spotify_id)

            title = album["name"]
            artist_name = album["artists"][0]["name"]
            release_date = album.get("release_date")
            cover_url = album["images"][0]["url"] if album.get("images") else None
            track_count = album.get("total_tracks", 0)

            # Get track durations for total length
            tracks = spotify.album_tracks(spotify_id)["items"]
            total_ms = sum(track.get("duration_ms", 0) for track in tracks)
            total_seconds = total_ms // 1000
            minutes = total_seconds // 60
            seconds = total_seconds % 60
            length = f"{minutes}:{seconds:02d}"

            # Check if album already exists (by title + artist)
            cursor.execute("""
                SELECT a.id
                FROM albums a
                JOIN artists ar ON a.artist_id = ar.id
                WHERE a.title = ? AND ar.name = ?
                LIMIT 1
            """, (title, artist_name))

            existing = cursor.fetchone()
            if existing:
                return {
                    "success": True,
                    "album_id": existing["id"],
                    "source": "database"
                }

            # Get or create artist
            cursor.execute("SELECT id FROM artists WHERE name = ?", (artist_name,))
            artist = cursor.fetchone()
            if artist:
                artist_id = artist["id"]
            else:
                cursor.execute("INSERT INTO artists (name) VALUES (?)", (artist_name,))
                artist_id = cursor.lastrowid

            # Insert album with spotify_id
            cursor.execute("""
                INSERT INTO albums (
                    title, artist_id, cover_image_url, release_date, length, tracks, spotify_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """, (title, artist_id, cover_url, release_date, length, track_count, spotify_id))

            album_id = cursor.lastrowid
            conn.commit()

            return {
                "success": True,
                "album_id": album_id,
                "source": "created",
                "spotify_id": spotify_id
            }

        except Exception as e:
            conn.rollback()
            return {
                "success": False,
                "error": str(e)
            }


def search_albums(query: str) -> List[Dict[str, Any]]:
    """
    Search for albums in local DB and Spotify.
    Returns combined results with deduplication.
    """
    if not query.strip():
        return []

    results = []

    # Local DB results
    with db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT a.id, a.title, a.cover_image_url, ar.name AS artist_name
            FROM albums a
            JOIN artists ar ON a.artist_id = ar.id
            WHERE LOWER(a.title) LIKE LOWER(?) OR LOWER(ar.name) LIKE LOWER(?)
            LIMIT 10
        """, (f"%{query}%", f"%{query}%"))

        db_albums = cursor.fetchall()

        for album in db_albums:
            results.append({
                "exists": True,
                "album_id": album["id"],
                "spotify_id": None,
                "title": album["title"],
                "artist": album["artist_name"],
                "coverImage": album["cover_image_url"]
            })

        existing_titles = {album["title"].lower() for album in db_albums}

    # Spotify results
    try:
        spotify = spotipy.Spotify(
            auth_manager=SpotifyClientCredentials(
                client_id=SPOTIPY_CLIENT_ID,
                client_secret=SPOTIPY_CLIENT_SECRET
            )
        )

        spotify_results = spotify.search(q=query, type="album", limit=10)
        spotify_items = spotify_results.get("albums", {}).get("items", [])

        for album in spotify_items:
            if album["name"].lower() in existing_titles:
                continue

            results.append({
                "exists": False,
                "album_id": None,
                "spotify_id": album["id"],
                "title": album["name"],
                "artist": album["artists"][0]["name"],
                "coverImage": album["images"][0]["url"] if album.get("images") else None
            })

    except Exception as e:
        print(f"Spotify search error: {e}")

    return results


def exchange_spotify_code(code: str) -> Dict[str, Any]:
    """
    Exchange Spotify authorization code for access token and user profile.
    """
    token_url = "https://accounts.spotify.com/api/token"
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    body = {
        "grant_type": "authorization_code",
        "code": code,
        "redirect_uri": SPOTIFY_REDIRECT_URI,
        "client_id": SPOTIPY_CLIENT_ID,
        "client_secret": SPOTIPY_CLIENT_SECRET
    }

    token_response = requests.post(token_url, headers=headers, data=body)

    if token_response.status_code != 200:
        return {
            "success": False,
            "error": "Token exchange failed",
            "details": token_response.text
        }

    token_data = token_response.json()
    access_token = token_data['access_token']

    # Fetch user profile
    profile_url = "https://api.spotify.com/v1/me"
    profile_headers = {"Authorization": f"Bearer {access_token}"}
    profile_resp = requests.get(profile_url, headers=profile_headers)

    if profile_resp.status_code != 200:
        return {
            "success": False,
            "error": "Profile fetch failed",
            "details": profile_resp.text
        }

    user_info = profile_resp.json()
    spotify_id = user_info.get('id')
    email = user_info.get('email')
    name = user_info.get('display_name')

    if not spotify_id:
        return {"success": False, "error": "Invalid profile data: missing ID"}

    return {
        "success": True,
        "spotify_id": spotify_id,
        "email": email,
        "name": name,
        "access_token": access_token
    }


def save_spotify_user(spotify_id: str, email: str, name: str) -> Dict[str, Any]:
    """
    Save or update Spotify user in database.
    """
    with db_connection() as conn:
        cursor = conn.cursor()

        try:
            cursor.execute("SELECT id, username FROM users WHERE email = ?", (email,))
            user = cursor.fetchone()

            if user:
                user_id = user['id']
                cursor.execute("UPDATE users SET username = ? WHERE id = ?", (name, user_id))
            else:
                cursor.execute("INSERT INTO users (username, email) VALUES (?, ?)", (name, email))
                user_id = cursor.lastrowid

            conn.commit()
            return {
                "success": True,
                "user_id": user_id,
                "username": name,
                "spotify_id": spotify_id
            }

        except Exception as e:
            conn.rollback()
            return {"success": False, "error": str(e)}