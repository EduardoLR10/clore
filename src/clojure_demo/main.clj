(ns clojure-demo.main
  (:gen-class)
  (:require
    [clojure-demo.core]
    [init.core :as init]
    [init.discovery :as discovery]))

(def bogus-uri
  "URI used by Datomic. In a proper project, you'd set this up on a separate
  process and using durable storage."
  "datomic:mem://clojure-demo")

(def no-fixtures [])

(defn -main
  [& _]
  (-> (discovery/static-scan '[clojure-demo])
    (discovery/bind {:datomic/uri #'bogus-uri
                     :datomic/fixtures #'no-fixtures})
    init/start
    init/stop-on-shutdown))
