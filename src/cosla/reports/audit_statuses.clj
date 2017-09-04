(ns cosla.reports.audit-statuses
  (:require [cosla.config :refer :all]
            [cosla.core :refer :all]
            [cosla.fetch :refer :all]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.logging :refer :all])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Console-Only Report on Issue Statuses

(defn audit-statuses-and-transitions []
  (info "Finding all known status transitions...")
  (let [{:keys [search-jql open-statuses closed-statuses]} *config*
        all-transitions (fetch-all-known-transitions)
        all-statuses (-> all-transitions vec flatten distinct sort)
        diff (set/difference (set all-statuses) (set/union open-statuses closed-statuses))]
    (infof "All known status transitions\n%s" (str/join \newline (sort all-transitions)))
    (infof "All known statuses:\n%s" (str/join \newline all-statuses))
    (infof "Expected statuses\nOpen: %s\nClosed: %s" (sort open-statuses) (sort closed-statuses))
    (if (seq diff)
      (warnf "Statuses found in JIRA but not configured as Open or Closed:\n%s" (str/join \newline diff))
      (info "All known statuses are a known \"Open\" or \"Closed\" status."))))

