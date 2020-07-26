(ns backpressure
  (:require
   [clojure.core.async :refer [chan go go-loop <! >!]]
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
  [f coll ^ScheduledExecutorService ses period chunk]
  (let [completion (promise)]
    (letfn [(do-next-chunk [xs]
              (log/debug "next-chunk: xs" xs)
              (let [some-xs (take chunk xs)
                    rest-xs (drop chunk xs)]
                (log/debug "next-chunk: scheduling" some-xs)
                (schedule (fn []
                            (log/debug "next-chunk: fn" some-xs)
                            (f some-xs)
                            (if (seq rest-xs)
                              (do-next-chunk rest-xs)
                              (deliver completion true)))
                          ses period)))]
      (do-next-chunk coll)
      completion)))

(defn put-in [chan x]
  (go (>! chan x)))

(defn looping-invoke [chan f]
  (go-loop [x (<! chan)]
    (f x)
    (recur (<! chan))))

(defn with-backpressure
  "Asynchronously execute f, returning a Promise."
  [f coll chunk-size delay-seconds])

;; sample function(s)

(defn print-id [id]
  (log/infof "About to print id %s..." id)
  (println ">>>>>>>>" id)
  id)

(defn log-id [writer id]
  (binding [*out* writer]
    ;; (.write writer id)
    (println id)
    id))

(defn print-ids [ids]
  (doseq [id ids]
    (println ">>>>>>>>" id)))

(defn print-and-log-ids [writer ids]
  (let [f (comp (partial log-id writer) print-id)]
    (doall (map f ids))))


(comment

  (with-open [writer (io/writer "completed.log" :append true)]
    (let [ids (take 10 (map #(str "foo" %) (range)))
          period 3 ;; sec
          chunk 3
          chan (chan chunk)
          ses (Executors/newScheduledThreadPool 1)
          completion (do-per-chunk (partial put-in chan) ids ses period chunk)]
      (looping-invoke chan (partial print-and-log-ids writer))
      (println ".....awaiting completion.....")
      (deref completion) ;; gotta block or else stream gets closed too soon
      (println "...done!")))

  (let [ids [:foo1 :bar1 :baz1 :foo2 :bar2 :baz2 :foo3 :bar3 :baz3 :foo4]
        period 3 ;; sec
        chunk 3
        chan (chan chunk)
        ses (Executors/newScheduledThreadPool 1)
        ]
    (looping-invoke chan print-ids)
    (do-per-chunk (partial put-in chan) ids ses period chunk))

  (let [ids [:foo1 :bar1 :baz1 :foo2 :bar2 :baz2 :foo3 :bar3 :baz3 :foo4]
        period 3 ;; sec
        grouping 3
        ses (Executors/newScheduledThreadPool 1)
        completion (do-per-chunk print-ids ids ses period grouping)]
    (println ".....awaiting completion.....")
    (deref completion)
    (println ".....done!"))

  )
