(ns purge
  (:require
   [backpressure :as bp]
   [oauth]
   [clj-http.client :as client]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]))

(defn load-env
  "Return env map loaded from env.edn file"
  [env-edn]
  (if (.exists (io/as-file env-edn))
    (edn/read (java.io.PushbackReader. (io/reader env-edn)))))

(defn handle-response [id response]
  (let [status (:status response)
        reason-phrase (:reason-phrase response)
        status-str (format "%d %s" status reason-phrase)]
    (log/debugf "http response for id %s: '%s'" id status-str)
    (case status
      200 id
      404 (do (log/warnf "%s not found, ignoring" id) id)
      429 (Exception. "Exceeded rate.")
      401 (Exception. (format "Unauthorized: %s" status-str))
      (Exception. (format "Unexpected response status: %s" status-str)))))

(defn get-tweet
  "Get a tweet from twitter"
  {:period (* 60 15) :chunk 900}
  [{:keys [api-key access-token] :as keys-and-tokens} id]
  (log/debugf "requesting %s..." id)
  (let [verb "GET"
        url "https://api.twitter.com/1.1/statuses/show.json"
        nonce (oauth/nonce)
        ts (oauth/timestamp)
        params ["id" id
                "include_entities" "false"
                "oauth_consumer_key" api-key
                "oauth_nonce" nonce 
                "oauth_signature_method" "HMAC-SHA1"
                "oauth_timestamp" ts
                "oauth_token" access-token
                "oauth_version" "1.0"
                "trim_user" "true"]
        request {:accept :json
                 :query-params (select-keys
                                (apply assoc {} params)
                                ["id" "include_entities" "trim_user"])
                 :headers
                 {
                  :Authorization (oauth/header keys-and-tokens nonce ts 
                                               (oauth/signature keys-and-tokens verb url params))
                  }
                 :throw-exceptions false}
        response (client/get url request)]
    (handle-response id response)))

(defn delete!
  "Delete a tweet from twitter"
  {:period (* 60 15) :chunk 300}
  [{:keys [api-key access-token] :as keys-and-tokens} id]
  (log/debugf "deleting %s..." id)
  (let [verb "POST"
        url "https://api.twitter.com/1.1/statuses/destroy.json"
        nonce (oauth/nonce)
        ts (oauth/timestamp)
        params ["id" id
                "oauth_consumer_key" api-key
                "oauth_nonce" nonce
                "oauth_signature_method" "HMAC-SHA1"
                "oauth_timestamp" ts
                "oauth_token" access-token
                "oauth_version" "1.0"
                "trim_user" "true"]
        request {:accept :json
                 :content-type :json
                 :query-params {"id" id "trim_user" "true"}
                 :headers
                 {
                  :Authorization (oauth/header keys-and-tokens nonce ts
                                               (oauth/signature keys-and-tokens verb url params))
                  }
                 :throw-exceptions false}
        response (client/post url request)]
    (handle-response id response)))

(defn unfavorite!
  "Unfavorite (unlike) a favorited|liked tweet from twitter"
  {:period (* 60 15) :chunk 300}
  [{:keys [api-key access-token] :as keys-and-tokens} id]
  (log/debugf "unfavoriting %s..." id)
  (let [verb "POST"
        url "https://api.twitter.com/1.1/favorites/destroy.json"
        nonce (oauth/nonce)
        ts (oauth/timestamp)
        params ["id" id
                "oauth_consumer_key" api-key
                "oauth_nonce" nonce
                "oauth_signature_method" "HMAC-SHA1"
                "oauth_timestamp" ts
                "oauth_token" access-token
                "oauth_version" "1.0"]
        request {:accept :json
                 :content-type :json
                 :query-params {"id" id}
                 :headers
                 {
                  :Authorization (oauth/header keys-and-tokens nonce ts
                                               (oauth/signature keys-and-tokens verb url params))
                  }
                 :throw-exceptions false}
        response (client/post url request)]
    (handle-response id response)))

(defn for-all-tweets*
  "Invoke a function `f` on each tweet id, using configured backpressure and logging. `f` is a
  function of two args: `env` map and single String tweet id."
  [f f-meta {:keys [tweets-file success-file retry-file] :as env}]
  (let [keys [:period :chunk]
        {:keys [chunk period]} (merge (select-keys f-meta keys) (select-keys env keys))
        env (merge (bp/backpressure 1 period chunk) env)]
    (with-open [done-w (io/writer success-file)
                retry-w (io/writer retry-file)
                to-purge (io/reader tweets-file)]
      (try
        (let [tweets (line-seq to-purge)
              pr (bp/with-backpressure env
                   (partial bp/id-try-catch-logging done-w retry-w (partial f env))
                   tweets)]
          (log/info "...awaiting completion...")
          (deref pr) ;; gotta block or else writer(s) get closed too soon
          (log/info "...done!"))
        (finally
          (bp/close env))))))

(defmacro for-all-tweets
  "Returns a function of one arg, an environment map, which, when invoked, invokes function 'f' on
  every tweet id specified by env, using configured backpressure and logging."
  [f]
  `(partial for-all-tweets* ~f (meta (var ~f))))

;; main functions

(def get-all-tweets (for-all-tweets get-tweet))

(def delete-all-tweets! (for-all-tweets delete!))

(def unfavorite-all-tweets! (for-all-tweets unfavorite!))

(defn echo
  "For test purposes only."
  {:period 60 :chunk 12}
  [keys-and-tokens id]
  (println ">>>>" id))

;; For test purposes only.
(def echo-tweet-ids (for-all-tweets echo))


(comment

  (merge (bp/backpressure 1 15 30) (load-env "env.edn"))

  (echo-tweet-ids (assoc (load-env "env.edn")
                         :tweets-file "likez.txt"
                         :success-file "likez.log"
                         :retry-file "likezretry.log"
                         :chunk 4
                         :period 8))

  ;; zero
  (delete-all-tweets! (load-env "env.edn"))

  ;; two
  (let [id "1194396125861699590"
        ;;id "115814863581876225"
        ]
    (delete! (load-env "env.edn") id))

  ;; four
  (let [id "115814863581876225"]
    (get-tweet (load-env "env.edn") id))


  (defn tweet-ids-to-delete []
    (let [path "/Users/scottbale/personal/twitter/purge.txt"]
      (with-open [rdr (clojure.java.io/reader path)]
        (into [] (line-seq rdr)))))

  (defn tweet-ids-purged []
    (let [path "/Users/scottbale/personal/twitter/purged.txt"]
      (with-open [rdr (clojure.java.io/reader path)]
        (into #{} (line-seq rdr)))))

  (defn is-id [^String line]
    (and (seq line) (not (.startsWith line "#"))))

  (defn tweet-ids-to-keep []
    (let [path "/Users/scottbale/personal/twitter/keep.txt"]
      (with-open [rdr (clojure.java.io/reader path)]
        (into [] (filter is-id (line-seq rdr))))))



  (count (tweet-ids-to-delete))
  (count (tweet-ids-to-keep))

  (let [ids (tweet-ids-to-delete)
        ids-to-keep (tweet-ids-to-keep) 
        ids-purged (tweet-ids-purged)
        ids-to-ignore (into ids-purged ids-to-keep)
        ]

    )


  )
