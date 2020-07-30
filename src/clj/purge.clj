(ns purge
  (:require
   [backpressure :as bp]
   [clj-http.client :as client]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log])
  (:import
   [java.net URLEncoder]
   [java.nio ByteBuffer]
   [java.util Base64]
   [org.apache.commons.codec.digest HmacUtils HmacAlgorithms]))

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

;; API auth header

(defn nonce 
  "Pseudo-random string made by
  1. 32 bytes of random data (8 invocations of rand, each returning a float)
  2. Add each to a ByteBuffer of size 32
  3. base64-encode the resulting byte array
  4. remove any +, /, = characters"
  [] 
  (let [bb (ByteBuffer/allocate 32)
        encoder (Base64/getEncoder)]
    (dotimes [_ 8] (.putFloat bb (rand)))
    (let [encoded-str (.encodeToString encoder (.array bb))]
      (apply str (remove #(#{\+ \/ \=} %) encoded-str)))))

(defn timestamp []
  (str (long (/ (System/currentTimeMillis) 1000))))

(defn %-enc [x] (URLEncoder/encode x "UTF-8"))

(defn param-str [params]
  (loop [[k v & ps :as all] params
         result []]
    (if (seq all)
      (recur ps (conj result "&" (%-enc k) "=" (%-enc v)))
      (apply str (rest result)))))

(defn base-signature-str [verb url params]
  (if (seq params)
    (str verb "&" (%-enc url) "&" (%-enc (param-str params)))
    (str verb "&" (%-enc url))))

(defn oauth-signature [{:keys [api-secret token-secret]} verb url params]
  (let [base-sig (base-signature-str verb url params)
        signing-key (str (%-enc api-secret) "&" (%-enc token-secret))
        encoder (Base64/getEncoder)]
    (.encodeToString encoder (.hmac (HmacUtils. HmacAlgorithms/HMAC_SHA_1 signing-key) base-sig))))

(defn oauth-header 
  "Returns oauth header string, given necessary params"
  [{:keys [api-key access-token]} nonce ts signature]
  (let [template "OAuth oauth_consumer_key=\"%s\", oauth_nonce=\"%s\", oauth_signature=\"%s\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"%s\", oauth_token=\"%s\", oauth_version=\"1.0\""]
    (format template 
            (%-enc api-key)
            (%-enc nonce) 
            (%-enc signature) 
            (%-enc ts)
            (%-enc access-token))))

;; API

(defn handle-response [id response]
  (let [status (:status response)
        status-str (get-in response [:headers "status"])
        limit-remaining (get-in response [:headers "x-rate-limit-remaining"])
        limit-reset (get-in response [:headers "x-rate-limit-reset"])]
    (log/debugf "http response for id %s: '%s'. Limit remaining: %s" id status-str limit-remaining)
    (case status
      200 id
      404 (do (log/warnf "%s not found, ignoring" id) id)
      429 (Exception. "Exceeded rate.")
      :default (Exception. (format "Unexpected response status: %s" status-str)))))

(defn get-tweet
  "Get a tweet from twitter"
  [{:keys [api-key access-token] :as keys-and-tokens} id]
  (log/debugf "requesting %s..." id)
  (let [verb "GET"
        url "https://api.twitter.com/1.1/statuses/show.json"
        nonce (nonce)
        ts (timestamp)
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
                  :Authorization (oauth-header keys-and-tokens nonce ts 
                                               (oauth-signature keys-and-tokens verb url params))
                  }}
        response (client/get url (assoc request :throw-exceptions false))]
    (handle-response id response)))


(defn delete!
  "Delete a tweet from twitter"
  [id]
  (log/debugf "deleting %s..." id)
  #_(client/post 
   (format "https://api.twitter.com/1.1/statuses/destroy/%s.json?trim_user=true" id)
   {:form-params body
    :content-type :json
    :oauth-token @token
    :throw-exceptions false
    :as :json})
  
  
  )

(comment

  ;; one
  (with-open [done (io/writer "tweets.log" :append true)
              to-purge (io/reader "/Users/scottbale/personal/twitter/purge.txt")]
    (let [chunk 500
          period 10
          env (bp/backpressure-env 1)
          bp (bp/backpressure env period chunk)
          tweets (line-seq to-purge)
          pr (bp/with-backpressure bp (comp (partial bp/log-id done) bp/print-id) tweets)]
      (log/info ".....awaiting completion.....")
      (deref pr) ;; gotta block or else writer(s) get closed too soon
      (log/info "...done!")))

  ;; two
  (let [;;id "1194396125861699590"
        id "1126617623985135618"]
    (get-tweet scratch/keys-and-tokens id))

  ;; three
  (with-open [done (io/writer "tweets.log")
              to-purge (io/reader "/Users/scottbale/personal/twitter/purge.txt")]
    (let [chunk 10
          period 10
          env (bp/backpressure-env 1)
          bp (bp/backpressure env period chunk)
          tweets (take 1000 (line-seq to-purge))
          pr (bp/with-backpressure bp
               (comp (partial bp/log-id done) (partial get-tweet scratch/keys-and-tokens))
               tweets)]
      (log/info ".....awaiting completion.....")
      (deref pr) ;; gotta block or else writer(s) get closed too soon
      (log/info "...done!")))


  (let [keys-and-tokens {:api-key "TODO"
                         :api-secret "TODO"
                         :access-token "TODO"
                         :token-secret "TODO"}
        id "TODO"
        ;; params ["trim_user" "true"]
        verb "GET"
        url "https://api.twitter.com/1.1/statuses/show.json"
        nonce (nonce)
        ts (timestamp)
        params ["id" id 
                "oauth_consumer_key" (keys-and-tokens :api-key)
                "oauth_nonce" nonce 
                "oauth_signature_method" "HMAC-SHA1"
                "oauth_timestamp" ts
                "oauth_token" (keys-and-tokens :access-token)
                "oauth_version" "1.0"]
        ] 

    ;; (param-str params)

    ;; (base-signature-str verb url params)

    ;; (oauth-signature keys-and-tokens verb url params)

    #_(oauth-header 
     keys-and-tokens
     nonce 
     ts 
     (oauth-signature keys-and-tokens verb url params))

    (get-tweet keys-and-tokens id)

    )

  (tweet-ids-to-delete)
  (tweet-ids-to-keep)

  (let [ids (tweet-ids-to-delete)
        ids-to-keep (tweet-ids-to-keep) 
        ids-purged (tweet-ids-purged)
        ids-to-ignore (into ids-purged ids-to-keep)
        ]
    
    )

  (let [encoder (Base64/getEncoder)]
    (.encodeToString encoder (.getBytes "foo")))

  ;; new HmacUtils(HmacAlgorithms.HMAC_SHA_1, key).hmacHex(string_to_sign)

  (let [key "foo"
        val "bar"
        encoder (Base64/getEncoder)] 
    (.encodeToString encoder (.hmac (HmacUtils. HmacAlgorithms/HMAC_SHA_1 key)  val)) 
    )

  (%-enc "foo/bar")

  ;; return ByteBuffer.allocate(4).putFloat(value).array();
  ;; System/currentTimeMillis

  )
