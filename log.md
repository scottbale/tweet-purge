## twitter API

https://developer.twitter.com/

* destroy tweet https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/post-statuses-destroy-id

        https://api.twitter.com/1.1/statuses/destroy/:id.json
    
    e.g.
    
        POST https://api.twitter.com/1.1/statuses/destroy/1234567890.json
        
  * params
  
        id = 
        trim_user = true

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
* `M-x cider-repl-set-ns` or `C-c M-n`
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
