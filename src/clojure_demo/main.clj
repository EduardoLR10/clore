(ns clojure-demo.main
  (:gen-class)
  (:require
    [init.core :as init]
    [init.discovery :as discovery]
    [clojure-demo.core]))

(def ^{:init/tags #{:datomic/uri}} bogus-uri
  "URI used by Datomic. In a proper project, you'd set this up on a separate
  process and using durable storage."
  "datomic:mem://clojure-demo")

(def ^{:init/tags #{:datomic/fixtures}} no-fixtures [])

(defn -main
  [& _]
  (-> (discovery/static-scan '[clojure-demo])
    init/start
    init/stop-on-shutdown))
