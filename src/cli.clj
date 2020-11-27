(ns cli
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [clojure.tools.cli :refer [parse-opts]]
   [purge :as p])
  (:gen-class))

(def action-map {"delete!" p/delete-all-tweets!
                 "unfavorite!" p/unfavorite-all-tweets!
                 "get" p/get-all-tweets
                 "echo" p/echo-tweet-ids})

(def cli-options
  [["-e" "--env env.edn" "environment file containing a map with the following keys and values (required unless otherwise indicated): \n\t:api-key, \n\t:api-secret, \n\t:access-token, \n\t:token-secret, \n\t:tweets-file - filename of newline-delimited tweets to be deleted \n\t:success-file (optional) filename to log successfully-deleted tweets to, defaults to `tweets.log` \n\t:retry-file (optional) filename to log tweet ids that need a retry, defaults to `retry.log`"
    :default "env.edn"]
   ["-h" "--help"]])

(defn usage [summary]
  (->> ["Delete tweets"
        ""
        "Usage: tweet-purge [options] action"
        "where action is one of [delete!|unfavorite!|get|echo]"
        "\tdelete! - delete each tweet"
        "\tunfavorite! - unfavorite (unlike) each tweet"
        "\tget - retrieve each tweet"
        "\techo - just print the tweet id, do not contact twitter"
        ""
        "Options:"
        summary
        ""
        "Example: tweet-purge --env myenv.edn delete!"
        ""]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn env [{:keys [env]}]
  (or env "env.edn"))

(defn validate-args [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}

      errors
      {:exit-message (error-msg errors) :ok? false}

      (first arguments)
      (let [action (first arguments)
            env-edn (env options)]
        (if-let [action-fn (action-map action)]
          (if-let [env (p/load-env env-edn)]
            {:action action-fn :env env :action-name action}
            {:exit-message (error-msg ["Bad env.edn file" env-edn]) :ok? false})
          {:exit-message (error-msg ["Unknown action" action]) :ok? false}))

      :else
      {:exit-message (usage summary)})))

(defn exit-msg [status message]
  (println message)
  (log/log (if (zero? status) :info :error)
           message)
  status)

(defn exit-err [e]
  (let [message (format "Caught %s: %s" (.. e getClass getSimpleName) (.getMessage e))
        status 1]
    (println message)
    (log/error e message)
    status))

(defn engage
  "Performs an action. `action` is a function from the purge namespace (it is one of the values of
  `action-map` in this ns). It is a function of one parameter, which is the env map loaded from
  env.edn file."
  [action action-name {:keys [tweets-file] :as env}]
  (let [msg (format "Will perform action '%s' on tweets from file '%s'." action-name tweets-file)]
    (println msg)
    (log/info msg)
    (action env)))

(defn main [& args]
  (try
    (let [{:keys [action action-name env exit-message ok?] :as a} (validate-args args)]
      (if action
        (do
          (engage action action-name env)
          0)
        (exit-msg (if ok? 0 1) exit-message)))
    (catch Exception e
      (exit-err e))))

(defn exit [status]
  (System/exit status))

(defn -main [& args]
  (exit (apply main args)))
