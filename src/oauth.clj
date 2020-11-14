(ns oauth
  (:import
   [java.net URLEncoder]
   [java.nio ByteBuffer]
   [java.util Base64]
   [org.apache.commons.codec.digest HmacUtils HmacAlgorithms]))

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

(defn signature [{:keys [api-secret token-secret]} verb url params]
  (let [base-sig (base-signature-str verb url params)
        signing-key (str (%-enc api-secret) "&" (%-enc token-secret))
        encoder (Base64/getEncoder)]
    (.encodeToString encoder (.hmac (HmacUtils. HmacAlgorithms/HMAC_SHA_1 signing-key) base-sig))))

(defn header 
  "Returns oauth header string, given necessary params"
  [{:keys [api-key access-token]} nonce ts signature]
  (let [template "OAuth oauth_consumer_key=\"%s\", oauth_nonce=\"%s\", oauth_signature=\"%s\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"%s\", oauth_token=\"%s\", oauth_version=\"1.0\""]
    (format template 
            (%-enc api-key)
            (%-enc nonce) 
            (%-enc signature) 
            (%-enc ts)
            (%-enc access-token))))

