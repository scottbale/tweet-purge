(ns purge
  (:require
   [backpressure :as bp]
   [clj-http.client :as client]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]))

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

;; main functions

(defn do-for-all-tweets [{:keys [tweets-file success-file retry-file] :as env} backpressure f]
  (with-open [done-w (io/writer success-file)
              retry-w (io/writer retry-file)
              to-purge (io/reader tweets-file)]
    (let [tweets (line-seq to-purge)
          pr (bp/with-backpressure backpressure
               (bp/id-try-catch-logging (partial f env) done-w retry-w)
               tweets)]
      (log/info ".....awaiting completion.....")
      (deref pr) ;; gotta block or else writer(s) get closed too soon
      (log/info "...done!"))))

(defn get-all-tweets [env]
  (let [chunk 900
        period (* 60 15) ;; 15 minutes rate limit
        env (bp/backpressure-env 1)
        bp (bp/backpressure env period chunk)] 
    (do-for-all-tweets env bp get-tweet)))

(defn delete-all-tweets! [env]
  (let [chunk 300
        period (* 60 15) ;; 15 minutes rate limit
        env (bp/backpressure-env 1)
        bp (bp/backpressure env period chunk)]
    (do-for-all-tweets env bp delete!)))

(comment

  ;; zero
  (delete-all-tweets! scratch/env)

  ;; two
  (let [id "1194396125861699590"
        ;;id "115814863581876225"
        ]
    (delete! scratch/env id))

  ;; four
  (let [id "115814863581876225"]
    (get-tweet scratch/env id))


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
