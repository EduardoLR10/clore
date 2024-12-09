(ns clojure-demo.datomic
  (:require
   [datomic.api :as d])
  (:import (java.time LocalDateTime ZoneId ZoneOffset))
  (:import (java.text SimpleDateFormat)))

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

(defn ^:private string->date
  [str-date]
  (prn str-date)
  (-> (SimpleDateFormat/new "yyyy-MM-dd") (.parse str-date)))

(defn add-reminder
  [conn {:keys [title until]} current-date]
  @(d/transact conn
               [{:reminder/id (d/squuid)
                 :reminder/title title
                 :reminder/created-at current-date
                 :reminder/until (string->date until)}]))

(defn delete-reminder
  [conn id]
  @(d/transact conn [[:db/retractEntity [:reminder/id id]]]))

(defn add-days [date days]
  (-> date
      .toInstant
      (LocalDateTime/ofInstant (ZoneId/of "UTC"))
      (.plusDays days)
      (.toInstant ZoneOffset/UTC)
      java.util.Date/from))

(def short-term-query
  '[:find [(pull ?reminder [:reminder/id :reminder/title :reminder/created-at :reminder/until]) ...]
    :in $ ?current-date
    :where
    [?reminder :reminder/until ?until]
    [(clojure-demo.datomic/add-days ?current-date 5) ?final-date]
    [(< ?until ?final-date)]])

(def long-term-query
  '[:find [(pull ?reminder [:reminder/id :reminder/title :reminder/created-at :reminder/until]) ...]
    :in $ ?current-date
    :where
    [?reminder :reminder/until ?until]
    [(clojure-demo.datomic/add-days ?current-date 5) ?final-date]
    [(>= ?until ?final-date)]])

(def term-query
  '[:find [(pull ?reminder [:reminder/id :reminder/title :reminder/created-at :reminder/until]) ...]
     :where [?reminder :reminder/id]])
