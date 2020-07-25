(ns backpressure
  (:require
   [clojure.core.async :refer [chan go <! >!! sliding-buffer poll!]]
   [clojure.tools.logging :as log])
  (:import
   [java.util Timer TimerTask]
   [java.util.concurrent Executors TimeUnit ScheduledExecutorService]))

;; java.util.Timer

(defn timer-task [f]
  (proxy [TimerTask] [] (run [] (f))))

(defn do-after [timer-task timer millis]
  (.schedule timer timer-task millis))

(defn periodically
  ([f millis] (periodically (Timer.) f millis))
  ([timer f millis]
   (let [tt (proxy [TimerTask] [] (run [] (f)))]
     (.scheduleAtFixedRate timer tt 0 millis)
     #(.cancel tt))
   ))

;; java.util.concurrent.ScheduledExecutorService

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
              (let [some-xs (take chunk xs)
                    rest-xs (drop chunk xs)]
                (if (seq some-xs)
                  (schedule (fn [] 
                              (f some-xs)
                              (do-next-chunk rest-xs))
                            ses period)
                  (deliver completion true))))]
      (do-next-chunk coll)
      completion)))

;; misc

(defn print-ids [ids]
  (doseq [id ids]
    (println ">>>>>>>>" id)))


(comment

  (let [ids [:foo1 :bar1 :baz1 :foo2 :bar2 :baz2 :foo3 :bar3 :baz3 :foo4]
        period 3 ;; sec
        grouping 3
        ses (Executors/newScheduledThreadPool 1)
        completion (do-per-chunk print-ids ids ses period grouping)]
      (println ".....awaiting completion.....")
      (deref completion)
      (println ".....done!"))

  (def timer-task (periodically foo 3000))

  (let [ids [:foo1 :bar1 :baz1 :foo2 :bar2 :baz2 :foo3 :bar3 :baz3 :foo4]
        period 3000 ;; milliseconds
        grouping 3
        timer (java.util.Timer.)
        completion (promise)]
    (letfn [(do-next-chunk [ids]
              (let [some-ids (take grouping ids)
                    rest-ids (drop grouping ids)]
                ;; (println ">>>>>do next chunk" some-ids)
                (if (seq some-ids)
                  (let [tt (timer-task (fn []
                                         ;; (println ">>>>>a timer task fn")
                                         (print-ids some-ids)
                                         (do-next-chunk rest-ids)))]
                    (do-after tt timer period))
                  (deliver completion true))))]
      (do-next-chunk ids)
      (println ".....awaiting completion.....")
      (deref completion)
      (println ".....done!")))

  )
