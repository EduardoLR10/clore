(ns clojure-demo.datomic
  (:require
    [datomic.api :as d]))

(defn connect
  {:init/inject [:datomic/uri :datomic/schema :datomic/fixtures]
   :init/tags #{:datomic/conn}
   :init/stop-fn #'d/release}
  [uri schema fixtures]
  (d/create-database uri)
  (let [conn (d/connect uri)]
    @(d/transact conn (concat schema fixtures))
    conn))

(def ^{:init/tags #{:datomic/schema}} schema
  ;; TODO: fill this in
  [])
