(ns clojure-demo.http
  (:require
    [clj-simple-router.core :as router]
    [datomic.api :as d]
    [org.httpkit.server :as httpkit]
    [rum.core :as rum]))

(defn stop! [server]
  (some-> server httpkit/server-stop! deref))

(def ^{:init/tags #{::httpkit/config}} config
  {:port 8080
   :legacy-return-value? false})

(defn server
  "The actual HTTP server.

  Note that HTTP Kit doesn't actually support HTTPS. You'll want to set up
  some reverse proxy for that, or perhaps switch to a different HTTP server."
  {:init/tags #{:http/server}
   :init/inject [:ring/handler ::httpkit/config]
   :init/stop-fn #'stop!}
  [handler config]
  (httpkit/run-server handler config))

(defn middleware
  "Ring middleware to be applied to the handler.
  Here is the appropriate place to address cross-cutting concerns, such as
  authentication, making the database available to the handler, etc."
  {:init/tags #{:ring/middleware}
   :init/inject [:partial :datomic/conn]}
  [conn base-handler]
  ;; TODO: static pages, authentication maybe
  (-> base-handler
    ((fn [handler]
       (fn [req]
         (handler (assoc req :datomic {:conn conn
                                       :db (d/db conn)})))))))

(defn handler
  "Constructs the actual handler for the requests.
  Uses the routing information and any additional middleware."
  {:init/tags #{:ring/handler}
   :init/inject [::routes :ring/middleware]}
  [routes middleware]
  (-> routes
    router/router
    middleware))

(def ^:init/name routes
  "Here is where we define the route-specific handlers.
  Since we're using clj-simple-router, this is just a map."
  ;; TODO: everything
  {"* /**"
   (fn [{[method path] :path-params}]
     {:status 200
      :body
      (rum/render-static-markup
        (list
          [:h1 "Demo"]
          [:p "Hello, Clojure!"]
          [:p "You requested: " method " /" path]))})})
