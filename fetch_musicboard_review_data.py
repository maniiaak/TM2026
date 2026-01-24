import requests
import json
from bs4 import BeautifulSoup
import re

URL = 'https://musicboard.app/jeunemaniak23/review/album/recherche-destruction/jolagreen23/'

def fetchmusicboardreview(review_url):
    response = requests.get(review_url)
    soup = BeautifulSoup(response.text, features="html.parser")

    listings = soup.select('.Initial-Schema')
    html = str(listings[0])

    match = re.search(r'<script.*?type="application/ld\+json".*?>(.*?)</script>', html, re.S)
    json_text = match.group(1)

    data = json.loads(json_text)

    review_data = {
        "meta": {
            "date_published": data["datePublished"],
            "author": data["author"]["name"],
        },
        "music": {
            "artist": data["itemReviewed"]["byArtist"]["name"],
            "album": data["itemReviewed"]["name"],
        },
        "review": {
            "body": data["reviewBody"],
            "rating": data["reviewRating"]["ratingValue"],
        }
    }

    return review_data


review = fetchmusicboardreview(URL)

print(f"Review for {review['music']['album']} by {review['music']['artist']}:")
print(f"Review by: {review['meta']['author']}")
print(f"Rating: {review['review']['rating']}")
print(f"Review body: {review['review']['body']}")
print(f"Published on: {review['meta']['date_published']}")
