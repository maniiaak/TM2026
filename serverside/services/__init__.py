"""
Services package - Business logic layer.
"""
from services.album_service import (
    get_all_albums,
    get_all_albums_with_stats,
    get_album_by_id,
    get_reviews_for_album,
    create_album_from_spotify,
    search_albums,
    exchange_spotify_code,
    save_spotify_user
)
from services.review_service import (
    get_album_reviews,
    submit_review,
    get_user_reviews
)
from services.home_service import (
    get_home_feed,
    get_popular_this_week_paginated,
    get_newly_reviewed_by_friends_paginated,
    get_popular_with_friends_paginated,
    schedule_cache_refresh,
    refresh_global_home_cache
)
from services.user_service import (
    get_user_stats,
    update_profile_image
)
from services.follow_service import (
    follow_user,
    unfollow_user
)

__all__ = [
    # Album service
    "get_all_albums",
    "get_all_albums_with_stats",
    "get_album_by_id",
    "get_reviews_for_album",
    "create_album_from_spotify",
    "search_albums",
    "exchange_spotify_code",
    "save_spotify_user",
    # Review service
    "get_album_reviews",
    "submit_review",
    "get_user_reviews",
    # Home service
    "get_home_feed",
    "get_popular_this_week_paginated",
    "get_newly_reviewed_by_friends_paginated",
    "get_popular_with_friends_paginated",
    "schedule_cache_refresh",
    "refresh_global_home_cache",
    # User service
    "get_user_stats",
    "update_profile_image",
    # Follow service
    "follow_user",
    "unfollow_user",
]