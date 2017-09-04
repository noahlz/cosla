(ns cosla.time
  (:require [cosla.config :refer :all]
            [clojure.tools.logging :refer :all]
            [clj-time.core :as time]
            [clj-time.local :as local]
            [clj-time.predicates :as predicate]
            [clj-time.periodic :as periodic]
            [clj-time.format :as time-format])
  (:gen-class))

(def long-date-format (time-format/formatters :date-time))

(defn parse-time [timestr]
  (time-format/parse long-date-format timestr))

;; TODO: local date w/timezone
(def basic-date-format (time-format/formatters :year-month-day))

(defn to-date-str [d] (time-format/unparse basic-date-format d))

(defn week-of-year [d] (time/week-number-of-year d))

(defn calc-interval-seconds
  "Accepts either a pair of dates, or a map with the keys :from-date and :to-date, and computes
   the time-interval between the dates in seconds."
  ([{:keys [from-date to-date]}] (calc-interval-seconds from-date to-date))
  ([from-date to-date] (time/in-seconds (time/interval from-date to-date))))

(defn seconds-to-days [s]
  (-> (time/seconds s)
      (.. toStandardHours getHours)
      (/ 24)
      double
      bigdec
      (.setScale 4 java.math.RoundingMode/HALF_UP)
      double))

(defn business-days
  "Get all the business days in a given start date and end date (default today), less holidays.
   Reads from config.edn by default."
  ([start-date end-date holidays]
    (for [day (periodic/periodic-seq start-date (time/days 1))
          :when (and (predicate/weekday? day) (not (holidays day)))
          :while (time/before? day end-date)]
      day))
  ([]
   (let [{{:keys [start-date end-date holidays]} :open-per-day} *config*
         start-date (if start-date
                      (local/to-local-date-time start-date)
                      (throw (Exception. "configuration or parameter \"start-date\" is required.")))
         end-date (if end-date
                    end-date
                    (local/local-now))
         holidays (set (map local/to-local-date-time holidays))]
     (business-days start-date end-date holidays))))

(defn close-of-business
  "Adds 18 hours to a given date (assumed to have hour 00:00), returning the instant at 6 PM local time."
  [date] (time/plus date (time/hours 18)))

(defn business-days-at-cob-time []
  (map close-of-business (business-days)))

