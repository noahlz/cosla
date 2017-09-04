(ns cosla.reports.t-time-in-status
  (:require [cosla.config :refer [*config*]]
            [cosla.reports.time-in-status :refer :all]
            [clj-time.local :as local]
            [midje.sweet :refer :all]))


(binding [*config* {:initial-status {:from "New" :to "Open"}
                    :open-statuses #{"New" "Open" "In Progress"}  
                    :closed-statuses #{"Fixed"}}]

  (fact "An issue Opened on 6/21, In Progress on 6/23 and Fixed on 6/29 has times in status of Open=2.0 and In Progress=6.0"
    (issues-with-days-in-each-status
      [{:issue "TEST-1"
        :current-status "Fixed"
        :created-on (local/to-local-date-time "2014-06-21") ;; Immediately New -> Open. Time spent in "New" is 0
        :status-changes [
          {:change-id 0 :change-date (local/to-local-date-time "2014-06-21") :from "New" :to "Open" }
          {:change-id 1 :change-date (local/to-local-date-time "2014-06-23") :from "Open" :to "In Progress" }
          {:change-id 2 :change-date (local/to-local-date-time "2014-06-29") :from "In Progress" :to "Fixed"}]}])
    => (contains [["TEST-1" "Open" 2.0] ["TEST-1" "In Progress" 6.0]] :in-any-order))

  (fact "Issues open for partials days round to two decimal places of precision (1 hour = 0.0417 days, 22 hours = 0.9167 days)."
    (issues-with-days-in-each-status
      [{:issue "TEST-3"
        :current-status "Fixed"
        :created-on (local/to-local-date-time "2014-06-21T00:00:00.000") 
        :status-changes [
          {:change-id 0 :change-date (local/to-local-date-time "2014-06-21T00:00:00.000") 
             :from "New" :to "Open" }
          {:change-id 1 :change-date (local/to-local-date-time "2014-06-21T01:00:00.000") 
             :from "Open" :to "In Progress" }
          {:change-id 2 :change-date (local/to-local-date-time "2014-06-21T23:00:00.000") 
             :from "In Progress" :to "Fixed"}]}])
    => (contains [["TEST-3" "Open" 0.0417] ["TEST-3" "In Progress" 0.9167]] :in-any-order))

  (fact "An issue with no staus changes has a duration of Open -> (Now)."
    (issues-with-days-in-each-status
      [{:issue "TEST-4"
        :current-status "Open"
        :created-on (local/to-local-date-time "2014-06-18") 
        :status-changes []}])
    => (just [(just ["TEST-4" "Open" pos?])]))
  
  (fact "Two issues with different histories generate different export rows."
    (issues-with-days-in-each-status
      [{:issue "TEST-5"
        :current-status "Fixed"
        :created-on (local/to-local-date-time "2014-06-21")
        :status-changes [
          {:change-id 0 :change-date (local/to-local-date-time "2014-06-21") :from "New" :to "Open"}
          {:change-id 1 :change-date (local/to-local-date-time "2014-06-23") :from "Open" :to "In Progress"}
          {:change-id 2 :change-date (local/to-local-date-time "2014-06-29") :from "In Progress" :to "Fixed"}]}
       {:issue "TEST-6"
        :current-status "Fixed"
        :created-on (local/to-local-date-time "2014-07-21")
        :status-changes [
          {:change-id 0 :change-date (local/to-local-date-time "2014-07-21") :from "New" :to "Open"}
          {:change-id 1 :change-date (local/to-local-date-time "2014-07-22") :from "Open" :to "In Progress"}
          {:change-id 2 :change-date (local/to-local-date-time "2014-07-25") :from "In Progress" :to "In Review"}
          {:change-id 3 :change-date (local/to-local-date-time "2014-07-29") :from "In Review" :to "Fixed"}]}])
    => (contains [["TEST-5" "New" 0.0] ["TEST-5" "Open" 2.0] ["TEST-5" "In Progress" 6.0]
                  ["TEST-6" "New" 0.0] ["TEST-6" "Open" 1.0] ["TEST-6" "In Progress" 3.0] ["TEST-6" "In Review" 4.0]] :in-any-order))

  (fact "An issue with one status change and currently in open status has a final duration that is the most recent status change-date -> (Now)"
    (issues-with-days-in-each-status
      [{:issue "TEST-7"
        :current-status "In Progress"
        :created-on (local/to-local-date-time "2014-07-01") 
        :status-changes [ 
          {:change-id 0 :change-date (local/to-local-date-time "2014-07-01") :from "New" :to "Open"}
          {:change-id 1 :change-date (local/to-local-date-time "2014-07-01") :from "Open" :to "In Progress"} ]}])
    => (contains [(just ["TEST-7" "New" 0.0]) (just ["TEST-7" "Open" 0.0]) (just ["TEST-7" "In Progress" pos?])] :in-any-order)))

