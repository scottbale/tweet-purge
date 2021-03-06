(ns backpressure
  "Twitter rate-limits some of its API endpoints and the number of tweet ids as input may exceed the
  rate limit.

  Implements backpressure thusly: input tweet ids are chunked into whatever chunk size the Twitter
  API rate limit requires. Each chunk is executed asynchronously using a ScheduledExecutorService.
  The first chunk is submitted right away, each subsequent chunk is scheduled to begin after the
  previous chunk ends and following a delay (period) required by the rate limit. A Promise is
  returned to the caller which is completed once all chunks complete."
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :as log])
  (:import
   [java.util.concurrent Executors TimeUnit ScheduledExecutorService]))

(defn submit
  "Execute `f` asynchronously using the provided ExecutorService"
  [f executor-service]
  (.submit executor-service f))

(defn schedule
  "Schedule `f` for execution `seconds` later using the provided ScheduledExecutorService"
  [f scheduled-executor-service seconds]
  (.schedule scheduled-executor-service f seconds TimeUnit/SECONDS))

(defn with-backpressure
  "aka 'Do per chunk': Asynchronously execute function `f` for each chunk of collection `coll`. First
  chunk is executed immediately, each subsequent one after `period` seconds. Return a Promise which
  will be completed when all chunks are completed. Uses provided ScheduledExecutorService. f should
  be a function of a single arg, which is a single item from `coll`. The time is measured from the
  end of the function invocation."
  [{:keys [executor period chunk]} f coll]
  (let [completion (promise)]
    (letfn [(do-next-chunk [xs chunk-nm]
              (log/debugf "starting %d chunk" chunk-nm)
              (let [some-xs (take chunk xs)
                    rest-xs (drop chunk xs)]
                (dorun (map f some-xs))
                (if (seq rest-xs)
                  (do
                    (log/tracef "next chunk %d: scheduling" chunk-nm)
                    (schedule #(do-next-chunk rest-xs (inc chunk-nm)) executor period))
                  (deliver completion true))))]
      (submit #(do-next-chunk coll 0) executor)
      completion)))

(defn backpressure
  "Creates a backpressure object to be used with `with-backpressure` function. `task-count` is the
  expected number of concurrent tasks requiring backpressure. `period` is the number of seconds
  pause in between chunks of work. `chunk` is the number of items in a chunk of work before pause.
  Implementation notes: returned object is a map. A scheduled thread pool Executor is created, which
  is used to schedule all future chunks after the first chunk. "
  [task-count period chunk]
  {:executor (Executors/newScheduledThreadPool (+ 2 task-count))
   :period period :chunk chunk})

(defn close
  "Close any resources of the `backpressure` obj. (Too bad `with-open` macro is limited to `.close`
  methods of pojos)."
  [{:keys [executor] :as bp}]
  (when executor
    (.shutdown executor)))

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

(defn id-try-catch-logging
  "Given the function `f` of one arg (a tweet id), invoke it within try-catch. On success, write the
  id to the `success-writer` Writer. If an exception is caught, log a warning and write the tweet id
  to the supplied `retry-writer` for later retrying."
  [success-writer retry-writer f id]
  (try
    (f id)
    (log-id success-writer id)
    (catch Exception e
      (log/warnf e "Caught %s, noting %s for later retry: %s" (.. e getClass getSimpleName) id (.getMessage e))
      (log-id retry-writer id))))

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
               (partial id-try-catch-logging done-w retry-w (partial maybe-print-id error-cnt))
               tweets)]
      (log/info ".....awaiting completion.....")
      (deref pr) ;; gotta block or else writer(s) get closed too soon
      (log/info "...done!")))



  ;; logging weirdness
  (log/info "log something standard")
  (future (log/info "log something async"))
  ;; (go (log/info "log something go block"))
  ;; only this last one prints to the repl (all four print to cider buffer)
  (let [ses (Executors/newScheduledThreadPool 1)]
    (.submit ses ^Runnable (fn [] (log/info "log something using Executor"))))


  )
