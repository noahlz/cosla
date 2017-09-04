(ns cosla.csv
  (:require [cosla.config :refer :all]
            [cosla.core :refer :all]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer :all])
  (:gen-class))

(defn write-report-csv [name-prefix headers data]
  (let [{:keys [output-file-suffix]} *config*
        output-file (str name-prefix output-file-suffix)]
      (infof "Writing to %s..." output-file)
      (with-open [f (io/writer output-file)]
        (csv/write-csv f [headers])
        (csv/write-csv f data))))

