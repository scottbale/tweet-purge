(ns debugger
  (:use
   [clojure.pprint :only (pprint)]))

;; I seem to eventually copy this into every Clojure project I work on
;; http://learnclojure.blogspot.com/2010/09/clojure-macro-tutorial-part-i-getting.html
(defmacro dbg
  "Wrap any expression in a call to dbg and the expression will be
  printed along with the namespace it is found in. The expresssion is
  passed through so this can be added without changing the evaluation
  of the expression."
  [x]
  (let [ns# *ns*]
    `(let [x# ~x]
       (println (str ~ns# ":" ~(:line (meta &form))) " - " '~x "=")
       (pprint x#)
       (if (meta x#)
         (do (println "with meta")
             (pprint (meta x#))))
       (flush)
       x#)))

