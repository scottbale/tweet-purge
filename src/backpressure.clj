(ns backpressure
  "Twitter rate-limits some of its API endpoints, and the number of tweet ids as input may exceed the
  rate limit.

  Implements backpressure thusly: input tweet ids are chunked and asynchronously added as inputs to
  core.async channel 'queue'. A ScheduledExecutorService is used to schedule the enqueuing of each
  chunk, scheduled according to the backpressure chunk size and period of time. Meanwhile, the
  'queue' is consumed in a go-loop which pulls each tweet id from it and invokes a function on it."
  (:require
   [clojure.core.async :refer [chan go go-loop >! >!! <!]]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log])
  (:import
   [java.util.concurrent Executors TimeUnit ScheduledExecutorService]))

(defn schedule [f scheduled-executor-service seconds]
  (.schedule scheduled-executor-service f seconds TimeUnit/SECONDS))

(defn do-per-chunk
  "Execute function f for each chunk of collection `coll`. First chunk is executed immediately, each
  subsequent one after `period` seconds. Return a Promise which will be completed when all chunks
  are completed. Uses provided ScheduledExecutorService. f should be a function of a single arg,
  which is a collection of at most length equal to chunk. The time is measured from the end of the
  function invocation."
  [{:keys [executor period chunk]} f coll]
  (let [completion (promise)]
    (letfn [(do-next-chunk [xs chunk-nm]
              (log/debugf "starting %d chunk" chunk-nm)
              (let [some-xs (take chunk xs)
                    rest-xs (drop chunk xs)]
                (f some-xs)
                (if (seq rest-xs)
                  (do
                    (log/tracef "next chunk %d: scheduling" chunk-nm)
                    (schedule #(do-next-chunk rest-xs (inc chunk-nm)) executor period))
                  (deliver completion true))))]
      (do-next-chunk coll 0)
      completion)))

(defn put-in
  "Synchronously add each of `coll` items to channel"
  [chan coll]
  (log/debugf "about to add %d items to chan" (count coll))
  (doseq [x coll]
    (log/tracef "(>!! chan %s)" x)
    (>!! chan x)))

(defn looping-invoke
  "In a go loop, repeatedly invoke function `f`, passing as a single arg each item retrieved from the
  channel `queue`. `finished` is a Promise: when `:done` poison pill is retrieved from the chan,
  deliver `true` to the Promise."
  [queue f finished]
  (go-loop [x (<! queue)]
    (log/tracef "(<! chan: %s)" x)
    (if (= x :done)
      (deliver finished true)
      (do
        (f x)
        (recur (<! queue))))))

(defn backpressure
  "Creates a backpressure object to be used with `with-backpressure` function. `task-count` is the
  expected number of concurrent tasks requiring backpressure. `period` is the number of seconds
  pause in between chunks of work. `chunk` is the number of items in a chunk of work before pause.
  Implementation notes: returned object is a map. A scheduled thread pool Executor is created, which
  is used to schedule all future chunks after the first chunk. "
  [task-count period chunk]
  {:executor (Executors/newScheduledThreadPool (+ 2 task-count))
   :period period :chunk chunk})

(defn with-backpressure
  "Asynchronously execute f on successive chunks of collection coll, returning a Promise.
  Backpressure map will indicate chunk size and period, in seconds, in between each chunk."
  [{:keys [executor chunk] :as backpressure} f coll]
  (let [queue (chan chunk)
        finished (promise)
        chunking (do-per-chunk backpressure (partial put-in queue) coll)]
    ;; `finished` promise is fulfilled at completion of go-loop within `looping-invoke`
    (looping-invoke queue f finished)
    (.submit executor
             (fn []
               (log/info "awaiting chunking...")
               ;; `chunking` is fulfilled once last chunk is enqueued in chan...
               (deref chunking)
               (log/info "chunking done, notifying chan...")
               (log/trace "(>!! chan :done)")
               ;; ...at which point this task enqueues a final poison pill which is
               ;; consumed within `looping-invoke` go-loop, which is how it knows to
               ;; terminate
               (>!! queue :done)))
    finished))

;; sample function(s)

(defn maybe-throw [error-cnt]
  (when (and
         (< 0 @error-cnt)
         (= 0 (rand-int 5)))
    (swap! error-cnt dec)
    (throw (Exception. "blammo!"))))

(defn log-id [writer id]
  (binding [*out* writer]
    ;; (.write writer id)
    (println id)
    id))

(defn id-try-catch-logging [f success-writer retry-writer]
  (fn [id]
    (try
      (f id)
      (log-id success-writer id)
      (catch Exception e
        (log/warnf e "Caught %s, re-enqueuing %s: %s" (.getName (.getClass e)) id (.getMessage e))
        (log-id retry-writer id)))))

(defn print-id [id]
  (log/debugf "print id %s..." id)
  (println ">>>>>>>>" id)
  id)

(defn maybe-print-id [error-cnt id]
  (log/debugf "Maybe print id %s..." id)
  (maybe-throw error-cnt)
  (print-id id))

(defn maybe-print-and-log-id [error-cnt writer id]
  (maybe-print-id error-cnt id)
  (log-id writer id))


(comment

  ;; simple test, some exceptions deliberately thrown
  (with-open [done-w (io/writer "tweets.log")
              retry-w (io/writer "retry.log")]
    (let [error-cnt (atom 3) ;; at most three errors
          period 5
          chunk 3
          bp (backpressure 1 period chunk)
          tweets (take 9 (map #(str "foo" %) (range)))
          pr (with-backpressure bp
               (id-try-catch-logging (partial maybe-print-id error-cnt) done-w retry-w)
               tweets)]
      (log/info ".....awaiting completion.....")
      (deref pr) ;; gotta block or else writer(s) get closed too soon
      (log/info "...done!")))



  ;; logging weirdness
  (log/info "log something standard")
  (future (log/info "log something async"))
  (go (log/info "log something go block"))
  ;; only this last one prints to the repl (all four print to cider buffer)
  (let [ses (Executors/newScheduledThreadPool 1)]
    (.submit ses ^Runnable (fn [] (log/info "log something using Executor"))))


  )
