(ns user
  (:require
    [clj-reload.core :as reload]
    [clore.core]
    [clojure.java.io :as io]
    [clojure.repl :refer :all]
    [datomic.api :as d]
    [init.core :as init]
    [init.discovery :as discovery]
    [juxt.dirwatch :as dirwatch]))

(defonce system (agent nil))

(defn see-error
  "Shows the error in system, if there is one.

  Use this function if something goes wrong in a reload to see
  what is going on."
  []
  (agent-error system))

(defn clear-error
  "Clears the error in the system, if there is one."
  []
  (restart-agent system nil))

(defn stop
  "Stops the system. Idempotent."
  []
  (send system
    #(some-> % init/stop))
  :stop)

(defn go
  "Starts the system. Idempotent."
  []
  (send system
    #(or %
       (-> (discovery/scan '[clore])
         (discovery/from-namespaces [(the-ns (ns-name *ns*))])
         init/start)))
  :go)

(def before-ns-unload #'stop)
(def after-ns-reload #'go)

(defn reset
  "Reloads any changed namespaces.
  Enters the new version of the current namespace, if changed.
  Starts the system if it's not running.
  Use it from the REPL to ensure your namespace is the latest version."
  []
  (reload/reload)
  (in-ns (ns-name *ns*))
  (go)
  :reset)

(defn hard-reset
  "Same as reset, except it always restarts the system and additionally
  wipes the database."
  []
  (some-> @system ::delete-datomic ((fn [f] (f))))
  (reset)
  :hard-reset)

(def ^{:init/tags #{:datomic/uri}} datomic-uri
  "URI used by Datomic. For development, we use an in-memory instance."
  "datomic:mem://clore")

(def ^{:init/tags #{:datomic/fixtures}} fixtures
  "Fixtures to insert initial data into the DB."
  ;; TODO: either fill this in or remove this component
  [])

(def ^{:init/tags #{:clj-reload/config}} reload-config
  "Configuration for clj-reload.

  The :mode option is non-standard, and is used by the auto-reload component.
  You may want to change it to something other than :auto if you prefer to
  manually reload your code changes."
  {:dirs ["src" "dev"]
   :mode :auto})

(defn clj-reload
  {:init/inject [:clj-reload/config]}
  [config]
  (reload/init config)
  :ok)

(defn auto-reload
  "Automatically reload code when editing.
  Does not update the REPL namespace, use reset for that."
  {:init/stop-fn #'dirwatch/close-watcher
   :init/inject [:clj-reload/config]}
  [{:keys [dirs mode]}]
  (apply dirwatch/watch-dir
    (fn [_] (reload/reload))
    (when (= mode :auto) (map io/file dirs))))

(defn delete-datomic
 "Takes a Datomic URI, stops the system and deletes the database.
  Don't call this yourself, use hard-reset instead."
 {:init/inject [:partial :datomic/uri]}
 [uri]
 (stop)
 (await system)
 (d/delete-database uri))
