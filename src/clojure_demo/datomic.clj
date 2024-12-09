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
  [{:db/ident     :reminder/id
    :db/unique    :db.unique/identity
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one}
   {:db/ident     :reminder/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident     :reminder/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident     :reminder/until
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}])
