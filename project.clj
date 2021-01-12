(defproject twitter-purge "0.0.1"
  :description "Delete most of my tweets"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v20.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/tools.logging "1.1.0"]
                 [cheshire "5.10.0"]
                 [clj-http "3.10.3" :exclusions [commons-codec]]
                 [commons-codec "1.14"]
                 [org.apache.logging.log4j/log4j-api "2.13.3"]
                 [org.apache.logging.log4j/log4j-core "2.13.3"]]
  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/log4j2-factory"]
  :main cli
  :aot [backpressure oauth purge cli]
)
