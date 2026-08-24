#!/usr/bin/env python3
"""
One-time repair for app_data.db if you seeded it before seed_app_db.py had
idempotency protection (i.e. you ran the seed command more than once and
ended up with duplicate rows).

What it does, in order:
    1. Reports counts of duplicate reviews / albums / artists so you can see
       the damage before touching anything.
    2. Dedupes exact-duplicate reviews (same user, album, rating, content,
       and timestamp -> these can only be re-import duplicates, since a
       genuine re-listen would have a different listened_at).
    3. Merges duplicate artists (same name) into one canonical row,
       repointing albums.artist_id first.
    4. Merges duplicate albums (same title + artist_id) into one canonical
       row, repointing reviews.album_id first.
    5. Re-prints counts after cleanup.

Run with --dry-run first to see what would change without modifying anything.

Usage:
    python fix_duplicates.py --dry-run
    python fix_duplicates.py
"""

import argparse
import sqlite3
import sys

DB_NAME = "app_data.db"


def report(conn, label):
    total_reviews = conn.execute("SELECT COUNT(*) FROM reviews").fetchone()[0]
    dup_reviews = conn.execute("""
                               SELECT COUNT(*) - COUNT(DISTINCT user_id || '|' || COALESCE(album_id,'') || '|' ||
                                                                COALESCE(rating,'') || '|' || content || '|' || COALESCE(created_at,''))
                               FROM reviews
                               """).fetchone()[0]

    total_artists = conn.execute("SELECT COUNT(*) FROM artists").fetchone()[0]
    dup_artists = conn.execute("SELECT COUNT(*) - COUNT(DISTINCT name) FROM artists").fetchone()[0]

    total_albums = conn.execute("SELECT COUNT(*) FROM albums").fetchone()[0]
    dup_albums = conn.execute("""
                              SELECT COUNT(*) - COUNT(DISTINCT title || '|' || artist_id) FROM albums
                              """).fetchone()[0]

    print(f"--- {label} ---", file=sys.stderr)
    print(f"  reviews: {total_reviews}  (exact-duplicate rows: {dup_reviews})", file=sys.stderr)
    print(f"  artists: {total_artists}  (name duplicates: {dup_artists})", file=sys.stderr)
    print(f"  albums:  {total_albums}  (title+artist duplicates: {dup_albums})", file=sys.stderr)
    print(file=sys.stderr)


def dedupe_reviews(conn, dry_run):
    dupes = conn.execute("""
                         SELECT id FROM reviews
                         WHERE id NOT IN (
                             SELECT MIN(id) FROM reviews
                             GROUP BY user_id, album_id, rating, content, created_at
                         )
                         """).fetchall()
    print(f"Exact-duplicate review rows to remove: {len(dupes)}", file=sys.stderr)
    if not dry_run and dupes:
        ids = [row[0] for row in dupes]
        conn.executemany("DELETE FROM reviews WHERE id = ?", [(i,) for i in ids])


def merge_duplicate_artists(conn, dry_run):
    rows = conn.execute("""
                        SELECT name, MIN(id) AS canonical_id, GROUP_CONCAT(id) AS all_ids
                        FROM artists GROUP BY name HAVING COUNT(*) > 1
                        """).fetchall()
    print(f"Duplicate artist names to merge: {len(rows)}", file=sys.stderr)
    for name, canonical_id, all_ids in rows:
        dupe_ids = [int(i) for i in all_ids.split(",") if int(i) != canonical_id]
        if dry_run:
            continue
        for dupe_id in dupe_ids:
            conn.execute("UPDATE albums SET artist_id = ? WHERE artist_id = ?", (canonical_id, dupe_id))
            conn.execute("DELETE FROM artists WHERE id = ?", (dupe_id,))


def merge_duplicate_albums(conn, dry_run):
    rows = conn.execute("""
                        SELECT title, artist_id, MIN(id) AS canonical_id, GROUP_CONCAT(id) AS all_ids
                        FROM albums GROUP BY title, artist_id HAVING COUNT(*) > 1
                        """).fetchall()
    print(f"Duplicate album (title+artist) groups to merge: {len(rows)}", file=sys.stderr)
    for title, artist_id, canonical_id, all_ids in rows:
        dupe_ids = [int(i) for i in all_ids.split(",") if int(i) != canonical_id]
        if dry_run:
            continue
        for dupe_id in dupe_ids:
            conn.execute("UPDATE reviews SET album_id = ? WHERE album_id = ?", (canonical_id, dupe_id))
            conn.execute("DELETE FROM albums WHERE id = ?", (dupe_id,))


def main():
    parser = argparse.ArgumentParser(description="Dedupe a Musicboard-seeded app_data.db")
    parser.add_argument("--dry-run", action="store_true", help="Report only, don't modify the database")
    parser.add_argument("--db", default=DB_NAME, help="Path to the SQLite database")
    args = parser.parse_args()

    conn = sqlite3.connect(args.db, timeout=30.0)
    conn.execute("PRAGMA foreign_keys = ON")
    conn.execute("PRAGMA journal_mode = WAL")
    conn.execute("PRAGMA busy_timeout = 30000")

    report(conn, "before")

    dedupe_reviews(conn, args.dry_run)
    merge_duplicate_artists(conn, args.dry_run)
    merge_duplicate_albums(conn, args.dry_run)

    if args.dry_run:
        print("Dry run only — no changes made. Re-run without --dry-run to apply.", file=sys.stderr)
        conn.rollback()
    else:
        conn.commit()
        report(conn, "after")
        print("Done.", file=sys.stderr)

    conn.close()


if __name__ == "__main__":
    main()