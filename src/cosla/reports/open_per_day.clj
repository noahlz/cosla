(ns cosla.reports.open-per-day
  (:require [cosla.config :refer :all]
            [cosla.core :refer :all]
            [cosla.csv :refer :all]
            [cosla.fetch :refer :all]
            [cosla.time :refer :all]
            [clojure.tools.logging :refer :all]
            [clj-time.core :as time]
            [clj-time.local :as local])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Issues Open Per-Day Report

(defn open-on-date? [open-statuses instant {:keys [from from-date to-date]}]
  (and (open-statuses from)
       (time/within? (time/interval from-date to-date) instant)))

(defn open-on-some-date [open-statuses date status-duration-history]
  (first (filter #(open-on-date? open-statuses date %) status-duration-history)))

(defn append-latest-status [status-change-history]
  (let [status-change-vec (vec status-change-history)
        last-entry (last status-change-vec)
        last-to-status (:to last-entry)
        current-status {:change-date (local/local-now) :from last-to-status :to nil}]
      (conj status-change-vec current-status)))

(defn issues-open-per-day [open-statuses report-dates minimized-issues]
  (->> (for [date report-dates
             issue minimized-issues
             :let [history (-> issue
                               status-change-history
                               append-latest-status
                               status-duration-history)
                   n (if (open-on-some-date open-statuses date history) 1 0)
                   _ (debugf "%s open on %s? %s" (:issue issue) date (pos? n))]]
         {date n})
      (apply merge-with +)
      sort
      (mapv (fn [[date n]] [(to-date-str date) (week-of-year date) n]))))

(defn warn-for-future-dates [report-dates]
  (let [now (time/now)]
  (doseq [d report-dates :when (time/after? d  now)]
    (warnf "Report date %s is in the mysterious future and will appear to have \"0\" open tickets." d))))

(defn issues-open-per-day-report []
  (let [_ (info "Running issues open-per-day report...")
        report-dates (business-days-at-cob-time)
        _ (warn-for-future-dates report-dates)
        {:keys [open-statuses]} *config*
        issues (fetch-minimized-issues)
        csv-ready-data (issues-open-per-day open-statuses report-dates issues)]
    (write-report-csv "open-per-day" ["Date" "WeekOfYear" "OpenIssueCount"] csv-ready-data)))

