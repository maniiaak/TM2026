import requests
from urllib.parse import urlencode

class MusicBrainzClient:
    def __init__(self, user_agent="MyMusicApp/1.0"):
        """
        Initialize the MusicBrainz client.

        Args:
            user_agent: Required by MusicBrainz to identify your application
        """
        self.base_url = "https://musicbrainz.org/ws/2/release"
        self.headers = {
            "User-Agent": user_agent,
            "Accept": "application/json"
        }

    def search_album(self, album_name, artist_name=None, limit=5):
        """
        Search for albums by name and optionally by artist.

        Args:
            album_name: Name of the album to search for
            artist_name: Optional artist name to narrow results
            limit: Maximum number of results to return

        Returns:
            List of matching releases (albums)
        """
        params = {
            "fmt": "json",
            "limit": limit
        }

        # Build search query
        query_parts = []
        if album_name:
            query_parts.append(f'release:"{album_name}"')
        if artist_name:
            query_parts.append(f'artist:"{artist_name}"')

        query = " AND ".join(query_parts)
        params["query"] = query

        try:
            response = requests.get(
                self.base_url,
                params=params,
                headers=self.headers,
                timeout=10
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error fetching data: {e}")
            return None

    def get_release_details(self, release_id):
        """
        Get detailed information about a specific release.

        Args:
            release_id: The MusicBrainz release ID

        Returns:
            Detailed release information
        """
        url = f"{self.base_url}/{release_id}"
        params = {
            "fmt": "json",
            "inc": "artists+recordings+media+labels+release-groups"
        }

        try:
            response = requests.get(
                url,
                params=params,
                headers=self.headers,
                timeout=10
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error fetching release details: {e}")
            return None

    def display_album_info(self, release_data):
        """
        Pretty-print album information.

        Args:
            release_data: Release data from MusicBrainz API
        """
        if not release_data:
            print("No data to display.")
            return

        print("\n" + "=" * 60)
        print("ALBUM INFORMATION")
        print("=" * 60)

        # Basic info
        title = release_data.get('title', 'Unknown')
        print(f"\n📀 Title: {title}")

        # Release date
        date = release_data.get('date', 'Unknown')
        print(f"📅 Release Date: {date}")

        # Status
        status = release_data.get('status', 'Unknown')
        print(f"🏷️  Status: {status}")

        # Artist(s)
        artists = release_data.get('artist-credit', [])
        if artists:
            artist_names = [a.get('artist', {}).get('name', 'Unknown') for a in artists]
            print(f"🎤 Artist(s): {', '.join(artist_names)}")

        # Label
        labels = release_data.get('label-info-list', [])
        if labels:
            label_names = [l.get('label', {}).get('name', 'Unknown') for l in labels]
            print(f"🏢 Label(s): {', '.join(label_names)}")

        # Tracks
        media_list = release_data.get('media', [])
        if media_list:
            total_tracks = sum(m.get('track-count', 0) for m in media_list)
            print(f"🎵 Total Tracks: {total_tracks}")

            # Show first few tracks as example
            track_num = 1
            for disc in media_list[:2]:  # Limit to first 2 discs
                tracks = disc.get('track-list', [])
                for track in tracks[:5]:  # Show first 5 tracks per disc
                    track_title = track.get('recording', {}).get('title', 'Unknown')
                    print(f"   {track_num}. {track_title}")
                    track_num += 1
            if total_tracks > 5:
                print(f"   ... and {total_tracks - 5} more tracks")

        # MusicBrainz ID
        mbid = release_data.get('id', 'N/A')
        print(f"\n🔗 MusicBrainz ID: {mbid}")
        print(f"🌐 URL: https://musicbrainz.org/release/{mbid}")
        print("=" * 60 + "\n")

    def search_and_display(self, album_name, artist_name=None):
        """
        Convenience method to search and display results.

        Args:
            album_name: Album name to search
            artist_name: Optional artist name
        """
        print(f"\n🔍 Searching for '{album_name}'...")
        if artist_name:
            print(f"   Artist filter: {artist_name}")

        results = self.search_album(album_name, artist_name)

        if not results or 'releases' not in results or not results['releases']:
            print("❌ No results found.")
            return

        releases = results['releases']
        print(f"\n✅ Found {len(releases)} result(s):\n")

        # Display search results
        for i, release in enumerate(releases, 1):
            title = release.get('title', 'Unknown')
            date = release.get('date', 'Unknown')
            artists = release.get('artist-credit', [])
            artist_name = artists[0]['artist']['name'] if artists else 'Unknown'
            release_id = release.get('id', '')

            print(f"{i}. {title} ({date}) - {artist_name}")
            print(f"   ID: {release_id}")

        # Allow user to select one for details
        try:
            selection = input("\nEnter number to see full details (or 0 to skip): ").strip()
            if selection.isdigit() and 0 < int(selection) <= len(releases):
                selected_id = releases[int(selection) - 1]['id']
                print("\n⏳ Fetching detailed information...")
                details = self.get_release_details(selected_id)
                self.display_album_info(details)
            else:
                print("Skipping detailed view.")
        except KeyboardInterrupt:
            print("\n\nCancelled by user.")
        except Exception as e:
            print(f"Error during selection: {e}")


def main():
    """Main entry point for the script."""
    print("=" * 60)
    print("MusicBrainz Album Info Lookup")
    print("=" * 60)

    # Create client
    client = MusicBrainzClient(user_agent="PythonMusicBrainzDemo/1.0")

    while True:
        print("\nOptions:")
        print("1. Search for an album")
        print("2. Exit")

        choice = input("\nSelect option: ").strip()

        if choice == "2":
            print("Goodbye!")
            break
        elif choice == "1":
            album = input("Enter album name: ").strip()
            if not album:
                print("Album name cannot be empty.")
                continue

            artist = input("Enter artist name (optional, press Enter to skip): ").strip()
            if not artist:
                artist = None

            client.search_and_display(album, artist)
        else:
            print("Invalid option. Try again.")


if __name__ == "__main__":
    # Install requirements first: pip install requests
    main()