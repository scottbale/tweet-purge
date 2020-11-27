import json
import sys

if not len(sys.argv) == 3:
    print "usage: python dislike.py filename_of_likes.json target_filename"
    sys.exit(1)

likes_filename = sys.argv[1]
target_filename = sys.argv[2]

print "saving list of like ids to un-favorite from {} to {}".format(likes_filename, target_filename)

with open(likes_filename) as data_file:
    data = json.load(data_file)

print "done parsing {}: length: {} ".format(likes_filename, len(data))

ids = set()
for like in data:
    l = like["like"]
    ids.add(l["tweetId"])

print "about to write results..."

with open(target_filename, "w") as out_file:
    for anid in ids:
        out_file.write(anid + "\n")
