(ns cosla.reports.time-in-status
  (:require [cosla.config :refer [*config*]]
            [cosla.core :refer :all]
            [cosla.csv :refer :all]
            [cosla.fetch :refer :all]
            [cosla.time :refer :all]
            [clojure.tools.logging :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as time-format])
  (:gen-class))

(defn status-with-duration-in-seconds [{:keys [from from-date to-date]}]
  {from (calc-interval-seconds from-date to-date)})

(defn if-open-append-duration-to-now [mini-issue duration-history]
  (let [{:keys [closed-statuses]} *config*
        {:keys [current-status]} mini-issue]
    (if (closed-statuses current-status)
      duration-history
      (let [from-date (if (seq duration-history)
                        (:to-date (last duration-history))
                        (:created-on mini-issue))
            open-duration-rec {:from current-status :from-date from-date :to nil :to-date (time/now)}]
        (conj duration-history open-duration-rec)))))

(defn issue-status-durations-in-seconds [mini-issue]
  (->> mini-issue
       status-change-history
       status-duration-history
       (if-open-append-duration-to-now mini-issue)
       (map status-with-duration-in-seconds)
       (apply merge-with +)))

(defn days-in-status [{:keys [issue] :as mini-issue}]
  (let [summary (issue-status-durations-in-seconds mini-issue)
        in-days (into {} (for [[k v] summary] [k (seconds-to-days v)]))]
  {:issue issue :status-durations in-days}))
;; TODO: Incanter dataset could go here, i.e. just :issue and arbitrary statuses as keys.

(defn issues-with-days-in-each-status [mini-issues]
  (mapv vec
    (for [rec (map days-in-status mini-issues)
          e (:status-durations rec)]
      (cons (:issue rec) e))))

(defn issue-time-in-each-status-report []
  (let [_ (info "Running time in each status by issue report...")
        issues (fetch-minimized-issues)
        csv-ready-data (issues-with-days-in-each-status issues)]
    ;; TODO: would be nice to have a "CurrentStatus" field for auditing data.
    (write-report-csv "days-in-status" ["Issue" "Status" "DaysInStatus"] csv-ready-data)))

