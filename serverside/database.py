"""
Database connection and helper functions.
Provides centralized database access with proper configuration.
"""
import sqlite3
from contextlib import contextmanager
from config import DATABASE, DB_TIMEOUT, DB_BUSY_TIMEOUT, DB_JOURNAL_MODE


def get_db_connection():
    """
    Create a new database connection with optimized settings.

    Returns:
        sqlite3.Connection: Configured database connection
    """
    conn = sqlite3.connect(DATABASE, timeout=DB_TIMEOUT)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    conn.execute(f"PRAGMA journal_mode = {DB_JOURNAL_MODE}")
    conn.execute(f"PRAGMA busy_timeout = {DB_BUSY_TIMEOUT}")
    return conn


def close_db(conn):
    """Close database connection if it exists."""
    if conn:
        conn.close()


@contextmanager
def db_connection():
    """
    Context manager for database connections.
    Ensures connections are properly closed even if exceptions occur.

    Usage:
        with db_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM albums")
            return cursor.fetchall()
    """
    conn = get_db_connection()
    try:
        yield conn
    finally:
        close_db(conn)


def check_tables():
    """Verify database schema and create missing tables."""
    with db_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = cursor.fetchall()
        print(f"🔍 Tables found in {DATABASE}: {[t[0] for t in tables]}")

        # Check reviews table specifically
        cursor.execute("PRAGMA table_info(reviews);")
        cols = cursor.fetchall()
        if cols:
            print(f" 'reviews' table exists with columns: {[c[1] for c in cols]}")
        else:
            print(" ERROR: 'reviews' table does NOT exist!")

        # Create follows table if it doesn't exist
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS follows (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                following_id INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users (id),
                FOREIGN KEY (following_id) REFERENCES users (id),
                UNIQUE(user_id, following_id)
            )
        """)
        conn.commit()