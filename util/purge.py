import json
import sys

if not len(sys.argv) == 4:
    print "usage: python purge.py filename_of_tweets_to_keep filename_of_all_tweets target_filename"
    sys.exit(1)

keep_filename = sys.argv[1]
tweets_filename = sys.argv[2]
purge_filename = sys.argv[3]

print "loading up tweets to keep from {}...".format(keep_filename)

ids_to_keep = set()

with open(keep_filename) as keep_file:
    line = keep_file.readline()
    cnt = 1
    while line:
        if line and not line.startswith("#"):
            ids_to_keep.add(line.strip())
        line = keep_file.readline()
        cnt += 1

print ("# tweet ids to keep: {}".format(len(ids_to_keep)))

print "loading up all tweets from {}...".format(tweets_filename)

with open(tweets_filename) as tweets_file:
    tweets = json.load(tweets_file)

print "done parsing all tweets: length: {} ".format(len(tweets))

print "writing ids to purge to {}...".format(purge_filename)

with open(purge_filename, "w") as purge_file:
    for tweet in tweets:
        tweet_id = tweet["id_str"]
        if tweet_id not in ids_to_keep:
            purge_file.write(tweet_id + "\n")
