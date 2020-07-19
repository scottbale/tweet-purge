(ns backpressure
  (:require
   [clojure.core.async :refer [chan go <! >!! sliding-buffer poll!]]
   [clojure.tools.logging :as log])
  (:import
   [java.util Timer TimerTask]))

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

(defn print-ids [ids]
  (doseq [id ids]
    (println ">>>>>>>>" id)))


(comment

  (def timer-task (periodically foo 3000))

  (let [ids [:foo1 :bar1 :baz1 :foo2 :bar2 :baz2 :foo3 :bar3 :baz3 :foo4]
        period 3000
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
