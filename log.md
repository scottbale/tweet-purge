## twitter API

https://developer.twitter.com/

* destroy tweet https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/post-statuses-destroy-id

        https://api.twitter.com/1.1/statuses/destroy/:id.json
    
    e.g.
    
        POST https://api.twitter.com/1.1/statuses/destroy/1234567890.json
        
  * params
  
        id = 
        trim_user = true

links
* https://clojuredocs.org/clojure.core/load-file
* https://github.com/dakrone/clj-http
* https://developer.twitter.com/en/docs/basics/authentication/oauth-1-0a/creating-a-signature
* https://developer.twitter.com/en/docs/basics/rate-limiting#:~:text=Rate%20limiting%20of%20the%20standard,per%20window%20per%20access%20token.
* https://developer.twitter.com/en/apps/18264608
* https://developer.twitter.com/en/docs/api-reference-index
* https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/get-statuses-show-id
* https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/post-statuses-destroy-id
* https://commons.apache.org/proper/commons-codec/apidocs/index.html?org/apache/commons/codec/digest/HmacUtils.html
* http://commons.apache.org/proper/commons-codec/


## 7/3/20

* Created `project.clj`, adopted standard project layout
* Organized|synchronized versions of `cider`, `tools.nrepl` between `project.clj` and
  `~/profiles.clj`
* upgraded to lein 2.9.3

Am able to connect to nrepl

    $ lein repl :headless :port 2112
    
and in emacs

    M-x cider-connect

Why does it say "retrieving `nrepl 0.6.0`"?

    $ lein repl :headless :port 2112
    Retrieving nrepl/nrepl/0.6.0/nrepl-0.6.0.pom from clojars
    Retrieving clojure-complete/clojure-complete/0.2.5/clojure-complete-0.2.5.pom from clojars
    Retrieving clojure-complete/clojure-complete/0.2.5/clojure-complete-0.2.5.jar from clojars
    Retrieving nrepl/nrepl/0.6.0/nrepl-0.6.0.jar from clojars
    nREPL server started on port 2112 on host 127.0.0.1 - nrepl://127.0.0.1:2112

weird...earlier attempt (before lein upgrade) mentions `nrepl 0.7.0`

    $ lein repl :headless :port 2112
    Retrieving cider/cider-nrepl/0.25.0/cider-nrepl-0.25.0.pom from clojars
    Retrieving nrepl/nrepl/0.7.0/nrepl-0.7.0.pom from clojars
    Retrieving nrepl/nrepl/0.7.0/nrepl-0.7.0.jar from clojars
    Retrieving cider/cider-nrepl/0.25.0/cider-nrepl-0.25.0.jar from clojars
    Warning: cider-nrepl requires Leiningen 2.8.3 or greater.
    Warning: cider-nrepl will not be included in your project.
    Warning: cider-nrepl requires Leiningen 2.8.3 or greater.
    Warning: cider-nrepl will not be included in your project.
    Retrieving org/clojure/clojure/1.10.1/clojure-1.10.1.pom from centra
    Retrieving org/clojure/clojure/1.10.1/clojure-1.10.1.jar from centrall
    ... omitted ...
    Retrieving org/clojure/tools.nrepl/0.2.13/tools.nrepl-0.2.13.pom from central
    Retrieving org/clojure/tools.nrepl/0.2.13/tools.nrepl-0.2.13.jar from central
    Retrieving clj-http/clj-http/3.10.1/clj-http-3.10.1.jar from clojars

https://github.com/nrepl/nrepl and history https://nrepl.org/nrepl/about/history.html
* `repl-y` is that newer, alternative to `nrepl.nrepl` that Scott H uses: `reply.reply`, see
  `~/.gradle/gradle.properties`
* repl-y https://github.com/trptcolin/reply
  * part of leiningen as of 2.x
* according to history, `nrepl.nrepl` became `tools.nrepl`, then diverged
* not sure why it's being dowloaded as a dependency?

cider-nrepl reminders
* `M-x cider-repl-set-ns` or `C-c M-n n`
* `C-c C-k` read|eval current buffer `cider-load-buffer`
* `C-x C-e` eval form at cursor
* `M-.` jump to symbol at cursor
* `M.,` jump back out

created `bilgewater` twitter app
* creates a Consumer API key | secret key pair
* todo create access token | secret token pair
* Stored in 1Password

## 7/9/20

links
* https://clojuredocs.org/clojure.core/load-file
* https://github.com/dakrone/clj-http
* https://developer.twitter.com/en/docs/basics/authentication/oauth-1-0a/creating-a-signature
* https://developer.twitter.com/en/docs/basics/rate-limiting#:~:text=Rate%20limiting%20of%20the%20standard,per%20window%20per%20access%20token.
* https://developer.twitter.com/en/apps/18264608
* https://developer.twitter.com/en/docs/api-reference-index
* https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/get-statuses-show-id
* https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/post-statuses-destroy-id
* https://commons.apache.org/proper/commons-codec/apidocs/index.html?org/apache/commons/codec/digest/HmacUtils.html
* http://commons.apache.org/proper/commons-codec/

## 7/15/20

Wasn't putting oauth Authorization header in headers map. Correcting that, just got first successful response.

## 7/18/20

created private git repo

## 7/19/20

got periodic TimerTask thing working in `backpressure` namespace

## 7/25/20

* Replaced Timer/TimerTask with ScheduledExecutorService
* Added core.async to backpressure
* TODO: `with-backpressure` fn; think about worker count mapping to number of go blocks for each
  chunk

## 7/26/20

* twiddling with asynchrony, completion, logging - WIP
* `backpressure`, `backpressure-env`, `with-backpressure`
* stoopidly had with-backpressure blocking on the completion of chunking - moved that into a future
* completion triggered by putting `:done` into the chan.. but what about errors and retries?
  * use `alts!` with a "done" chan and a queue chan, giving priority to the queue, for retrying

## 7/27/20

* just enqueue single id's, not chunk collection of ids (makes retrying simpler)
* retry with try-catch, simulate exceptions

## 7/29/20

* Dry run tying most everything together but only doing "GET tweet id" via the API.
* Also tried a `delete!` but with an already deleted tweet, got a `401` response
* Still TODO: main method somehow accepting tokens and secrets, and possibly input filename

## 8/4/20

* Put oauth functions in their own namespace
* Make `backpressure` map a param to `do-for-all-tweets`
* Put python scripts in `bin` dir
* Did todo with `alts!`

## 8/5/20

* bug fixes in `purge` around properly formatting url and request for delete
* bug fixes in `backpressure` around `alts!`
* trying to kick off the real delete all... I fear rate limit is 300 per 3 hours, which will take my
  program over 24 hours :(
* bug fix in `backpressure`: do the first chunk immediately

## 8/6/20

* Retry messed up, can't put id back in the `queue` because it's likely already at rate limit.
  Punting, just writing retry id's to separate log file. Simplifies some things.
* moving filename strings to `scratch/env`

## 11/10/20

Ran a test of deleting about 375 tweets. First chunk of exactly 300 deleted and then as expected the
process waited for the 15 minute backpressure window. But then when it resumed, it only deleted one
more tweet and then stopped.

## 11/12/20

Found the bug: `backpressure/put-in` was doing its thing inside a `go` block, which means it was
async and was racing with `do-per-chunk` promise deref and adding `:done` poison pill. Fixed it by
removing the `go` block and using the synchronous `>!!`. `put-in` passed as `f` to `do-per-chunk` is
intended to be synchronous; `do-per-chunk` queues up the first chunk synchronously and then
schedules each subsequent chunk enqueuing on the executor. Added some docstrings as I have
everything loaded back into my brain for the time being.

https://github.com/clojure/tools.cli
https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md#uberjar

## 11/13/20

Moved source files up under `src`, where Leiningen prefers them to be located by default. Now
everything compiles for `uberjar` and I no longer have to compile each namespace manually in the
repl.

## 11/14/20

Simplified `backpressure` API, collapsed two functions into one (did away with `backpressure-env`).
Also, in `purge`, merge backpressure map into env map, allowing backpressure params to be overriden
from env.edn file if so desired (helps me with tinkering when running app from cli).

Improved logging of caught exceptions. Still unsure whether I should print stack trace each time.
Yesterday's test demonstrated that exceptions tend to come in bulk, and that quickly generates a lot
of logging.

Commented out logging to console|stdout (`log4j2.properties`)

Moved `load-env` function to `purge` ns so I could use it there when testing in repl, apart from
`cli`.

Added lots of docstrings.

It occurs to me: could I do away with core.async in `backpressure` and just invoke function from
within each task scheduled on scheduled Executor? Does using the channel buy me anything? Ease of
testing?

`purge` ns wasn't requiring `oauth` ns, which it needed to do even though I wasn't aliasing the ns.

Tweaking of CLI, including getting `System/exit` working correctly; both printing and logging a
start message in all cases; redirecting stderr to `/dev/null` because of noisy invalid cookie warnings.

## 11/20/20

TIL (was reminded) that metadata is put on a function _var_ . The `^` reader macro adds metadata to
the function symbol `'foo` at read time, and the `defn` macro copies metadata to the function var
`#'foo` at compile time. It's unclear whether the function itself can have metadata anymore.

Annotated each individual tweet function with metadata describing its default backpressure, e.g.

    (defn ^{:period (* 60 15) :chunk 300} delete! ...)

Wrote a macro to make more concise main functions, e.g.

    (def delete-all-tweets! (for-all-tweets delete!))

Can now override `:period` and `:chunk` in environment

## 11/22/20

TIL in emacs, open a log file in a buffer then `M-x auto-revert-mode` set to true, to effectively
tail the log. https://stackoverflow.com/a/19589885/2495576

## 11/23/20

Tweaked `with-backpressure` to `submit` initial chunk using Executor rather than executing it
synchronously. Now every chunk takes place in an executor thread, and the Promise is returned
immediately - `with-backpressure` is truly completely async now. Also switched to `dorun` rather
than `doseq`.

Added a `close` function to `backpressure` ns so that ScheduledExecutor can be shut down. Too bad
`with-open` macro only works on `.close` method of pojos. I did some googling about `with-open` on a
protocol with a `close` function, came across this old ticket
https://clojure.atlassian.net/browse/CLJ-2

Next: Revisit `id-try-catch-logging` and see if, by rearranging arg order, it can be invoked using
`partial` rather than having the function itself return another function.

Implemented `unfavorite!`

## 11/24/20

Ran `unfavorite` on my entire file of 7428 liked tweets. Glad for my log file: I can see I had 2706
http 200s and 5281 http 404s. My twitter profile still shows about 5.7k likes, down from over 8k.
Puzzling. I don't think I've liked that many tweets since I downloaded my data. Could many of these
404s be retried? If so, Would be nice to have logged them to a separate file (like I do the retry
file, which is empty).

Put `try-catch` around main method, and log any caught Throwable (because at the moment the bash
start script is redireting stderr to `/dev/null`). And/or maybe redirect stderr to some file. Or
just solve the mystery of these invalid cookie header warnings. Sample:

    Nov 23, 2020 9:24:05 PM org.apache.http.client.protocol.ResponseProcessCookies processCookies
    WARNING: Invalid cookie header: "set-cookie: personalization_id="v1_REDACTED=="; Max-Age=63072000; Expires=Thu, 24 Nov 2022 03:24:05 GMT; Path=/; Domain=.twitter.com; Secure; SameSite=None". Invalid 'expires' attribute: Thu, 24 Nov 2022 03:24:05 GMT
    Nov 23, 2020 9:24:05 PM org.apache.http.client.protocol.ResponseProcessCookies processCookies
    WARNING: Invalid cookie header: "set-cookie: guest_id=v1%REDACTED; Max-Age=63072000; Expires=Thu, 24 Nov 2022 03:24:05 GMT; Path=/; Domain=.twitter.com; Secure; SameSite=None". Invalid 'expires' attribute: Thu, 24 Nov 2022 03:24:05 GMT

Pairs of warnings like these two repeatedly logged. Each time, looks like different values for
`personalization_id` and `guest_id`. `personalization_id` looks like maybe base 64 encoding.

I just noticed I'm inconsistent on where I place the map of metadata on `defn`s in the `purge`
namespace.

## 11/26/20

checklist to make repo public:
* **done** clean up `README.md`, document steps for someone else to use this
* tar up jar and script, upload to GH as a release?
* **done** `try-catch` main method
* **done** consistent metadata
* **done** `id-try-catch-logging` todo
* remove most commented code snippets, possibly convert to unit tests

re-arranged subdirectories of files; bash scripts WIP

## 11/27/20

Upgraded `clj-http` lib; set `:cookie-policy :none` on all requests to completely ignore cookies, no
longer seeing the "invalid cookie header" warnings.

Now that those warnings are gone, it simplifies the startup script and logging: I tore out the
redirect of stderr to a startup log file.

Created sample env.edn file. Tweaked dist script.

## 12/7/20

Better README, tagged v. 0.0.1, made repo public

## 12/8/20

corrected backpressure docstring; inverted order of `try-finally` and `with-open`; removed unused
dep; removed some commented code

## 1/11/21

twitter list get members
https://developer.twitter.com/en/docs/twitter-api/v1/accounts-and-users/create-manage-lists/api-reference/get-lists-members

## Appendix

sample delete response

    {:cached nil,
     :request-time 634,
     :repeatable? false,
     :protocol-version {:name "HTTP", :major 1, :minor 1},
     :streaming? true,
     :http-client
     #object[org.apache.http.impl.client.InternalHttpClient 0x50d21cdd "org.apache.http.impl.client.InternalHttpClient@50d21cdd"],
     :chunked? false,
     :cookies
     {"personalization_id"
      {:discard false,
       :domain "twitter.com",
       :expires #inst "2022-08-05T15:27:03.305-00:00",
       :path "/",
       :secure true,
       :value "\"redacted\"",
       :version 0},
      "lang" {:discard true, :path "/", :secure false, :value "en", :version 0},
      "guest_id"
      {:discard false,
       :domain "twitter.com",
       :expires #inst "2022-08-05T15:27:03.305-00:00",
       :path "/",
       :secure true,
       :value "redacted",
       :version 0}},
     :reason-phrase "OK",
     :headers
     {"server" "tsa_b",
      "x-twitter-response-tags" "BouncerCompliant",
      "content-type" "application/json;charset=utf-8",
      "x-content-type-options" "nosniff",
      "content-length" "587",
      "x-connection-hash" "redacted",
      "x-frame-options" "SAMEORIGIN",
      "strict-transport-security" "max-age=631138519",
      "connection" "close",
      "x-response-time" "55",
      "pragma" "no-cache",
      "status" "200 OK",
      "expires" "Tue, 31 Mar 1981 05:00:00 GMT",
      "date" "Wed, 05 Aug 2020 15:27:02 GMT",
      "content-disposition" "attachment; filename=json.json",
      "last-modified" "Wed, 05 Aug 2020 15:27:02 GMT",
      "x-access-level" "read-write",
      "x-xss-protection" "0",
      "cache-control" "no-cache, no-store, must-revalidate, pre-check=0, post-check=0",
      "x-transaction" "007f7e670034f3d2"},
     :orig-content-encoding "gzip",
     :status 200,
     :length 587,
     :body
     "{\"created_at\":\"Tue Nov 12 23:26:41 +0000 2019\",\"id\":1194396125861699590,\"id_str\":\"1194396125861699590\",\"text\":\"RT @rsa: Sometimes I get close to rethinking every decision I ever made in my life \\u2013 and then I realize that I just need coffee.\",\"truncated\":false,\"entities\":{\"hashtags\":[],\"symbols\":[],\"user_mentions\":[{\"screen_name\":\"rsa\",\"name\":\"rsa\",\"id\":6735,\"id_str\":\"6735\",\"indices\":[3,7]}],\"urls\":[]},\"source\":\"\\u003ca href=\\\"https:\\/\\/mobile.twitter.com\\\" rel=\\\"nofollow\\\"\\u003eTwitter Web App\\u003c\\/a\\u003e\",\"in_reply_to_status_id\":null,\"in_reply_to_status_id_str\":null,\"in_reply_to_user_id\":null,\"in_reply_to_user_id_str\":null,\"in_reply_to_screen_name\":null,\"user\":{\"id\":15001217,\"id_str\":\"15001217\"},\"geo\":null,\"coordinates\":null,\"place\":null,\"contributors\":null,\"retweeted_status\":{\"created_at\":\"Sat Sep 03 20:19:55 +0000 2011\",\"id\":110084629926658048,\"id_str\":\"110084629926658048\",\"text\":\"Sometimes I get close to rethinking every decision I ever made in my life \\u2013 and then I realize that I just need coffee.\",\"truncated\":false,\"entities\":{\"hashtags\":[],\"symbols\":[],\"user_mentions\":[],\"urls\":[]},\"source\":\"\\u003ca href=\\\"http:\\/\\/twitter.com\\\" rel=\\\"nofollow\\\"\\u003eTwitter Web Client\\u003c\\/a\\u003e\",\"in_reply_to_status_id\":null,\"in_reply_to_status_id_str\":null,\"in_reply_to_user_id\":null,\"in_reply_to_user_id_str\":null,\"in_reply_to_screen_name\":null,\"user\":{\"id\":6735,\"id_str\":\"6735\"},\"geo\":null,\"coordinates\":null,\"place\":null,\"contributors\":null,\"is_quote_status\":false,\"retweet_count\":14,\"favorite_count\":27,\"favorited\":false,\"retweeted\":true,\"lang\":\"en\"},\"is_quote_status\":false,\"retweet_count\":14,\"favorite_count\":0,\"favorited\":false,\"retweeted\":true,\"lang\":\"en\"}",
     :trace-redirects []}
