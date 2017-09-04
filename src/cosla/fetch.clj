(ns cosla.fetch
  (:require [cosla.config :refer :all]
            [cosla.core :refer :all]
            [clojure.string :as strings]
            [clojure.tools.logging :refer :all]
            [clojure.data.json :refer [read-json]]
            [clj-http.client :as http]
            [clj-http.cookies :refer :all])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Retreive Data from JIRA via REST API

;; The HTTP Session with JIRA. Bind the result of (jira-connect) to this var.
;; i.e.
;; (binding [*session* (jira-connect config)]
;;   (fetch-issue-keys))
(declare ^:dynamic *session*)

(defn jira-connect
  "Connect to JIRA and return a session or bust. Throws an exception if it fails. Use try-jira-connect for more user-friendly error handling."
  ([config]
    (binding [*config* config]
      (jira-connect)))
  ([]
    (let [{:keys [jira-url username config-password]} *config*
          password (if (strings/blank? config-password) (System/getenv "COSLA_PASSWORD") config-password)
          login-url (str jira-url "/rest/auth/1/session")
          session (cookie-store)
          response (http/post login-url {:content-type :json
                                         :cookie-store session
                                         :form-params {:username username :password password}})]
      (info "Connected to JIRA as" username)
      (debug "Response:" response)
      session)))

(defn try-jira-connect
  "Given a configuration map containing jira-url, username and password, connect to JIRA using basic authentication and return the cookie store for subsequent requests. One of the following will occur:
  - Success: returns the HTTP session cookie.
  - For HTTP errors: log an error message and return nil.
  - If not an HTTP error, re-throw the specific exception."
  [config]
  (try (jira-connect config)
    (catch Exception ex
      (let [{:keys [object]} (ex-data ex)]
        (if object
          (errorf "HTTP Error: %s %s" (:status object) (:body object))
          (throw ex))))))

(defn issue-keys
  "All this short function does is extract the issue keys from a list of JIRA search results.

We need to do this because JIRA does not include issue histories in search results, so you will need to iterate over the keys and pull each one down individually."
  [results] (vec (pmap :key results)))

(defn fetch-issue-keys "Return a list of issues given a JQL query. The no-arg version uses the search-jql, max-results and jira-url from your configuration."
  ([]
   (let [{:keys [jira-url search-jql max-results]} *config*]
     (fetch-issue-keys jira-url search-jql max-results)))
  ([jira-url search-jql max-results]
    (let [url (format "%s/rest/api/2/search?jql=%s&maxResults=%s" jira-url search-jql max-results)
          _ (info "Searching for issues:" url)
          start (System/currentTimeMillis) ;; todo: introduce timing macro
          {total :total maxResults :maxResults issues :issues} (-> (http/get url {:cookie-store *session*}) :body read-json)
          duration (-> (System/currentTimeMillis) (- start) (/ 1000))
          _ (if (> total maxResults) (warnf "JQL clause \"%s\" returned %d issues (max=%d) in %d seconds" search-jql total maxResults (long duration)))]
      (issue-keys issues))))

(defn fetch-issue
  ([issue-key] (fetch-issue issue-key (:jira-url *config*)))
  ([issue-key jira-url]
    (let [issue-url (format "%s/rest/api/2/issue/%s?expand=changelog" jira-url issue-key)
          _ (info "Fetching issue:" issue-key)
          {body :body} (http/get issue-url {:cookie-store *session*})]
      (read-json body))))

(defn fetch-minimized-issues []
  "Fetch issues from JIRA from the configured jql query, minimized via the function minimize-issue."
  (let [{:keys [jira-url search-jql max-results]} *config*
        issue-keys (fetch-issue-keys)
        _ (infof "Found %d issue keys..." (count issue-keys))
        start (System/currentTimeMillis) ;; todo: introduce timing macro
        issues-with-changes (vec (pmap #(minimize-issue (fetch-issue %)) issue-keys))
        duration (-> (System/currentTimeMillis) (- start) (/ 1000))]
        _ (infof "Fetched %d issues in %d seconds." (count issues-with-changes) (long duration))
    issues-with-changes))

(defn fetch-all-known-statuses []
  (->> (fetch-minimized-issues)
       (map :status-changes)
       flatten
       (map (fn [{:keys [from to]}] [from to]))
       flatten
       set))

(defn fetch-all-known-transitions []
  (->> (fetch-minimized-issues)
       (mapv status-change-history)
       flatten
       (mapv (fn [{:keys [from to]}] [from to]))
       sort
       set))
