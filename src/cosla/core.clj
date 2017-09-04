(ns cosla.core
  (:require [cosla.config :refer :all]
            [cosla.time :refer :all]
            [clojure.tools.logging :refer :all]
            [clojure.edn :as edn])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Convert JIRA data to Clojure structures
;;
;; To understand the below, you'll need to inspect the
;; JSON-representation of JIRA issues.
;; https://docs.atlassian.com/jira/REST/latest/
;; The general idea is that we convert the JSON to Clojure data
;; structures and then strip it down to the fields we need for
;; computing SLA metrics: issues with their timestamped status transition history.

(defn status-change?  [item] (= "status" (:field item)))

(defn has-status-change-item? [{items :items}] (some status-change? items))

(defn extract-status-changes [issue]
  (filter has-status-change-item? (get-in issue [:changelog :histories])))

(defn status-change-record [{change-id :id change-date :created items :items}]
  (let [status-item (first (filter status-change? items))
        {from :fromString to :toString} status-item]
    {:change-id change-id
     :change-date (parse-time change-date)
     :from from
     :to to}))

(defn minimize-issue
  "Strip down a JIRA issue map (parsed from JSON) to a minimal set of fields that we need for reporting."
  [issue]
  (let [{issue-key :key} issue
        {fields :fields} issue
        {:keys [summary created]} fields]
    {:issue issue-key
     :summary summary
     :current-status (get-in fields [:status :name])
     :priority (get-in fields [:priority :name])
     :issue-type (get-in fields [:issuetype :name])
     :created-on (parse-time created)
     :status-changes (->> (extract-status-changes issue)
                          (mapv status-change-record))}))

(defn status-change-history [mini-issue]
  (let [{{:keys [from to]} :initial-status} *config*
        {status-changes :status-changes} mini-issue
        init-status {:change-date (:created-on mini-issue) :from from :to to}
        status-changes (into [init-status] status-changes)]
    (sort-by :change-date status-changes)))

(defn status-duration-history [change-history]
  (for [status-change (partition 2 1 change-history)
        :let [[{from-date :change-date} {from-status :from to-status :to to-date :change-date}] status-change]]
    {:from from-status :from-date from-date :to to-status :to-date to-date}))

(defn jira-issue-url
  ([issue-key] (jira-issue-url issue-key (:jira-url *config*)))
  ([issue-key jira-url] (format "%s/browse/%s" jira-url issue-key)))
