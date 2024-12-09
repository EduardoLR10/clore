(ns clore.main
  (:gen-class)
  (:require
    [clore.core]
    [init.core :as init]
    [init.discovery :as discovery]))

(def bogus-uri
  "URI used by Datomic. In a proper project, you'd set this up on a separate
  process and using durable storage."
  "datomic:mem://clore")

(def no-fixtures [])

(defn -main
  [& _]
  (-> (discovery/static-scan '[clore])
    (discovery/bind {:datomic/uri #'bogus-uri
                     :datomic/fixtures #'no-fixtures})
    init/start
    init/stop-on-shutdown))
