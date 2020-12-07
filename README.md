tweet purge
===========

I wrote a program to delete most of my old tweets and likes from my [Twitter account](https://twitter.com/scottbale).

Usage
-----

If you want to use this program yourself, you'll have to use your own Twitter credentials. (I have not set this app up to authenticate on someone else's behalf using my app credentials.) This means, if you haven't already, you'll have to apply for and create a [twitter developer account](https://developer.twitter.com/en/apply-for-access) and generate your own API key and secret pair and your own app access token and secret pair. These four values then need to be placed in the [env.edn](env.edn.sample) file.

You'll also have to create a newline-separated text file of tweet ids to be deleted or unfavorited. I did this by [downloading my twitter archive](https://help.twitter.com/en/managing-your-account/how-to-download-your-twitter-archive).

### Quick start

Extract release tarball.

``` shell
$ ./bin/tweet-purge --help
```

### Build from source

``` shell
$ lein uberjar
$ tools/dist.sh
```

Tarball will be in `build/tweet-purge.tar`.
