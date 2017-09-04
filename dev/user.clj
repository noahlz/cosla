(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
    [clojure.java.io :as io]
    [clojure.java.javadoc :refer (javadoc)]
    [clojure.pprint :refer (pprint)]
    [clojure.reflect :refer (reflect)]
    [clojure.repl :refer (apropos dir doc find-doc pst source)]
    [clojure.set :as set]
    [clojure.stacktrace :refer [print-stack-trace]]
    [clojure.string :as str]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :refer (refresh refresh-all)]

    ;; Project-specific imports:
    [cosla.config :refer :all]
    [cosla.core :refer :all]
    [cosla.csv :refer :all]
    [cosla.fetch :refer :all]
    [cosla.time :refer :all]
    [cosla.reports.audit-statuses :refer :all] 
    [cosla.reports.open-per-day :refer :all] 
    [cosla.reports.time-to-close :refer :all] 
    [cosla.reports.time-in-status :refer :all]
    [clojure.edn :only [read-string] :as edn]
    [clj-time.core :as time]
    [clj-time.local :as local]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; My ad-hoc testing functions 

(def ^:dynamic *test-config* (delay (load-config)))
(def ^:dynamic *test-session* (delay (try-jira-connect (force *test-config*)))) 

(defmacro testfn [fname]
  (let [sym (->> fname name (str "test-") symbol)]
    `(defn ~sym [& args#]
       (binding [*config* (force *test-config*) 
                 *session* (force *test-session*)]
         (apply ~fname args#)))))

;; Test the below functions using the form (test-<fn>), 
;; i.e. (test-fetch-issue "SUPPORT-1821")
(testfn fetch-issue)
(testfn fetch-issue-keys)
(testfn fetch-minimized-issues)
(testfn fetch-all-known-statuses)
(testfn fetch-all-known-transitions)
(testfn status-change-history)
(testfn issue-time-to-close-report)
(testfn issues-with-days-in-each-status)

(defn fetch-minimized-issue [issue-key]
  (minimize-issue (fetch-issue issue-key)))

(defn time-to-close [issue-key]
  (->> (fetch-issue issue-key) 
       minimize-issue 
       status-change-history 
       sum-status-change-interval-in-days))

(testfn fetch-minimized-issue)
(testfn time-to-close)

(defn adhoc-issues-open-per-day [days-back]
  (let [start (time/minus (local/local-now) (time/days days-back))
        issues (fetch-minimized-issues)
        {open-statuses :open-statuses } *config*
        {{holidays :holidays} :open-per-day} *config*
        holidays (set (map local/to-local-date-time holidays))
        dates (business-days start (local/local-now) holidays)]
    (issues-open-per-day open-statuses dates issues)))

(testfn business-days)
(testfn business-days-at-cob-time)
(testfn adhoc-issues-open-per-day)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stuart Sierra boilerplate

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  ;;no-op 
  )

(defn start
  "Starts the system running, updates the Var #'system."
  []
  ;; no-op 
  )

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  ;; no-op
  )

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))
