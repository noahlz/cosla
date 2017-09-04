(ns cosla.main
  (:require [cosla.config :refer :all]
            [cosla.core :refer :all]
            [cosla.fetch :refer :all]
            [cosla.reports.audit-statuses :refer [audit-statuses-and-transitions]]
            [cosla.reports.open-per-day :refer [issues-open-per-day-report]]
            [cosla.reports.time-to-close :refer [issue-time-to-close-report]]
            [cosla.reports.time-in-status :refer [issue-time-in-each-status-report]]
            [clojure.tools.logging :refer :all]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]
            [clojure.edn :as edn])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Command Line App

(defn usage [options-summary]
  (str/join \newline
    ["Get SLA metrics from your JIRA."
     ""
     "Usage:"
     "From Leiningen: lein run -m cosla.main [REPORT]"
     "From Java: java -jar <compiled.jar> [--config CONFIG.EDN | --help] [REPORT]"
     ""
     "Options:"
     options-summary
     ""
     "Reports:"
     "- time-to-close"
     "- open-per-day"
     "- time-in-status"
     "- audit-statuses (console only)"
     ""
     "Getting Started:"
     "Copy example.config.edn to config.edn and edit it with your username/password and desired JQL query"]))

(defn exit
  ([status] (System/exit status))
  ([status msg]
    (println msg)
    (System/exit status)))

(defn error-msg [errors]
  (str "Errors:\n"
    (str/join \newline errors)))

(def cli-options
  [[nil "--config <config.edn>" nil :default "config.edn"]
   ["-h" "--help"]])

(defn select-report [name]
  (case name
    "time-to-close"  issue-time-to-close-report
    "open-per-day"   issues-open-per-day-report
    "time-in-status" issue-time-in-each-status-report
    "audit-statuses" audit-statuses-and-transitions))

(defn -main [& args]
  (let [{:keys [options errors arguments summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary)) (not= 1 (count arguments)) (exit 1 (usage summary))
      errors          (exit 1 (error-msg errors)))
    (let [config (load-config (:config options))
          session (if config (try-jira-connect config))]
      (if-not (and config session) (exit 1))
      (binding [*config* config
                *session* session]
        (let [report-name (first arguments)
              reportfn (select-report report-name)
              start (System/currentTimeMillis)
              _ (reportfn)
              duration (-> (System/currentTimeMillis) (- start) (/ 1000))] ;; todo: timing macro
          (infof "Finished report in %d seconds." (int duration)))
        (shutdown-agents)))))
