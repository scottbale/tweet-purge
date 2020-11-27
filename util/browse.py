import json

print "creating digest of all tweet.json into browse.json"

with open('tweet.json') as data_file:
    data = json.load(data_file)

print "done parsing tweets.js: length: %i " % len(data)

digests = []
for tweet in data:
    digest = {}
    digest["id"] = tweet["id_str"]
    digest["text"] = tweet["full_text"]
    digest["ts"] = tweet["created_at"]
    digest["liked"] = tweet["favorite_count"]
    digest["RT"] = tweet["retweet_count"]
    digests.append(digest)

print "about to write results..."

with open('browse.json', "w") as out_file:
    json.dump(digests, out_file, indent=0)
