(ns cosla.reports.time-to-close
  (:require [cosla.config :refer :all]
            [cosla.core :refer :all]
            [cosla.csv :refer :all]
            [cosla.fetch :refer :all]
            [cosla.time :refer :all]
            [clojure.tools.logging :refer :all]
            [clj-time.core :as time])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Issue Duration-Open Report

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Filtering only times a ticket was "Open"
;;
;; Sometimes we only want to include periods of time during
;; which the ticket was open.
;;
;; Status Transitions where a ticket is "Open"
;; 1) Open -> Open
;; 2) Open -> Closed
;;
;; Status Transitions where a ticket is "Closed"
;; 1) Closed -> Closed
;; 2) Closed -> Open

(defn started-in-open-status? [open-statuses {from :from}]
  (open-statuses from))

(defn only-open-durations [open-statuses duration-history]
  (filter #(started-in-open-status? open-statuses %) duration-history))

(defn closed? [{:keys [current-status]} closed-statuses]
  (closed-statuses current-status))

(defn sum-status-change-interval-in-days [status-change-history]
  (->> status-change-history
       status-duration-history
       (only-open-durations (:open-statuses *config*))
       (map calc-interval-seconds)
       (apply +)
       seconds-to-days))

(defn issues-with-time-to-close [mini-issues]
  (let [{:keys [open-statuses closed-statuses]} *config*]
    (for [i mini-issues
          :when (do (infof "%s current status is %s" (:issue i) (:current-status i))
                    (closed? i closed-statuses))
          :let [history (status-change-history i)
                days-open (sum-status-change-interval-in-days history)
                resolved-on (:change-date (last history))
                {:keys [issue issue-type summary priority current-status created-on]} i]]
        {:issue issue
         :issue-type issue-type
         :summary summary
         :priority priority
         :resolve-status (:to (last history))
         :current-status current-status
         :created-on (to-date-str created-on)
         :resolved-on (to-date-str resolved-on)
         :resolved-on-week-of-year (week-of-year resolved-on)
         :days-open days-open})))

(defn duration-open-report-record-row
  ([rec] (duration-open-report-record-row (:jira-url *config*) rec))
  ;; use map with a vector of keys to get map values in a specific order. http://stackoverflow.com/a/7184638/7507
  ([jira-url rec]
    (let [fields-to-extract [:issue :issue-type :days-open :resolved-on :resolved-on-week-of-year :created-on :resolve-status :current-status :priority :summary]
          field-values      (mapv rec fields-to-extract)
          url               (jira-issue-url (:issue rec) jira-url)]
      (conj field-values url))))

(defonce duration-open-report-headers
  ["Issue" "IssueType" "DaysToResolve" "ResolvedOn" "ResolvedOnWeekOfYear" "CreatedOn" "ResolutionStatus" "CurrentStatus" "Priority" "Summary" "URL"])

(defn issue-time-to-close-report []
  (let [_ (info "Running time-to-close report...")
        issue-to-record (partial duration-open-report-record-row (:jira-url *config*))
        csv-ready-data  (->> (fetch-minimized-issues)
                             issues-with-time-to-close
                             (mapv issue-to-record))]
    (write-report-csv "time-to-close" duration-open-report-headers csv-ready-data)))

