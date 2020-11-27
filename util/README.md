These are just a couple python scripts I hacked together to produce the `purge.txt` file, the input to the Clojure program, which is just a newline-delimited list of the ids of my tweets that I want to delete.

I started by downloading my personal data from Twitter. That zip file contains a file named `tweet.js`. I hand-edited that to make it a valid `.json` file (just had to delete the `window.YTD.tweet.part0 = [` at the beginning of the file).

I then ran `python browse.py` on that `tweet.json` file to produce `browse.json`, which was just a convenient format for me to read through and pick out the tweets I wanted to keep.

I hand-copied the ids of the tweets I wanted to keep into `keep.txt`.

Then I ran `python purge.py`, which subtracts the ids in `keep.txt` from the ones in `tweet.json` to produce a file, `purge.txt` of the ids of the tweets that I want deleted:

    python purge.py keep.txt tweet.json purge.txt
