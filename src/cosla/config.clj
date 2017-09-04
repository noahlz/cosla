(ns cosla.config
   (:require [clojure.tools.logging :refer :all]
             [clojure.edn :as edn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The application configuration as a thread-local global
;; so we don't have to pass it as a parameter all over the place.
;; Just be sure to bind it with a value before calling a function
;; that requires it. I.e.
;;
;;   (binding [*config* (end/read-string (slurp "config.edn"))]
;;     (jira-connect))
;;
(declare ^:dynamic *config*)

(def default-config-location "config.edn")
(def instructions "Instructions: Rename example.config.edn to config.edn and edit it to add your credentials / jql query, etc. Run with --help for more information.")

(defn load-config
  "Load the configuration from an edn file, returns the config map or nil.  Default config location: ./config.edn"
  ([] (load-config default-config-location))
  ([config-file-location]
    (try
      (let [config (edn/read-string (slurp config-file-location))]
        (info "Loaded configuration from" config-file-location)
        config)
    (catch java.io.FileNotFoundException ex
      (errorf "%s\n%s" (.getMessage ex) instructions)))))
