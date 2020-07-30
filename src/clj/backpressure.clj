(ns backpressure
  (:require
   [clojure.core.async :refer [chan go go-loop <! >! >!!]]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log])
  (:import
   [java.util.concurrent Executors TimeUnit ScheduledExecutorService]))

(defn schedule [f scheduled-executor-service seconds]
  (.schedule scheduled-executor-service f seconds TimeUnit/SECONDS))

(defn do-per-chunk
  "Execute function f for each chunk of collection `coll` after `period` seconds. Return a Promise
  which will be completed when all chunks are completed. Use provided ScheduledExecutorService. f
  should be a function of a single arg, which is a collection of at most length equal to chunk. The
  time is measured from the end of the function invocation."
  [{:keys [executor period chunk] :as env} f coll]
  (let [completion (promise)]
    (letfn [(do-next-chunk [xs chunk-nm]
              (log/debugf "starting %d chunk" chunk-nm)
              (let [some-xs (take chunk xs)
                    rest-xs (drop chunk xs)]
                (log/trace "next chunk %d: scheduling" chunk-nm)
                (schedule (fn []
                            (f some-xs)
                            (if (seq rest-xs)
                              (do-next-chunk rest-xs (inc chunk-nm))
                              (deliver completion true)))
                          executor period)))]
      (do-next-chunk coll 0)
      completion)))

(defn put-in [chan coll]
  (go
    (doseq [x coll]
      (>! chan x))))

(defn looping-invoke [chan f finished]
  (go-loop [x (<! chan)]
    (if (= x :done)
      (deliver finished true)
      (do
        (try
          (f x)
          (catch Exception e
            (log/warnf #_e "Caught exception, re-enqueuing %s" x)
            (>! chan x)))
        (recur (<! chan))))))

(defn backpressure-env [task-count]
  {:executor (Executors/newScheduledThreadPool (+ 2 task-count))})

(defn backpressure [env period chunk]
  (assoc env :period period :chunk chunk))

(defn with-backpressure
  "Asynchronously execute f on successive chunks of collection coll, returning a Promise.
  Environment will indicate chunk size and period, in seconds, in between each chunk."
  [{:keys [executor period chunk] :as env} f coll]
  (let [chan (chan chunk)
        finished (promise)
        chunking (do-per-chunk env (partial put-in chan) coll)]
    (looping-invoke chan f finished)
    (.submit executor
             (fn []
               (log/info "awaiting chunking...")
               (deref chunking)
               (log/info "chunking done, notifying chan...")
               (>!! chan :done)))
    finished))

;; sample function(s)

(defn maybe-throw [error-cnt]
  (when (and
         (< 0 @error-cnt)
         (= 0 (rand-int 5)))
    (swap! error-cnt dec)
    (throw (Exception. "blammo!"))))

(defn print-id [id]
  (log/debugf "print id %s..." id)
  (println ">>>>>>>>" id)
  id)

(defn maybe-print-id [error-cnt id]
  (log/debugf "Maybe print id %s..." id)
  (maybe-throw error-cnt)
  (print-id id))

(defn log-id [writer id]
  (binding [*out* writer]
    ;; (.write writer id)
    (println id)
    id))

(defn maybe-print-and-log-id [error-cnt writer id]
  (maybe-print-id error-cnt id)
  (log-id writer id))


(comment

  (with-open [w1 (io/writer "tweets.log" :append true)
              w2 (io/writer "likes.log" :append true)]
    (let [error-cnt (atom 3) ;; at most three errors
          env (backpressure-env 2)
          b1 (backpressure env 5 3)
          b2 (backpressure env 3 5)
          tweets (take 15 (map #(str "foo" %) (range)))
          likes (take 22 (map #(str "bar" %) (range)))
          p1 (with-backpressure b1 (comp (partial log-id w1) (partial maybe-print-id error-cnt))
               tweets)
          p2 (with-backpressure b2 (partial maybe-print-and-log-id error-cnt w2) likes)]
      (log/info ".....awaiting completion.....")
      (deref p1) ;; gotta block or else writer(s) get closed too soon
      (deref p2)
      (log/info "...done!")))


  ;; logging weirdness
  (log/info "log something standard")
  (future (log/info "log something async"))
  (go (log/info "log something go block"))
  ;; only this last one prints to the repl (all four print to cider buffer)
  (let [ses (Executors/newScheduledThreadPool 1)]
    (.submit ses ^Runnable (fn [] (log/info "log something using Executor"))))


  )
