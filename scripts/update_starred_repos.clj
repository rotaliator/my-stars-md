#!/usr/bin/env bb

(ns update-starred-repos
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def github-api-url "https://api.github.com")
(def output-file "README.md")

(defn get-token []
  (or (System/getenv "GITHUB_TOKEN")
      (println "Warning: GITHUB_TOKEN not set. API requests will be unauthenticated and rate-limited.")))

(def make-request
  (memoize
   (fn
     [url]
     (println "Fetching:" url)
     (let [token   (get-token)
           headers (cond-> {"Accept" "application/vnd.github.v3+json"}
                     token (assoc "Authorization" (str "token " token)))
           resp    (curl/get url {:headers headers})
           status  (:status resp)]
       (if (= 200 status)
         resp
         (do
           (println "Error fetching" url "status:" status)
           (println "Response:" (:body resp))))))))

(defn parse-json [s]
  (json/parse-string  s true))


(defn get-starred-repos [username]
  (let [url (str github-api-url "/users/" username "/starred?per_page=100")]
    (->> (iteration make-request
                    {:vf    #(-> % :body parse-json)
                     :kf    (fn [resp]
                              (let [link-header (get-in resp [:headers "link"])]
                                (when (str/includes? link-header "rel=\"next\"")
                                  (->> (str/split link-header #",")
                                       (map str/trim)
                                       (filter #(str/includes? % "rel=\"next\""))
                                       first
                                       (re-find #"<([^>]+)>")
                                       second))))
                     :initk url})
         (sequence cat))))


(defn generate-markdown [repos]
  (let [header       "# My Starred Repositories\n\n"
        table-header "| Repository | Description | Language | Stars |\n|------------|-------------|----------|-------|"

        rows (for [repo repos]
               (let [full-name        (:full_name repo)
                     html-url         (:html_url repo)
                     description      (or (:description repo) "")
                     language         (or (:language repo) "")
                     stargazers-count (:stargazers_count repo)]
                 (str "| [" full-name "](" html-url ") | "
                      (str/replace description "|" "\\|") " | "
                      language " | " stargazers-count " |")))]
    (str header
         (str/join "\n" (cons table-header rows))
         "\n\n*Updated: " (java.time.LocalDate/now) "*\n")))

(defn get-username-from-token []
  (let [token (get-token)]
    (when token
      (let [resp (curl/get (str github-api-url "/user")
                           {:headers {"Authorization" (str "token " token)
                                      "Accept" "application/vnd.github.v3+json"}})]
        (when (= 200 (:status resp))
          (-> resp :body parse-json :login))))))

(defn -main []
  (println "Starting update of starred repositories list...")
  (let [username (or (get-username-from-token)
                     (do
                       (println "Could not determine username from token, using repository owner")
                       (-> (curl/get (str github-api-url "/repos/" (System/getenv "GITHUB_REPOSITORY"))
                                     {:headers {"Accept" "application/vnd.github.v3+json"}})
                           :body
                           parse-json
                           :owner
                           :login)))
        _        (println "Fetching starred repos for user:" username)
        repos    (get-starred-repos username)]
    (if (seq repos)
      (do
        (println "Found" (count repos) "starred repositories")
        (let [markdown (generate-markdown repos)]
          (spit output-file markdown)
          (println "Generated" output-file)))
      (println "No starred repositories found or error occurred."))))

(-main)
